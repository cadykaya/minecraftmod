package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.core.magic.Casting;
import com.cadykaya.interregnum.core.magic.DropForge;
import com.cadykaya.interregnum.core.magic.Grimoire;
import com.cadykaya.interregnum.core.magic.Spell;
import com.cadykaya.interregnum.system.ChapterSavedData;
import com.cadykaya.interregnum.system.unraveling.Unraveling;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Casting {@link DropForge}. See that class for what the spell is and why it does nothing
 * by itself.
 *
 * Identical in shape to {@link LightenSpell}, down to the line count, and that is worth
 * one sentence: five spells now open a zone and every one of them is taught-check, open,
 * pay. The differences between spells live entirely in their tables and their handlers,
 * which is what it means for the school system to be a system.
 */
public final class DropForgeSpell {
    private DropForgeSpell() {}

    /** What one cast did: whether it took, how many places it cost, and why not. */
    public record Cast(boolean opened, int frayed, String refused) {
        static Cast no(String why) {
            return new Cast(false, 0, why);
        }
    }

    public static Cast cast(ServerLevel level, BlockPos pos, Grimoire grimoire) {
        if (!Casting.permitted(grimoire, DropForge.SCHOOL)) {
            return Cast.no("unlearned");
        }
        Zones.open(level, Spell.DROP_FORGE,
                DropForge.zoneAt(pos.getX(), pos.getY(), pos.getZ(), level.getGameTime()));
        if (!Casting.drawsOnTheCorpse(level.dimension() == Level.OVERWORLD)) {
            return new Cast(true, 0, "");
        }
        ChapterSavedData data = ChapterSavedData.get(level.getServer());
        int frayed = Unraveling.frayAround(level, pos, data, level.getRandom(),
                Casting.FRAY_RADIUS, Casting.FRAY_SAMPLES);
        return new Cast(true, frayed, "");
    }
}
