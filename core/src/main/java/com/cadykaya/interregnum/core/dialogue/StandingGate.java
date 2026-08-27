package com.cadykaya.interregnum.core.dialogue;

import com.cadykaya.interregnum.core.regard.Institution;
import com.cadykaya.interregnum.core.regard.RegardState;
import com.cadykaya.interregnum.core.regard.Standing;

import java.util.EnumMap;
import java.util.Map;

/**
 * "Only if they think well enough of you" -- and "only while they still do not".
 *
 * `docs/WORLD.md` has regard existing so that institutions treat you differently, and
 * until now nothing consulted it: it was written and never read. This is the first
 * thing that reads it. An option can require a floor (the Wardenate will only discuss
 * this with somebody it trusts), a ceiling (you can only ask why you are being
 * followed while you are still being followed), or both.
 *
 * <h2>The ceiling is not decoration</h2>
 *
 * It is tempting to support only a floor, on the grounds that gating is about earning
 * things. Half the good lines in this mod are the other way round: a scene that stops
 * being available once somebody likes you is how a relationship reads as having
 * *moved* rather than as having accumulated. Content you can lose by being liked is
 * content that makes standing feel like a relationship instead of a score.
 *
 * <h2>THE_GHOST is not "everyone at nought"</h2>
 *
 * A player who has not killed a god has no relationship with the dead one at all --
 * {@link RegardState} pins it immovably at zero, which reads as WARY, which would
 * satisfy any floor at WARY or below. That is the wrong answer to a question nobody
 * asked: the option is not "you are on neutral terms with the ghost", it is "there is
 * no ghost in your life". So a gate naming THE_GHOST admits only its killer.
 */
public record StandingGate(Map<Institution, Standing> atLeast,
                           Map<Institution, Standing> atMost) {

    /** No opinion required. Most options are this. */
    public static final StandingGate OPEN = new StandingGate(Map.of(), Map.of());

    public StandingGate {
        atLeast = copy(atLeast);
        atMost = copy(atMost);
    }

    private static Map<Institution, Standing> copy(Map<Institution, Standing> in) {
        // EnumMap rather than Map.copyOf: iteration order is declaration order and
        // stays that way, so a failure message naming several institutions reads the
        // same on every run (docs/LESSONS.md #19).
        return in.isEmpty() ? Map.of() : Map.copyOf(new EnumMap<>(in));
    }

    public boolean isOpen() {
        return atLeast.isEmpty() && atMost.isEmpty();
    }

    /**
     * Does this player's standing admit them?
     *
     * A null record means somebody with no history at all -- a fresh player, or a
     * participant who is not a player. They are treated as having every institution's
     * default standing, which is what a fresh {@link RegardState} would say anyway;
     * the alternative, refusing every gate outright, would hide ordinary options from
     * anyone who had not yet spoken to somebody.
     */
    public boolean admits(RegardState regard) {
        if (isOpen()) {
            return true;
        }
        RegardState state = regard != null ? regard : new RegardState(false);
        for (var e : atLeast.entrySet()) {
            if (!has(state, e.getKey())
                    || state.standing(e.getKey()).ordinal() < e.getValue().ordinal()) {
                return false;
            }
        }
        for (var e : atMost.entrySet()) {
            if (!has(state, e.getKey())
                    || state.standing(e.getKey()).ordinal() > e.getValue().ordinal()) {
                return false;
            }
        }
        return true;
    }

    /** Is there a relationship here to have an opinion at all? */
    private static boolean has(RegardState state, Institution institution) {
        return institution != Institution.THE_GHOST || state.isKiller();
    }
}
