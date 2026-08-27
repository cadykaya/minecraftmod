package com.cadykaya.interregnum.core.magic;

/**
 * <b>Rot</b> — the Turning's fourth spell, and the one that was decided rather than
 * sketched.
 *
 * `WORLD.md`, locked: *"Age a thing forward **past its end**: compost, spoil, collapse.
 * **Never aimed at a player or a mob.**"* And, on why it needed deciding: *"the obvious
 * reading of 'age it past its end' is an instant-kill, and **every spell is a world-verb**
 * rules that out — so it ages the things that HAVE an end and leaves creatures alone. A
 * school that broke the doctrine would take the doctrine with it."*
 *
 * <h2>The doctrine is kept by construction, not by a rule</h2>
 *
 * This is a **block conversion**, and it runs on the same table machinery as the ageing
 * chain and the unraveling — one mechanism, three callers, and `WORLD.md`'s locked reuse
 * note. A table of blocks has no way to name a cow. So the constraint that mattered enough
 * to be written in bold is not enforced anywhere: there is nothing to enforce, because the
 * spell has no vocabulary for a creature.
 *
 * That is the stronger version, and it is the same shape {@link Ripen} arrives at from the
 * other side. Ripen asks for something with growing left to do, and a player has no such
 * state. Rot asks a table what a block becomes, and a player is not a block. Neither spell
 * contains the word *player*.
 *
 * <h2>It is the ageing table continued, and it is one-way</h2>
 *
 * The Turning's chains stop at mossy cobblestone, mossy stone bricks and cobbled deepslate
 * — terminal states, things with no next step. Rot gives them one, and it is the same
 * sentence finished rather than a second idea: `stone → cobblestone → mossy cobblestone` is
 * what a wall does over a long time, and collapsing is what it does after that.
 *
 * *Rewind* reads the ageing table backwards, so anything in that table can be undone —
 * which is what *keeping every past* means when it is a block. Nothing Rot does is in that
 * table. **Past a thing's end there is no past left to keep**, and the god that remembers
 * everything remembers what a thing *was*, not what is left of it.
 */
public final class Rot {
    private Rot() {}

    /** The school this belongs to. */
    public static final School SCHOOL = School.TURNING;

    /**
     * What a cast found.
     *
     * There is no `CREATURE` here and there never will be. See the class javadoc: the
     * spell asks a table of blocks a question, and a creature is not an answer that
     * question has.
     */
    public enum Subject {
        /** A block with somewhere past its end to go. */
        THING,
        /**
         * Nothing that could rot.
         *
         * A block the table has no opinion about, a creature, a player, or air. All of
         * them are the same answer for the same reason, which is that the spell only
         * knows one kind of question.
         */
        NOTHING
    }
}
