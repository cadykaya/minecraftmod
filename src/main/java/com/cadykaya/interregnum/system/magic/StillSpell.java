package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.core.magic.Casting;
import com.cadykaya.interregnum.core.magic.Grimoire;
import com.cadykaya.interregnum.core.magic.Spell;
import com.cadykaya.interregnum.core.magic.Still;
import com.cadykaya.interregnum.system.ChapterSavedData;
import com.cadykaya.interregnum.system.unraveling.Unraveling;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Casting {@link Still}. See that class for how it differs from Hush, its own school-mate. */
public final class StillSpell {
    private StillSpell() {}

    /** What one cast did. */
    public record Cast(boolean opened, int frayed, String refused) {
        static Cast no(String why) {
            return new Cast(false, 0, why);
        }
    }

    public static Cast cast(ServerLevel level, BlockPos pos, Grimoire grimoire) {
        if (!Casting.permitted(grimoire, Still.SCHOOL)) {
            return Cast.no("unlearned");
        }
        Zones.open(level, Spell.STILL,
                Still.zoneAt(pos.getX(), pos.getY(), pos.getZ(), level.getGameTime()));
        if (!Casting.drawsOnTheCorpse(level.dimension() == Level.OVERWORLD)) {
            return new Cast(true, 0, "");
        }
        ChapterSavedData data = ChapterSavedData.get(level.getServer());
        int frayed = Unraveling.frayAround(level, pos, data, level.getRandom(),
                Casting.FRAY_RADIUS, Casting.FRAY_SAMPLES);
        return new Cast(true, frayed, "");
    }
}
