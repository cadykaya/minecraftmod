package com.cadykaya.interregnum.core.spatial;

/**
 * Which way to point something.
 *
 * Two lines of arithmetic, in `core/`, with tests -- because the version inlined at
 * the call site was wrong and shipped, and because there is no way to catch it where
 * it lives. A mob's facing is set once at placement and then immediately overwritten
 * by whatever it looks at next, so by the time anything can observe the entity the
 * evidence is gone. An assertion about a mob's yaw is an assertion about *when you
 * looked*; an assertion about this function is an assertion about the code.
 *
 * Minecraft's convention, which is the part that is easy to get backwards: yaw 0
 * looks along +z (south), 90 along -x (west), 180 along -z, -90 along +x.
 */
public final class Facing {
    private Facing() {}

    /**
     * The yaw that looks along the vector (dx, dz).
     *
     * Note the negation: {@code atan2} grows counter-clockwise from +x, Minecraft's
     * yaw grows clockwise from +z, and the two disagree by exactly a sign. Placing a
     * keeper beside a shrine, the vector wanted is the one pointing BACK to the
     * centre, so the caller passes the negated offset -- and those two negations do
     * not cancel, which is precisely the mistake that put a shrine-keeper's back to
     * the box they exist to attend.
     */
    public static float yawToward(double dx, double dz) {
        return (float) -Math.toDegrees(Math.atan2(dx, dz));
    }
}
