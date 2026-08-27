package com.cadykaya.interregnum.core.portal;

import com.cadykaya.interregnum.core.magic.Spell;

/**
 * The Anchorite's portal: <b>a shaft you do not build, but let go into.</b>
 *
 * `WORLD.md`, locked: *"Each god's portal is opened by the school that god teaches"*, and
 * for this god — *"A shaft you do not build but **let go into** — it takes anything
 * unanchored, which there means everything. Going down, into the place where down does
 * not hold. Opened with **Lighten** / **Loft**."*
 *
 * <h2>The spell does not make a door. It makes the floor stop mattering</h2>
 *
 * Nothing here creates a block, a frame or a portal entity. A *Lighten* zone cast in the
 * Anchorite's world simply means that within its footprint, weight is the god's to decide
 * — and what the god decides is that an unanchored thing keeps going. So the portal is
 * the ordinary spell, cast in the one world where the spell's own law is the world's law
 * too, and the door is the absence of anything to hold on to.
 *
 * That is why this is a rule about **ticks spent unsupported** rather than about a
 * position. You do not walk through it. You stop holding on, and you keep not holding on
 * for long enough that it counts as having meant it.
 *
 * <h2>A shaft is a column, not a cube</h2>
 *
 * The zone {@link com.cadykaya.interregnum.core.magic.Lighten} opens is a cube of radius
 * five, and the shaft is that cube's **footprint**, taken through the whole height of the
 * world. Anything else would not be a shaft — a cube five blocks tall is a room, and an
 * entity falling out of the bottom of it after a quarter of a second would never once
 * reach {@link #SURRENDER_TICKS}. The word in `WORLD.md` is *shaft*, and a shaft is
 * vertical.
 *
 * <h2>Both ways, one rule, because the far side is upside down</h2>
 *
 * The layer below is *"the place where down does not hold"*. So the same act returns you:
 * let go inside a lightened shaft, and the shaft carries you the way that world's weight
 * goes — which is up. One rule with a sign that the world chooses is the whole reason
 * this is a portal rather than two teleports, and it means a player who worked out how to
 * get in already knows how to get out.
 */
public final class Descent {
    private Descent() {}

    /**
     * The spell that opens it.
     *
     * `WORLD.md` names *Lighten* / *Loft* — the school, either of its verbs. Only the
     * first is the shaft: *Loft* picks up a building, and a portal you could open by
     * carrying a shed through it would be a portal opened by cargo. The school is what
     * `WORLD.md` gates the door on, and *Lighten* is the school's verb for weight itself.
     */
    public static final Spell KEY = Spell.LIGHTEN;

    /**
     * How long you have to be holding on to nothing, in ticks.
     *
     * Two seconds, and the number is chosen against an ordinary jump. A player jumping on
     * the spot is airborne for roughly twenty-four ticks, so a jump — or a stumble, or a
     * step off a kerb — must never be enough. *Letting go* is a thing you do on purpose,
     * and a door that opened by accident in a world whose whole law is that nothing stays
     * put would be a door nobody could stand next to.
     *
     * It is also long enough to be visible. Two seconds of falling is time to see where
     * you are going, which matters more here than anywhere else in the mod, because the
     * other side is a place with no way back except the way you came.
     */
    public static final int SURRENDER_TICKS = 40;

    /** The two layers the shaft joins. */
    public enum Layer {
        /** The Anchorite's surface — `interregnum:mass_authority`. */
        SURFACE,
        /** Under it, where down does not hold. */
        LOWER;

        /** Where the shaft goes from here. */
        public Layer beyond() {
            return this == SURFACE ? LOWER : SURFACE;
        }

        /**
         * Which way the shaft carries you out of this layer.
         *
         * Down from the surface, because gravity is already doing it and the spell only
         * has to stop the ground from interrupting. Up from below, because there is no
         * such thing as down there — so on that side the shaft has to supply the movement
         * itself, at {@link #RISE_PER_TICK}.
         */
        public boolean downward() {
            return this == SURFACE;
        }
    }

    /**
     * One tick of not holding on.
     *
     * @param held      how long this thing has been letting go, from the last call
     * @param inShaft   whether it is inside the shaft's footprint
     * @param holding   whether it has hold of something — standing on ground, in a boat,
     *                  on a ladder. Anything at all counts, which is the point: the
     *                  shaft takes *unanchored* things, and a thing touching the world
     *                  is anchored by definition.
     * @return the new count, and zero the instant either condition breaks.
     *
     * Resetting on BOTH is deliberate. Counting only while inside means stepping out of
     * the footprint cancels it, so the shaft has an edge a player can find by walking —
     * the same reasoning that gives every zone in this mod a boundary. Counting only
     * while unsupported means landing cancels it, so the descent is a continuous act
     * rather than an accumulated one. A version that remembered across a landing would
     * let somebody fall into the shaft in four separate hops, which is not letting go.
     */
    public static int letGo(int held, boolean inShaft, boolean holding) {
        return !inShaft || holding ? 0 : held + 1;
    }

    /**
     * How fast the shaft carries something upward, in blocks per tick, on the side where
     * down does not hold.
     *
     * Half a block: a slow, obvious rise that clears the two seconds with room to spare
     * and is nothing like falling. The surface side has no equivalent constant because it
     * does not need one — gravity is already the god's argument there, and the spell's
     * only job is to stop the floor from interrupting it.
     *
     * It is a SPEED and not an acceleration on purpose. Acceleration means the way out of
     * the under-layer is slow at exactly the moment a player is deciding whether it is
     * working, and then far too fast by the time it does.
     */
    public static final double RISE_PER_TICK = 0.5;

    /**
     * Does the shaft have to do the moving itself?
     *
     * <b>Only below, and there it lifts everything inside it — standing or not.</b>
     *
     * The asymmetry is the layers, and it was found by a check rather than reasoned out.
     * On the surface the shaft supplies nothing: gravity is already the god's argument and
     * the spell's whole job is to stop the floor from interrupting it, so you let go and
     * the world does the rest.
     *
     * Below, weight points the other way, and a version that only lifted things which had
     * already let go could never move anybody: you arrive standing on the ground, and
     * there is no cliff to step off and nothing to let go OF. The shaft would be a
     * one-way door into a place with no ferry law naming it, which is a trap. So down
     * there it picks you up off the floor, which is the same sentence as *down does not
     * hold* said in movement instead of prose.
     *
     * `holding` is deliberately not a parameter. Whether you had hold of something is the
     * question the surface asks; below, it is the thing the shaft overrules.
     */
    public static boolean lifts(Layer layer, boolean inShaft) {
        return inShaft && !layer.downward();
    }

    /** Has it let go for long enough to go through? */
    public static boolean opens(int held) {
        return held >= SURRENDER_TICKS;
    }
}
