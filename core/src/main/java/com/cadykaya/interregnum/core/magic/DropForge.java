package com.cadykaya.interregnum.core.magic;

/**
 * <b>Drop-forge</b> — the Anchorite's second spell.
 *
 * `WORLD.md`, locked: *"**Weight** (Anchorite): … *Drop-forge* — **crafting by
 * crushing**."*
 *
 * <h2>The spell does not crush anything</h2>
 *
 * That is the whole design. A drop-forge is a patch of ground where <b>an impact means
 * something</b> — and an impact is not something the spell provides. You have to go and
 * get the weight, get it above the thing, and let it go. Cast into an empty room it does
 * nothing at all, forever, and then lapses.
 *
 * This is the strongest reading of `WORLD.md`'s locked doctrine that a spell is a
 * world-verb whose *"combat use falls out of its world use, never the reverse"*. There is
 * nothing here to aim. What changes is what gravity is *for* inside a few metres of
 * ground, and everything that then falls into it — your anvil, your sand, a gravel
 * ceiling you undermined by accident — is treated the same way.
 *
 * <h2>It is the other half of its own school</h2>
 *
 * *Lighten* takes weight away so a thing can be moved. *Drop-forge* is what that thing is
 * for once it is above where you want it. The Anchorite's kit is one idea in two
 * directions, and the two spells compose into a single motion: lighten the anvil, walk it
 * up, drop it.
 *
 * <h2>It bites down, and that is not an accident to be patched out</h2>
 *
 * Cobblestone crushes to gravel, and gravel falls. So a weight dropped onto a cobble
 * floor makes the floor under itself fall, follows it down, and crushes again — until the
 * chain runs out of table or the drop leaves the zone. A drop-forge sinks.
 *
 * That is left in on purpose. It is the honest consequence of two rules that are each
 * correct on their own, it is bounded by the radius rather than by a special case, and it
 * is the clearest possible demonstration of what the spell actually did: you did not
 * convert a block, you made this ground somewhere that impacts count.
 *
 * They also <b>cannot overlap</b>, and that is not a bug to be fixed. Inside a Lighten
 * nothing falls, so inside a Lighten nothing lands, so a drop-forge under a low-gravity
 * field is a forge with the hammer floating over it. You must choose which law this piece
 * of ground is under. A player who tries both at once learns the Anchorite's whole
 * argument in about four seconds.
 */
public final class DropForge {
    private DropForge() {}

    /** The school this belongs to. */
    public static final School SCHOOL = School.WEIGHT;

    /**
     * How far the forge reaches, in blocks.
     *
     * Smaller than a {@link Lighten}, which is *"a room, not a region"*. This is a
     * workbench: you stand at it and work, and the thing you are working on is in front
     * of you. A large drop-forge would be a quarry — cast it over a hillside, undermine
     * something, and walk away while the terrain processed itself — and the spell would
     * stop being an act and become a machine.
     */
    public static final int RADIUS = 3;

    /**
     * How long it holds, in ticks.
     *
     * A minute — twice a {@link Lighten}, and for a reason particular to this spell: it
     * is the only one in the kit that asks the caster to go and *fetch* something before
     * it can do anything. Half a minute is not enough time to climb with an anvil, and a
     * spell whose duration expires during its own setup would read as broken rather than
     * brief.
     */
    public static final long DURATION_TICKS = 20L * 60;

    /** The zone one cast opens. */
    public static Zone zoneAt(int x, int y, int z, long nowTick) {
        return new Zone(x, y, z, RADIUS, nowTick + DURATION_TICKS);
    }
}
