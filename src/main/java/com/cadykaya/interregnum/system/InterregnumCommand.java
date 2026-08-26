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
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
    /**
     * The grimoire of the player named by a string argument, or null if it is not an id.
     *
     * Shared by `cast` and `learn` so both agree about who somebody is. Returns the
     * live object -- `learn` writes through it and marks the store dirty.
     */
    private static com.cadykaya.interregnum.core.magic.Grimoire grimoireOf(
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String arg) {
        try {
            java.util.UUID id = java.util.UUID.fromString(StringArgumentType.getString(ctx, arg));
            return com.cadykaya.interregnum.system.magic.GrimoireSavedData
                    .get(ctx.getSource().getServer()).of(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

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
    /**
     * @param pad where to put the hull down, or null to use the destination's own dock.
     * @param sail whether this is a crossing at all. A null pad used to mean "only
     *        check" -- which stopped working the moment a null pad became a legitimate
     *        way to say "sail to the dock", so the two questions are now two parameters
     *        rather than one overloaded one.
     */
    private static int ferryCheck(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
                                  BlockPos pad, boolean sail) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
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
        if (!sail) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "ferry=clear law=" + lawId + " total=" + cap.hull().manifest().total()), false);
            return 1;
        }
        // Where the law says this crossing goes. Not a parameter: the destination is a
        // property of the law the hull was cleared against, so a hull cleared for the
        // Quiet One cannot be sailed anywhere else. The boarding notice a player read
        // on the dock names the destination, and this is what makes that true.
        var target = com.cadykaya.interregnum.system.ferry.FerryLaws.destinationOf(lawId);
        net.minecraft.server.level.ServerLevel to =
                ctx.getSource().getServer().getLevel(target);
        if (to == null) {
            // A law may legitimately name a dimension another datapack supplies, so
            // this is refused HERE rather than at load -- where it can name what is
            // missing instead of taking down every law in the file.
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "ferry=refused reason=no such destination " + target.identifier()), false);
            return 0;
        }
        // No pad named: go where the dock is. WORLD.md says a crossing arrives "at the
        // far pad", and until there was one the arrival position was an argument an
        // operator typed -- a mail service whose destination is a parameter is not one.
        // The three-argument form stays for the nudge case, where a hull is moved a few
        // blocks inside one world and there is no dock involved.
        BlockPos arrival = pad != null
                ? pad
                : com.cadykaya.interregnum.system.ferry.FerryPad.ensure(to);
        if (arrival == null) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "ferry=refused reason=no pad in " + target.identifier()), false);
            return 0;
        }
        if (pad == null && com.cadykaya.interregnum.system.ferry.FerryPad
                .occupied(to, arrival)) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "ferry=refused reason=berth occupied at " + target.identifier()), false);
            return 0;
        }
        com.cadykaya.interregnum.system.ferry.Ferry
                .place(ctx.getSource().getLevel(), cap.hull(), keel, to, arrival);
        // The return leg, filed on departure. A mail service knows where its vessels came
        // from; see Voyages for why the way home is a record rather than a fifth law.
        com.cadykaya.interregnum.system.ferry.Voyages.get(ctx.getSource().getServer())
                .departed(ctx.getSource().getLevel().dimension(), keel, target, arrival);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "ferry=sailed law=" + lawId + " to=" + target.identifier()
                        + " total=" + cap.hull().manifest().total()), true);
        return cap.hull().manifest().total();
    }

    /**
     * Send a ferry back where it came from.
     *
     * A separate verb rather than a fifth law, and {@link
     * com.cadykaya.interregnum.system.ferry.Voyages} carries the argument: every other
     * crossing is a god's policy about its own world, and the overworld has nobody left
     * to have one. This is a mail service returning a vessel to the depot it left.
     *
     * No checklist, therefore, and that is not an oversight -- there is no authority at
     * the far end to run one. What a player brings home from a god's world is between
     * them and the Wardenate.
     */
    private static int ferryHome(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        BlockPos keel = BlockPosArgument.getLoadedBlockPos(ctx, "keel");
        net.minecraft.server.level.ServerLevel here = ctx.getSource().getLevel();
        var voyages = com.cadykaya.interregnum.system.ferry.Voyages
                .get(ctx.getSource().getServer());
        var origin = voyages.originOf(here.dimension(), keel);
        if (origin == null) {
            // The bureaucratic answer, and the true one: this vessel is not on our books.
            // A keel a player built themselves has never sailed, so it has no way home
            // in the sense this verb means -- it is already there.
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "ferry=refused reason=no return leg on file"), false);
            return 0;
        }
        var cap = com.cadykaya.interregnum.system.ferry.Ferry.capture(here, keel);
        if (!cap.ok()) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "ferry=refused reason=" + cap.refusal()), false);
            return 0;
        }
        net.minecraft.server.level.ServerLevel back =
                ctx.getSource().getServer().getLevel(origin.level());
        if (back == null) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "ferry=refused reason=no such origin " + origin.level().identifier()), false);
            return 0;
        }
        // The same rule the far pad has, for the same reason: a hull put down on top of
        // another hull silently replaces whatever shared a coordinate. A berth is a berth
        // whichever direction you are going.
        if (com.cadykaya.interregnum.system.ferry.FerryPad.occupied(back, origin.keel())) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "ferry=refused reason=berth occupied at " + origin.level().identifier()), false);
            return 0;
        }
        com.cadykaya.interregnum.system.ferry.Ferry
                .place(here, cap.hull(), keel, back, origin.keel());
        voyages.arrivedHome(here.dimension(), keel);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "ferry=returned to=" + origin.level().identifier()
                        + " total=" + cap.hull().manifest().total()), true);
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

        // Band 3, from the console. Asking "what leaks here" needs a position and a
        // world; a player would just walk there and notice, which a headless server
        // cannot do.
        // Band 4's tending stamp, read and written.
        //
        // `tend` exists because a headless server has no players, and tending is defined
        // as somebody being somewhere. Without it the only way to exercise the stamp
        // would be to reimplement the tick handler inside a check, which would test the
        // check. It is not a back door: it writes exactly what standing there writes.
        root = root.then(Commands.literal("attrition")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("at")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ctx -> {
                                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                    var level = ctx.getSource().getLevel();
                                    var at = net.minecraft.world.level.ChunkPos.containing(pos);
                                    var data = com.cadykaya.interregnum.system.ChapterSavedData
                                            .get(ctx.getSource().getServer());
                                    long since = com.cadykaya.interregnum.system.attrition.Tending
                                            .sinceTended(level, at);
                                    boolean stale = com.cadykaya.interregnum.system.attrition.Tending
                                            .stale(level, at);
                                    boolean fraying = com.cadykaya.interregnum.core.attrition
                                            .Attrition.fraying(data.band());
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "attrition sinceTended=" + since + " stale=" + stale
                                                    + " fraying=" + fraying
                                                    + " band=" + data.band()), false);
                                    return stale && fraying ? 1 : 0;
                                })))
                // The seam. Band 4's rates are slow on purpose, and a check that waited
                // for a roll to land would be testing the random number generator rather
                // than the law. `null` for the random skips ONLY the chance roll; the
                // overworld gate, the band gate, the staleness gate and the claim ledger
                // all still apply.
                .then(Commands.literal("generalise")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ctx -> {
                                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                    var level = ctx.getSource().getLevel();
                                    var data = com.cadykaya.interregnum.system.ChapterSavedData
                                            .get(ctx.getSource().getServer());
                                    final String r = com.cadykaya.interregnum.system.attrition
                                            .Generalise.step(level, pos, data, null);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "attrition-step=" + r), false);
                                    return r.startsWith("minecraft:") ? 1 : 0;
                                })))
                .then(Commands.literal("abandon")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ctx -> {
                                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                    var level = ctx.getSource().getLevel();
                                    var at = net.minecraft.world.level.ChunkPos.containing(pos);
                                    com.cadykaya.interregnum.system.attrition.Tending
                                            .abandon(level, at);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "attrition abandoned " + at.x() + " " + at.z()), false);
                                    return 1;
                                })))
                .then(Commands.literal("tend")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ctx -> {
                                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                    var level = ctx.getSource().getLevel();
                                    var at = net.minecraft.world.level.ChunkPos.containing(pos);
                                    com.cadykaya.interregnum.system.attrition.Tending
                                            .tendAround(level, at);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "attrition tended " + at.x() + " " + at.z()), false);
                                    return 1;
                                }))));

        // The first spell. A command rather than an item because the LAW is what is
        // being built here and the law is what a headless server can drive; how a player
        // comes to know Weather -- learning it in the Hearth-Turner's world, per
        // WORLD.md's "schools, one per god, learned in their worlds" -- is the next
        // increment and is recorded in HANDOFF rather than guessed at.
        root = root.then(Commands.literal("cast")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                // A CASTER, not just a position. Casting is something somebody does,
                // and what they have been taught decides whether they can -- so the
                // command takes the same player id `interregnum regard` does rather than
                // casting anonymously from the console. A seam that could cast with
                // nobody behind it would be a seam that skipped the prerequisite, and
                // then the check driving it would prove nothing about the rule.
                .then(Commands.literal("weather")
                        .then(Commands.argument("who", StringArgumentType.string())
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> {
                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                            var g = grimoireOf(ctx, "who");
                                            var cast = com.cadykaya.interregnum.system.magic.Weather
                                                    .cast(ctx.getSource().getLevel(), pos, g);
                                            String became = cast.worked()
                                                    ? net.minecraft.core.registries.BuiltInRegistries.BLOCK
                                                            .getKey(cast.became().getBlock()).toString()
                                                    : cast.refused();
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "cast=weather became=" + became
                                                            + " frayed=" + cast.frayed()), false);
                                            return cast.worked() ? 1 : 0;
                                        })))));

        root = root.then(Commands.literal("cast")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("lighten")
                        .then(Commands.argument("who", StringArgumentType.string())
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> {
                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                            var level = ctx.getSource().getLevel();
                                            var cast = com.cadykaya.interregnum.system.magic.LightenSpell
                                                    .cast(level, pos, grimoireOf(ctx, "who"));
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "cast=lighten opened=" + cast.opened()
                                                            + " frayed=" + cast.frayed()
                                                            + " refused=" + cast.refused()
                                                            + " zones=" + com.cadykaya.interregnum
                                                                    .system.magic.Zones.count(level, com.cadykaya.interregnum.core.magic.Spell.LIGHTEN)),
                                                    false);
                                            return cast.opened() ? 1 : 0;
                                        })))));

        root = root.then(Commands.literal("cast")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("bridgeroot")
                        .then(Commands.argument("who", StringArgumentType.string())
                                .then(Commands.argument("from", BlockPosArgument.blockPos())
                                        .then(Commands.argument("toward", BlockPosArgument.blockPos())
                                                .executes(ctx -> {
                                                    BlockPos from = BlockPosArgument.getLoadedBlockPos(ctx, "from");
                                                    BlockPos toward = BlockPosArgument.getLoadedBlockPos(ctx, "toward");
                                                    var cast = com.cadykaya.interregnum.system.magic
                                                            .BridgerootSpell.cast(ctx.getSource().getLevel(),
                                                                    from, toward, grimoireOf(ctx, "who"));
                                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                                            "cast=bridgeroot grew=" + cast.grew()
                                                                    + " frayed=" + cast.frayed()
                                                                    + " refused=" + cast.refused()), false);
                                                    return cast.grew() > 0 ? 1 : 0;
                                                }))))));

        root = root.then(Commands.literal("cast")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("hush")
                        .then(Commands.argument("who", StringArgumentType.string())
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> {
                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                            var level = ctx.getSource().getLevel();
                                            var cast = com.cadykaya.interregnum.system.magic.HushSpell
                                                    .cast(level, pos, grimoireOf(ctx, "who"));
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "cast=hush opened=" + cast.opened()
                                                            + " frayed=" + cast.frayed()
                                                            + " refused=" + cast.refused()), false);
                                            return cast.opened() ? 1 : 0;
                                        })))));

        root = root.then(Commands.literal("cast")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("rewind")
                        .then(Commands.argument("who", StringArgumentType.string())
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> {
                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                            var cast = com.cadykaya.interregnum.system.magic.RewindSpell
                                                    .cast(ctx.getSource().getLevel(), pos,
                                                            grimoireOf(ctx, "who"));
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "cast=rewind became="
                                                            + com.cadykaya.interregnum.system.magic
                                                                    .RewindSpell.describe(cast)
                                                            + " frayed=" + cast.frayed()), false);
                                            return cast.worked() ? 1 : 0;
                                        })))));

        root = root.then(Commands.literal("cast")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("wildgrowth")
                        .then(Commands.argument("who", StringArgumentType.string())
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> {
                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                            var cast = com.cadykaya.interregnum.system.magic.WildgrowthSpell
                                                    .cast(ctx.getSource().getLevel(), pos,
                                                            grimoireOf(ctx, "who"));
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "cast=wildgrowth grew=" + cast.grew()
                                                            + " frayed=" + cast.frayed()
                                                            + " refused=" + cast.refused()), false);
                                            return cast.worked() ? 1 : 0;
                                        })))));

        root = root.then(Commands.literal("cast")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("dropforge")
                        .then(Commands.argument("who", StringArgumentType.string())
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> {
                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                            ServerLevel level = ctx.getSource().getLevel();
                                            var cast = com.cadykaya.interregnum.system.magic.DropForgeSpell
                                                    .cast(level, pos, grimoireOf(ctx, "who"));
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "cast=dropforge opened=" + cast.opened()
                                                            + " frayed=" + cast.frayed()
                                                            + " refused=" + cast.refused()
                                                            + " zones=" + com.cadykaya.interregnum
                                                                    .system.magic.Zones.count(level,
                                                                            com.cadykaya.interregnum.core.magic.Spell.DROP_FORGE)),
                                                    false);
                                            return cast.opened() ? 1 : 0;
                                        })))));

        root = root.then(Commands.literal("cast")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("still")
                        .then(Commands.argument("who", StringArgumentType.string())
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> {
                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                            var cast = com.cadykaya.interregnum.system.magic.StillSpell
                                                    .cast(ctx.getSource().getLevel(), pos,
                                                            grimoireOf(ctx, "who"));
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "cast=still opened=" + cast.opened()
                                                            + " frayed=" + cast.frayed()
                                                            + " refused=" + cast.refused()), false);
                                            return cast.opened() ? 1 : 0;
                                        })))));

        // Teaching, the operator seam. In play a school is taught by a scene -- see the
        // `teaches` field on a dialogue node -- and this is the same path with a
        // different caller, the way `record deicide` is for the deicide.
        root = root.then(Commands.literal("learn")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("who", StringArgumentType.string())
                        .then(Commands.argument("school", StringArgumentType.string())
                                .executes(ctx -> {
                                    String raw = StringArgumentType.getString(ctx, "school");
                                    com.cadykaya.interregnum.core.magic.School school;
                                    try {
                                        school = com.cadykaya.interregnum.core.magic.School
                                                .valueOf(raw.toUpperCase(java.util.Locale.ROOT));
                                    } catch (IllegalArgumentException e) {
                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "learn=no-such-school"), false);
                                        return 0;
                                    }
                                    var g = grimoireOf(ctx, "who");
                                    if (g == null) {
                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "learn=not-a-player-id"), false);
                                        return 0;
                                    }
                                    boolean fresh = g.learn(school);
                                    com.cadykaya.interregnum.system.magic.GrimoireSavedData
                                            .get(ctx.getSource().getServer()).touch();
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "learn=" + raw.toLowerCase(java.util.Locale.ROOT)
                                                    + " new=" + fresh
                                                    + " known=" + g.size()), false);
                                    return 1;
                                }))));

        root = root.then(Commands.literal("exodus")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("at")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ctx -> {
                                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                    var level = ctx.getSource().getLevel();
                                    var data = com.cadykaya.interregnum.system.ChapterSavedData
                                            .get(ctx.getSource().getServer());
                                    String law = com.cadykaya.interregnum.system.exodus.Leaks
                                            .describe(level, pos, data);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "exodus=" + law + " band=" + data.band()), false);
                                    return law.equals("none") ? 0 : 1;
                                })))
                // Which god WOULD leak at a chunk, asked of the pure function with no
                // shrine and no band involved. The seam exists so a check can FIND a
                // chunk that draws a particular law instead of predicting the hash --
                // a check that recomputed `lawAt` would be a restatement of the code
                // rather than a test of it.
                .then(Commands.literal("law")
                        .then(Commands.argument("cx", IntegerArgumentType.integer())
                                .then(Commands.argument("cz", IntegerArgumentType.integer())
                                        .executes(ctx -> {
                                            int cx = IntegerArgumentType.getInteger(ctx, "cx");
                                            int cz = IntegerArgumentType.getInteger(ctx, "cz");
                                            var law = com.cadykaya.interregnum.core.exodus.Exodus
                                                    .lawAt(cx, cz);
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "exodus-law " + cx + " " + cz + " = "
                                                            + law.name().toLowerCase(
                                                                    java.util.Locale.ROOT)), false);
                                            return 1;
                                        })))));

        // The mail, from the console. A letter is a thing a player reads, and a headless
        // server has nobody to read it -- the same seam as `warden post` and `ferry`.
        root = root.then(Commands.literal("letter")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("read")
                        .then(Commands.argument("god", StringArgumentType.word())
                                .executes(ctx -> {
                                    String god = StringArgumentType.getString(ctx, "god");
                                    var letter = com.cadykaya.interregnum.system.letters
                                            .Letters.forGod(god);
                                    if (letter == null) {
                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "letter=none for " + god), false);
                                        return 0;
                                    }
                                    // `To --` when there is no addressee. The em dash is
                                    // the Quiet One's whole character and it is not a
                                    // fallback for a missing value -- see core Letter.
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "letter=" + letter.id() + " to="
                                                    + letter.addressee().orElse("--")), false);
                                    // Through LetterPage, which is also what the item
                                    // uses. This used to render the page itself, which
                                    // was fine while nothing else could open a letter --
                                    // and the moment the ITEM could, it would have been
                                    // two renderers to keep in step, with only one of
                                    // them reachable by a check.
                                    for (Component line : com.cadykaya.interregnum.system
                                            .letters.LetterPage.of(letter.id())) {
                                        ctx.getSource().sendSuccess(() -> line, false);
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("seal")
                        .then(Commands.argument("god", StringArgumentType.word())
                                .then(Commands.argument("at", BlockPosArgument.blockPos())
                                        .executes(ctx -> {
                                            String god = StringArgumentType.getString(ctx, "god");
                                            BlockPos at = BlockPosArgument.getLoadedBlockPos(ctx, "at");
                                            if (com.cadykaya.interregnum.system.letters
                                                    .Letters.forGod(god) == null) {
                                                ctx.getSource().sendSuccess(() -> Component.literal(
                                                        "letter=none for " + god), false);
                                                return 0;
                                            }
                                            var stack = new net.minecraft.world.item.ItemStack(
                                                    com.cadykaya.interregnum.registry.ModItems
                                                            .SEALED_LETTER.get());
                                            stack.set(com.cadykaya.interregnum.registry.ModComponents
                                                    .LETTER.get(), god);
                                            var level = ctx.getSource().getLevel();
                                            var drop = new net.minecraft.world.entity.item.ItemEntity(
                                                    level, at.getX() + 0.5, at.getY() + 0.5,
                                                    at.getZ() + 0.5, stack);
                                            drop.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                                            level.addFreshEntity(drop);
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "letter=sealed " + god), true);
                                            return 1;
                                        })))));

        // The Turning, from the console. Ageing is slow on purpose and its most
        // important property is a CHAIN, so waiting for two rolls to land on one block
        // would make a categorical fact statistical for nothing. Same seam as
        // `unravel at` and `warden post`.
        root = root.then(Commands.literal("turning")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("age")
                        .then(Commands.argument("at", BlockPosArgument.blockPos())
                                .executes(ctx -> {
                                    BlockPos at = BlockPosArgument.getLoadedBlockPos(ctx, "at");
                                    var became = com.cadykaya.interregnum.system.hearth.Hearth
                                            .ageOnce(ctx.getSource().getLevel(), at);
                                    String what = became == null ? "nothing"
                                            : net.minecraft.core.registries.BuiltInRegistries.BLOCK
                                                    .getKey(became.getBlock()).toString();
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "turning=" + what), true);
                                    return became == null ? 0 : 1;
                                }))));

        // The crossing, from the console. A keel is a block a player right-clicks and
        // a headless server has nobody to click it, so this is the seam that makes the
        // whole mechanism reachable from CI -- the same shape as `unravel at` and
        // `warden post`.
        // The same page a player gets for touching a keel. Two callers, one path -- see
        // FerryDocket: a right-click cannot be driven from a headless server, so a docket
        // only the block could produce is a docket no check can read.
        // The pool, read back. There is no other way to see it: clasts are items lying in
        // the world, and counting entities cannot tell an unclaimed one from one a player
        // has picked up -- while the number that matters is how many the world has GIVEN
        // UP, which is the finite thing WORLD.md locks.
        // The same notice the block hands back. A right-click cannot be driven from a
        // headless server, so this is how the steles' text is reachable by a check --
        // the arrangement FerryDocket and LetterPage already use.
        root = root.then(Commands.literal("stele")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("read")
                        .then(Commands.argument("at", BlockPosArgument.blockPos())
                                .executes(ctx -> {
                                    BlockPos at = BlockPosArgument.getLoadedBlockPos(ctx, "at");
                                    var lines = com.cadykaya.interregnum.system.stele
                                            .SteleReading.of(ctx.getSource().getLevel(), at);
                                    for (Component line : lines) {
                                        ctx.getSource().sendSuccess(() -> line, false);
                                    }
                                    return lines.size();
                                }))));

        root = root.then(Commands.literal("clasts")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> {
                    var pool = com.cadykaya.interregnum.system.clast.ClastsSavedData
                            .get(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "clasts=" + pool.issued() + " remaining=" + pool.remaining()
                                    + " total="
                                    + com.cadykaya.interregnum.core.clast.Clasts.TOTAL),
                            false);
                    return pool.issued();
                }));

        root = root.then(Commands.literal("ferry")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("inspect")
                        .then(Commands.argument("keel", BlockPosArgument.blockPos())
                                .executes(ctx -> {
                                    BlockPos keel = BlockPosArgument.getLoadedBlockPos(ctx, "keel");
                                    var lines = com.cadykaya.interregnum.system.ferry.FerryDocket
                                            .of(ctx.getSource().getLevel(), keel);
                                    for (Component line : lines) {
                                        ctx.getSource().sendSuccess(() -> line, false);
                                    }
                                    return lines.size();
                                }))));

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
                                        .executes(ctx -> ferryCheck(ctx, null, false)))))
                .then(Commands.literal("home")
                        .then(Commands.argument("keel", BlockPosArgument.blockPos())
                                .executes(InterregnumCommand::ferryHome)))
                .then(Commands.literal("sail")
                        .then(Commands.argument("keel", BlockPosArgument.blockPos())
                                .then(Commands.argument("law", StringArgumentType.word())
                                        // Two arguments: sail to that world's dock.
                                        .executes(ctx -> ferryCheck(ctx, null, true))
                                        // Three: put it down exactly there. Kept for the
                                        // nudge, which moves a hull inside one world.
                                        .then(Commands.argument("pad", BlockPosArgument.blockPos())
                                                .executes(ctx -> ferryCheck(ctx,
                                                        BlockPosArgument.getLoadedBlockPos(ctx, "pad"),
                                                        true)))))));

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
