package com.cadykaya.interregnum.system.portal;

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
 * Going through a door, whichever god's it is.
 *
 * <h2>One arrival, shared, because arriving is not where the gods differ</h2>
 *
 * The Anchorite's shaft and the Verdant's grown door are nothing alike — one is a hole you
 * fall down and the other is a tree you stand under — and `WORLD.md` says so: *"a portal
 * per god was four ideas; a portal each god's own school opens is one."* What is one is
 * the moment of passage. Every portal has to put somebody down on the far side without
 * burying them, hurting them, or bouncing them straight back, and there is no version of
 * that which is characteristic of a god.
 *
 * Keeping it here rather than copying it per portal is not tidiness. The three failures it
 * avoids are each silent and each shows up in a different world.
 */
public final class Crossing {
    private Crossing() {}

    /**
     * How far above the far side's ground you arrive.
     *
     * Two blocks, a step rather than a drop. The tempting version drops you in at the top
     * of the world still moving, on the reasoning that you should keep going the way you
     * came — and it is wrong twice: it puts you through terrain that has not loaded, and
     * it turns a short commitment into a long one you cannot see the end of.
     */
    private static final int STEP = 2;

    /**
     * Take one thing to another world.
     *
     * @return the thing on the far side, or null if it did not go. Cross-dimension travel
     *         BUILDS A NEW ENTITY and removes the old one, so the caller must not keep
     *         using the reference it passed in. The UUID is preserved, which is why every
     *         caller clears its own counters explicitly rather than trusting the crossing
     *         to have lost them.
     */
    public static Entity into(ServerLevel from, Entity entity, ResourceKey<Level> destination) {
        ServerLevel to = from.getServer().getLevel(destination);
        if (to == null) {
            return null;
        }
        Entity landed = entity.teleport(new TeleportTransition(
                to,
                Vec3.atBottomCenterOf(arrival(to, entity.blockPosition())),
                // Not the velocity you left with. Something that has been falling for two
                // seconds is at terminal speed, and arriving that way means taking the
                // fall damage for it off a two-block step. A door does not hurt anybody.
                Vec3.ZERO,
                entity.getYRot(), entity.getXRot(),
                TeleportTransition.PLACE_PORTAL_TICKET));
        if (landed != null) {
            // Vanilla's own cooldown. Nothing here depends on it -- each portal clears the
            // state that would re-trigger it -- but it is the mechanism the game already
            // has for "just came through", and a portal that left it unset would be lying
            // to everything else that asks.
            landed.setPortalCooldown();
        }
        return landed;
    }

    /**
     * Where the far side puts you.
     *
     * Same column, on that world's own ground. `MOTION_BLOCKING` rather than
     * `WORLD_SURFACE` because the question actually being asked is *what would stop a
     * fall*, and the two differ over water and leaves in exactly the cases where the
     * difference matters.
     *
     * The pocket is cleared unconditionally. A destination computed from a heightmap is
     * right about the terrain and knows nothing about what somebody built there since,
     * and arriving inside a floor is the one failure a portal must not have.
     */
    private static BlockPos arrival(ServerLevel to, BlockPos from) {
        int ground = to.getHeight(Heightmap.Types.MOTION_BLOCKING, from.getX(), from.getZ());
        BlockPos landing = new BlockPos(from.getX(), ground + STEP, from.getZ());
        for (int dy = 0; dy <= 1; dy++) {
            BlockPos at = landing.above(dy);
            if (!to.getBlockState(at).isAir()) {
                to.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        return landing;
    }
}
