package com.cadykaya.interregnum.core.haunt;

/**
 * How often the dead god does something a second person can see.
 *
 * `WORLD.md`, locked: *"**Rarely, a manifestation is server-real** — a bystander sees the
 * door move too. Not a sanity bar: a **credibility problem**."*
 *
 * <h2>The distinction the numbers have to carry</h2>
 *
 * Everything else on the Haunt's list is rendered for one player, and a mod that only ever
 * did that has built a sanity meter — the killer sees things, everybody knows the killer
 * sees things, and nobody has to decide anything. The locked text refuses that reading in
 * as many words. What makes it a *credibility problem* instead is that the rare ones are
 * <b>real</b>: the door did move, the person beside you did see it, and neither of you can
 * tell whether it meant anything.
 *
 * So the rate is the whole design, and it is bounded on both sides:
 *
 * <ul>
 *   <li><b>Too often and it is a haunted house.</b> Doors that swing every minute are
 *       weather. Nobody's account of them is in question, because there is nothing to
 *       doubt.</li>
 *   <li><b>Too rare and it never happens.</b> A beat tuned so finely that a session never
 *       contains one is a beat that exists only in the source code.</li>
 * </ul>
 *
 * <b>[NEEDS PLAYTEST]</b> — like every other rate in this mod, and more than most. What is
 * being tuned here is how plausible a person sounds when they say a door opened by itself,
 * and no amount of arithmetic settles that.
 */
public final class Manifestation {
    private Manifestation() {}

    /**
     * How often the world even asks the question, in ticks. Ten seconds.
     *
     * Separate from the odds below because they answer different questions: this one is
     * "how much work is this per player" and the odds are "how often does it happen". A
     * single number would have tied the cost of the feature to its rarity, and then making
     * the ghost rarer would have made it cheaper -- which is the wrong pressure on
     * whoever tunes it next.
     */
    public static final int INTERVAL_TICKS = 200;

    /**
     * One in this many of those checks manifests.
     *
     * With the interval above that is a mean of about fifteen minutes of play between
     * manifestations -- a few in a long session, none at all in a short one. That
     * unevenness is not a defect of the tuning; it is the mechanism. A thing that happens
     * on a schedule is a mechanic, and a thing that happens sometimes is a rumour.
     */
    public static final int ODDS = 90;

    /**
     * How far from the killer the ghost will reach for something to move, in blocks.
     *
     * Near enough that the killer is plainly the centre of it -- a door moving across the
     * valley is just a door moving. This has to be close enough that a bystander who saw
     * it also saw who was standing there.
     */
    public static final int REACH = 8;

    /** Is this the tick on which the world asks? */
    public static boolean due(long gameTime) {
        return Math.floorMod(gameTime, INTERVAL_TICKS) == 0;
    }

    /** Mean ticks between manifestations, for anybody tuning the two numbers above. */
    public static long meanTicksBetween() {
        return (long) INTERVAL_TICKS * ODDS;
    }
}
