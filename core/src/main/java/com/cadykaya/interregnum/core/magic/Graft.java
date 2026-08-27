package com.cadykaya.interregnum.core.magic;

/**
 * <b>Graft</b> — the Verdant's fourth spell, and the last of the sixteen.
 *
 * `WORLD.md`, locked: *"Join two growing things, or a growing thing to a block, so one
 * feeds the other — and **a plant lives somewhere it could not**."*
 *
 * <h2>The only spell in the kit that acts on a relationship</h2>
 *
 * Everything else here acts on a **place** ({@link Lighten}, {@link Hush}), a **volume**
 * ({@link Wildgrowth}), a **thing** ({@link Moor}, {@link Quell}), or a **person**
 * ({@link HeldBreath}). This one acts on the fact that two positions are joined, and the
 * join is the whole spell: neither end does anything on its own, and cutting either one
 * ends it.
 *
 * That is why it was left until last. It needed something no other verb in the mod
 * required — a ledger of **pairs** — and building it for the first spell that wanted it
 * would have been building it on speculation.
 *
 * <h2>What "lives somewhere it could not" means, exactly</h2>
 *
 * A plant that has no business being where it is. Wheat on bare stone; a sapling on a
 * ceiling. Vanilla removes those the moment anything makes it look — and while the graft
 * holds, this puts them back.
 *
 * So the spell does not make the ground suitable and does not make the plant hardier. It
 * makes something **else** responsible for keeping it alive, which is what a graft is. Cut
 * the stock and the scion is on its own, which is to say it is gone within the tick.
 *
 * <h2>It is not a way to keep a thing for ever</h2>
 *
 * The stock is an ordinary plant in an ordinary place, and anybody can cut it — including
 * the world, which grows over it, ages it, and unravels around it. A graft is exactly as
 * durable as the least protected end of it, and that is the honest shape for a spell whose
 * fiction is that one living thing is carrying another.
 */
public final class Graft {
    private Graft() {}

    /** The school this belongs to. */
    public static final School SCHOOL = School.VERDANCY;

    /**
     * How far apart the two ends may be, in blocks.
     *
     * Eight, matching {@link Hedge#MAX_LENGTH}: a graft is a thing you reach across a
     * garden, not across a valley. It is also the distance at which a person can still see
     * both ends of what they are joining, which matters more for this spell than for any
     * other in the kit — a link you cannot see both ends of is a link you will forget you
     * made.
     */
    public static final int MAX_SPAN = 8;

    /**
     * The most joins one world may hold.
     *
     * Sixty-four. Every one of them costs a check on a timer for as long as it lasts, and
     * a spell that can be cast without limit into a system that costs something per cast
     * is a way to make a world slow by being patient. It is also plenty: a graft is a thing
     * you do to one plant you care about.
     */
    public static final int MAX_JOINS = 64;

    /**
     * How often the joins are looked at, in ticks.
     *
     * Every ten. A scion is restored on the next look rather than the next tick, so
     * something that cuts one will see it gone for up to half a second — which reads as
     * the plant being *held* there by something rather than as an invulnerable block, and
     * is the right feel as well as ten times less work.
     */
    public static final int TENDED_EVERY = 10;

    /**
     * Is this the tick on which the joins are looked at?
     *
     * `floorMod` matches {@link com.cadykaya.interregnum.core.haunt.Manifestation#due} and
     * every other interval in this mod. It is a habit rather than a fix here: tested
     * against zero, `floorMod` and `%` agree for negative game times as well as positive
     * ones. They differ everywhere else, which is why the habit is worth keeping.
     */
    public static boolean due(long gameTime) {
        return Math.floorMod(gameTime, TENDED_EVERY) == 0;
    }

    /** How far apart two ends are, by the measure every region in this mod uses. */
    public static int apart(int dx, int dy, int dz) {
        return Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
    }

    /** Whether a join between two points this far apart may be made. */
    public static boolean reaches(int dx, int dy, int dz) {
        int span = apart(dx, dy, dz);
        // Zero is not a short graft, it is a plant joined to itself -- which would be a
        // join nothing could ever cut, since cutting the stock IS cutting the scion.
        return span > 0 && span <= MAX_SPAN;
    }

    /** Why a graft did or did not take. */
    public enum Outcome {
        /** Joined. One is feeding the other. */
        TAKEN,
        /** There is nothing growing at the stock end. */
        NOTHING_TO_GRAFT,
        /** The scion end is not free. */
        OCCUPIED,
        /** Too far apart, or the same position twice. */
        OUT_OF_REACH,
        /** This world is holding as many joins as it will. */
        TOO_MANY,
        /** The caster has not been taught Verdancy. */
        UNLEARNED
    }
}
