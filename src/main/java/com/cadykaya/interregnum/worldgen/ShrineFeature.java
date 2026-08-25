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
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.ChestBlock;

import com.cadykaya.interregnum.data.ModChestLoot;

import com.cadykaya.interregnum.core.spatial.Facing;
import com.cadykaya.interregnum.registry.ModBlocks;
import com.cadykaya.interregnum.registry.ModEntities;
import com.cadykaya.interregnum.content.entity.ShrineKeeperEntity;
import net.minecraft.world.entity.EntitySpawnReason;

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
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

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

        // Clear the air above the centre so the shrine is visible from above -- one
        // buried under a bush is one nobody finds.
        for (int h = 1; h <= 3; h++) {
            BlockPos p = new BlockPos(cx, floorY + h, cz);
            BlockState s = level.getBlockState(p);
            if (!s.isAir() && s.canBeReplaced()) {
                setBlock(level, p, Blocks.AIR.defaultBlockState());
            }
        }

        // The offering box, standing on the inscribed stone in the middle of the
        // court. Deliberately obvious. A shrine you have to dig up is a puzzle; this
        // has to read as "there is a chest here", because the opening of this mod is
        // a player doing the most ordinary thing in Minecraft and it being deicide.
        BlockPos boxPos = new BlockPos(cx, floorY + 1, cz);
        setBlock(level, boxPos, Blocks.CHEST.defaultBlockState()
                .setValue(ChestBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(rand)));
        RandomizableContainer.setBlockEntityLootTable(level, rand, boxPos, ModChestLoot.SHRINE);

        placeKeeper(level, rand, cx, cz, floorY);

        return placedAnything;
    }

    /**
     * Somebody to tend it.
     *
     * One keeper per shrine, from worldgen, because `WORLD.md` has villagers tending
     * shrines as ordinary Chapter 0 life -- they are scenery a player walks past for
     * hours, exactly like the Warden statues, and exactly like the statues that is
     * what makes the moment everything changes land.
     *
     * The spot is chosen rather than fixed: the court has missing paving by design,
     * so the first candidate tile with solid footing and two blocks of headroom wins
     * and the shrine gets nobody if none of them do. A keeper standing in a wall or
     * hovering over a cave mouth is worse than a shrine with no keeper.
     *
     * They face the offering box, because that is what they are here for and it is
     * the first thing that tells a player these two things go together.
     */
    private static void placeKeeper(WorldGenLevel level, RandomSource rand,
                                    int cx, int cz, int floorY) {
        // Edges before corners: beside the box reads as attending it, while a corner
        // reads as loitering.
        int[][] spots = {{1, 0}, {-1, 0}, {0, 1}, {0, -1},
                         {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
        for (int[] d : spots) {
            BlockPos stand = new BlockPos(cx + d[0], floorY + 1, cz + d[1]);
            if (!level.getBlockState(stand.below()).isSolid()
                    || !level.getBlockState(stand).isAir()
                    || !level.getBlockState(stand.above()).isAir()) {
                continue;
            }
            ShrineKeeperEntity keeper = ModEntities.SHRINE_KEEPER.get()
                    .create(level.getLevel(), EntitySpawnReason.STRUCTURE);
            if (keeper == null) {
                return;
            }
            // Facing the box: the offset points away from the centre, so the
            // vector to look along is its negation. The arithmetic lives in
            // core/spatial/Facing where it can be tested -- a mob's yaw is
            // overwritten by whatever it looks at next, so there is no way to
            // assert this on a live entity after the fact.
            // (`moveTo` is `snapTo` in 26.2; the BlockPos overload bottom-centres.)
            float facing = Facing.yawToward(-d[0], -d[1]);
            keeper.snapTo(stand, facing, 0.0F);
            // Tethered to the court. Without this they stroll away from the shrine
            // they exist to attend -- and, being persistent, never come back.
            keeper.setHomeTo(new BlockPos(cx, floorY + 1, cz), ShrineKeeperEntity.TETHER);
            keeper.setPersistenceRequired();
            // The return value is NOT decorative. `addFreshEntity` answers false when
            // the level declines the entity -- most plausibly because the chunk is
            // not in a state to accept one at this instant during generation -- and
            // ignoring it produces a shrine with no keeper, silently, with nothing
            // in any log to say so. That is the exact shape of failure this project
            // keeps finding, so it gets a line.
            if (!level.addFreshEntity(keeper)) {
                LOG.warn("The shrine at {},{} could not seat its keeper: the level "
                        + "refused the entity at {}.", cx, cz, stand);
            }
            return;
        }
    }
}
