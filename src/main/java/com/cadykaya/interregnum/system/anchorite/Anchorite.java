package com.cadykaya.interregnum.system.anchorite;

import com.cadykaya.interregnum.worldgen.ModDimensions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;

/**
 * The Anchorite's law: <b>unanchored things rise.</b>
 *
 * <h2>This was already promised, in the mod's own words</h2>
 *
 * The ferry has been refusing sand, gravel and anvils for the Anchorite's crossing
 * since before this world existed, and the refusal it prints says why:
 *
 * <blockquote>
 * Refused for the crossing to the Mass Authority. Nothing that pours. Where you are
 * going, unanchored things go up, and they do not stop.
 * </blockquote>
 *
 * A player has read that before they arrive. This class is the sentence coming true.
 * Which is also the constraint on it: whatever this does has to be the thing that line
 * describes, because the line shipped first and a world that contradicts its own
 * boarding notice is worse than one with no notice at all.
 *
 * <h2>Why this is code when the Quiet One's law was data</h2>
 *
 * 26.2's `dimension_type` attributes cover a lot -- beds, raids, lava, music, snow
 * golems -- and every one of the Quiet One's rules turned out to be in there. Weight is
 * not. There is no gravity attribute, no fall-damage attribute, and no attribute that
 * inverts anything; the full list was read out of `EnvironmentAttributes` rather than
 * guessed at. So this law costs a tick handler where the other cost a JSON field, and
 * that asymmetry is worth recording rather than hiding: the platform made one god cheap
 * and the next one not.
 *
 * <h2>"And they do not stop" is load-bearing</h2>
 *
 * A rising block never satisfies {@link FallingBlockEntity}'s ground test, so it never
 * places itself. It climbs until it is past the build height and vanilla's own timeout
 * discards it. Nothing had to be written to make that happen, and it is exactly what
 * the boarding notice promises: things go up and they are gone. A version that stuck
 * blocks to ceilings would be a nicer toy and a broken promise.
 *
 * <h2>What this deliberately does NOT lift</h2>
 *
 * Only {@link FallingBlockEntity} -- which is precisely the class the ferry's law
 * names, since sand, red sand, gravel and anvils are all of them. Dropped items,
 * players, mobs and minecarts are untouched.
 *
 * That restraint is a decision, and the owner has since confirmed it: <b>death here does
 * not cost the inventory.</b> "Unanchored things" plainly includes a dropped item, and
 * lifting those away would mean every death in this world costs everything with no way
 * to chase it -- either the best scene in the mod or the reason nobody comes back. It
 * was put to the owner rather than decided here, and the answer was no.
 *
 * If it is ever revisited, the version to revisit is not "lift items and accept the
 * loss": it is answering the question the boarding notice leaves open -- *where do they
 * go?* -- so that the law stays literally true and the loss becomes an errand. See
 * HANDOFF, "Waiting on owner".
 */
public final class Anchorite {
    private Anchorite() {}

    /**
     * Matches vanilla's own falling gravity, negated.
     *
     * Deliberately the same magnitude rather than a hand-tuned number: the world is not
     * "low gravity", it is gravity pointing the wrong way, and a player who has watched
     * sand fall for a hundred hours should recognise the speed.
     */
    public static final double RISE = 0.04;

    /** Terminal rise, so a block leaves in a reasonable time instead of creeping. */
    public static final double MAX_RISE = 1.6;

    /**
     * @return whether this entity is one the Anchorite's law has an opinion about.
     *
     * Split out from the handler so the condition is one readable thing rather than a
     * clause inside an event method -- and so the class reads as a law rather than as
     * a callback.
     */
    public static boolean unanchored(Entity entity) {
        return entity instanceof FallingBlockEntity;
    }

    /** Whether this level is the Anchorite's. */
    public static boolean holds(Entity entity) {
        return entity.level().dimension() == ModDimensions.MASS_AUTHORITY;
    }

    /**
     * Turn one tick of falling into a tick of rising.
     *
     * `setNoGravity` first, every tick rather than once: an entity that reloads, or
     * that something else has touched, arrives with gravity back on, and a law that
     * only applied at spawn would be a law with exceptions nobody could predict.
     */
    public static void lift(Entity entity) {
        entity.setNoGravity(true);
        var move = entity.getDeltaMovement();
        double up = Math.min(move.y + RISE, MAX_RISE);
        entity.setDeltaMovement(move.x, up, move.z);
    }
}
