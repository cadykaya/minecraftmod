package com.cadykaya.interregnum.system.portal;

import com.cadykaya.interregnum.core.portal.Descent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * How long each thing has been letting go.
 *
 * <h2>In memory, like the zones it counts against</h2>
 *
 * A shaft is a {@link com.cadykaya.interregnum.core.magic.Zone} and zones are not
 * persisted — see that class for why. A counter that outlived the field it was counting
 * inside would be a fall that resumed, thirty real-world days later, in a world where
 * nobody had cast anything.
 *
 * It is also two seconds of state. Everything permanent in this mod is somebody's regard,
 * somebody's grimoire or something the world has forgotten; a number that is wrong for
 * forty ticks after a crash is not in that category.
 *
 * <h2>Keyed by UUID, and it has to be</h2>
 *
 * A cross-dimension teleport DESTROYS the entity and builds a new one on the far side —
 * {@code Entity#teleport} returns a different object — so an identity map keyed on the
 * entity would leak one entry per crossing and never match anybody again. The UUID
 * survives the crossing, which is also why {@link #forget} is called explicitly on the
 * way through: the count is about this side of the door, and arriving with thirty-nine
 * ticks of falling still on the clock would send you straight back.
 */
public final class Descending {
    private Descending() {}

    private static final Map<UUID, Integer> HELD = new HashMap<>();

    /**
     * Record one tick, and say whether the shaft has taken them.
     *
     * The count is dropped rather than stored when it falls to zero, so a world where
     * nobody is falling through anything holds an empty map. Almost every entity in the
     * game is standing on something almost all of the time, and this runs on their ticks.
     */
    public static boolean tick(UUID who, boolean inShaft, boolean holding) {
        int held = Descent.letGo(HELD.getOrDefault(who, 0), inShaft, holding);
        if (held == 0) {
            HELD.remove(who);
            return false;
        }
        HELD.put(who, held);
        return Descent.opens(held);
    }

    /** How long this one has been letting go. For the command seam. */
    public static int held(UUID who) {
        return HELD.getOrDefault(who, 0);
    }

    /** Reset — on the way through the door, and on the way out of the world. */
    public static void forget(UUID who) {
        HELD.remove(who);
    }

    /** How many things are letting go right now, anywhere. For the command seam. */
    public static int falling() {
        return HELD.size();
    }

    /**
     * Forget everything, on server shutdown.
     *
     * Same reason as {@link com.cadykaya.interregnum.system.magic.Zones#clear}: a static
     * map outlives a world when two share a JVM, and a count carried into the next world
     * is a count about somebody who is not there.
     */
    public static void clear() {
        HELD.clear();
    }
}
