package com.cadykaya.interregnum.core.magic;

/**
 * <b>Rewind</b> — the Turning's second spell: *"repair by un-aging"*.
 *
 * `WORLD.md` names it alongside *Weather*, and the pair is the school. One reads the
 * ageing table forwards, the other backwards. **One table, two directions**, which is the
 * same doctrine that made the Turning and the unraveling share a registry in the first
 * place — and it is why the second spell in this school cost almost no new machinery.
 *
 * <h2>It may touch what a player built, and that is the decision</h2>
 *
 * Every other system in this mod consults the claim ledger and refuses anything somebody
 * placed: the unraveling, attrition, the Turning's own clock, *Weather*. Rewind does not,
 * and the reason is that the ledger exists to stop **the world** eating your work — not to
 * stop *you* working on it.
 *
 * Refusing here would make the spell useless at exactly its purpose. *"Repair by
 * un-aging"* is a thing you do to your own wall; a Rewind that could only mend
 * naturally-generated stone would be a spell for tidying up caves. The guarantee is that
 * nothing takes your work away, and un-cracking your own brick does not take anything.
 *
 * <h2>Some things have more than one past</h2>
 *
 * A block reached by two different rules cannot be un-aged, and the table returns nothing
 * rather than guessing. In the unraveling's own table a dandelion and a poppy both become
 * a dead bush, so a dead bush has no single past — only a choice, and it is not this
 * spell's to make.
 *
 * That refusal is the most characterful thing about the whole school. **The god that keeps
 * every version of everything is precisely the one that will not invent one.** A player who
 * meets it will read the refusal as fussiness and be exactly right.
 */
public final class Rewind {
    private Rewind() {}

    /** The school this belongs to. */
    public static final School SCHOOL = School.TURNING;
}
