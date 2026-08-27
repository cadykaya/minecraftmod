package com.cadykaya.interregnum.core.letters;

/**
 * The safe way to read the dead god's hand: have somebody else do it first.
 *
 * `WORLD.md`, locked: *"Raw god-script … read without transcription **at the ferry's
 * desk** marks the reader. Knowledge-as-hazard; **the codex desk is the safe path**."* And:
 * *"The safe path costs time and a trip to the desk. The unsafe path costs nothing at all,
 * which is exactly why people will take it."*
 *
 * <h2>The cost is the trip, and the clock is what makes it one</h2>
 *
 * A desk that transcribed on the click would be a free lever: hold the letter to it, take
 * it back, read it safely, and the hazard is a formality nobody notices. What makes the
 * unsafe path tempting is that the safe one takes **the letter out of your hands for a
 * while** — you cannot carry it, deliver it, or read it, and the thing you wanted to know
 * is exactly as unknown as it was.
 *
 * <h2>A transcription is the world's, not the reader's</h2>
 *
 * Once a letter has been through a desk it is transcribed for **everybody**. That is what a
 * clerk is for, and it is the only social mechanic in the reading lane: on a server the
 * first person to be patient pays for everyone who comes after, and the impatient ones have
 * already read it raw by then.
 *
 * It also means the hazard is finite for letters and permanent for stones — there are four
 * letters and they can all be made safe, and a carved shrine stone cannot be brought to a
 * desk and never becomes anything but raw. *"Most people don't bother"*, and a stone in a
 * ruin is always the god's own hand.
 */
public final class Transcription {
    private Transcription() {}

    /**
     * How long a clerk's work takes, in ticks.
     *
     * Thirty seconds. Long enough that you put the letter down and go and do something —
     * which is the cost, and the reason anybody skips it — and short enough that the safe
     * path is genuinely available rather than a thing the design merely claims exists.
     *
     * <b>[NEEDS PLAYTEST]</b>, like every other duration here. What is being tuned is how
     * impatient a person has to be before they read a dead god's mail over its own desk,
     * and no amount of arithmetic settles that.
     */
    public static final long TAKES_TICKS = 20L * 30;

    /** Is the clerk finished? */
    public static boolean done(long lodgedAt, long now) {
        return now - lodgedAt >= TAKES_TICKS;
    }

    /**
     * Ticks still to go, floored at zero.
     *
     * For the seam that reports on a desk. Floored rather than allowed to go negative
     * because a finished desk that reports "-400 remaining" is a desk somebody will write
     * a second condition around.
     */
    public static long remaining(long lodgedAt, long now) {
        return Math.max(0, TAKES_TICKS - (now - lodgedAt));
    }
}
