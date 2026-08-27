package com.cadykaya.interregnum.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.content.loot.GodLivesCondition;

public final class ModLootConditions {
    private ModLootConditions() {}

    public static final DeferredRegister<MapCodec<? extends LootItemCondition>> CONDITIONS =
            DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, Interregnum.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends LootItemCondition>,
            MapCodec<GodLivesCondition>> GOD_LIVES =
            CONDITIONS.register("god_lives", () -> GodLivesCondition.MAP_CODEC);

    public static void register(IEventBus modBus) {
        CONDITIONS.register(modBus);
    }
}
