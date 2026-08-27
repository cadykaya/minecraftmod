package com.cadykaya.interregnum.core.magic;

/**
 * Every spell that exists, and which school teaches it.
 *
 * <h2>Why this exists, and what it fixed before it happened</h2>
 *
 * Spell zones used to be keyed by {@link School}. That was correct while each school had
 * at most one zone spell and became wrong the moment a school had two — and the Quiet
 * One's kit has exactly that: *Hush* silences, *Still* freezes, and both are Silence.
 * Keyed by school, standing in a Hush would have frozen falling blocks and standing in a
 * Still would have muted creepers.
 *
 * This is the SECOND time that shape of bug has come up here. The first was one list of
 * zones per world, which broke as soon as a second spell of any school opened one. Both
 * have the same signature and it is worth naming: <b>a key that is unique today because
 * only one thing uses it.</b> Nothing fails when the second thing arrives; the two simply
 * become each other, and from inside either one it looks like both working.
 *
 * So the key is the spell. A school can grow a kit without any of it colliding, which is
 * what `WORLD.md` plainly intends when it lists four verbs per god and calls full kits
 * design-phase work.
 *
 * <h2>And it has already earned it twice</h2>
 *
 * *Drop-forge* is the Anchorite's second, and like *Lighten* it is a zone. So Weight now
 * has the same pair Silence does — two spells, both zones, one school — and keyed by
 * school those two would be a worse collision than the first, because these two are
 * <b>opposites</b>: inside a Lighten nothing falls, and a Drop-forge is a place where
 * things land. Casting either would have produced a zone that lifted the weight it was
 * waiting for.
 *
 * That is the argument for narrowing a key BEFORE the second user shows up. The fix cost
 * nothing when Hush and Still forced it and would have cost a bug hunt here.
 */
public enum Spell {
    /** The Turning's: age a block a step. */
    WEATHER(School.TURNING),
    /** The Turning's: un-age a block a step, where it had one past. */
    REWIND(School.TURNING),
    /** The Anchorite's: a shared low-gravity zone. */
    LIGHTEN(School.WEIGHT),
    /** The Anchorite's: ground on which a landing weight crushes what it lands on. */
    DROP_FORGE(School.WEIGHT),
    /** The Verdant's: grow a span of real, persistent blocks. */
    BRIDGEROOT(School.VERDANCY),
    /** The Verdant's: everything in a small volume lurches forward at once. */
    WILDGROWTH(School.VERDANCY),
    /** The Quiet One's: a silence, in which nothing alerts and no fuse completes. */
    HUSH(School.SILENCE),
    /** The Quiet One's: things already in motion stop where they are. */
    STILL(School.SILENCE),
    /** The Quiet One's: one creature's throwing arm, taken away and carried with it. */
    QUELL(School.SILENCE),
    /** The Anchorite's: a small structure, weightless, picked up and set down again. */
    LOFT(School.WEIGHT),
    /** The Quiet One's: your own sound, taken away -- and your voice with it. */
    HELD_BREATH(School.SILENCE),
    /** The Anchorite's: one thing fixed where it is, against any push. */
    MOOR(School.WEIGHT),
    /** The Hearth-Turner's: one living thing carried forward to what it was becoming. */
    RIPEN(School.TURNING),
    /** The Hearth-Turner's: one thing carried past its end, and never a creature. */
    ROT(School.TURNING),
    /** The Verdant's: a living wall that comes back thicker where it is cut. */
    HEDGE(School.VERDANCY);

    private final School school;

    Spell(School school) {
        this.school = school;
    }

    /** Who teaches it. Learning a school teaches every spell in it. */
    public School school() {
        return school;
    }
}
