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
 */
public enum Spell {
    /** The Turning's: age a block a step. */
    WEATHER(School.TURNING),
    /** The Turning's: un-age a block a step, where it had one past. */
    REWIND(School.TURNING),
    /** The Anchorite's: a shared low-gravity zone. */
    LIGHTEN(School.WEIGHT),
    /** The Verdant's: grow a span of real, persistent blocks. */
    BRIDGEROOT(School.VERDANCY),
    /** The Quiet One's: a silence, in which nothing alerts and no fuse completes. */
    HUSH(School.SILENCE),
    /** The Quiet One's: things already in motion stop where they are. */
    STILL(School.SILENCE);

    private final School school;

    Spell(School school) {
        this.school = school;
    }

    /** Who teaches it. Learning a school teaches every spell in it. */
    public School school() {
        return school;
    }
}
