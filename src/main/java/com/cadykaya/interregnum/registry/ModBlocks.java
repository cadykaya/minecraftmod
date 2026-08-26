package com.cadykaya.interregnum.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;

import com.cadykaya.interregnum.content.block.WardenStatueBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
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

    /**
     * The warning steles. Chapter 0 dressing that players read as ruin flavour for
     * hours, and which after the death is the only instruction anyone left behind.
     * A rotatable pillar so a builder can lay one on its side without it looking
     * wrong -- and so a toppled one reads as toppled.
     */
    public static final DeferredBlock<Block> WARNING_STELE = BLOCKS.register(
            "warning_stele",
            key -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .setId(net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(3.0F, 9.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    /** A Warden, standing still. See WardenStatueBlock for why that matters. */
    public static final DeferredBlock<WardenStatueBlock> WARDEN_STATUE = BLOCKS.register(
            "warden_statue",
            key -> new WardenStatueBlock(BlockBehaviour.Properties.of()
                    .setId(net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.BLOCK, key))
                    .mapColor(MapColor.METAL)
                    .strength(4.0F, 12.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    /**
     * The mail-ferry's keel. See {@link com.cadykaya.interregnum.system.ferry.Ferry}
     * for what it does; this is only what it IS.
     *
     * Wood, not the god's masonry -- and deliberately cheap to break. The keel is
     * the one piece of infrastructure in the mod that a player builds for
     * themselves, and a keel that resisted a hand would read as furniture placed
     * by somebody else. It is also the only block whose position is load-bearing:
     * the ferry captures from here, so it must be findable at a glance, which is
     * what the brass ring on the top face is for.
     */
    public static final DeferredBlock<com.cadykaya.interregnum.content.block.FerryKeelBlock>
            FERRY_KEEL = BLOCKS.register(
            "ferry_keel",
            key -> new com.cadykaya.interregnum.content.block.FerryKeelBlock(
                    BlockBehaviour.Properties.of()
                            .setId(net.minecraft.resources.ResourceKey.create(
                                    net.minecraft.core.registries.Registries.BLOCK, key))
                            .mapColor(MapColor.WOOD)
                            .strength(2.0F, 3.0F)
                            .sound(SoundType.WOOD)));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
