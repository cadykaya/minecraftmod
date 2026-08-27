package com.cadykaya.interregnum.system.letters;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

import com.cadykaya.interregnum.Interregnum;

/** Game-bus wiring for the mail. */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class LetterEvents {
    private LetterEvents() {}

    private static final Identifier LETTERS_LISTENER =
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "letters");

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(LETTERS_LISTENER, new Letters());
    }
}
