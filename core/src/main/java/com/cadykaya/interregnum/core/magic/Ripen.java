package com.cadykaya.interregnum.core.magic;

/**
 * <b>Ripen</b> — the Turning's third spell, and the kind half of the school.
 *
 * `WORLD.md`, locked: *"Age a living thing forward: crop, sapling, animal. **The kind half
 * of the school.**"*
 *
 * <h2>The school had only ever done this to stone</h2>
 *
 * *Weather* ages a block and *Rewind* un-ages it, and both work on the Turning's table —
 * masonry, wearing and cracking and greening. That is the whole of what this god's magic
 * has been able to touch, and it is a strange gap for a god whose law is **keeping every
 * past**: living things have more past than walls do.
 *
 * This is that gap closed, and the locked phrase says how. *The kind half*: forward, to
 * maturity, and no further. Age a crop and it fruits. Age a sapling and it is a tree. Age a
 * calf and it is a cow, which is the one thing in the mod that does that.
 *
 * <h2>It stops at grown, and that is the line between this and its own twin</h2>
 *
 * An adult animal is not ripened. There is nowhere kind for it to go — *forward* from
 * grown is toward the end, and that is *Rot*'s country, which `WORLD.md` locks as **never
 * aimed at a player or a mob**. The two spells share a school and a direction and are
 * separated by exactly this: one takes a thing to what it was going to become, and the
 * other takes it past that.
 *
 * A player is never a subject at all. Not by a rule — by the shape of what a subject is:
 * the spell asks for something that is not yet grown, and there is no such state for a
 * person in this game.
 *
 * <h2>Aimed, and only at one thing</h2>
 *
 * Like {@link Quell} and {@link Moor}, and unlike its own school-mate *Wildgrowth*, which
 * sweeps a volume and is the Verdant's **hazard**. `WORLD.md` puts those two on opposite
 * sides of the same idea deliberately: accelerating growth is dangerous when it is
 * indiscriminate and kind when it is chosen. Same verb, and the aim is the whole difference.
 */
public final class Ripen {
    private Ripen() {}

    /** The school this belongs to. */
    public static final School SCHOOL = School.TURNING;

    /**
     * How far the spell reaches for something living, in blocks.
     *
     * Five, matching {@link Quell} and {@link Moor}. Every aimed spell in this mod reaches
     * the same distance, and the number means the same thing in each: about as far as a
     * person could point and be understood.
     */
    public static final int REACH = 5;

    /**
     * How many times one cast asks the world to grow the thing before giving up.
     *
     * <b>It is a patience, not a dose.</b> A cast advances a plant by exactly one growth
     * step and then stops — so a player learns what one cast is worth, which is `Wildgrowth`'s
     * argument for a fixed number and the same conclusion by a different road.
     *
     * The road matters because vanilla growth is a **dice roll**: a crop on unhydrated
     * farmland advances on roughly one random tick in twenty-six, so a fixed count of
     * pushes is a spell that works sometimes. Eight pushes was tried and gave about a one
     * in four chance of doing anything at all, which is not a verb.
     *
     * So the count bounds how long the spell keeps asking rather than how much it delivers.
     * Two hundred and fifty-six attempts against a one-in-twenty-six event leaves about
     * four chances in ten thousand of a cast that visibly did nothing — and the loop stops
     * the instant the block changes, so the ordinary cost is a handful of rolls.
     */
    public static final int ATTEMPTS = 256;

    /** What a cast found, and what it did with it. */
    public enum Subject {
        /** A young animal, now grown. */
        CREATURE,
        /** Something growing in the ground, now further along. */
        PLANT,
        /**
         * Nothing that could be ripened.
         *
         * Grown animals are here, and so are players and stone. The spell does not refuse
         * them by naming them; it simply asks for something with growing left to do.
         */
        NOTHING
    }
}
