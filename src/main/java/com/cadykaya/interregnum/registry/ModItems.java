package com.cadykaya.interregnum.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.cadykaya.interregnum.Interregnum;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Interregnum.MOD_ID);

    // Block items, so the blocks are obtainable and show in the tab.
    // registerSimpleBlockItem returns DeferredItem<BlockItem>, not <Item>.
    public static final DeferredItem<BlockItem> SHRINE_STONE =
            ITEMS.registerSimpleBlockItem(ModBlocks.SHRINE_STONE);
    public static final DeferredItem<BlockItem> SHRINE_STONE_CARVED =
            ITEMS.registerSimpleBlockItem(ModBlocks.SHRINE_STONE_CARVED);

    public static final DeferredItem<BlockItem> WARNING_STELE =
            ITEMS.registerSimpleBlockItem(ModBlocks.WARNING_STELE);

    public static final DeferredItem<BlockItem> WARDEN_STATUE =
            ITEMS.registerSimpleBlockItem(ModBlocks.WARDEN_STATUE);

    public static final DeferredItem<BlockItem> FERRY_KEEL =
            ITEMS.registerSimpleBlockItem(ModBlocks.FERRY_KEEL);

    /**
     * The heart. Deliberately named "A Warm Gold Thing" in en_us: in Chapter 0 the
     * player has no reason to know what it is, and the item telling them would give
     * away the only secret the opening has.
     */
    public static final DeferredItem<Item> GOD_HEART =
            ITEMS.registerSimpleItem("god_heart", p -> p.stacksTo(1).fireResistant());

    /** A shard of the shattered god. Finite; attuning one makes a Theoclast. */
    public static final DeferredItem<Item> CLAST =
            ITEMS.registerSimpleItem("clast", p -> p.stacksTo(16));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
