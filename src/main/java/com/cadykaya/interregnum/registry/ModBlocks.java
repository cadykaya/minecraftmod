package com.cadykaya.interregnum.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.cadykaya.interregnum.Interregnum;

/**
 * Every block, in one place. Concentrating registration here is what makes a
 * version bump a handful of files rather than a repo-wide sweep -- see
 * docs/ARCHITECTURE.md "Why all registration lives in registry/".
 */
public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(Interregnum.MOD_ID);

    // Properties arrive pre-seeded with the block's registry id, so these take a
    // UnaryOperator rather than a fresh Properties -- building one by hand loses the
    // id and fails at registration. (Verified against the 26.2.0.67 sources, not
    // remembered: DeferredRegister.Blocks#registerSimpleBlock.)

    /** Worked masonry under a god's attention. Held: cool, regular, barely worn. */
    public static final DeferredBlock<Block> SHRINE_STONE = BLOCKS.registerSimpleBlock(
            "shrine_stone",
            p -> p.mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0F, 8.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE));

    /** The same masonry, carrying a band of the dead god's script. */
    public static final DeferredBlock<Block> SHRINE_STONE_CARVED = BLOCKS.registerSimpleBlock(
            "shrine_stone_carved",
            p -> p.mapColor(MapColor.COLOR_GRAY)
                    .strength(2.5F, 8.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
