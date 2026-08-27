package com.cadykaya.interregnum.system.unraveling;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import com.cadykaya.interregnum.Interregnum;

/**
 * Game-bus wiring for the unraveling: the datapack listener, and the clock.
 *
 * `LevelTickEvent.Post` fires on both logical sides, so the first thing the handler
 * does is establish it is on the server. Nothing about the unraveling may be
 * decided on a client -- a client that disagreed would show a player a world that
 * is not there.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class UnravelingEvents {
    private UnravelingEvents() {}

    private static final Identifier UNRAVELING_LISTENER =
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "unraveling");

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(UNRAVELING_LISTENER, new UnravelingLoader());
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            Unraveling.tick(level);
        }
    }
}
