package com.cadykaya.interregnum.core.portal;

import com.cadykaya.interregnum.core.magic.Spell;

import java.util.List;

/**
 * The Hearth-Turner's portal: <b>a door that is open at one hour only.</b>
 *
 * `WORLD.md`, locked: *"Always present, open **at one hour only** — and the sky is fixed,
 * so you cannot wait for it. You have to **make** the hour happen. Opened with *Weather* /
 * *Rewind* — the school is the clock."*
 *
 * <h2>The hour is not a time of day. It is how old the doorway is</h2>
 *
 * This god's world has `hasFixedTime` set — the one dimension entitled to a stopped sky,
 * because a world whose law is keeping every past does not get to have an afternoon that
 * becomes evening. So there is no hour in the sky to wait for, and the locked line says so.
 *
 * What this god's school actually moves is **the age of things**. The Turning's table runs
 * `stone_bricks → cracked → mossy`, *Weather* walks it forward and *Rewind* walks it back,
 * and that chain is the clock the locked line means. An hour here is a stage, and the
 * doorway keeps one.
 *
 * <h2>Why the middle stage, and only the middle</h2>
 *
 * The open hour is deliberately the one in the middle of a three-link chain, which makes it
 * **the only stage reachable from both directions** — you weather a new frame forward into
 * it, or rewind an old one back into it. Both of the verbs `WORLD.md` names for this door
 * are load-bearing, and neither is a spare.
 *
 * It is also what makes *"you cannot wait for it"* true without forbidding anything. The
 * hour does come round on its own: that world ages what stands in it. It does not stay.
 * Wait long enough and every frame you ever built is mossy and shut, and the only way back
 * to the hour is the school.
 *
 * <h2>You walk through this one</h2>
 *
 * The Anchorite's asks you to let go; the Verdant's asks you to stand still; this one asks
 * nothing at all, and that is the character of the three together. All the difficulty here
 * is in making the hour — once it is made, the thing in front of you is a door, and what
 * you do with a door is walk through it.
 */
public final class Hour {
    private Hour() {}

    /** One step of the frame, relative to the block you stand in. */
    public record Offset(int dx, int dy, int dz) {}

    /**
     * Forward along the chain. The verb that ages a new frame into the hour.
     *
     * Both of these are `KEY`s, unlike the other two portals where the school had a spare
     * verb. Here the school IS the clock and a clock needs both hands.
     */
    public static final Spell FORWARD = Spell.WEATHER;

    /** Backward along the chain. The verb that brings an old frame back to the hour. */
    public static final Spell BACK = Spell.REWIND;

    /**
     * The gap: where a person stands, and what must be empty.
     *
     * Two blocks, feet and head. A doorway you cannot occupy is a window.
     */
    public static final List<Offset> GAP = List.of(
            new Offset(0, 0, 0),
            new Offset(0, 1, 0));

    /** Underfoot. */
    public static final Offset SILL = new Offset(0, -1, 0);

    /** Overhead. */
    public static final Offset LINTEL = new Offset(0, 2, 0);

    /**
     * The two posts, on one axis or the other.
     *
     * A doorway is a hole in a wall, so it is framed on two opposite sides and **open on
     * the other two** — those are the way through. Boxing all four would be a shaft with a
     * lid on it, and nothing could walk in.
     *
     * Which axis is not the player's decision to justify, only to make: a wall runs the way
     * it runs. Both are accepted, and the geometry is otherwise identical.
     *
     * @param alongX whether the posts stand to east and west (so you pass north-south).
     */
    public static List<Offset> posts(boolean alongX) {
        return alongX
                ? List.of(new Offset(-1, 0, 0), new Offset(-1, 1, 0),
                          new Offset(1, 0, 0), new Offset(1, 1, 0))
                : List.of(new Offset(0, 0, -1), new Offset(0, 1, -1),
                          new Offset(0, 0, 1), new Offset(0, 1, 1));
    }

    /**
     * Every block that has to be at the hour, for one orientation.
     *
     * Six: two posts of two blocks each, a sill and a lintel. Small enough to build by hand
     * out of what a wall is already made of, and large enough that six blocks all being at
     * the same stage is a thing somebody did rather than a thing that happened.
     */
    public static List<Offset> frame(boolean alongX) {
        List<Offset> posts = posts(alongX);
        return List.of(posts.get(0), posts.get(1), posts.get(2), posts.get(3), SILL, LINTEL);
    }

    /**
     * Did this thing just walk in?
     *
     * The rising edge, and it is the whole of the passage rule. Taking anything that is
     * merely standing in a doorway would mean arriving on the far side, standing in the
     * far door, and going straight back — the flicker every portal has to answer, and this
     * is the cheapest honest answer: a door acts when you cross its threshold, not while
     * you are in it.
     *
     * It also means you can stand in your own doorway with it open and not be taken, which
     * is what a doorway is for.
     */
    public static boolean enters(boolean wasInside, boolean nowInside) {
        return nowInside && !wasInside;
    }
}
