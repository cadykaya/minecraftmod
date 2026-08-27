package com.cadykaya.interregnum.core.regard;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Before and after, for the purpose of noticing that something changed.
 *
 * Regard moves from several unrelated places -- a conversation settling, a god dying,
 * a keeper killed -- and every one of them needs the same question answered
 * afterwards: did any of that cross a line? Answering it here, on values rather than
 * on events, means a new way to move regard gets the notices for free instead of
 * having to remember them. The alternative -- each caller working out its own
 * crossings from its own deltas -- is how one of them quietly stops.
 *
 * Deliberately NOT derived from the applied deltas. You can recover a before-value by
 * subtracting a delta from an after-value, and it is wrong the moment anything else
 * touches the same institution in between: two effects of -20 each land as one
 * crossing, not two, and reconstruction would report the second one from a baseline
 * that never existed. A snapshot is a fact; arithmetic on deltas is a guess.
 */
public final class Standings {
    private Standings() {}

    /** Everyone's current opinion, to be compared against later. */
    public static Map<Institution, Standing> snapshot(RegardState state) {
        Map<Institution, Standing> out = new EnumMap<>(Institution.class);
        for (Institution i : Institution.values()) {
            out.put(i, state.standing(i));
        }
        return out;
    }

    /**
     * What crossed, between a snapshot and now.
     *
     * Ordered by {@link Institution}'s declaration order rather than by whatever a map
     * iterates, because these become lines of text a player reads in sequence and a
     * set of messages that shuffles between runs is both untestable and unsettling
     * (docs/LESSONS.md #19).
     *
     * An institution missing from {@code before} is treated as having been at its
     * value then, not as having changed: a snapshot taken of a state that did not yet
     * know about an institution must not manufacture news about it.
     */
    public static List<BandChange> since(Map<Institution, Standing> before, RegardState after) {
        List<BandChange> changes = new ArrayList<>();
        for (Institution i : Institution.values()) {
            Standing was = before.get(i);
            Standing now = after.standing(i);
            if (was != null && was != now) {
                changes.add(new BandChange(i, was, now));
            }
        }
        return changes;
    }
}
