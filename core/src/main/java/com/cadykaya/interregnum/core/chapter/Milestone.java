package com.cadykaya.interregnum.core.chapter;

/**
 * A thing the server has done, once, forever. Milestones are the ONLY input to
 * chapter progression -- the unraveling is keyed to the players' own progress, not
 * to wall-clock time, so a server that stops playing is never punished for it and a
 * server that rushes is never protected. See docs/WORLD.md "Chapter structure".
 */
public enum Milestone {
    /** The heart was taken. Chapter 0 ends here and nothing un-ends it. */
    DEICIDE,
    /** First contact with a woken Warden -- the world answers back. */
    WARDEN_CONTACT,
    /** A clast was attuned: the first Theoclast exists. */
    FIRST_ATTUNEMENT,
    /**
     * The dead god has spoken to its killer for the first time.
     *
     * Not a chapter prerequisite, and deliberately so: the Haunt is a thread rather
     * than a gate, and a killer who never sleeps must not be able to stall the whole
     * world's progression. It is recorded here because "once, forever, server-wide"
     * is exactly what a milestone is, and there is only ever one killer to haunt.
     */
    HAUNT_OPENED,
    /** The ferry was recommissioned and made one crossing. */
    FIRST_CROSSING,
    /** A god's letter was delivered. Repeatable across gods; see LettersDelivered. */
    LETTER_DELIVERED,
    /** A successor was installed, the job was taken, or the last god fell. */
    SUCCESSION
}
