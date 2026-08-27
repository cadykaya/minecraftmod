package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.Interregnum;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Zones do not outlive the server that held them.
 *
 * {@link Zones} keeps its map statically, which is right for something deliberately not
 * persisted -- and wrong the moment two worlds share a JVM, as they do in a development
 * run. A zone left over from the last world would apply to the next one at coordinates
 * that mean something else entirely, and the symptom would be gravel floating somewhere
 * nobody cast anything.
 *
 * Cheap, and the kind of thing that is obvious only after it has happened once.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class ZoneEvents {
    private ZoneEvents() {}

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Zones.clear();
    }
}
