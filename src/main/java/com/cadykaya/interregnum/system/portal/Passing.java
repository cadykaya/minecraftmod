package com.cadykaya.interregnum.system.portal;

import com.cadykaya.interregnum.core.portal.Hour;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Who was standing in a doorway last tick.
 *
 * The whole of the Hearth-Turner's passage state, and it is one bit per thing: {@link
 * Hour#enters} fires on the rising edge, so this only has to remember whether the edge has
 * already been crossed.
 *
 * <h2>Why a set and not a counter</h2>
 *
 * The other two portals ask you for something over time — two seconds of not holding on,
 * three of not moving. This one asks nothing: all its difficulty is in making the hour, and
 * once it is made the thing in front of you is a door. So there is no duration to keep,
 * only a threshold to have crossed.
 *
 * In memory, like every other portal's per-thing state, and for the same reason: a set
 * restored from disk would be a set about people who logged out.
 */
public final class Passing {
    private Passing() {}

    private static final Set<UUID> INSIDE = new HashSet<>();

    /**
     * Record where this thing is, and say whether it just walked in.
     *
     * @param nowInside whether it is standing in the gap of an open doorway.
     */
    public static boolean tick(UUID who, boolean nowInside) {
        boolean was = INSIDE.contains(who);
        if (nowInside) {
            INSIDE.add(who);
        } else {
            INSIDE.remove(who);
        }
        return Hour.enters(was, nowInside);
    }

    /**
     * Mark something as already inside, without it having entered.
     *
     * Called on arrival. Something that comes out standing in the far door must not read
     * as having crossed its threshold, or it goes straight back -- and clearing the state
     * instead would produce exactly that, because the next tick would then be a rising
     * edge. Stepping out of the far door is what re-arms it.
     */
    public static void arrived(UUID who) {
        INSIDE.add(who);
    }

    /** Whether this one is currently standing in a doorway. For the command seam. */
    public static boolean inside(UUID who) {
        return INSIDE.contains(who);
    }

    /** How many things are standing in doorways. For the command seam. */
    public static int standing() {
        return INSIDE.size();
    }

    public static void forget(UUID who) {
        INSIDE.remove(who);
    }

    /** Forget everything, on server shutdown. Same reason as {@link Descending#clear}. */
    public static void clear() {
        INSIDE.clear();
    }
}
