package com.cadykaya.interregnum.system.portal;

import com.cadykaya.interregnum.core.magic.Zone;
import com.cadykaya.interregnum.core.portal.Stillness;
import com.cadykaya.interregnum.system.magic.Zones;
import com.cadykaya.interregnum.worldgen.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Quiet One's door, at the point where it touches the world.
 *
 * {@link Stillness} is the rule; this is the place it is asked about.
 *
 * <h2>The only door with no second condition</h2>
 *
 * The other three ask something of the thing standing in them — let go, hold still, cross
 * the threshold. This asks nothing of anybody, because what it is measuring is not a
 * person. A silence is a fact about a place, so the door is open or it is not, and
 * everything inside it goes.
 *
 * That difference is the god. The Verdant's door answers about you; this one answers about
 * the world, which means somebody else's footstep closes yours and yours closes theirs.
 */
public final class Silence {
    private Silence() {}

    /** Whether this level is one of the Quiet One's two. */
    public static boolean holds(ServerLevel level) {
        ResourceKey<Level> here = level.dimension();
        return here == ModDimensions.UNRESPONSIVE
                || here == ModDimensions.UNRESPONSIVE_LOWER;
    }

    /** Where a door in this world leads. Null if this world has none. */
    public static ResourceKey<Level> beyond(ServerLevel level) {
        ResourceKey<Level> here = level.dimension();
        if (here == ModDimensions.UNRESPONSIVE) {
            return ModDimensions.UNRESPONSIVE_LOWER;
        }
        return here == ModDimensions.UNRESPONSIVE_LOWER
                ? ModDimensions.UNRESPONSIVE : null;
    }

    /** Every silence covering a position. */
    public static List<Zone> around(ServerLevel level, BlockPos pos) {
        return holds(level) ? Zones.zonesCovering(level, Stillness.KEY, pos) : List.of();
    }

    /**
     * Something happened here.
     *
     * Called for every vanilla game event in one of this god's worlds. A noise disturbs
     * EVERY silence it falls inside, not merely the nearest -- overlapping casts are one
     * region as far as the world is concerned, and forgiving a door because another door
     * was closer would make a wall of overlapping zones quieter than a single one.
     */
    public static int broke(ServerLevel level, BlockPos where) {
        List<Zone> zones = around(level, where);
        for (Zone zone : zones) {
            Hushed.broken(level, zone, level.getGameTime());
        }
        return zones.size();
    }

    /**
     * Is a door open here?
     *
     * Any silence covering the position that has held long enough. Overlapping zones are
     * generous in this direction and strict in the other -- a noise breaks all of them, and
     * any one of them being quiet is enough -- which is the right way round: casting twice
     * should help, and it should not buy you a quieter world than casting once.
     */
    public static boolean open(ServerLevel level, BlockPos pos) {
        long now = level.getGameTime();
        for (Zone zone : around(level, pos)) {
            if (Stillness.quiet(Hushed.since(level, zone), now)) {
                return true;
            }
        }
        return false;
    }

    /**
     * How long the quietest -- that is, the longest-held -- silence here has stood.
     *
     * For the command seam, and it reports the BEST of the overlapping zones so the number
     * agrees with {@link #open}. A reporting seam that disagreed with the door would send
     * whoever read it looking in the wrong place.
     */
    public static long held(ServerLevel level, BlockPos pos) {
        long now = level.getGameTime();
        long best = -1;
        for (Zone zone : around(level, pos)) {
            best = Math.max(best, now - Hushed.since(level, zone));
        }
        return best;
    }

    /**
     * Take one thing through.
     *
     * @return the thing on the far side, or null. The reference passed in is dead
     *         afterwards -- see {@link Crossing#into}.
     */
    public static Entity take(ServerLevel from, Entity entity) {
        ResourceKey<Level> to = beyond(from);
        return to == null ? null : Crossing.into(from, entity, to);
    }
}
