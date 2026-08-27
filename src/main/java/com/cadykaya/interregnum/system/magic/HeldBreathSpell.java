package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.core.magic.Casting;
import com.cadykaya.interregnum.core.magic.Grimoire;
import com.cadykaya.interregnum.core.magic.HeldBreath;
import com.cadykaya.interregnum.system.ChapterSavedData;
import com.cadykaya.interregnum.system.unraveling.Unraveling;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Casting {@link HeldBreath}. See that class for what the spell is and what it costs.
 *
 * <h2>It cannot be cast on somebody else, and it cannot be cast twice</h2>
 *
 * The caster is the subject: `WORLD.md` says *"your own sound"*, and a version that could
 * be aimed would be a silence you inflict, which is a different spell and a much worse one
 * — every stealth complaint about the mod would be about the person who used it on you.
 *
 * Re-casting is not refused so much as impossible: the second cast would be a spoken word,
 * and the first cast took the voice that would say it. {@link Speech} does the refusing, up
 * where the word is heard, which is exactly where the fiction says the refusal happens.
 */
public final class HeldBreathSpell {
    private HeldBreathSpell() {}

    /** What one cast did. */
    public record Cast(boolean held, int frayed, String refused) {
        static Cast no(String why) {
            return new Cast(false, 0, why);
        }
    }

    public static Cast cast(ServerLevel level, BlockPos at, UUID who, Grimoire grimoire) {
        if (!Casting.permitted(grimoire, HeldBreath.SCHOOL)) {
            return Cast.no("unlearned");
        }
        Holding.hold(level, who);
        if (!Casting.drawsOnTheCorpse(level.dimension() == Level.OVERWORLD)) {
            return new Cast(true, 0, "");
        }
        ChapterSavedData data = ChapterSavedData.get(level.getServer());
        int frayed = Unraveling.frayAround(level, at, data, level.getRandom(),
                Casting.FRAY_RADIUS, Casting.FRAY_SAMPLES);
        return new Cast(true, frayed, "");
    }
}
