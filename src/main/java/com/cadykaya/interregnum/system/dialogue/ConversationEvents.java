package com.cadykaya.interregnum.system.dialogue;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import com.cadykaya.interregnum.Interregnum;

/**
 * The clock and the door for live conversations.
 *
 * Three hooks, each closing a way a table can be left hanging: a server that
 * restarts with tables in memory, a player who logs out mid-sentence, and a player
 * who simply stops answering.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class ConversationEvents {
    private ConversationEvents() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // Conversations are not saved, so nothing may be carried over from a
        // previous run of the same JVM either -- in a dev environment that is
        // exactly how stale state survives into a "fresh" world.
        Conversations.reset();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Conversations.tick(event.getServer());
    }

    /**
     * Sleep is when the dead god can reach its killer.
     *
     * On WAKING rather than on lying down: the conversation needs somebody conscious
     * enough to answer it, and Minecraft's sleep is a skip rather than a duration.
     * What the player gets is the dream they just had, which is how dream scenes
     * work everywhere else too.
     *
     * Three lines, because every decision is in {@link TheHaunt#offer} where a
     * command can reach it -- a headless server has no sleeping players and this
     * handler would otherwise be the only path to a [LOCKED] headline beat.
     */
    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.level().getServer() != null) {
            TheHaunt.offer(player.level().getServer(), player.getUUID(), false);
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // Immediately, not on the timeout: a player who has disconnected is not
        // thinking about their answer, and the rest of the table should not spend a
        // minute finding that out.
        if (event.getEntity() instanceof ServerPlayer player) {
            Conversations.leave(player.level().getServer(), player.getUUID().toString());
        }
    }
}
