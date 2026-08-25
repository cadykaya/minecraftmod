package com.cadykaya.interregnum.system;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import com.cadykaya.interregnum.content.block.WardenStatueBlock;

/**
 * Opening one chunk's worth of eyes.
 *
 * Scans by SECTION and skips any section whose palette does not contain a statue,
 * which is almost all of them. A naive 16x16x384 loop per chunk would be ~98,000
 * block lookups on every chunk load, forever; the palette check turns that into a
 * handful of comparisons for the overwhelming majority of chunks.
 */
public final class WardenWake {
    private WardenWake() {}

    public static int wakeChunk(ServerLevel level, LevelChunk chunk) {
        int woken = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinY();
        LevelChunkSection[] sections = chunk.getSections();

        for (int si = 0; si < sections.length; si++) {
            LevelChunkSection section = sections[si];
            if (section == null || section.hasOnlyAir()) {
                continue;
            }
            // Cheap rejection: does this section's palette mention a statue at all?
            if (!section.maybeHas(s -> s.getBlock() instanceof WardenStatueBlock)) {
                continue;
            }
            int baseY = minY + (si << 4);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (!(state.getBlock() instanceof WardenStatueBlock)
                                || state.getValue(WardenStatueBlock.WOKEN)) {
                            continue;
                        }
                        pos.set(chunk.getPos().getMinBlockX() + x, baseY + y,
                                chunk.getPos().getMinBlockZ() + z);
                        if (WardenStatueBlock.wake(level, pos, state)) {
                            woken++;
                        }
                    }
                }
            }
        }
        return woken;
    }
}
