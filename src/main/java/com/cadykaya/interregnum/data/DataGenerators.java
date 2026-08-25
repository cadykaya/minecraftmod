package com.cadykaya.interregnum.data;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;

import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.worldgen.ModWorldgen;

/**
 * Datagen entrypoint. Dev-time only -- this class never runs in a shipped game.
 *
 * Per docs/DATAGEN.md: if it is JSON under data/ or assets/, it is generated, not
 * typed. The generated output is committed, and CI regenerates and fails on a
 * dirty tree, because output that is older than its source produces no symptom at
 * all until much later.
 */
// NB: EventBusSubscriber in 26.2 has only value() and modid() -- there is no
// bus() parameter any more. Which bus a handler lands on is inferred from the
// event type itself, so GatherDataEvent.Server routes to the mod bus on its own.
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class DataGenerators {
    private DataGenerators() {}

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Server event) {
        DataGenerator gen = event.getGenerator();
        PackOutput out = gen.getPackOutput();

        // Worldgen: configured + placed features, emitted as datapack JSON.
        RegistrySetBuilder worldgen = new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, ModWorldgen::bootstrapConfigured)
                .add(Registries.PLACED_FEATURE, ModWorldgen::bootstrapPlaced)
                .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ModWorldgen::bootstrapBiomeModifiers);
        gen.addProvider(true, new DatapackBuiltinEntriesProvider(
                out, event.getLookupProvider(), worldgen, Set.of(Interregnum.MOD_ID)));

        gen.addProvider(true, new LootTableProvider(
                out,
                Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(
                        ModBlockLoot::new, LootContextParamSets.BLOCK)),
                event.getLookupProvider()));
    }
}
