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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

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

    /**
     * How far above the far side's ground you arrive.
     *
     * Two blocks, which is a step rather than a drop. The tempting version puts you in at
     * the top of the world still falling, on the reasoning that you fell in and should
     * keep falling — and it is wrong twice: it drops you through terrain you have not
     * loaded yet, and it makes a two-second commitment into a twelve-second one you
     * cannot see the end of. The shaft is the door; the fall was how you opened it.
     */
    private static final int STEP_DOWN = 2;

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
     * @return the entity on the far side, or null if it did not go. Cross-dimension
     *         travel builds a NEW entity and removes the old one, so the caller must not
     *         keep using the one it passed in.
     */
    public static Entity take(ServerLevel from, Entity entity) {
        Descent.Layer layer = layerOf(from);
        if (layer == null) {
            return null;
        }
        ServerLevel to = from.getServer().getLevel(levelOf(layer.beyond()));
        if (to == null) {
            return null;
        }
        BlockPos landing = arrival(to, entity.blockPosition());
        // FORGET BEFORE THE CROSSING, not after: the UUID survives a dimension change, so
        // an entity that arrived with its count intact would satisfy `opens` on its first
        // tick on the far side and bounce straight back. The symptom is a thing flickering
        // between two worlds forever, and the fix is one line in the right order.
        Descending.forget(entity.getUUID());
        Entity landed = entity.teleport(new TeleportTransition(
                to,
                Vec3.atBottomCenterOf(landing),
                // Arriving with the velocity you left with means arriving at terminal
                // speed after two seconds of falling, and taking the fall damage for it
                // from a two-block step. The door does not hurt anybody.
                Vec3.ZERO,
                entity.getYRot(), entity.getXRot(),
                TeleportTransition.PLACE_PORTAL_TICKET));
        if (landed != null) {
            // Vanilla's own cooldown, so a thing that goes through and immediately lets
            // go again is not eligible for a moment. Nothing depends on it -- the count
            // was cleared above -- but it is the mechanism the game already has for
            // "just came through a portal", and a portal that did not set it would be
            // lying to everything else that asks.
            landed.setPortalCooldown();
            Descending.forget(landed.getUUID());
        }
        return landed;
    }

    /**
     * Where the far side puts you.
     *
     * Same column, on that world's own ground. The heightmap is `MOTION_BLOCKING` rather
     * than `WORLD_SURFACE` because what is wanted is the first thing that would stop a
     * fall, which is the question actually being asked, and the two differ over water and
     * leaves in exactly the cases where the difference matters.
     *
     * The pocket is cleared unconditionally. A destination computed from a heightmap is
     * right about the terrain and knows nothing about what a player built there since, and
     * arriving inside somebody's floor is the one failure a portal must not have.
     */
    private static BlockPos arrival(ServerLevel to, BlockPos from) {
        int ground = to.getHeight(Heightmap.Types.MOTION_BLOCKING, from.getX(), from.getZ());
        BlockPos landing = new BlockPos(from.getX(), ground + STEP_DOWN, from.getZ());
        for (int dy = 0; dy <= 1; dy++) {
            BlockPos at = landing.above(dy);
            if (!to.getBlockState(at).isAir()) {
                to.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        return landing;
    }

    /** The spell that opens it, for anything that wants to say so. */
    public static Spell key() {
        return Descent.KEY;
    }
}
