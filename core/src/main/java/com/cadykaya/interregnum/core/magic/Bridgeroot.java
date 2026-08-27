package com.cadykaya.interregnum.core.magic;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Bridgeroot</b> — the Verdant's first spell.
 *
 * `WORLD.md`, locked: *"**Verdancy** (Verdant): *Bridgeroot* — grow a living span toward
 * your gaze, **real persistent blocks**."*
 *
 * "Real persistent blocks" is the whole design brief and it is unusual enough to be worth
 * dwelling on. Most games' bridge spells are temporary platforms that evaporate — which
 * is a *movement ability* wearing a spell's clothes. This one leaves actual world behind.
 * You can build a house out of it. Somebody else can walk across it a year later. It is a
 * verb that changes the world and does not change back, which is what `WORLD.md` means by
 * a world-verb, and it is why the third spell in this mod is the one that finally makes
 * magic feel like a tool rather than an effect.
 *
 * <h2>The third shape</h2>
 *
 * *Weather* changes a block. *Lighten* encloses a region. This one <b>creates</b>. Having
 * all three means the school system has been shown to carry genuinely different kinds of
 * verb rather than one mechanism with three names.
 *
 * <h2>The geometry is here because it is arithmetic</h2>
 *
 * Which blocks a span occupies is integer geometry with no game in it, so it is decided
 * here and tested without one. A Bresenham-style walk along the dominant axis: one step
 * per block on that axis, with the other two carried along proportionally, so a span
 * toward a diagonal target is continuous rather than a staircase with gaps in it.
 *
 * Gaps are the failure that matters. A bridge you can fall through is worse than no
 * bridge, because you only find out in the middle of it.
 */
public final class Bridgeroot {
    private Bridgeroot() {}

    /** The school this belongs to. */
    public static final School SCHOOL = School.VERDANCY;

    /**
     * The longest span one cast will grow, in blocks.
     *
     * Twelve. Far enough to cross a ravine or reach a ledge, short enough that crossing
     * anything large is several casts and therefore several decisions — and in the
     * overworld, several costs. A spell that spanned any distance would make the fraying
     * a rounding error and the ban unenforceable in practice.
     */
    public static final int MAX_SPAN = 12;

    /**
     * The blocks a span from the origin toward a target would occupy, as offsets.
     *
     * The origin itself is NOT included: the caster is standing there, and a spell that
     * grew a block into the space you occupy would be a spell that suffocates you the
     * first time you use it correctly.
     *
     * @return offsets in order, nearest first, at most {@link #MAX_SPAN} of them. Empty
     *         if the target is the origin, since a span of no length is not a short
     *         bridge, it is a mistake.
     */
    public static List<int[]> span(int dx, int dy, int dz) {
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        List<int[]> out = new ArrayList<>();
        if (steps == 0) {
            return out;
        }
        int reach = Math.min(steps, MAX_SPAN);
        for (int i = 1; i <= reach; i++) {
            // Rounded rather than truncated, so the span stays centred on the true line
            // instead of drifting consistently toward the origin on the minor axes.
            out.add(new int[] {
                    Math.round((float) dx * i / steps),
                    Math.round((float) dy * i / steps),
                    Math.round((float) dz * i / steps)});
        }
        return out;
    }
}
