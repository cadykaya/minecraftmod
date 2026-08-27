package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.core.magic.HeldBreath;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Who is holding their breath.
 *
 * <h2>Not per world, unlike {@link Quelled}</h2>
 *
 * A quelled blaze belongs to the world it is in — it cannot leave, and a key that ignored
 * the level would let one world's quelling follow a mob id into another. A person can leave.
 * Somebody who holds their breath and walks into a portal is still holding it on the far
 * side, and a per-level map would have silently ended the spell at the door — which is the
 * one place {@link HeldBreath} is most for.
 *
 * <h2>In memory, and swept from the read</h2>
 *
 * Fifteen seconds of somebody being quiet is not worth a save file, and the same lazy sweep
 * every other short-lived state here uses: {@link #holds} drops what has expired as it goes,
 * so the cost of housekeeping is paid by the lookup that wanted the answer.
 *
 * This one is read from a game-event handler, which fires for every footstep in the world,
 * so the empty case has to be an early-out on an empty map rather than a scan. It is.
 */
public final class Holding {
    private Holding() {}

    private static final Map<UUID, Long> HELD = new HashMap<>();

    /** Take somebody's voice and their noise for {@link HeldBreath#DURATION_TICKS}. */
    public static void hold(ServerLevel level, UUID who) {
        HELD.put(who, HeldBreath.expiryAt(level.getGameTime()));
    }

    /** Is this person holding their breath right now? */
    public static boolean holds(ServerLevel level, UUID who) {
        if (HELD.isEmpty()) {
            return false;
        }
        Long expiry = HELD.get(who);
        if (expiry == null) {
            return false;
        }
        if (!HeldBreath.holds(expiry, level.getGameTime())) {
            HELD.remove(who);
            return false;
        }
        return true;
    }

    /** How many ticks this one has left. Zero for anybody breathing. For the seam. */
    public static long left(ServerLevel level, UUID who) {
        Long expiry = HELD.get(who);
        return expiry == null || !holds(level, who) ? 0 : expiry - level.getGameTime();
    }

    /** How many are holding. For the command seam. */
    public static int held() {
        return HELD.size();
    }

    /** Forget everything, on server shutdown. Same reason as the zones. */
    public static void clear() {
        HELD.clear();
    }
}
