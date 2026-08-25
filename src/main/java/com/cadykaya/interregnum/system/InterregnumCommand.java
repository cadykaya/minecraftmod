package com.cadykaya.interregnum.system;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.core.chapter.Milestone;
import com.cadykaya.interregnum.system.claim.Claims;
import com.cadykaya.interregnum.system.unraveling.Unraveling;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.arguments.EntityArgument;
import com.cadykaya.interregnum.core.dialogue.Resolution;
import com.cadykaya.interregnum.system.dialogue.Conversations;
import com.cadykaya.interregnum.system.dialogue.ConversationView;
import com.cadykaya.interregnum.system.dialogue.TheHaunt;

/**
 * `/interregnum` -- read and drive the world's progress.
 *
 * This exists for two reasons and both are real: an operator needs to be able to
 * see what chapter a server is in, and the deicide is otherwise unreachable in a
 * test. `status` is readable by anyone; anything that MOVES the world is level 2,
 * because advancing a chapter is irreversible and server-wide.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class InterregnumCommand {
    private InterregnumCommand() {}

    /** Claim or release every block between two corners. */
    private static int region(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
                              boolean claim) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        BlockPos a = BlockPosArgument.getLoadedBlockPos(ctx, "from");
        BlockPos b = BlockPosArgument.getLoadedBlockPos(ctx, "to");
        ServerLevel level = ctx.getSource().getLevel();
        int n = 0;
        for (BlockPos pos : BlockPos.betweenClosed(a, b)) {
            if (claim) {
                Claims.record(level, pos);
            } else {
                Claims.forget(level, pos);
            }
            n++;
        }
        final int total = n;
        ctx.getSource().sendSuccess(() -> Component.literal(
                (claim ? "claimed " : "released ") + total + " position(s)"), true);
        return total;
    }


    /**
     * Validate a hull against a destination law, and sail it if a pad was given.
     *
     * One method for both `check` and `sail` on purpose: a `check` that could pass
     * while `sail` refused (or worse, the reverse) would make the checklist a lie,
     * and the checklist is the entire teaching mechanism.
     */
    private static int ferryCheck(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
                                  BlockPos pad) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        BlockPos keel = BlockPosArgument.getLoadedBlockPos(ctx, "keel");
        String lawId = StringArgumentType.getString(ctx, "law");
        var law = com.cadykaya.interregnum.system.ferry.FerryLaws.of(lawId);
        if (law == null) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "ferry=refused reason=no such law " + lawId), false);
            return 0;
        }
        var cap = com.cadykaya.interregnum.system.ferry.Ferry
                .capture(ctx.getSource().getLevel(), keel);
        if (!cap.ok()) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "ferry=refused reason=" + cap.refusal()), false);
            return 0;
        }
        var bad = com.cadykaya.interregnum.system.ferry.Ferry.checklist(cap.hull(), law);
        if (!bad.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "ferry=held law=" + lawId + " violations=" + bad.size()), false);
            for (var v : bad) {
                ctx.getSource().sendSuccess(() -> Component.literal(
                        "  ferry-notice " + v.rule() + " " + v.blockId()
                                + " x" + v.count() + " [" + v.reasonKey() + "]"), false);
            }
            return 0;
        }
        if (pad == null) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "ferry=clear law=" + lawId + " total=" + cap.hull().manifest().total()), false);
            return 1;
        }
        com.cadykaya.interregnum.system.ferry.Ferry
                .place(ctx.getSource().getLevel(), cap.hull(), keel, pad);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "ferry=sailed law=" + lawId + " total=" + cap.hull().manifest().total()), true);
        return cap.hull().manifest().total();
    }

    /** One line describing where a table currently stands. */
    private static String describe(Conversations.Table t) {
        var node = t.node();
        StringBuilder sb = new StringBuilder("scene=").append(t.scene)
                .append(" node=").append(node.id())
                .append(" speaker=").append(node.speaker())
                .append(" rule=").append(node.rule())
                .append(" options=");
        for (var o : node.options()) {
            sb.append(o.id());
            if (!o.requiredTags().isEmpty()) {
                sb.append(o.requiredTags());
            }
            sb.append(',');
        }
        sb.append(" waiting=");
        var picks = t.conversation.picks();
        for (String p : t.conversation.participants()) {
            if (!picks.containsKey(p)) {
                sb.append(p).append(',');
            }
        }
        return sb.toString();
    }

    /**
     * `/interregnum talk` -- run a conversation from the console.
     *
     * The honest justification, because this one looks the most like a test hook of
     * anything in this file: a conversation is the only system in the mod whose
     * whole state lives in memory for a few minutes and then is gone. When a table
     * wedges -- somebody's client desynced, an option will not take, a node will not
     * resolve -- there is nothing to inspect afterwards and no log of what the table
     * was waiting on. `status` answers that, and `say`/`leave` are how an operator
     * unsticks it without kicking everyone.
     *
     * It is also, not coincidentally, the only way to exercise multiplayer dialogue
     * on a server with no players: participants are opaque ids by design, so the
     * vote, tie, unanimity and walk-away rules can all be asserted headlessly.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> talk() {
        return Commands.literal("talk")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("start")
                        .then(Commands.argument("scene", net.minecraft.commands.arguments.IdentifierArgument.id())
                                .then(Commands.argument("participants", StringArgumentType.string())
                                        .executes(ctx -> start(ctx, null))
                                        .then(Commands.argument("speaker", EntityArgument.entity())
                                                .executes(ctx -> start(ctx,
                                                        EntityArgument.getEntity(ctx, "speaker")))))))
                .then(Commands.literal("say")
                        .then(Commands.argument("who", StringArgumentType.string())
                                .then(Commands.argument("option", StringArgumentType.string())
                                        .executes(ctx -> {
                                            String who = StringArgumentType.getString(ctx, "who");
                                            String opt = StringArgumentType.getString(ctx, "option");
                                            var was = Conversations.of(who);
                                            Resolution r;
                                            try {
                                                r = Conversations.submit(
                                                        ctx.getSource().getServer(), who, opt);
                                            } catch (IllegalArgumentException | IllegalStateException e) {
                                                ctx.getSource().sendSuccess(() -> Component.literal(
                                                        "talk=refused reason=" + e.getMessage()), false);
                                                return 0;
                                            }
                                            if (r == null) {
                                                var t = Conversations.of(who);
                                                ctx.getSource().sendSuccess(() -> Component.literal(
                                                        "talk=pending " + describe(t)), false);
                                                return 1;
                                            }
                                            var t = Conversations.of(who);
                                            final Resolution res = r;
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "talk=" + res.kind()
                                                            + " chose=" + (res.chosen() == null
                                                                    ? "none" : res.chosen().id())
                                                            + " stances=" + res.stances()
                                                            + (t == null
                                                                    ? " ended=true final=" + was.node().id()
                                                                    : " " + describe(t))), false);
                                            // The table is gone but the object is not, and its
                                            // terminal node is the last thing the players were
                                            // shown. An operator who just ended somebody's
                                            // conversation should be able to see how it ended.
                                            if (t == null) {
                                                for (String line : ConversationView.plain(
                                                        ConversationView.render(was, who, java.util.Set.of()))) {
                                                    ctx.getSource().sendSuccess(
                                                            () -> Component.literal("show| " + line), false);
                                                }
                                            }
                                            return 1;
                                        }))))
                .then(Commands.literal("show")
                        .then(Commands.argument("who", StringArgumentType.string())
                                .executes(ctx -> show(ctx, java.util.Set.of()))
                                .then(Commands.argument("tags", StringArgumentType.string())
                                        .executes(ctx -> show(ctx, java.util.Set.of(
                                                StringArgumentType.getString(ctx, "tags")
                                                        .split("[,+]")))))))
                .then(Commands.literal("scene")
                        .then(Commands.argument("who", EntityArgument.entity())
                                .executes(ctx -> {
                                    // Asks the same question a right-click asks, for
                                    // every mob that answers it. A headless server can
                                    // never reach `mobInteract`, so without this the
                                    // choice an NPC makes about how to open is only
                                    // observable by playing -- which is to say, not
                                    // observable at all from CI.
                                    var e = EntityArgument.getEntity(ctx, "who");
                                    net.minecraft.resources.Identifier scene = null;
                                    if (e instanceof com.cadykaya.interregnum.content.entity
                                            .ShrineKeeperEntity keeper) {
                                        scene = keeper.openingScene();
                                    } else if (e instanceof com.cadykaya.interregnum.content
                                            .entity.WardenEntity warden) {
                                        scene = warden.openingScene(ctx.getSource().getServer());
                                    }
                                    if (scene == null) {
                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "scene=none reason=nothing to say"), false);
                                        return 0;
                                    }
                                    final var chosen = scene;
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "scene=" + chosen), false);
                                    return 1;
                                })))
                .then(Commands.literal("status")
                        .then(Commands.argument("who", StringArgumentType.string())
                                .executes(ctx -> {
                                    String who = StringArgumentType.getString(ctx, "who");
                                    var t = Conversations.of(who);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            t == null ? "talk=none active=" + Conversations.active()
                                                    : "talk=live " + describe(t)), false);
                                    return t == null ? 0 : 1;
                                })))
                .then(Commands.literal("leave")
                        .then(Commands.argument("who", StringArgumentType.string())
                                .executes(ctx -> {
                                    String who = StringArgumentType.getString(ctx, "who");
                                    Resolution r = Conversations.leave(ctx.getSource().getServer(), who);
                                    var t = Conversations.of(who);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "talk=left resolved="
                                                    + (r == null ? "none"
                                                            : r.kind() + "/" + (r.chosen() == null
                                                                    ? "none" : r.chosen().id()))
                                                    + " active=" + Conversations.active()
                                                    + (t == null ? "" : " " + describe(t))), false);
                                    return 1;
                                })));
    }

    /**
     * What is this participant actually looking at?
     *
     * The one question that is impossible to answer any other way. A player says the
     * option is not there; the option is gated on a tag; nothing in any log says
     * which lines that player was sent. This renders their exact view, and takes an
     * optional tag set so an operator can ask "what would a Theoclast see here?"
     * without giving anybody anything.
     */
    private static int show(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
                            java.util.Set<String> tags) {
        String who = StringArgumentType.getString(ctx, "who");
        var t = Conversations.of(who);
        if (t == null) {
            ctx.getSource().sendSuccess(() -> Component.literal("show=none"), false);
            return 0;
        }
        // The viewer's standing, looked up by the id they are seated under. Without
        // this a headless check cannot exercise `standing_at_least` at all -- there
        // is no client here, and `show` is the only way to see what a player sees.
        // `peek` so that merely LOOKING at a conversation never creates a record.
        com.cadykaya.interregnum.core.regard.RegardState regard = null;
        try {
            regard = RegardSavedData.get(ctx.getSource().getServer())
                    .peek(java.util.UUID.fromString(who));
        } catch (IllegalArgumentException e) {
            // not a player id; no record to consult, and no gate to satisfy
        }
        for (String line
                : ConversationView.plain(ConversationView.render(t, who, tags, regard))) {
            ctx.getSource().sendSuccess(() -> Component.literal("show| " + line), false);
        }
        return 1;
    }

    private static int haunt(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
                            boolean force) {
        String who = StringArgumentType.getString(ctx, "who");
        java.util.UUID uuid;
        try {
            uuid = java.util.UUID.fromString(who);
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "haunt=refused reason=not a player id"), false);
            return 0;
        }
        var outcome = TheHaunt.offer(ctx.getSource().getServer(), uuid, force);
        ctx.getSource().sendSuccess(() -> Component.literal("haunt=" + outcome), true);
        return outcome == TheHaunt.Outcome.OPENED ? 1 : 0;
    }

    private static int start(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
                             net.minecraft.world.entity.Entity speaker) {
        var id = net.minecraft.commands.arguments.IdentifierArgument.getId(ctx, "scene");
        // Comma OR plus, because brigadier's unquoted strings do not allow commas:
        // `kaya,p2,p3` parses as `kaya` and then fails on trailing data, with an
        // error that points at the end of the line and blames whatever is there.
        // `kaya+p2+p3` needs no quoting; "kaya,p2,p3" quoted works too.
        var who = java.util.List.of(
                StringArgumentType.getString(ctx, "participants").split("[,+]"));
        Conversations.Table t;
        try {
            t = Conversations.open(ctx.getSource().getServer(), id, who, speaker);
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "talk=refused reason=" + e.getMessage()), false);
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("talk=open " + describe(t)), true);
        return 1;
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("interregnum")
                .then(Commands.literal("show")
                        .then(Commands.argument("who", StringArgumentType.string())
                                .executes(ctx -> show(ctx, java.util.Set.of()))
                                .then(Commands.argument("tags", StringArgumentType.string())
                                        .executes(ctx -> show(ctx, java.util.Set.of(
                                                StringArgumentType.getString(ctx, "tags")
                                                        .split("[,+]")))))))
                .then(Commands.literal("scene")
                        .then(Commands.argument("who", EntityArgument.entity())
                                .executes(ctx -> {
                                    // Asks the same question a right-click asks, for
                                    // every mob that answers it. A headless server can
                                    // never reach `mobInteract`, so without this the
                                    // choice an NPC makes about how to open is only
                                    // observable by playing -- which is to say, not
                                    // observable at all from CI.
                                    var e = EntityArgument.getEntity(ctx, "who");
                                    net.minecraft.resources.Identifier scene = null;
                                    if (e instanceof com.cadykaya.interregnum.content.entity
                                            .ShrineKeeperEntity keeper) {
                                        scene = keeper.openingScene();
                                    } else if (e instanceof com.cadykaya.interregnum.content
                                            .entity.WardenEntity warden) {
                                        scene = warden.openingScene(ctx.getSource().getServer());
                                    }
                                    if (scene == null) {
                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "scene=none reason=nothing to say"), false);
                                        return 0;
                                    }
                                    final var chosen = scene;
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "scene=" + chosen), false);
                                    return 1;
                                })))
                .then(Commands.literal("status").executes(ctx -> {
                    ChapterSavedData data = ChapterSavedData.get(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "chapter=" + data.chapter()
                                    + " band=" + data.band()
                                    + " dormant=" + data.mechanicsDormant()
                                    + " letters=" + data.lettersDelivered()
                                    + " killer=" + (data.killer() == null ? "none" : data.killer())
                                    + " ticks=" + Unraveling.ticksObserved()
                                    + " passes=" + Unraveling.passesRun()), false);
                    return 1;
                }));

        // One `record` node with a child per milestone. Permission style is
        // `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)` in 26.2 --
        // `source.hasPermission(int)` no longer exists; permissions moved to a
        // PermissionSet/PermissionCheck model. Read from the vanilla commands, not
        // remembered.
        LiteralArgumentBuilder<CommandSourceStack> record = Commands.literal("record")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        // The claim system: read it, and edit it.
        //
        // `record`/`forget` are not test hooks. Blocks placed before this mod was
        // installed are invisible to the tracker, so any existing world has builds
        // it does not know about -- an operator needs a way to say "this is ours".
        // They also happen to be the only way to exercise the tracker on a headless
        // server, which is why the event handler can stay three lines long.
        root = root.then(Commands.literal("claim")
                .then(Commands.literal("at")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ctx -> {
                                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                    ServerLevel level = ctx.getSource().getLevel();
                                    boolean claimed = Claims.isClaimed(level, pos);
                                    int count = Claims.count(level, pos);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "claimed=" + claimed + " chunkPlacements=" + count), false);
                                    return 1;
                                })))
                .then(Commands.literal("record")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("from", BlockPosArgument.blockPos())
                                .then(Commands.argument("to", BlockPosArgument.blockPos())
                                        .executes(ctx -> region(ctx, true)))))
                .then(Commands.literal("forget")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("from", BlockPosArgument.blockPos())
                                .then(Commands.argument("to", BlockPosArgument.blockPos())
                                        .executes(ctx -> region(ctx, false))))));

        // The unraveling: ask what it would do, and make it do it.
        //
        // Not test hooks either, and for a sharper reason than the claim commands.
        // The unraveling is probabilistic, world-wide, and invisible in the small:
        // when a server owner says "it is eating my world" or "it is doing nothing",
        // there is no way to settle it by looking. `at` answers for one block and
        // names the gate that stopped it; `sweep` runs a measured burst and reports
        // a rate. Both were needed to tune the numbers in bands.json in the first
        // place -- see tools/unravel_rate_probe.sh.
        root = root.then(Commands.literal("unravel")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("at")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ctx -> {
                                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                    ServerLevel level = ctx.getSource().getLevel();
                                    ChapterSavedData data =
                                            ChapterSavedData.get(ctx.getSource().getServer());
                                    // Certain: the operator asked what the rule does
                                    // HERE, not what it does on average.
                                    Unraveling.Decision d = Unraveling.apply(
                                            level, pos, data, level.getRandom(), true);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "unravel=" + d.outcome()
                                                    + " rule=" + d.rule()
                                                    + " thin=" + Unraveling.isThinPlace(level, pos, data)
                                                    + " now=" + net.minecraft.core.registries.BuiltInRegistries.BLOCK
                                                            .getKey(level.getBlockState(pos).getBlock())), true);
                                    return d.outcome() == Unraveling.Outcome.CONVERTED ? 1 : 0;
                                })))
                .then(Commands.literal("sweep")
                        .then(Commands.argument("radius", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 128))
                                .then(Commands.argument("samples", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 100000))
                                        .executes(ctx -> {
                                            ServerLevel level = ctx.getSource().getLevel();
                                            ChapterSavedData data =
                                                    ChapterSavedData.get(ctx.getSource().getServer());
                                            int radius = com.mojang.brigadier.arguments.IntegerArgumentType
                                                    .getInteger(ctx, "radius");
                                            int samples = com.mojang.brigadier.arguments.IntegerArgumentType
                                                    .getInteger(ctx, "samples");
                                            int n = Unraveling.sampleAround(level,
                                                    BlockPos.containing(ctx.getSource().getPosition()),
                                                    data, level.getRandom(), radius, samples);
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "swept=" + samples + " converted=" + n), true);
                                            return n;
                                        })))));

        // Reading the record. Bands, never numbers, even here -- the day this prints
        // a score is the day somebody optimises against it, and the whole point of
        // regard is that it is a relationship rather than a meter.
        root = root.then(Commands.literal("regard")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("who", StringArgumentType.string())
                        .executes(ctx -> {
                            String who = StringArgumentType.getString(ctx, "who");
                            java.util.UUID uuid;
                            try {
                                uuid = java.util.UUID.fromString(who);
                            } catch (IllegalArgumentException e) {
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                        "regard=none reason=not a player id"), false);
                                return 0;
                            }
                            var state = RegardSavedData.get(ctx.getSource().getServer()).peek(uuid);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    state == null ? "regard=none"
                                            : "regard= " + RegardSavedData.describe(state)), false);
                            return state == null ? 0 : 1;
                        })
                        // Moving the record by hand. A gamemaster affordance in the
                        // same shape as `record deicide` and `unravel at`, and the
                        // only way a headless check can put a player at a standing
                        // and then look at what the world offers them -- reaching
                        // TRUSTED through actual conversation would take a dozen
                        // scenes that do not exist yet.
                        //
                        // Routed through RegardNotices like every other mover, so it
                        // cannot become a back door that changes standing without the
                        // player being told. If an admin nudges you into a new band,
                        // you hear about it exactly as you would have otherwise.
                        .then(Commands.literal("adjust")
                                .then(Commands.argument("institution", StringArgumentType.word())
                                        .then(Commands.argument("delta", com.mojang.brigadier.arguments.IntegerArgumentType.integer(-200, 200))
                                                .executes(ctx -> {
                                                    String who = StringArgumentType.getString(ctx, "who");
                                                    String inst = StringArgumentType.getString(ctx, "institution");
                                                    int delta = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "delta");
                                                    java.util.UUID uuid;
                                                    com.cadykaya.interregnum.core.regard.Institution institution;
                                                    try {
                                                        uuid = java.util.UUID.fromString(who);
                                                        institution = com.cadykaya.interregnum.core.regard.Institution.valueOf(
                                                                inst.toUpperCase(java.util.Locale.ROOT));
                                                    } catch (IllegalArgumentException e) {
                                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                                "adjust=none reason=" + e.getMessage()), false);
                                                        return 0;
                                                    }
                                                    var server = ctx.getSource().getServer();
                                                    var data = RegardSavedData.get(server);
                                                    int[] moved = new int[1];
                                                    com.cadykaya.interregnum.system.regard.RegardNotices.around(server, uuid, () -> {
                                                        moved[0] = data.of(server, uuid)
                                                                .adjust(institution, delta);
                                                        data.touch();
                                                    });
                                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                                            "adjust= " + institution + " moved=" + moved[0]
                                                                    + " now=" + data.of(server, uuid)
                                                                            .standing(institution)), true);
                                                    return 1;
                                                }))))));

        // Re-issuing the dream. A player who slept through a crash has lost the
        // only scripted delivery this scene has and there is no other way back to
        // it. `force` skips the once-only check and nothing else -- an admin with
        // good intentions still cannot hand the ghost's private conversation to
        // somebody who did not kill it.
        // Driving one posting sweep by hand. The tick handler runs this around every
        // player, and a headless server has no players at all -- so without a command
        // seam the entire mechanism would be unreachable from CI. `interregnum
        // unravel at` exists for exactly the same reason and takes the same shape.
        root = root.then(Commands.literal("warden")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("post")
                        .then(Commands.argument("at", BlockPosArgument.blockPos())
                                .executes(ctx -> {
                                    BlockPos at = BlockPosArgument.getLoadedBlockPos(ctx, "at");
                                    int n = com.cadykaya.interregnum.system.warden.StatuePosting
                                            .postAround(ctx.getSource().getLevel(), at);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "posted=" + n), true);
                                    return n;
                                }))));

        // The crossing, from the console. A keel is a block a player right-clicks and
        // a headless server has nobody to click it, so this is the seam that makes the
        // whole mechanism reachable from CI -- the same shape as `unravel at` and
        // `warden post`.
        root = root.then(Commands.literal("ferry")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("manifest")
                        .then(Commands.argument("keel", BlockPosArgument.blockPos())
                                .executes(ctx -> {
                                    BlockPos keel = BlockPosArgument.getLoadedBlockPos(ctx, "keel");
                                    var cap = com.cadykaya.interregnum.system.ferry.Ferry
                                            .capture(ctx.getSource().getLevel(), keel);
                                    if (!cap.ok()) {
                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "ferry=refused reason=" + cap.refusal()), false);
                                        return 0;
                                    }
                                    var m = cap.hull().manifest();
                                    StringBuilder sb = new StringBuilder("ferry=manifest total=")
                                            .append(m.total());
                                    m.blocks().forEach((b, n) ->
                                            sb.append(' ').append(b).append('x').append(n));
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal(sb.toString()), false);
                                    return m.total();
                                })))
                .then(Commands.literal("check")
                        .then(Commands.argument("keel", BlockPosArgument.blockPos())
                                .then(Commands.argument("law", StringArgumentType.word())
                                        .executes(ctx -> ferryCheck(ctx, null)))))
                .then(Commands.literal("sail")
                        .then(Commands.argument("keel", BlockPosArgument.blockPos())
                                .then(Commands.argument("law", StringArgumentType.word())
                                        .then(Commands.argument("pad", BlockPosArgument.blockPos())
                                                .executes(ctx -> ferryCheck(ctx,
                                                        BlockPosArgument.getLoadedBlockPos(ctx, "pad"))))))));

        root = root.then(Commands.literal("haunt")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("dream")
                        .then(Commands.argument("who", StringArgumentType.string())
                                .executes(ctx -> haunt(ctx, false))
                                .then(Commands.literal("force")
                                        .executes(ctx -> haunt(ctx, true))))));

        root = root.then(talk());

        // `reply` is the ONLY player-facing node in this command, and it is
        // deliberately outside `talk`: everything under `talk` moves other people's
        // conversations and is gamemaster-gated, while this can only ever speak for
        // whoever ran it. It exists because the clickable options in chat have to
        // run *something*, and a permission-gated command would make dialogue
        // playable only by operators.
        root = root.then(Commands.literal("reply")
                .then(Commands.argument("option", StringArgumentType.string())
                        .executes(ctx -> {
                            var player = ctx.getSource().getPlayer();
                            if (player == null) {
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                        "talk=refused reason=only a player can reply"), false);
                                return 0;
                            }
                            String opt = StringArgumentType.getString(ctx, "option");
                            try {
                                Conversations.submit(ctx.getSource().getServer(),
                                        player.getUUID().toString(), opt);
                            } catch (IllegalArgumentException | IllegalStateException e) {
                                player.sendSystemMessage(Component.literal(e.getMessage()));
                                return 0;
                            }
                            return 1;
                        })));

        for (Milestone m : Milestone.values()) {
            // DEICIDE takes an optional killer id. Not a test hook: a server that
            // restored a backup, or migrated, needs a way to say who the First
            // Theoclast is -- the whole regard scar and the ghost's private
            // relationship hang off that one UUID, and there is otherwise no way to
            // set it after the fact.
            if (m == Milestone.DEICIDE) {
                record = record.then(Commands.literal("deicide")
                        .then(Commands.argument("killer", StringArgumentType.string())
                                .executes(ctx -> {
                                    java.util.UUID who;
                                    try {
                                        who = java.util.UUID.fromString(
                                                StringArgumentType.getString(ctx, "killer"));
                                    } catch (IllegalArgumentException e) {
                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "refused reason=not a player id"), false);
                                        return 0;
                                    }
                                    ChapterSavedData data =
                                            ChapterSavedData.get(ctx.getSource().getServer());
                                    boolean isNew = Deicide.commit(
                                            ctx.getSource().getServer(), who,
                                            ctx.getSource().getLevel(),
                                            BlockPos.containing(ctx.getSource().getPosition()));
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            (isNew ? "recorded " : "already recorded ")
                                                    + "DEICIDE by " + who
                                                    + "; chapter=" + data.chapter()), true);
                                    return 1;
                                }))
                        .executes(ctx -> {
                            ChapterSavedData data =
                                    ChapterSavedData.get(ctx.getSource().getServer());
                            boolean isNew = Deicide.commit(ctx.getSource().getServer(), null,
                                    ctx.getSource().getLevel(),
                                    BlockPos.containing(ctx.getSource().getPosition()));
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    (isNew ? "recorded " : "already recorded ") + "DEICIDE"
                                            + "; chapter=" + data.chapter()), true);
                            return 1;
                        }));
                continue;
            }
            record = record.then(Commands.literal(m.name().toLowerCase(java.util.Locale.ROOT))
                    .executes(ctx -> {
                        ChapterSavedData data = ChapterSavedData.get(ctx.getSource().getServer());
                        // DEICIDE is not just a milestone -- it has consequences, and
                        // they live in exactly one place so this path and the pickup
                        // path can never drift apart.
                        // The command runs somewhere, so it has a site -- which is
                        // what makes the crater reachable on a headless server.
                        boolean isNew = (m == Milestone.DEICIDE)
                                ? Deicide.commit(ctx.getSource().getServer(), null,
                                        ctx.getSource().getLevel(),
                                        net.minecraft.core.BlockPos.containing(
                                                ctx.getSource().getPosition()))
                                : data.record(m);
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                (isNew ? "recorded " : "already recorded ") + m.name()
                                        + "; chapter=" + data.chapter()), true);
                        return 1;
                    }));
        }
        event.getDispatcher().register(root.then(record));
    }
}
