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
    /** The ferry was recommissioned and made one crossing. */
    FIRST_CROSSING,
    /** A god's letter was delivered. Repeatable across gods; see LettersDelivered. */
    LETTER_DELIVERED,
    /** A successor was installed, the job was taken, or the last god fell. */
    SUCCESSION
}
