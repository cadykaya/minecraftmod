package com.cadykaya.interregnum.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Set;

import com.cadykaya.interregnum.registry.ModBlocks;

/**
 * Block drops.
 *
 * A block with no loot table drops NOTHING, silently -- the single most common
 * "my block vanished" report in modding, and `tools/registry_check.py` was written
 * to catch exactly that. It found all three of ours.
 *
 * Chapter 0 note: shrine stone and steles drop themselves. They are the god's
 * masonry and there is an argument for making them unobtainable so a player cannot
 * farm a shrine into a house -- flagged for the owner in docs/HANDOFF.md. Dropping
 * themselves is the reversible default; making them precious later is a one-line
 * change here, whereas shipping them unobtainable and disappointing someone who
 * mined one is not.
 */
public class ModBlockLoot extends BlockLootSubProvider {
    public ModBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
    }

    @Override
    protected void generate() {
        // Plain masonry: yours if you want it. The world does not stop you taking
        // the god's things -- that permissiveness IS the opening, and a shrine you
        // are forbidden to touch could never have been looted in the first place.
        dropSelf(ModBlocks.SHRINE_STONE.get());

        // But the carved stone drops PLAIN stone. You may take the stone; you may
        // never take the word. Nothing announces this: a player mines an inscribed
        // block, gets an uninscribed one, and learns -- before any lore exists --
        // that the script is not a decoration and not a material.
        //
        // It also makes carved stone genuinely finite: found only, never made. The
        // ability to inscribe is then a real Theoclast reward later, because by
        // then the player has spent hours knowing they cannot.
        dropOther(ModBlocks.SHRINE_STONE_CARVED.get(), ModBlocks.SHRINE_STONE.get());

        // A warning you can carry is a warning you can misplace. On a server that
        // is a genuinely interesting thing for someone to do, and it stays legal.
        dropSelf(ModBlocks.WARNING_STELE.get());

        // Statues drop themselves, and they drop the SAME item whether awake or
        // asleep. Whether a Warden's eyes are open is a fact about the world, not
        // about the block -- re-place one after the death and it is awake again,
        // because the god is still dead.
        dropSelf(ModBlocks.WARDEN_STATUE.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        // Only OUR blocks. The default walks the whole block registry, which would
        // make this provider assert on every vanilla block we never touched.
        return ModBlocks.BLOCKS.getEntries().stream().map(e -> (Block) e.value()).toList();
    }
}
