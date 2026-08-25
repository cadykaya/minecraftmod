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

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("interregnum")
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

        for (Milestone m : Milestone.values()) {
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
