package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.core.magic.Quell;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The creatures currently quelled, per world.
 *
 * <h2>Keyed by entity, which is what makes it a different thing from {@link Zones}</h2>
 *
 * A zone is somewhere; this is something one creature carries. So the key is the mob's
 * id and not a position, and a quelled blaze stays quelled across the room it flies to.
 * There is no radius here at all -- {@link Quell#REACH} decides who gets picked, once, at
 * the moment of the cast, and after that the spell has nothing to do with where anybody is.
 *
 * <h2>In memory, for the same reason zones are</h2>
 *
 * Forty-five seconds of a mob not shooting is not worth a save file, and a quelling that
 * outlived the server would leave a blaze somebody stopped a week ago still unable to
 * defend itself with nothing anywhere saying why.
 *
 * <h2>Swept from the read, not from a tick</h2>
 *
 * {@link #holds} drops what has expired as it goes, so housekeeping is paid for by the
 * lookup that wanted the answer. A sweeper on its own timer would be a second thing to
 * keep in step with this one. Same arrangement as {@link Zones}, and for the same reason.
 */
public final class Quelled {
    private Quelled() {}

    private static final Map<ResourceKey<Level>, Map<UUID, Long>> ACTIVE = new HashMap<>();

    /** Take one creature's throwing arm until {@link Quell#DURATION_TICKS} have passed. */
    public static void mark(ServerLevel level, UUID mob) {
        ACTIVE.computeIfAbsent(level.dimension(), k -> new HashMap<>())
                .put(mob, Quell.expiryAt(level.getGameTime()));
    }

    /**
     * Is this creature quelled right now?
     *
     * Called from a projectile spawning, which happens rarely -- but "rarely" here means
     * every arrow in a skeleton fight, so the empty-world case is still an early-out on a
     * map lookup rather than a scan.
     */
    public static boolean holds(ServerLevel level, UUID mob) {
        Map<UUID, Long> byMob = ACTIVE.get(level.dimension());
        if (byMob == null || byMob.isEmpty()) {
            return false;
        }
        long now = level.getGameTime();
        byMob.values().removeIf(expiry -> !Quell.holds(expiry, now));
        Long expiry = byMob.get(mob);
        return expiry != null && Quell.holds(expiry, now);
    }

    /** How many creatures are quelled here. For the command seam. */
    public static int count(ServerLevel level) {
        Map<UUID, Long> byMob = ACTIVE.get(level.dimension());
        if (byMob == null) {
            return 0;
        }
        long now = level.getGameTime();
        byMob.values().removeIf(expiry -> !Quell.holds(expiry, now));
        return byMob.size();
    }

    /**
     * Forget everything, on server shutdown.
     *
     * Entity ids are not reused across worlds, so a leftover entry is harmless in a way
     * a leftover {@link Zones} entry is not -- a stale coordinate means something else in
     * the next world, a stale UUID means nothing anywhere. It is cleared anyway, because
     * a static map that only grows is a leak whether or not it is ever read.
     */
    public static void clear() {
        ACTIVE.clear();
    }
}
