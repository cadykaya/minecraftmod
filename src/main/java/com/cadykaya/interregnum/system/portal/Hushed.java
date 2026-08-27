package com.cadykaya.interregnum.system.portal;

import com.cadykaya.interregnum.core.magic.Zone;
import com.cadykaya.interregnum.core.portal.Stillness;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * When each silence was last broken.
 *
 * <h2>Keyed by the zone itself</h2>
 *
 * {@link Zone} is a record, so two zones are the same zone when they are the same cube
 * expiring at the same tick — which is exactly the identity wanted here, and means no id
 * had to be invented for a thing that already had one. A zone that lapses takes its entry
 * with it on the next sweep.
 *
 * <h2>Absence means "never disturbed", not "silent since the beginning of time"</h2>
 *
 * A zone with no entry has not been quiet for ever; it has been quiet since it was cast,
 * which is a different and much shorter span. {@link Stillness#since} takes the later of
 * the opening tick and the last noise for precisely this reason, and this class never
 * answers the question on its own — it only reports the noise.
 */
public final class Hushed {
    private Hushed() {}

    private static final Map<ResourceKey<Level>, Map<Zone, Long>> DISTURBED = new HashMap<>();

    /** Something happened inside this zone at this tick. */
    public static void broken(ServerLevel level, Zone zone, long tick) {
        DISTURBED.computeIfAbsent(level.dimension(), k -> new HashMap<>()).put(zone, tick);
    }

    /**
     * When the silence in this zone last started, counting a fresh cast as a start.
     *
     * Sweeps lapsed zones out of the level's map as it goes, so a world where nobody has
     * cast anything for an hour holds nothing.
     */
    public static long since(ServerLevel level, Zone zone) {
        Map<Zone, Long> here = DISTURBED.get(level.dimension());
        if (here == null) {
            return Stillness.openedAt(zone);
        }
        long now = level.getGameTime();
        here.keySet().removeIf(z -> z.expired(now));
        return Stillness.since(Stillness.openedAt(zone), here.getOrDefault(zone, Long.MIN_VALUE));
    }

    /** How many silences are being tracked as broken. For the command seam. */
    public static int disturbed(ServerLevel level) {
        Map<Zone, Long> here = DISTURBED.get(level.dimension());
        return here == null ? 0 : here.size();
    }

    /** Forget everything, on server shutdown. Same reason as the zones themselves. */
    public static void clear() {
        DISTURBED.clear();
    }
}
