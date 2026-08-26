package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.core.magic.Casting;
import com.cadykaya.interregnum.core.magic.Grimoire;
import com.cadykaya.interregnum.core.magic.Hush;
import com.cadykaya.interregnum.system.ChapterSavedData;
import com.cadykaya.interregnum.system.unraveling.Unraveling;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Casting {@link Hush}. See that class for what the spell is and what it does not claim. */
public final class HushSpell {
    private HushSpell() {}

    /** What one cast did. */
    public record Cast(boolean opened, int frayed, String refused) {
        static Cast no(String why) {
            return new Cast(false, 0, why);
        }
    }

    public static Cast cast(ServerLevel level, BlockPos pos, Grimoire grimoire) {
        if (!Casting.permitted(grimoire, Hush.SCHOOL)) {
            return Cast.no("unlearned");
        }
        Zones.open(level, com.cadykaya.interregnum.core.magic.Spell.HUSH,
                Hush.zoneAt(pos.getX(), pos.getY(), pos.getZ(), level.getGameTime()));
        if (!Casting.drawsOnTheCorpse(level.dimension() == Level.OVERWORLD)) {
            return new Cast(true, 0, "");
        }
        ChapterSavedData data = ChapterSavedData.get(level.getServer());
        int frayed = Unraveling.frayAround(level, pos, data, level.getRandom(),
                Casting.FRAY_RADIUS, Casting.FRAY_SAMPLES);
        return new Cast(true, frayed, "");
    }
}
