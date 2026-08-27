package com.cadykaya.interregnum.core.magic;

/**
 * <b>Held-breath</b> — the Quiet One's fourth spell, and the only one that costs the school.
 *
 * `WORLD.md`, locked: *"Your own sound, taken away. Nothing tracks you while you hold it —
 * **and you cannot cast, because casting is a spoken word.** Power for silence, exactly."*
 *
 * <h2>It is the casting affordance's own consequence, made into a spell</h2>
 *
 * `WORLD.md` lists three things that fall out of casting being *a word you are on record as
 * having said*, and the second is this one: *"you cannot cast silently — which is what makes
 * Held-breath interesting rather than a stealth trinket: while you hold it you have no
 * voice, so you have no spells."*
 *
 * That is the whole design, and it is why this spell could not have been written before the
 * spoken word was. A stealth ability with no cost is a trinket. This one takes the school
 * away for as long as it lasts — including the word that would end it. You do not put it
 * down; you wait.
 *
 * <h2>What "nothing tracks you" means, server-side</h2>
 *
 * Two things, and they are the two the Quiet One's law is made of everywhere else in this
 * mod:
 *
 * <ul>
 *   <li><b>You post no vibration.</b> Every game event you would have caused is suppressed
 *       — footsteps, blocks placed, doors — which is what a sculk sensor hears and what
 *       {@link com.cadykaya.interregnum.core.portal.Stillness} listens for. Walking is what
 *       makes that god's door hard to reach, and `WORLD.md` names this spell *"for the last
 *       few steps"* precisely there.</li>
 *   <li><b>Nothing acquires you.</b> Same refusal {@code Hush} makes inside its zone, on
 *       the same event, for one person instead of a room.</li>
 * </ul>
 *
 * The audible half is not claimed, for the reason {@link Hush} gives at length: it lives on
 * a client, and a headless server hears nothing.
 */
public final class HeldBreath {
    private HeldBreath() {}

    /** The school this belongs to. */
    public static final School SCHOOL = School.SILENCE;

    /**
     * How long you can hold it, in ticks.
     *
     * Fifteen seconds — the shortest of the four Silence spells, and it has to be. The
     * others cost a cast; this one costs **every cast**, and there is no way to end it
     * early because ending it would take a word. A minute of that is not a trade, it is a
     * punishment for having tried the spell once.
     *
     * Fifteen is also measured against what it is for. The Quiet One's door needs five
     * seconds of unbroken silence; this gives you those five and about ten to walk in on,
     * which is *"for the last few steps"* said as a number.
     */
    public static final long DURATION_TICKS = 20L * 15;

    /** When a breath held now would run out. */
    public static long expiryAt(long nowTick) {
        return nowTick + DURATION_TICKS;
    }

    /**
     * Is a breath that runs out at {@code expiry} still held at {@code nowTick}?
     *
     * Strictly greater, matching every other duration in this mod: the spell is in force on
     * the exact tick it expires. An off-by-one nobody would ever see, except that here it
     * would land on the last tick of a five-second silence somebody spent a spell on.
     */
    public static boolean holds(long expiry, long nowTick) {
        return nowTick <= expiry;
    }
}
