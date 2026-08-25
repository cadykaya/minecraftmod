package com.cadykaya.interregnum.system;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.core.chapter.Milestone;

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

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("interregnum")
                .then(Commands.literal("status").executes(ctx -> {
                    ChapterSavedData data = ChapterSavedData.get(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "chapter=" + data.chapter()
                                    + " band=" + data.band()
                                    + " dormant=" + data.mechanicsDormant()
                                    + " letters=" + data.lettersDelivered()), false);
                    return 1;
                }));

        // One `record` node with a child per milestone. Permission style is
        // `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)` in 26.2 --
        // `source.hasPermission(int)` no longer exists; permissions moved to a
        // PermissionSet/PermissionCheck model. Read from the vanilla commands, not
        // remembered.
        LiteralArgumentBuilder<CommandSourceStack> record = Commands.literal("record")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        for (Milestone m : Milestone.values()) {
            record = record.then(Commands.literal(m.name().toLowerCase(java.util.Locale.ROOT))
                    .executes(ctx -> {
                        ChapterSavedData data = ChapterSavedData.get(ctx.getSource().getServer());
                        boolean isNew = data.record(m);
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                (isNew ? "recorded " : "already recorded ") + m.name()
                                        + "; chapter=" + data.chapter()), true);
                        return 1;
                    }));
        }
        event.getDispatcher().register(root.then(record));
    }
}
