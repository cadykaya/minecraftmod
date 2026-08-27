package com.cadykaya.interregnum.core.magic;

/**
 * <b>Still</b> — the Quiet One's second spell.
 *
 * `WORLD.md`: *"**Still** — freeze primed TNT / falling block **mid-state**."*
 *
 * The last word is the spell. Not "prevent" and not "defuse" — the thing is already
 * happening and it **stops where it is**, holding the state it was in. A block of sand
 * caught halfway down does not fall and does not land; it is simply a thing in the air
 * that used to be moving. Primed TNT keeps its fuse and does not spend it.
 *
 * <h2>How it differs from Hush, which is the same school</h2>
 *
 * *Hush* is about **information**: nothing hears, nothing notices, no fuse completes
 * because a fuse is a sound. *Still* is about **motion**: what is already in flight stops.
 * A creeper walking toward you is unaffected by Still — it was never a thing in mid-state
 * — and a falling anvil is unaffected by Hush, because an anvil was never listening.
 *
 * They overlap on exactly one object, primed TNT, and they treat it differently: Hush
 * denies it the sound it needs and Still denies it the moment. That is what two spells in
 * one school should look like — the same god's law, approached from two sides.
 *
 * <h2>Held, not cancelled</h2>
 *
 * Nothing is destroyed and nothing is defused. When the zone lapses, everything in it
 * resumes: the sand falls, the TNT goes off. A spell that deleted the hazard would be a
 * damage button with extra steps, and `WORLD.md` is explicit that a spell's combat use
 * has to fall out of its world use. The world use is *pause*, and pausing an explosive
 * happens to be worth a great deal in a fight.
 */
public final class Still {
    private Still() {}

    /** The school this belongs to. */
    public static final School SCHOOL = School.SILENCE;

    /**
     * How far the stillness reaches, in blocks.
     *
     * Small — four. This is a precise instrument, not an area denial: you use it on the
     * one thing that is about to go wrong. A wide Still would freeze a whole cave's worth
     * of gravel and turn every collapse into a non-event.
     */
    public static final int RADIUS = 4;

    /**
     * How long it holds, in ticks.
     *
     * Ten seconds. Long enough to walk out from under whatever you stopped, short enough
     * that it is a reprieve rather than a solution -- and everything it held resumes when
     * it lapses, so the reprieve is all it ever was.
     */
    public static final long DURATION_TICKS = 20L * 10;

    /** The zone one cast opens. */
    public static Zone zoneAt(int x, int y, int z, long nowTick) {
        return new Zone(x, y, z, RADIUS, nowTick + DURATION_TICKS);
    }
}
