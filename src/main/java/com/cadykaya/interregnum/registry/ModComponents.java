package com.cadykaya.interregnum.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.cadykaya.interregnum.Interregnum;

/**
 * Data components: facts an item stack carries about itself.
 *
 * Concentrated here for the same reason registration is, per docs/ARCHITECTURE.md.
 */
public final class ModComponents {
    private ModComponents() {}

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Interregnum.MOD_ID);

    /**
     * Which of the dead god's letters this stack is.
     *
     * A plain string id -- `verdant`, `quiet_one` -- resolved against the loaded post
     * when somebody reads it. Deliberately NOT the addressee: the item must never
     * carry `Rill`, because a stack in a hotbar is a string a player can see, and the
     * whole point is that the names are unheard until the letter is opened.
     * `tools/letters_check.py` would catch the name appearing in the lang file, and it
     * would not catch it appearing in a component -- so the rule is kept here, in the
     * one place that decides what an item knows about itself.
     *
     * Persistent AND network-synchronised: the client needs it to render a tooltip,
     * and a letter that forgot which letter it was on relog would be a lost questline.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> LETTER =
            COMPONENTS.register("letter", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}
