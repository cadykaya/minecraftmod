package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.core.magic.School;
import com.cadykaya.interregnum.core.magic.Zone;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The spell zones currently in force, per world.
 *
 * <h2>In memory, and the class says why</h2>
 *
 * See {@link Zone}: a spell whose effect outlived the server could strand somebody inside
 * a field cast by a player who has since left. Everything permanent in this mod is
 * persisted; half a minute of altered physics is deliberately not.
 *
 * <h2>Swept lazily, from the same read that asks about them</h2>
 *
 * There is no tick handler here. Expired zones are dropped by {@link #covering}, which is
 * called by the entity tick that needs the answer anyway — so the cost of housekeeping is
 * paid by the thing that benefits from it, and a world with no zones costs one empty-list
 * check. A sweeper on its own timer would be a second thing to keep in step with this one.
 */
public final class Zones {
    private Zones() {}

/**
     * Which spell opened a zone.
     *
     * Zones are keyed by school rather than pooled, because two spells now open them and
     * they mean different things: standing in a Lighten field must not silence a creeper,
     * and standing in a Hush must not make the gravel float. A single list would have
     * made every zone do everything the moment the second spell shipped -- and it would
     * have looked like the spells working, from inside either one.
     */
    private static final Map<ResourceKey<Level>, Map<School, List<Zone>>> ACTIVE =
            new HashMap<>();

    /** Open a zone belonging to one school. */
    public static void open(ServerLevel level, School school, Zone zone) {
        ACTIVE.computeIfAbsent(level.dimension(), k -> new HashMap<>())
                .computeIfAbsent(school, k -> new ArrayList<>()).add(zone);
    }

    /**
     * Is this position inside a zone that is still in force?
     *
     * Sweeps as it goes. Called from an entity tick, so it must stay cheap for the
     * overwhelmingly common case of no zones at all in this world -- which is the
     * `isEmpty` early-out, and is why the map is keyed by dimension rather than scanned.
     */
    public static boolean covering(ServerLevel level, School school, BlockPos pos) {
        Map<School, List<Zone>> bySchool = ACTIVE.get(level.dimension());
        if (bySchool == null) {
            return false;
        }
        List<Zone> zones = bySchool.get(school);
        if (zones == null || zones.isEmpty()) {
            return false;
        }
        long now = level.getGameTime();
        boolean inside = false;
        var it = zones.iterator();
        while (it.hasNext()) {
            Zone z = it.next();
            if (z.expired(now)) {
                it.remove();
                continue;
            }
            if (z.covers(pos.getX(), pos.getY(), pos.getZ())) {
                inside = true;
                // No early return: the sweep is the other half of this method's job and
                // stopping here would leave expired zones behind whenever a live one
                // happened to be found first.
            }
        }
        return inside;
    }

    /** How many zones are in force here. For the command seam. */
    public static int count(ServerLevel level, School school) {
        Map<School, List<Zone>> bySchool = ACTIVE.get(level.dimension());
        if (bySchool == null) {
            return 0;
        }
        List<Zone> zones = bySchool.get(school);
        if (zones == null) {
            return 0;
        }
        zones.removeIf(z -> z.expired(level.getGameTime()));
        return zones.size();
    }

    /**
     * Forget everything, on server shutdown.
     *
     * A static map outlives a world in a dev environment where servers start and stop in
     * one JVM, and a zone left over from the last world would apply to the next one at
     * coordinates that mean something else entirely.
     */
    public static void clear() {
        ACTIVE.clear();
    }
}
