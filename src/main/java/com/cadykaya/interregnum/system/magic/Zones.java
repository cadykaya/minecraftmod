package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.core.magic.Spell;
import com.cadykaya.interregnum.core.magic.Zone;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

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
     * Zones are keyed by SPELL, not by school and not pooled. Both narrowings were
     * forced by the same shape of bug arriving twice: one list per world broke when a
     * second spell of any school opened a zone, and keying by school broke when one
     * school turned out to have two zone spells -- Hush and Still are both Silence.
     *
     * Nothing fails when the colliding case arrives. The two zones simply become each
     * other, and from inside either spell that looks exactly like both of them working.
     * See {@link Spell}.
     */
    private static final Map<ResourceKey<Level>, Map<Spell, List<Zone>>> ACTIVE =
            new HashMap<>();

    /** Open a zone belonging to one school. */
    public static void open(ServerLevel level, Spell spell, Zone zone) {
        ACTIVE.computeIfAbsent(level.dimension(), k -> new HashMap<>())
                .computeIfAbsent(spell, k -> new ArrayList<>()).add(zone);
    }

    /**
     * Is this position inside a zone that is still in force?
     *
     * Sweeps as it goes. Called from an entity tick, so it must stay cheap for the
     * overwhelmingly common case of no zones at all in this world -- which is the
     * `isEmpty` early-out, and is why the map is keyed by dimension rather than scanned.
     */
    public static boolean covering(ServerLevel level, Spell spell, BlockPos pos) {
        return inForce(level, spell, z -> z.covers(pos.getX(), pos.getY(), pos.getZ()));
    }

    /**
     * Is this position under, over or inside a zone -- anywhere in its column?
     *
     * The Anchorite's shaft, and nothing else. See {@link Zone#coversColumn}: the cube is
     * where the spell's physics apply and the column is where the world has stopped
     * having a floor, and the two have to agree about the edge, which is why the test
     * lives on the zone rather than here.
     */
    public static boolean columnCovering(ServerLevel level, Spell spell, BlockPos pos) {
        return inForce(level, spell, z -> z.coversColumn(pos.getX(), pos.getZ()));
    }

    /**
     * Every zone of one spell that covers a position.
     *
     * The Quiet One's door, and nothing else. That portal IS a {@link Hush} zone, so it
     * needs the zones themselves rather than a yes or no -- it has to know which door a
     * noise disturbed, and a boolean cannot say.
     *
     * Sweeps like the other reads, and returns a fresh list rather than a view: the caller
     * marks zones noisy while iterating, and handing out the live list would be handing
     * out the sweep's own iteration target.
     */
    public static List<Zone> zonesCovering(ServerLevel level, Spell spell, BlockPos pos) {
        Map<Spell, List<Zone>> bySpell = ACTIVE.get(level.dimension());
        if (bySpell == null) {
            return List.of();
        }
        List<Zone> zones = bySpell.get(spell);
        if (zones == null || zones.isEmpty()) {
            return List.of();
        }
        long now = level.getGameTime();
        List<Zone> found = new ArrayList<>(1);
        var it = zones.iterator();
        while (it.hasNext()) {
            Zone z = it.next();
            if (z.expired(now)) {
                it.remove();
            } else if (z.covers(pos.getX(), pos.getY(), pos.getZ())) {
                found.add(z);
            }
        }
        return found;
    }

    /**
     * The sweep, and whatever question is being asked of what survives it.
     *
     * Both public queries run through here so that housekeeping cannot drift between
     * them: a second copy of this loop that forgot to remove expired zones would leave
     * the two callers disagreeing about which zones exist, and the symptom would be a
     * shaft that outlived the field that opened it.
     */
    private static boolean inForce(ServerLevel level, Spell spell, Predicate<Zone> test) {
        Map<Spell, List<Zone>> bySpell = ACTIVE.get(level.dimension());
        if (bySpell == null) {
            return false;
        }
        List<Zone> zones = bySpell.get(spell);
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
            if (test.test(z)) {
                inside = true;
                // No early return: the sweep is the other half of this method's job and
                // stopping here would leave expired zones behind whenever a live one
                // happened to be found first.
            }
        }
        return inside;
    }

    /** How many zones are in force here. For the command seam. */
    public static int count(ServerLevel level, Spell spell) {
        Map<Spell, List<Zone>> bySpell = ACTIVE.get(level.dimension());
        if (bySpell == null) {
            return 0;
        }
        List<Zone> zones = bySpell.get(spell);
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
