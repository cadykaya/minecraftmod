package com.cadykaya.interregnum.system.claim;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import com.cadykaya.interregnum.registry.ModAttachments;

/**
 * Asking whether a position is somebody's work.
 *
 * One place, so the answer cannot differ between the crater, the unraveling, and
 * whatever asks next. Every caller is about to do something irreversible to a
 * block, and this is the question that decides whether they may.
 */
public final class Claims {
    private Claims() {}

    /** Record that a player put a block here. */
    public static void record(LevelReader level, BlockPos pos) {
        ChunkAccess chunk = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false);
        if (chunk == null) {
            return;
        }
        chunk.getData(ModAttachments.PLACED_BLOCKS.get()).add(pos, chunk.getMinY());
        chunk.markUnsaved();
    }

    /** Forget a position, because whatever was there has been broken. */
    public static void forget(LevelReader level, BlockPos pos) {
        ChunkAccess chunk = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false);
        if (chunk == null || !chunk.hasData(ModAttachments.PLACED_BLOCKS.get())) {
            return;
        }
        chunk.getData(ModAttachments.PLACED_BLOCKS.get()).remove(pos, chunk.getMinY());
        chunk.markUnsaved();
    }

    /**
     * Did a person put this here?
     *
     * Fails CLOSED: an unloaded chunk answers "yes, leave it alone". The unraveling
     * has no business touching ground nobody is looking at anyway, and the cost of
     * being wrong in this direction is a block that does not decay, against a block
     * that a person made and can never get back.
     */
    public static boolean isClaimed(LevelReader level, BlockPos pos) {
        ChunkAccess chunk = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false);
        if (chunk == null) {
            return true;
        }
        if (!chunk.hasData(ModAttachments.PLACED_BLOCKS.get())) {
            return false;      // nobody has ever built here; do not allocate one to ask
        }
        return chunk.getData(ModAttachments.PLACED_BLOCKS.get()).contains(pos, chunk.getMinY());
    }

    /** For diagnostics: how many placements this chunk remembers. */
    public static int count(LevelReader level, BlockPos pos) {
        ChunkAccess chunk = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false);
        if (chunk == null || !chunk.hasData(ModAttachments.PLACED_BLOCKS.get())) {
            return 0;
        }
        return chunk.getData(ModAttachments.PLACED_BLOCKS.get()).size();
    }
}
