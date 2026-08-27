package com.cadykaya.interregnum.content.dialogue;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

import com.cadykaya.interregnum.Interregnum;

/**
 * Game-bus wiring for the dialogue loader. Server-side only -- conversations are
 * resolved on the logical server and pushed to clients, per docs/ARCHITECTURE.md
 * ("Game state changes on the logical server. Always.").
 *
 * The event is AddServerReloadListenersEvent, and it takes an Identifier key used
 * for dependency sorting. (Signature read from the 26.2.0.67 sources; the older
 * AddReloadListenerEvent name no longer exists.)
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class DialogueEvents {
    private DialogueEvents() {}

    private static final Identifier DIALOGUE_LISTENER =
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "dialogue");

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(DIALOGUE_LISTENER, new DialogueLoader());
    }
}
