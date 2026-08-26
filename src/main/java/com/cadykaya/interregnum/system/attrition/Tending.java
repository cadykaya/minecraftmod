package com.cadykaya.interregnum.system.attrition;

import com.cadykaya.interregnum.core.attrition.Attrition;
import com.cadykaya.interregnum.registry.ModAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * Reading and writing the tending stamp. See {@link Attrition} for what it is for.
 *
 * The whole file is a seam: it owns the "how do I get at the attachment" detail so that
 * neither the tick handler nor the command has to know about it, and so the answer to
 * "is this ground stale" is written once.
 */
public final class Tending {
    private Tending() {}

    /**
     * Record that somebody is here now.
     *
     * `getChunk(..., false)` so tending never LOADS a chunk -- the same rule the statue
     * sweep, the ageing tick and the exodus leak all follow. Tending an unloaded chunk
     * would be meaningless anyway: nobody is standing in it.
     */
    public static void tend(ServerLevel level, int cx, int cz) {
        ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.FULL, false);
        if (chunk == null) {
            return;
        }
        if (chunk.getData(ModAttachments.TENDED).tend(level.getGameTime())) {
            chunk.markUnsaved();
        }
    }

/**
     * Everything one person standing here tends.
     *
     * Shared deliberately: the tick handler calls this once per player and the command
     * seam calls it once, so `interregnum attrition tend` is not a parallel
     * implementation that could drift from the real one -- it is the same method with a
     * different caller, the way `interregnum record deicide` is the second legitimate
     * caller of the deicide path rather than a hook bolted on for tests.
     */
    public static void tendAround(ServerLevel level, ChunkPos at) {
        for (int dx = -Attrition.TEND_RADIUS_CHUNKS; dx <= Attrition.TEND_RADIUS_CHUNKS; dx++) {
            for (int dz = -Attrition.TEND_RADIUS_CHUNKS; dz <= Attrition.TEND_RADIUS_CHUNKS; dz++) {
                // Asked through the core rule rather than implied by the loop bounds, so
                // the shape of the tended region lives in one place and the self-test's
                // assertions about it are assertions about this.
                if (Attrition.tends(dx, dz)) {
                    tend(level, at.x() + dx, at.z() + dz);
                }
            }
        }
    }

    /**
     * Has this ground gone unattended long enough to start losing what made it itself?
     *
     * <b>Fails closed, like {@link com.cadykaya.interregnum.system.claim.Claims}.</b> An
     * unloaded chunk answers "not stale" -- attrition must never act on ground it cannot
     * see, and a missing answer must never be read as permission. An unstamped one
     * answers the same way, for the reason in {@link Tended}.
     *
     * <b>Pure.</b> An earlier version stamped an unstamped chunk here, which made asking
     * a question change the answer: the same probe returned "never tended" and then
     * "tended just now", and a check that asked twice would have measured itself. First
     * sight is stamped where first sight actually happens -- chunk load, in
     * {@link TendingEvents} -- and reading is only reading.
     */
    public static boolean stale(ServerLevel level, ChunkPos at) {
        ChunkAccess chunk = level.getChunk(at.x(), at.z(), ChunkStatus.FULL, false);
        if (chunk == null) {
            return false;
        }
        Tended tended = chunk.getData(ModAttachments.TENDED);
        return !tended.isUnstamped()
                && Attrition.stale(tended.lastTended(), level.getGameTime());
    }

    /**
     * Stamp this chunk as last tended long enough ago that it has gone stale.
     *
     * An operator verb with a real meaning -- *mark this region abandoned* -- and the
     * only way a check can reach the rest of band 4's law, because ground goes stale
     * after twenty minutes and no CI run waits that out. `/time add` cannot help: it
     * moves dayTime while the stamp is compared against gameTime.
     *
     * <b>It is a state-setter, not a bypass.</b> It writes the same field {@link #tend}
     * writes, with a different value; every gate downstream -- the overworld, the band,
     * the claim ledger -- still applies to ground marked this way, exactly as it would to
     * ground that got there by being ignored for twenty minutes. The threshold ITSELF is
     * arithmetic and is proven in core's self-test, on both sides of the boundary.
     */
    public static void abandon(ServerLevel level, ChunkPos at) {
        ChunkAccess chunk = level.getChunk(at.x(), at.z(), ChunkStatus.FULL, false);
        if (chunk == null) {
            return;
        }
        chunk.getData(ModAttachments.TENDED)
                .tend(level.getGameTime() - Attrition.FRAY_AFTER_TICKS - 1);
        chunk.markUnsaved();
    }

    /** For the command seam: how long since anybody was here, in ticks. -1 if unknown. */
    public static long sinceTended(ServerLevel level, ChunkPos at) {
        ChunkAccess chunk = level.getChunk(at.x(), at.z(), ChunkStatus.FULL, false);
        if (chunk == null) {
            return -1;
        }
        Tended tended = chunk.getData(ModAttachments.TENDED);
        return tended.isUnstamped() ? -1 : level.getGameTime() - tended.lastTended();
    }
}
