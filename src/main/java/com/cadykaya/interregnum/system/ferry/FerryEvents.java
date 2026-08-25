package com.cadykaya.interregnum.system.ferry;

import com.cadykaya.interregnum.Interregnum;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

/** Datapack wiring for the crossing laws. */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class FerryEvents {
    private FerryEvents() {}

    private static final Identifier FERRY_LISTENER =
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "ferry");

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(FERRY_LISTENER, new FerryLaws());
    }
}
