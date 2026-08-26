package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.core.magic.Casting;
import com.cadykaya.interregnum.core.magic.Grimoire;
import com.cadykaya.interregnum.core.magic.Lighten;
import com.cadykaya.interregnum.system.ChapterSavedData;
import com.cadykaya.interregnum.system.unraveling.Unraveling;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Casting {@link Lighten}. See that class for what the spell is and why it has a shape.
 *
 * The whole of this file is the two rules every spell shares — you must have been taught
 * it, and at home it costs — plus one line that opens the zone. That symmetry with
 * {@link Weather} is the point of having a second spell at all: it is what shows the
 * school system is a system rather than one hardcoded case.
 */
public final class LightenSpell {
    private LightenSpell() {}

    /** What one cast did: whether it took, how many places it cost, and why not. */
    public record Cast(boolean opened, int frayed, String refused) {
        static Cast no(String why) {
            return new Cast(false, 0, why);
        }
    }

    public static Cast cast(ServerLevel level, BlockPos pos, Grimoire grimoire) {
        if (!Casting.permitted(grimoire, Lighten.SCHOOL)) {
            return Cast.no("unlearned");
        }
        Zones.open(level, Lighten.zoneAt(pos.getX(), pos.getY(), pos.getZ(),
                level.getGameTime()));
        if (!Casting.drawsOnTheCorpse(level.dimension() == Level.OVERWORLD)) {
            return new Cast(true, 0, "");
        }
        ChapterSavedData data = ChapterSavedData.get(level.getServer());
        int frayed = Unraveling.frayAround(level, pos, data, level.getRandom(),
                Casting.FRAY_RADIUS, Casting.FRAY_SAMPLES);
        return new Cast(true, frayed, "");
    }
}
