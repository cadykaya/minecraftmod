package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.core.magic.Moor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The things currently moored, per world, and exactly where each of them is.
 *
 * <h2>The position is the spell</h2>
 *
 * {@link Quelled} stores an expiry and nothing else, because a quelled blaze is quelled
 * wherever it flies to. This one has to remember **where**: the whole of {@link Moor} is
 * that the thing does not move, and a mooring that only knew *when* would have to ask the
 * entity where it was — which is asking the thing that is being pushed where it has been
 * pushed to.
 *
 * The position is captured once, at the cast, and never updated. That is what makes the
 * spell an anchor rather than a brake: a brake lets a thing drift and then slows it, and
 * this puts it back.
 *
 * <h2>Per world, unlike {@link Holding}</h2>
 *
 * A person who holds their breath and walks through a portal is still holding it, so that
 * map ignores the level. A moored thing cannot go anywhere by definition — that is the
 * spell — so a position without a world would be a coordinate that means something
 * different in every one of the nine, and the first thing to cross a portal while moored
 * would be snapped to the wrong place in the wrong world.
 */
public final class Moored {
    private Moored() {}

    /** Where a thing was moored, and when the mooring lets go. */
    private record Anchor(Vec3 where, long expiry) {}

    private static final Map<ResourceKey<Level>, Map<UUID, Anchor>> ACTIVE = new HashMap<>();

    /** Fix one thing where it is for {@link Moor#DURATION_TICKS}. */
    public static void moor(ServerLevel level, UUID what, Vec3 where) {
        ACTIVE.computeIfAbsent(level.dimension(), k -> new HashMap<>())
                .put(what, new Anchor(where, Moor.expiryAt(level.getGameTime())));
    }

    /**
     * Where this thing is moored, or null if it is free.
     *
     * Swept from the read, like every other short-lived state here: the lookup that wanted
     * the answer pays for the housekeeping. Called from an entity tick, so the common case
     * of a world with nothing moored in it is an early-out on an empty map.
     */
    public static Vec3 anchorOf(ServerLevel level, UUID what) {
        Map<UUID, Anchor> here = ACTIVE.get(level.dimension());
        if (here == null || here.isEmpty()) {
            return null;
        }
        Anchor anchor = here.get(what);
        if (anchor == null) {
            return null;
        }
        if (!Moor.holds(anchor.expiry(), level.getGameTime())) {
            here.remove(what);
            return null;
        }
        return anchor.where();
    }

    /** Is this thing moored? */
    public static boolean holds(ServerLevel level, UUID what) {
        return anchorOf(level, what) != null;
    }

    /** How many things are moored here. For the command seam. */
    public static int count(ServerLevel level) {
        Map<UUID, Anchor> here = ACTIVE.get(level.dimension());
        if (here == null) {
            return 0;
        }
        here.values().removeIf(a -> !Moor.holds(a.expiry(), level.getGameTime()));
        return here.size();
    }

    /** Forget everything, on server shutdown. Same reason as the zones. */
    public static void clear() {
        ACTIVE.clear();
    }
}
