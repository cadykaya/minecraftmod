package com.cadykaya.interregnum.system.portal;

import com.cadykaya.interregnum.core.magic.Spell;
import com.cadykaya.interregnum.core.portal.Descent;
import com.cadykaya.interregnum.system.magic.Zones;
import com.cadykaya.interregnum.worldgen.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * The Anchorite's shaft, at the point where it actually moves something.
 *
 * {@link Descent} is the rule; this is the world. Everything that decides lives in one
 * place, so that the entity tick, the command seam and any later caller are asking the
 * same question — the same arrangement as the rite, the spoken word and the ferry's
 * crossing.
 */
public final class Shaft {
    private Shaft() {}

    /** What happened when something let go. */
    public enum Outcome {
        /** It went through. There is one fewer thing in this world. */
        THROUGH,
        /** This world has no shaft — it is not one of the Anchorite's two layers. */
        NO_SHAFT,
        /** Nothing has let go here for long enough. */
        HOLDING_ON
    }

    /**
     * Which layer this is, or null for a world the shaft has no opinion about.
     *
     * The Anchorite's two levels and nothing else. A Lighten zone in the overworld, or in
     * another god's world, is the spell and only the spell: `WORLD.md` locks the portal
     * as *this god's*, and a door that opened wherever the school was cast would make
     * every god's system reachable from every other one without a ferry.
     */
    public static Descent.Layer layerOf(ServerLevel level) {
        ResourceKey<Level> here = level.dimension();
        if (here == ModDimensions.MASS_AUTHORITY) {
            return Descent.Layer.SURFACE;
        }
        return here == ModDimensions.MASS_AUTHORITY_LOWER ? Descent.Layer.LOWER : null;
    }

    /** The level a layer names. */
    public static ResourceKey<Level> levelOf(Descent.Layer layer) {
        return layer == Descent.Layer.SURFACE
                ? ModDimensions.MASS_AUTHORITY : ModDimensions.MASS_AUTHORITY_LOWER;
    }

    /**
     * Is the shaft open here?
     *
     * A Lighten zone's footprint, in one of the Anchorite's two layers. Nothing about the
     * entity: the shaft is a fact about a place, and asking it separately from whether
     * anybody has let go is what lets the command seam report on a shaft with nobody in
     * it.
     */
    public static boolean open(ServerLevel level, BlockPos pos) {
        return layerOf(level) != null && Zones.columnCovering(level, Descent.KEY, pos);
    }

    /**
     * Carry one thing upward, on the side where down does not hold.
     *
     * <h2>Why this is not {@code Anchorite.lift}</h2>
     *
     * It is the same law and deliberately not the same method, which is the opposite of
     * the choice {@code AnchoriteEvents} makes and the reason is worth keeping. That
     * method switches gravity OFF and then nudges upward, which is exactly right for the
     * only thing it is ever applied to: a falling block rises until vanilla discards it
     * past the build height, and nothing ever has to be switched back on.
     *
     * A pig is not discarded. Switching its gravity off inside a field that lapses after
     * thirty seconds leaves a pig hanging in the air of a world with no sky, and nothing
     * anywhere would ever turn it back on — the flag outlives the spell, the caster and
     * the session. So this sets a velocity every tick instead and touches no flags at
     * all: when the field goes, the last delta decays and the world takes it back with no
     * bookkeeping, because there is nothing to undo.
     *
     * The fall distance goes with it. Two seconds of rising ends in a landing on the far
     * side, and a portal that hurt you for using it would be a portal nobody used twice.
     */
    public static void buoy(Entity entity) {
        var move = entity.getDeltaMovement();
        entity.setDeltaMovement(move.x, Descent.RISE_PER_TICK, move.z);
        entity.resetFallDistance();
    }

    /**
     * Take one thing through.
     *
     * @return the thing on the far side, or null if it did not go. See
     *         {@link Crossing#into}: the reference passed in is dead afterwards.
     */
    public static Entity take(ServerLevel from, Entity entity) {
        Descent.Layer layer = layerOf(from);
        if (layer == null) {
            return null;
        }
        // FORGET BEFORE THE CROSSING, not after: the UUID survives a dimension change, so
        // something that arrived with its count intact would satisfy `opens` on its first
        // tick on the far side and bounce straight back. The symptom is a thing flickering
        // between two worlds for ever, and the fix is one line in the right order.
        Descending.forget(entity.getUUID());
        Entity landed = Crossing.into(from, entity, levelOf(layer.beyond()));
        if (landed != null) {
            Descending.forget(landed.getUUID());
        }
        return landed;
    }


    /** The spell that opens it, for anything that wants to say so. */
    public static Spell key() {
        return Descent.KEY;
    }
}
