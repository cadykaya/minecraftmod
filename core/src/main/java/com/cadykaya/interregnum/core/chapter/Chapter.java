package com.cadykaya.interregnum.core.chapter;

import java.util.Set;

/**
 * How far the interregnum has gone. Each chapter names the unraveling band the
 * overworld is under; the content of each band is data (see the unraveling table),
 * not encoded here.
 */
public enum Chapter {
    /** Vanilla. The mod adds structures and nothing else. No rule changes at all. */
    DORMANT(0),
    /** The god is dead. Sky wrong, statues woken, magic now draws on the corpse. */
    VIGIL(1),
    /** Wardens enforcing in earnest; the first rules begin to slip. */
    ENFORCEMENT(2),
    /** The ways are open; the overworld is visibly spending itself. */
    EXODUS(3),
    /** Geography frays at the edges. The last band before a successor or an ending. */
    ATTRITION(4),
    /** Resolved, one way or another. */
    SUCCESSION(5);

    public final int band;

    Chapter(int band) { this.band = band; }

    /**
     * The milestones that must ALL be present to be at least this chapter.
     * Deliberately a pure function of the milestone set: chapter is derived, never
     * stored, so it cannot drift out of sync with what the world has actually done.
     */
    Set<Milestone> requires() {
        return switch (this) {
            case DORMANT -> Set.of();
            case VIGIL -> Set.of(Milestone.DEICIDE);
            case ENFORCEMENT -> Set.of(Milestone.DEICIDE, Milestone.WARDEN_CONTACT);
            case EXODUS -> Set.of(Milestone.DEICIDE, Milestone.WARDEN_CONTACT,
                                  Milestone.FIRST_CROSSING);
            case ATTRITION -> Set.of(Milestone.DEICIDE, Milestone.WARDEN_CONTACT,
                                     Milestone.FIRST_CROSSING, Milestone.LETTER_DELIVERED);
            case SUCCESSION -> Set.of(Milestone.SUCCESSION);
        };
    }
}
