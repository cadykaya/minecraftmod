package com.cadykaya.interregnum.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.util.RandomSource;
import net.minecraft.core.Direction;

import com.cadykaya.interregnum.registry.ModBlocks;

/**
 * A wayside shrine: a small paved square with a stele at each corner and one
 * inscribed stone at the centre where the heart sits.
 *
 * Built in code rather than from an NBT template, deliberately. An NBT template
 * has to be built by hand in a running client, which is not something this project
 * can do or review in a diff; a coded feature is art-as-code like everything else
 * here, and it can respond to the terrain it lands on instead of stamping a fixed
 * block of stone into a hillside.
 *
 * Chapter 0 contract: this places NOTHING but our own blocks, and only where the
 * ground is solid and roughly level. It never replaces a block the player put
 * there -- worldgen runs before players exist, but the rule is written down because
 * a later "regenerate shrines" tool would be exactly where it gets broken.
 */
public class ShrineFeature extends Feature<NoneFeatureConfiguration> {
    /** Half-width of the paving. 2 gives a 5x5 court, which reads at a distance
     *  without dominating a hillside. */
    private static final int RADIUS = 2;
    /** How level the ground must be across the footprint before we build. */
    private static final int MAX_RELIEF = 2;
    /** How far above/below the centre's surface the other columns may be searched. */
    private static final int SEARCH_SLACK = 12;

    /**
     * First solid block at or below {@code fromY}, scanning down to {@code toY}.
     *
     * Deliberately NOT a heightmap lookup. The worldgen heightmaps
     * (WORLD_SURFACE_WG, OCEAN_FLOOR_WG) are only primed during generation, so a
     * feature that reads one throws "Unprimed heightmap" the moment it is invoked
     * on a live chunk -- which means it cannot be run with `/place`, and therefore
     * cannot be verified without a client. Scanning works in both contexts, so the
     * shrine is testable and `/place` stays a first-class way to use it.
     *
     * @return the Y of the topmost solid block, or {@link Integer#MIN_VALUE}.
     */
    private static int findSurface(WorldGenLevel level, int x, int z, int fromY, int toY) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = fromY; y >= toY; y--) {
            if (level.getBlockState(p.set(x, y, z)).isSolid()) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    public ShrineFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rand = ctx.random();
        BlockPos origin = ctx.origin();

        int cx = origin.getX();
        int cz = origin.getZ();

        // Find the centre column first, scanning the whole build range once, then
        // search the other 24 columns in a narrow band around it. Correct either
        // way; this keeps a rare feature from doing 25 full-height scans.
        int centreY = findSurface(level, cx, cz, level.getMaxY(), level.getMinY());
        if (centreY == Integer.MIN_VALUE) {
            return false;
        }

        // Reject broken ground rather than terracing it. A shrine cut into a cliff
        // looks like a bug; a shrine that simply is not there looks like nothing.
        int lo = centreY, hi = centreY;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                int h = findSurface(level, cx + dx, cz + dz,
                        centreY + SEARCH_SLACK, centreY - SEARCH_SLACK);
                if (h == Integer.MIN_VALUE) {
                    return false;          // a hole in the footprint: not a shrine site
                }
                lo = Math.min(lo, h);
                hi = Math.max(hi, h);
            }
        }
        if (hi - lo > MAX_RELIEF) {
            return false;
        }

        BlockState paving = ModBlocks.SHRINE_STONE.get().defaultBlockState();
        BlockState carved = ModBlocks.SHRINE_STONE_CARVED.get().defaultBlockState();
        BlockState stele = ModBlocks.WARNING_STELE.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);

        // findSurface returns the topmost SOLID block, so paving replaces it rather
        // than stacking on top: a shrine sunk flush into the ground, not perched.
        int floorY = lo;
        boolean placedAnything = false;

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                BlockPos p = new BlockPos(cx + dx, floorY, cz + dz);
                if (!level.getBlockState(p.below()).isSolid()) {
                    continue;                      // never pave over a void or a cave mouth
                }
                boolean corner = Math.abs(dx) == RADIUS && Math.abs(dz) == RADIUS;
                boolean centre = dx == 0 && dz == 0;

                // A shrine nobody has maintained since the Isolation. Missing paving
                // is the cheapest possible "this is old", and it must never eat the
                // centre stone or the player loses the thing the shrine is FOR.
                if (!centre && !corner && rand.nextFloat() < 0.18F) {
                    continue;
                }

                setBlock(level, p, centre ? carved : paving);
                placedAnything = true;

                if (corner) {
                    // Steles lean toward ruin: most stand, some have fallen away.
                    int height = rand.nextFloat() < 0.25F ? 1 : 2;
                    for (int h = 1; h <= height; h++) {
                        BlockPos sp = p.above(h);
                        if (level.getBlockState(sp).isAir()
                                || level.getBlockState(sp).canBeReplaced()) {
                            setBlock(level, sp, stele);
                        }
                    }
                }
            }
        }

        // Clear the air directly above the centre so the inscribed stone is visible
        // from above -- a shrine buried under a bush is a shrine nobody finds.
        for (int h = 1; h <= 3; h++) {
            BlockPos p = new BlockPos(cx, floorY + h, cz);
            BlockState s = level.getBlockState(p);
            if (!s.isAir() && s.canBeReplaced()) {
                setBlock(level, p, Blocks.AIR.defaultBlockState());
            }
        }
        return placedAnything;
    }
}
