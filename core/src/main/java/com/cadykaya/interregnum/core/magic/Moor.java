package com.cadykaya.interregnum.core.magic;

/**
 * <b>Moor</b> — the Anchorite's fourth spell, and the answer to its own god's law.
 *
 * `WORLD.md`, locked: *"The exact opposite of *Lighten*: fix a thing where it is, against
 * any push. **Not water, not pistons, not the Anchorite's own law.**"*
 *
 * <h2>Three named forces, and all three push the same kind of thing</h2>
 *
 * The list looked at first like three mechanisms. It is one. Water pushes entities; a
 * piston pushes what stands in front of it; and the Anchorite's law lifts a falling block,
 * which is an entity too. So a moored thing is an **entity fixed in place**, and one rule —
 * its position does not change — refuses all three without knowing what any of them is.
 *
 * That is also why it is not a zone. {@link Lighten} makes a room weightless and everything
 * in it is subject; this is aimed at one thing and does nothing whatever to the rest, which
 * gives the Weight school the same pairing Silence has in {@code Hush} and {@code Quell}: a
 * place you change, and a thing you change.
 *
 * <h2>"Not the Anchorite's own law" is the line that makes it a spell worth having</h2>
 *
 * In that god's world unanchored things rise *and they do not stop* — the boarding notice
 * has promised it since before the world existed, and a falling block that gets away climbs
 * past the build height and is gone. This is the school's answer to that, taught by the god
 * whose law it defeats, which is the most Anchorite thing in the kit: the way to keep
 * something in a world where nothing stays put is to have learned from the thing that keeps
 * nothing.
 */
public final class Moor {
    private Moor() {}

    /** The school this belongs to. */
    public static final School SCHOOL = School.WEIGHT;

    /**
     * How far from the cast a thing may be and still be the one moored, in blocks.
     *
     * Five, matching {@link Quell} exactly and for its reason: at a range where you could
     * not say which thing you meant, an aimed spell stops being aimed. The two spells are
     * the same shape in different schools and it would be a small cruelty to give them
     * different arms.
     */
    public static final int REACH = 5;

    /**
     * How long it holds, in ticks.
     *
     * One minute — the longest duration in the mod, and it belongs to the least dramatic
     * spell in it. Everything else in the kit is something you do *during* a moment: a
     * silence when a fight has gone wrong, a field to cross a gap, a surge at a garden bed.
     * An anchor is a thing you set and then stop thinking about, and one that lapsed while
     * you were relying on it would not be an anchor, it would be a delay.
     */
    public static final long DURATION_TICKS = 20L * 60;

    /** When a mooring cast now would let go. */
    public static long expiryAt(long nowTick) {
        return nowTick + DURATION_TICKS;
    }

    /** Is a mooring that lets go at {@code expiry} still holding at {@code nowTick}? */
    public static boolean holds(long expiry, long nowTick) {
        return nowTick <= expiry;
    }
}
