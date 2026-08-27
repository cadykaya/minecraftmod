package com.cadykaya.interregnum.data;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraft.data.loot.LootTableSubProvider;

import java.util.function.BiConsumer;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.content.loot.GodLivesCondition;
import com.cadykaya.interregnum.registry.ModItems;

/**
 * What is in a shrine's offering box.
 *
 * Two pools, and the split is the whole design:
 *
 *   1. OFFERINGS -- always. Bread, a candle, a little copper: the things people
 *      leave at a roadside shrine. Mundane on purpose. A player has to open enough
 *      of these to stop expecting anything.
 *
 *   2. THE HEART -- a 12% roll, and only while the god still lives. Loot tables
 *      roll when a container is first opened, so every shrine is a candidate until
 *      one of them pays out, and none of them are afterwards. No shrine is chosen
 *      in advance and nothing is tracked per-shrine; the heart is simply somewhere
 *      until it is taken, and then it is nowhere.
 *
 * At 12% a player opens ~8 shrines on average first, and at roughly six minutes of
 * walking per shrine that is a long Chapter 0 -- which is the intent. They are
 * meant to settle in before the world breaks. It can also happen on the first one,
 * and that is a better story, not a bug. **[NEEDS PLAYTEST]**
 */
public class ModChestLoot implements LootTableSubProvider {
    public static final ResourceKey<LootTable> SHRINE = ResourceKey.create(
            Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "chests/shrine"));

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> out) {
        out.accept(SHRINE, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 4.0F))
                        .add(LootItem.lootTableItem(Items.BREAD)
                                .setWeight(10)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                        .add(LootItem.lootTableItem(Items.CANDLE).setWeight(10))
                        .add(LootItem.lootTableItem(Items.BONE).setWeight(8))
                        .add(LootItem.lootTableItem(Items.WHEAT)
                                .setWeight(8)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                        .add(LootItem.lootTableItem(Items.COPPER_INGOT).setWeight(6))
                        .add(LootItem.lootTableItem(Items.AMETHYST_SHARD).setWeight(3)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(GodLivesCondition.godLives())
                        .when(LootItemRandomChanceCondition.randomChance(0.12F))
                        .add(LootItem.lootTableItem(ModItems.GOD_HEART.get()))));
    }
}
