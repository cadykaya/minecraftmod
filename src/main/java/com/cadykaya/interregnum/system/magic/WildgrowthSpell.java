package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.core.magic.Casting;
import com.cadykaya.interregnum.core.magic.Grimoire;
import com.cadykaya.interregnum.core.magic.Wildgrowth;
import com.cadykaya.interregnum.system.ChapterSavedData;
import com.cadykaya.interregnum.system.unraveling.Unraveling;
import com.cadykaya.interregnum.system.verdant.Verdant;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Casting {@link Wildgrowth}. See that class for what the spell is and where the claim
 * ledger falls.
 *
 * One line of it is the spell and the rest is the two rules every spell shares. The one
 * line calls {@link Verdant#quicken} — the Verdant's own law, which the Verdant's world
 * and band 3's leaks already run — rather than a growth routine of its own.
 */
public final class WildgrowthSpell {
    private WildgrowthSpell() {}

    /** What one cast did: how many blocks moved on, what it cost, and why not. */
    public record Cast(int grew, int frayed, String refused) {
        static Cast no(String why) {
            return new Cast(0, 0, why);
        }

        /** Whether the cast happened at all. A surge that found nothing alive still cast. */
        public boolean worked() {
            return refused.isEmpty();
        }
    }

    public static Cast cast(ServerLevel level, BlockPos pos, Grimoire grimoire) {
        if (!Casting.permitted(grimoire, Wildgrowth.SCHOOL)) {
            return Cast.no("unlearned");
        }
        // TRUE: the ledger gates what you did not aim at, and a cube is full of it.
        int grew = Verdant.quicken(level, pos, Wildgrowth.RADIUS, Wildgrowth.PUSHES, true);
        if (!Casting.drawsOnTheCorpse(level.dimension() == Level.OVERWORLD)) {
            return new Cast(grew, 0, "");
        }
        ChapterSavedData data = ChapterSavedData.get(level.getServer());
        int frayed = Unraveling.frayAround(level, pos, data, level.getRandom(),
                Casting.FRAY_RADIUS, Casting.FRAY_SAMPLES);
        return new Cast(grew, frayed, "");
    }
}
