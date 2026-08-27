package com.cadykaya.interregnum.core.portal;

import com.cadykaya.interregnum.core.magic.Hush;
import com.cadykaya.interregnum.core.magic.Spell;
import com.cadykaya.interregnum.core.magic.Zone;

/**
 * The Quiet One's portal: <b>it opens when nothing near it makes a sound.</b>
 *
 * `WORLD.md`, locked: *"Opens when **nothing near it makes a sound**. The only one of the
 * four a player can close by accident. Opened with *Hush* — and *Held-breath*, for the
 * last few steps."*
 *
 * <h2>What "a sound" means, when nobody can hear one</h2>
 *
 * `Hush`'s own javadoc records the wall this god keeps running into: *"the Quiet One's law
 * is the one law whose most characteristic form lives on a client"*, and a headless server
 * hears nothing. So a door keyed on audible sound would be a door nothing in this project
 * could ever verify, and the spell already refuses to claim the audible half.
 *
 * The way through is that **the game already has a server-side model of a noise, and it is
 * not the sound system.** A game event — the vibration a sculk sensor listens for — is
 * posted with a position every time something happens that a listening thing could notice:
 * a footstep, a block placed, a door swung, an item picked up. It is dispatched on the
 * server, it is exact, and it is the game's own answer to *did something just happen
 * audibly here*.
 *
 * So this door listens the way sculk listens. Nothing about it depends on anybody hearing
 * anything, which is the only reason it can be a door at all.
 *
 * <h2>Every event counts, and that is the point rather than a simplification</h2>
 *
 * No allow-list. `Verdant` refuses to keep a list of growable blocks and this refuses for
 * the same reason: a hand-picked set of "real" noises would be out of date the first time
 * the game shipped a new one, and — worse — it would let a player learn which noises are
 * free. `WORLD.md` calls this *"the only one of the four a player can close by accident"*,
 * and an accident is precisely the thing you did not know counted.
 *
 * <h2>Why the door IS the spell's zone</h2>
 *
 * The other three portals are a thing you cast, a thing you plant and a thing you build.
 * This one is a thing you <b>stop doing</b>, and it needs a boundary anyway — *"nothing
 * near <b>it</b>"* has to have an "it". {@link Hush} already supplies exactly that: a cube
 * of declared silence, cast by the school this god teaches, that you can walk out of.
 *
 * <h2>It is shared, and that is the god</h2>
 *
 * The Verdant's door asks YOU to stand still and answers about you. This one asks the
 * WORLD to be quiet and answers about the place — so somebody else's footstep closes your
 * door, and yours closes theirs. Of the four gods this is the one whose law was never about
 * the person standing in front of it.
 *
 * <h2>Held-breath</h2>
 *
 * `WORLD.md` names it *"for the last few steps"*, and it is unbuilt — one of the six spells
 * still to come. Walking posts a game event on every step, so the last approach to a door
 * you have opened is the hardest part of using it, and that spell is the answer. Nothing
 * here waits on it: the door works without, and is merely harder to reach.
 */
public final class Stillness {
    private Stillness() {}

    /**
     * The spell that opens it — and the only portal key that is also the boundary.
     *
     * Elsewhere the school's verb makes a door somewhere. Here it makes the *place the
     * question is asked about*, which is why this is the one door with no second
     * condition to satisfy: casting it is the whole of the making, and everything after
     * that is not doing things.
     */
    public static final Spell KEY = Spell.HUSH;

    /**
     * How long the silence has to hold, in ticks.
     *
     * Five seconds, against a {@link Hush} zone that lasts twenty. That ratio is the
     * design: one cast gives you four chances to be quiet for long enough, so a door
     * closed by an accident is a setback rather than a wasted spell — and a fourth
     * accident is a message about how much noise you are making.
     *
     * Long enough to be impossible while walking (a step posts an event) and short enough
     * that standing still is a pause rather than a vigil. The Verdant's three seconds
     * costs you a wait; this costs you every verb you have.
     */
    public static final int STILL_TICKS = 100;

    /**
     * When this zone opened.
     *
     * A zone knows when it ends, not when it began; the difference is the spell's fixed
     * duration. Derived rather than stored so there is no second number to keep in step
     * with {@link Hush#DURATION_TICKS} -- a stored opening time that drifted would make
     * the door available earlier or later than the silence it is measuring.
     */
    public static long openedAt(Zone zone) {
        return zone.expiresAtTick() - Hush.DURATION_TICKS;
    }

    /**
     * The moment the silence started counting from.
     *
     * The later of the two: a zone that has never been disturbed counts from when it was
     * cast, and one that has counts from the last noise. Taking the maximum is what makes
     * a disturbance *reset* rather than merely delay -- without it, an early noise inside
     * a long zone would be forgiven by the passage of time.
     */
    public static long since(long opened, long lastNoise) {
        return Math.max(opened, lastNoise);
    }

    /** Has it been quiet long enough? */
    public static boolean quiet(long since, long now) {
        return now - since >= STILL_TICKS;
    }
}
