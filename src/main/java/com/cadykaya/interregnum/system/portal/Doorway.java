package com.cadykaya.interregnum.system.portal;

import com.cadykaya.interregnum.core.portal.Hour;
import com.cadykaya.interregnum.worldgen.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The Hearth-Turner's door, at the point where it touches the world.
 *
 * {@link Hour} is the rule; this is the stone.
 *
 * <h2>Nothing is remembered at all — not even a position</h2>
 *
 * The Anchorite's shaft keeps counters; the Verdant's door keeps a ledger of what was
 * planted. This keeps nothing. A doorway is a **shape the world is currently in**, and the
 * only question ever asked is whether the six blocks around a gap are all at the hour right
 * now. Take one out, or let the world age it one stage further, and the same question
 * answers no on the very next tick with nothing having been told.
 *
 * That is the closest any of the three comes to `WORLD.md`'s *"always present"*: there is
 * no portal object to create or destroy, only a wall that is sometimes a door.
 */
public final class Doorway {
    private Doorway() {}

    /**
     * The hour.
     *
     * `stone_bricks → cracked_stone_bricks → mossy_stone_bricks` is the Turning's own
     * table, and this is the middle link — <b>the only stage in it reachable from both
     * directions.</b> *Weather* ages a fresh frame into the hour; *Rewind* brings an old
     * one back to it. `WORLD.md` names both verbs for this door, and picking either end of
     * the chain would have made one of them a spare.
     *
     * Copper would have been the obvious material and it is deliberately not used: the
     * ageing table's own comment rules it out, because vanilla oxidises copper by random
     * tick everywhere and the two clocks would fight over the same blocks.
     */
    public static final Block AT_THE_HOUR = Blocks.CRACKED_STONE_BRICKS;

    /** Whether this level is one of the Hearth-Turner's two. */
    public static boolean holds(ServerLevel level) {
        ResourceKey<Level> here = level.dimension();
        return here == ModDimensions.TEMPORAL_AUTHORITY
                || here == ModDimensions.TEMPORAL_AUTHORITY_LOWER;
    }

    /** Where a door in this world leads. Null if this world has none. */
    public static ResourceKey<Level> beyond(ServerLevel level) {
        ResourceKey<Level> here = level.dimension();
        if (here == ModDimensions.TEMPORAL_AUTHORITY) {
            return ModDimensions.TEMPORAL_AUTHORITY_LOWER;
        }
        return here == ModDimensions.TEMPORAL_AUTHORITY_LOWER
                ? ModDimensions.TEMPORAL_AUTHORITY : null;
    }

    /**
     * Is the block somebody is standing in the gap of an open doorway?
     *
     * Checked at their FEET and, failing that, one block down — because the gap is two
     * blocks tall and a person standing in a doorway occupies both. Without the second
     * look, a doorway would only work if you were somehow standing in its lintel.
     */
    public static boolean inGap(ServerLevel level, BlockPos at) {
        return standing(level, at) || standing(level, at.below());
    }

    /** Is {@code feet} the lower half of an open doorway? */
    private static boolean standing(ServerLevel level, BlockPos feet) {
        for (Hour.Offset gap : Hour.GAP) {
            if (!level.getBlockState(at(feet, gap)).isAir()) {
                return false;
            }
        }
        // Either orientation. A wall runs the way it runs, and which axis it happens to
        // run on is not something a player should have to get right.
        return framed(level, feet, true) || framed(level, feet, false);
    }

    /** Is every block of the frame at the hour? */
    private static boolean framed(ServerLevel level, BlockPos feet, boolean alongX) {
        for (Hour.Offset o : Hour.frame(alongX)) {
            if (!level.getBlockState(at(feet, o)).is(AT_THE_HOUR)) {
                return false;
            }
        }
        return true;
    }

    private static BlockPos at(BlockPos feet, Hour.Offset o) {
        return feet.offset(o.dx(), o.dy(), o.dz());
    }

    /**
     * Take one thing through, and build the door it comes out of.
     *
     * <h2>The far side gets a frame, and it is not a convenience</h2>
     *
     * A door you can only walk through in one direction is not a door, and a player who
     * carried no stone brick would be stranded in a world with no ferry law naming it. The
     * two other portals do not have this problem — a shaft is a spell you can cast again,
     * and a grown door leaves a tree standing — but this one is six blocks somebody built,
     * and there is nothing on the far side that built anything.
     *
     * So the door is stamped where you arrive, at the hour, and the thing on the far side
     * is what you walked into. Which is also the most literal reading of `WORLD.md`'s
     * *"always present"*: the same door, from both sides.
     */
    public static Entity take(ServerLevel from, Entity entity) {
        ResourceKey<Level> to = beyond(from);
        if (to == null) {
            return null;
        }
        Passing.forget(entity.getUUID());
        Entity landed = Crossing.into(from, entity, to);
        if (landed == null) {
            return null;
        }
        if (landed.level() instanceof ServerLevel far) {
            stamp(far, landed.blockPosition());
        }
        // Marked as INSIDE on the far side rather than cleared, so the arrival tick is not
        // a rising edge. `enters` only fires on a threshold being crossed, and stepping
        // out of the far door is what re-arms it.
        Passing.arrived(landed.getUUID());
        return landed;
    }

    /**
     * Build a doorway around a position, at the hour.
     *
     * Orientation is arbitrary and fixed rather than random: a stamped door that came out
     * a different way round each time would make the far side unrecognisable, and the one
     * thing a person needs from an arrival is to know which way they came in.
     */
    public static void stamp(ServerLevel level, BlockPos feet) {
        for (Hour.Offset o : Hour.frame(true)) {
            level.setBlock(at(feet, o), AT_THE_HOUR.defaultBlockState(), 3);
        }
        for (Hour.Offset gap : Hour.GAP) {
            level.setBlock(at(feet, gap), Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
