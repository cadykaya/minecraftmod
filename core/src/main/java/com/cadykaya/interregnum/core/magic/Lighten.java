package com.cadykaya.interregnum.core.magic;

/**
 * <b>Lighten</b> — the Anchorite's first spell.
 *
 * `WORLD.md`, locked: *"**Weight** (Anchorite): *Lighten* — shared low-gravity zone,
 * **mobs float too**."*
 *
 * Those last three words are the spell. It is not a buff you put on yourself; it is a
 * piece of the world briefly obeying the Anchorite's law instead of the overworld's, and
 * everything inside it is subject — you, the skeleton chasing you, the gravel above your
 * head. `WORLD.md` requires that a spell's combat use *"falls out of its world use, never
 * the reverse"*, and this is the clearest case in the kit: you cannot aim it at anybody.
 * You can only change the rules where they happen to be.
 *
 * <h2>It is the Anchorite's own law, borrowed</h2>
 *
 * The zone does not implement floating. It makes {@code Anchorite.lift} apply where it
 * would not otherwise — the same method the Mass Authority runs on every unanchored thing
 * in it, and the same method band 3's leaks call in the overworld. Three callers, one
 * law: a god's world, a patch of ground that has forgotten which god it belongs to, and
 * now a person who has learned how to ask.
 *
 * That progression is the school system's whole argument. You met the law as a place, met
 * it again as a wrongness leaking into your own world, and the third time you are the one
 * doing it.
 */
public final class Lighten {
    private Lighten() {}

    /** The school this belongs to. */
    public static final School SCHOOL = School.WEIGHT;

    /**
     * How far the zone reaches, in blocks.
     *
     * Five: a room, not a region. Big enough to stand in with something else, small
     * enough that its edge is findable by walking — which is how a player learns it has
     * one, and therefore that it is a rule rather than the world breaking.
     */
    public static final int RADIUS = 5;

    /**
     * How long it holds, in ticks.
     *
     * Half a minute. Long enough to be a plan — get under the gravel, walk the anvil
     * across the room — and short enough that it is never scenery. A permanent
     * low-gravity field is a terrain feature; a temporary one is a verb.
     */
    public static final long DURATION_TICKS = 20L * 30;

    /** The zone one cast opens. */
    public static Zone zoneAt(int x, int y, int z, long nowTick) {
        return new Zone(x, y, z, RADIUS, nowTick + DURATION_TICKS);
    }
}
