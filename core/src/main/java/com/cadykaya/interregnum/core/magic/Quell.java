package com.cadykaya.interregnum.core.magic;

/**
 * <b>Quell</b> — the Quiet One's third spell.
 *
 * `WORLD.md`, locked: *"**Silence** (Quiet One): … *Quell* — strip one ability (a blaze
 * that cannot ignite)."*
 *
 * <h2>Which ability, and why it is not a menu</h2>
 *
 * The locked text names the verb and gives exactly one example, and the example decides
 * the reading: <b>Quell takes away the throwing arm.</b> Whatever a quelled mob tries to
 * launch never leaves it. A blaze that cannot ignite is that rule applied to a blaze; a
 * skeleton that cannot loose an arrow and a witch that cannot throw are the same rule
 * applied to them, not separate cases anybody had to enumerate.
 *
 * It would have been easy to make this a spell with a mode per mob — no teleport for an
 * enderman, no climb for a spider — and that would be four spells wearing one name.
 * "Strip ONE ability" is singular in the locked text, and one ability defined uniformly
 * is the only reading where the word means anything. The other abilities are other
 * spells, and the kit has two names left in it.
 *
 * <h2>Why it is not a zone</h2>
 *
 * {@link Hush} and {@link Still} are places. This is a thing done to a creature, and it
 * follows the creature: a mob that walks out of a Hush can shout again, and a quelled
 * blaze is quelled in whatever room it flies to. That difference is the whole reason both
 * exist — a silence is somewhere you stand, and a quelling is something one thing carries.
 *
 * <h2>What is not claimed</h2>
 *
 * Nothing about sound. The Quiet One's most characteristic effects are audible and this
 * container has no client, so — exactly as in {@link Hush} — the audible half is recorded
 * as unverified rather than implemented on the strength of a comment. What is enforced is
 * a server-side fact: the projectile is never added to the world.
 */
public final class Quell {
    private Quell() {}

    /** The school this belongs to. */
    public static final School SCHOOL = School.SILENCE;

    /**
     * How far from the cast a mob may be and still be the one quelled, in blocks.
     *
     * Short, and shorter than any zone in the mod. A zone is a room you make; this is
     * something you do to a specific creature, and at a range where you could not say
     * which creature you meant it would stop being that. Five blocks is about the
     * distance at which a player would say "that one".
     */
    public static final int REACH = 5;

    /**
     * How long it holds, in ticks.
     *
     * Forty-five seconds — the longest of the three Silence spells, and it is the one
     * that should be. Hush is twenty seconds because it is what you do when a fight has
     * gone wrong and a long one would turn every encounter into somewhere you simply
     * stand. This is the opposite: it is aimed, it costs a cast per creature, and it does
     * nothing at all to the seven other things in the room. A short duration would make
     * it strictly worse than the silence for the same price.
     */
    public static final long DURATION_TICKS = 20L * 45;

    /** When a quelling cast now would wear off. */
    public static long expiryAt(long nowTick) {
        return nowTick + DURATION_TICKS;
    }

    /** Is a quelling that expires at {@code expiry} still in force at {@code nowTick}? */
    public static boolean holds(long expiry, long nowTick) {
        return nowTick < expiry;
    }
}
