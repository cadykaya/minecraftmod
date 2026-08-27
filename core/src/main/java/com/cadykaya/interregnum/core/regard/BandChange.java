package com.cadykaya.interregnum.core.regard;

/**
 * One institution's opinion of you crossing into a different band.
 *
 * This is the only regard event a player is ever told about, and the reason it is a
 * type rather than a number is the whole design: `docs/WORLD.md` bans the karma bar,
 * so what surfaces is not "+5 Villages" but the fact that something changed its mind
 * about you. A delta is bookkeeping. A crossing is news.
 *
 * The direction is part of the event and not an afterthought. Falling into WARY from
 * KNOWN is a different sentence than rising into WARY from RESENTED -- one is a door
 * closing and the other is a door opening, and the band alone cannot tell them apart.
 */
public record BandChange(Institution institution, Standing from, Standing to) {

    public BandChange {
        if (from == to) {
            // Not pedantry: a "crossing" that did not cross would produce a message
            // telling the player that nothing happened, which is worse than silence.
            throw new IllegalArgumentException(
                    "a band change must change bands: " + institution + " stayed " + from);
        }
    }

    /** Did they think better of you, or worse? */
    public boolean rose() {
        return to.ordinal() > from.ordinal();
    }
}
