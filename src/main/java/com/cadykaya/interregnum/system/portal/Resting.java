package com.cadykaya.interregnum.system.portal;

import com.cadykaya.interregnum.core.portal.Rooting;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * How long each thing has been standing still under a grown door, and where it was.
 *
 * <h2>Two things, one map, because they are one question</h2>
 *
 * Stillness cannot be read off an entity: the game records where something is, not whether
 * it went anywhere. So this keeps last tick's block position beside the count, and
 * "moved" is the comparison between them.
 *
 * Block position rather than exact position, deliberately. Turning on the spot, shuffling,
 * and the ordinary drift of standing on a slope all have to count as still, or the door is
 * one nobody can hold open -- see {@link Rooting#rest}.
 *
 * <h2>In memory, like the Anchorite's counters and unlike its ledger</h2>
 *
 * The DOOR is persisted ({@link Plantings}); standing under it is not. Three seconds of
 * having not moved yet is not a fact about the world, and a count restored from disk would
 * be a count about somebody who logged out.
 */
public final class Resting {
    private Resting() {}

    private record Held(int ticks, BlockPos where) {}

    private static final Map<UUID, Held> HELD = new HashMap<>();

    /**
     * Record one tick, and say whether the door has taken them.
     *
     * The first tick under a door is never still: there is nothing to compare against, so
     * the position is remembered and the count starts at zero. That costs one tick out of
     * sixty and is the honest answer -- inventing a "did not move" for a thing whose
     * previous position is unknown would let something that teleported in count as having
     * stood there.
     */
    public static boolean tick(UUID who, boolean under, BlockPos now) {
        Held was = HELD.get(who);
        boolean moved = was == null || !was.where().equals(now);
        int ticks = Rooting.rest(was == null ? 0 : was.ticks(), under, moved);
        if (!under) {
            HELD.remove(who);
            return false;
        }
        HELD.put(who, new Held(ticks, now.immutable()));
        return Rooting.opens(ticks);
    }

    /** How long this one has stood still. For the command seam. */
    public static int held(UUID who) {
        Held was = HELD.get(who);
        return was == null ? 0 : was.ticks();
    }

    /** Reset -- on the way through the door. */
    public static void forget(UUID who) {
        HELD.remove(who);
    }

    /** How many things are standing under a door right now. For the command seam. */
    public static int waiting() {
        return HELD.size();
    }

    /** Forget everything, on server shutdown. Same reason as {@link Descending#clear}. */
    public static void clear() {
        HELD.clear();
    }
}
