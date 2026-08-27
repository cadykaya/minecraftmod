package com.cadykaya.interregnum.core.clast;

/**
 * How many pieces of the dead god there are, and where they land.
 *
 * `WORLD.md`, locked: *"The god's power enters its killer. An ordinary Minecraft body
 * cannot hold it. The overflow detonates outward, scattering **splinters** at shrines and
 * the crater."* And: *"the shattered god-pieces are **clasts** (item). Anyone may attune
 * one; **clasts are finite** — the class is a server negotiation."*
 *
 * <h2>Finite is the mechanic, so the count is the mechanic</h2>
 *
 * Everything else in this mod that produces things produces them from a rule — the
 * unraveling converts whatever is there, the Verdant grows whatever can grow. This does
 * not. There is a number, it is small, and when it is gone it is gone, because
 * *"the class is a server negotiation"* only means anything if there are fewer clasts than
 * there are people who want one.
 *
 * That is why the pool lives here rather than falling out of how many shrines a world
 * happens to have generated. A world with forty shrines would otherwise hand out forty
 * classes, and the negotiation the locked text describes would never happen.
 *
 * <h2>[NEEDS PLAYTEST] — the number</h2>
 *
 * `WORLD.md` marks the count as needing playtest and does not give one. {@link #TOTAL} is
 * seven: small enough that a server of any size has to decide who gets one, odd so that it
 * cannot be split evenly by two factions, and more than the four gods so that holding a
 * full set is not the obvious goal. It is a starting position for a playtest, not an
 * answer, and it is one constant because that is what makes it cheap to change.
 */
public final class Clasts {
    private Clasts() {}

    /** Every clast that will ever exist in one world. */
    public static final int TOTAL = 7;

    /**
     * How many land in the crater, at the moment of the death.
     *
     * Three of seven: the crater is where it happened, so it gets the largest single
     * share — but not most of them, because a killer who could pick up the whole class by
     * standing still would make *"anyone may attune one"* untrue on the first day. The
     * rest are out in the world, at shrines, and have to be found.
     */
    public static final int AT_CRATER = 3;

    /** How many a shrine yields, the first time anybody is there to see it. */
    public static final int AT_SHRINE = 1;

    /**
     * How many of a request the pool can actually meet.
     *
     * The whole of the finiteness, in one line, and deliberately not a throw: a shrine
     * that loads after the pool is empty is the ordinary case, not an error. It simply
     * has nothing in it, which is what a world that has already been picked over looks
     * like.
     *
     * @param issued how many have been handed out already
     * @param want how many this site would like
     * @return how many it gets, never negative and never more than remains
     */
    public static int issue(int issued, int want) {
        if (want <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(want, TOTAL - Math.max(0, issued)));
    }

    /** How many are still out there to find. */
    public static int remaining(int issued) {
        return Math.max(0, TOTAL - Math.max(0, issued));
    }
}
