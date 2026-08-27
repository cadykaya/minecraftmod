package com.cadykaya.interregnum.core.haunt;

/**
 * What reading the dead god's own handwriting costs.
 *
 * `WORLD.md`, locked: *"Raw god-script (letters, shrine inscriptions) read without
 * transcription at the ferry's desk **marks** the reader. Knowledge-as-hazard; the codex
 * desk is the safe path."* — and, in as many words: *"**'marks' means one specific thing:
 * the ghost gets louder.** Reading raw script raises your manifestation rate… Nothing else
 * changes. No affliction bar, no debuff, no visions system to build."*
 *
 * <h2>One number, and the refusal is the design</h2>
 *
 * This class exists to make a hazard out of a single existing rate rather than out of a new
 * system. There is no status effect, no counter a player watches, and nothing to cure. The
 * only thing reading changes is how often {@link Manifestation} says yes — which is to say,
 * how often a door moves while somebody else is standing there.
 *
 * That is enough because of what it makes the hazard *be*. The punishment for knowing too
 * much is **being known**: you read the god's mail without going through the desk, and the
 * god notices you have been reading its mail. It lands on the one axis `WORLD.md` already
 * calls *a credibility problem rather than a sanity bar* — the more you have read, the
 * harder your account of your own world is to defend, and the less anybody can tell whether
 * that is the reading or the reader.
 *
 * <h2>Distinct pieces, not repeated readings</h2>
 *
 * The ledger counts *what you have read*, not *how many times you have read*. Re-reading a
 * stone you have already read changes nothing, because you already know what it says —
 * knowledge is the hazard, and a second look is not more knowledge. It also means the total
 * is bounded by how much script a world contains, which is what stops this being a meter
 * anybody can grind in either direction.
 *
 * <h2>Bounded, because a metronome is not a rumour</h2>
 *
 * {@link Manifestation} argues at length that a thing which happens on a schedule is a
 * mechanic and a thing which happens sometimes is a rumour. Letting readings drive the odds
 * to one would hand a determined reader a haunted house — doors every ten seconds, nothing
 * for anybody to doubt, and the credibility problem solved in the worst direction. So the
 * loudest the ghost ever gets is still rare enough to be deniable.
 */
public final class Script {
    private Script() {}

    /**
     * How much one distinct piece of raw script takes off the odds.
     *
     * Ten, against a base of ninety — so the first thing you read is worth about eleven per
     * cent and the seventh takes you to the floor. Large enough that a player who reads one
     * letter and then wonders whether it mattered is right to wonder; small enough that
     * nobody can point at a single reading as the cause of anything, which is the whole
     * texture this hazard is for.
     */
    public static final int STEP = 10;

    /**
     * The lowest the odds ever go, however much you have read.
     *
     * Twenty: a mean of about three and a half minutes of play between manifestations,
     * against fifteen for somebody who has read nothing. Four times as loud, and still not a
     * schedule — a person could sit through one and see nothing, which is the property that
     * has to survive.
     */
    public static final int LOUDEST = 20;

    /**
     * The manifestation odds for somebody who has read this many distinct pieces.
     *
     * @param read how many distinct pieces of raw script this person has read. Zero for
     *             everybody who has gone through the desk, or simply not looked.
     */
    public static int oddsFor(int read) {
        return Math.max(LOUDEST, Manifestation.ODDS - read * STEP);
    }

    /** Mean ticks between manifestations for a reader this deep. For anybody tuning it. */
    public static long meanTicksBetween(int read) {
        return (long) Manifestation.INTERVAL_TICKS * oddsFor(read);
    }
}
