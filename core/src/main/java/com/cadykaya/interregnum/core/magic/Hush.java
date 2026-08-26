package com.cadykaya.interregnum.core.magic;

/**
 * <b>Hush</b> — the Quiet One's first spell, and the fourth school.
 *
 * `WORLD.md`, locked: *"**Silence** (Quiet One): *Hush* — true no-sound zone: sculk
 * blind, mobs cannot alert, **a creeper that cannot hiss cannot detonate**."*
 *
 * <h2>The bolded clause is the whole spell, and it is not a joke</h2>
 *
 * A creeper's fuse is a sound. Take the sound away and the mechanism it belongs to has
 * nothing to complete. That is the mod's doctrine — *"every spell is a world-verb… its
 * combat use falls out of its world use, never the reverse"* — arriving at its most
 * literal: Hush is not a defensive ability. It is silence, and silence happens to be
 * fatal to a thing that kills by announcing itself.
 *
 * A player will work out what it does to creepers about two seconds after being told what
 * it does to sound, and that moment of working it out is worth more than any number of
 * tooltips.
 *
 * <h2>What is enforced here, and what cannot be</h2>
 *
 * Two of the three clauses are server-side facts and are enforced:
 *
 * <ul>
 *   <li><b>Mobs cannot alert.</b> Nothing inside acquires a target.</li>
 *   <li><b>A creeper cannot detonate.</b> Its fuse is wound back every tick, so it can
 *       chase and threaten and never complete.</li>
 * </ul>
 *
 * The third — the <i>audible</i> silence, and sculk going blind to it — is client-side
 * and cannot be verified in this container at all, so it is <b>not claimed</b>. It is the
 * same wall band 3 met: the Quiet One's law is the one law whose most characteristic form
 * lives on a client. Recorded rather than quietly implemented on the strength of a
 * comment.
 */
public final class Hush {
    private Hush() {}

    /** The school this belongs to. */
    public static final School SCHOOL = School.SILENCE;

    /**
     * How far the silence reaches, in blocks.
     *
     * Wider than {@link Lighten}'s field, because this one is defensive in effect and a
     * room you cannot fit a fight inside is a room the spell does not help with. Still
     * bounded, still a cube, still something you can walk out of — which for this spell
     * is the tense part rather than the informative one.
     */
    public static final int RADIUS = 8;

    /**
     * How long it holds, in ticks.
     *
     * Twenty seconds — shorter than Lighten's half minute. Lighten is a tool you plan
     * around; this is a thing you do when something has gone wrong, and a long silence
     * would turn every encounter into a place you simply stand until it is over.
     */
    public static final long DURATION_TICKS = 20L * 20;

    /** The zone one cast opens. */
    public static Zone zoneAt(int x, int y, int z, long nowTick) {
        return new Zone(x, y, z, RADIUS, nowTick + DURATION_TICKS);
    }
}
