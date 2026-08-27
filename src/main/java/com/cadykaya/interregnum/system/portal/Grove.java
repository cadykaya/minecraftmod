package com.cadykaya.interregnum.system.portal;

import com.cadykaya.interregnum.core.portal.Rooting;
import com.cadykaya.interregnum.worldgen.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * The Verdant's grown door, at the point where it touches the world.
 *
 * {@link Rooting} is the rule; this is the ground it is read off.
 *
 * <h2>Nothing here is remembered except the position</h2>
 *
 * {@link Plantings} holds where somebody planted. Everything else — seeded, open, gone —
 * is asked of the blocks every time, which is what makes `WORLD.md`'s *"closes when cut"*
 * a fact rather than a promise kept by a listener. Fell the tree by any means at all,
 * including ones the game does not report as breaking a block, and the next question
 * about that position answers GONE.
 */
public final class Grove {
    private Grove() {}

    /**
     * A tree, by the game's own definition of one.
     *
     * Tags rather than a list of blocks, and for the reason {@code Verdant} gives about
     * growth: a hand-written list of logs would be out of date the first time Mojang
     * shipped a wood type, and this door has to work for whatever a player found to plant.
     *
     * `SAPLINGS` is not in `BlockTags` in 26.x -- it lives on `BlockItemTags` and is
     * narrowed with `.block()`. Looked up rather than assumed; the `BlockTags` constant a
     * pre-26 guide would have you write does not exist and does not compile.
     */
    private static final TagKey<Block> SAPLINGS = BlockItemTags.SAPLINGS.block();

    /** Whether this level is one of the Verdant's two. */
    public static boolean holds(ServerLevel level) {
        ResourceKey<Level> here = level.dimension();
        return here == ModDimensions.GREEN_AUTHORITY
                || here == ModDimensions.GREEN_AUTHORITY_LOWER;
    }

    /** Where a door in this world leads. Null if this world has none. */
    public static ResourceKey<Level> beyond(ServerLevel level) {
        ResourceKey<Level> here = level.dimension();
        if (here == ModDimensions.GREEN_AUTHORITY) {
            return ModDimensions.GREEN_AUTHORITY_LOWER;
        }
        return here == ModDimensions.GREEN_AUTHORITY_LOWER
                ? ModDimensions.GREEN_AUTHORITY : null;
    }

    /**
     * What stands at a planted position, and drop it from the ledger if nothing does.
     *
     * The sweep lives here rather than on a timer, so the cost of housekeeping is paid by
     * the read that benefits from it -- the same arrangement {@code Zones} uses, and for
     * the same reason: a second thing on its own clock is a second thing to keep in step.
     */
    public static Rooting.State state(ServerLevel level, BlockPos planted) {
        var here = level.getBlockState(planted);
        Rooting.State state = Rooting.state(here.is(BlockTags.LOGS), here.is(SAPLINGS));
        if (state == Rooting.State.GONE) {
            Plantings.get(level).forget(planted);
        }
        return state;
    }

    /**
     * Is this position within reach of an open door?
     *
     * Chebyshev against every planted position in this world, which is the same way every
     * other region in this mod is measured and is cheap for the reason that matters: the
     * ledger holds doors somebody deliberately grew, and there are never many.
     *
     * @return the trunk it is standing under, or null.
     */
    public static BlockPos openNear(ServerLevel level, BlockPos pos) {
        if (!holds(level)) {
            return null;
        }
        for (BlockPos planted : Plantings.get(level).all()) {
            if (Math.abs(pos.getX() - planted.getX()) > Rooting.REACH
                    || Math.abs(pos.getZ() - planted.getZ()) > Rooting.REACH
                    || Math.abs(pos.getY() - planted.getY()) > Rooting.REACH) {
                continue;
            }
            if (state(level, planted) == Rooting.State.OPEN) {
                return planted;
            }
        }
        return null;
    }

    /**
     * Push every planted door inside a volume, ignoring the claim ledger.
     *
     * <h2>The one place a spell reaches past the ledger, and it has a name</h2>
     *
     * {@code Wildgrowth} sweeps a cube and spares what a player put there, because
     * `LESSONS.md` #35's rule is that <b>the ledger gates what you did not aim at</b> and a
     * cube is full of things nobody aimed at. Correct, and it collided head-on with this
     * portal the moment both existed: a sapling somebody planted IS claimed, so the
     * school's own verb would step over the one thing the caster was aiming at, and
     * `WORLD.md`'s locked rule — *each god's portal is opened by the school that god
     * teaches* — would be false in the only case it is about.
     *
     * The resolution is the existing rule read properly rather than an exception to it. A
     * planted door is not somebody's work you happened to sweep; it is a position a person
     * deliberately registered as a door, in a ledger that exists for that. Nothing else in
     * the cube loses its protection: a greenhouse is still spared, and this reaches
     * <em>only</em> positions {@link Plantings} holds.
     *
     * @return how many doors were pushed.
     */
    public static int ripen(ServerLevel level, BlockPos centre, int radius, int pushes) {
        if (!holds(level)) {
            return 0;
        }
        int pushed = 0;
        for (BlockPos planted : Plantings.get(level).all()) {
            if (Math.abs(centre.getX() - planted.getX()) > radius
                    || Math.abs(centre.getY() - planted.getY()) > radius
                    || Math.abs(centre.getZ() - planted.getZ()) > radius) {
                continue;
            }
            for (int n = 0; n < pushes; n++) {
                var state = level.getBlockState(planted);
                // Asked every push, the same way `Verdant.quicken` asks: a sapling that
                // has become a trunk is done, and a position that stopped being growable
                // must stop costing anything.
                if (!state.isRandomlyTicking()) {
                    break;
                }
                state.randomTick(level, planted, level.getRandom());
            }
            pushed++;
        }
        return pushed;
    }

    /** Whether a block being placed here is somebody planting a door. */
    public static boolean isPlanting(ServerLevel level, BlockPos pos) {
        return holds(level) && level.getBlockState(pos).is(SAPLINGS);
    }

    /**
     * Take one thing through.
     *
     * @return the thing on the far side, or null. The reference passed in is dead
     *         afterwards -- see {@link Crossing#into}.
     */
    public static Entity take(ServerLevel from, Entity entity) {
        ResourceKey<Level> to = beyond(from);
        if (to == null) {
            return null;
        }
        // Cleared before, for the reason Shaft.take gives: the UUID survives the crossing,
        // and something arriving with a full count would go straight back.
        Resting.forget(entity.getUUID());
        Entity landed = Crossing.into(from, entity, to);
        if (landed != null) {
            Resting.forget(landed.getUUID());
        }
        return landed;
    }
}
