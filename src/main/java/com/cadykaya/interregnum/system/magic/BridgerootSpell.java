package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.core.magic.Bridgeroot;
import com.cadykaya.interregnum.core.magic.Casting;
import com.cadykaya.interregnum.core.magic.Grimoire;
import com.cadykaya.interregnum.system.ChapterSavedData;
import com.cadykaya.interregnum.system.claim.Claims;
import com.cadykaya.interregnum.system.unraveling.Unraveling;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Casting {@link Bridgeroot}. See that class for what the spell is and why its geometry
 * lives in `core`.
 *
 * <h2>What it grows is YOURS, and that is the decision worth arguing about</h2>
 *
 * Every block a span leaves behind is recorded through {@link Claims}, exactly as if you
 * had placed it by hand. The consequence is that the apocalypse will not eat it: the
 * unraveling, the Turning and band 4's attrition all consult the same ledger and all
 * refuse anything a player put there.
 *
 * That is the only reading that makes `WORLD.md`'s *"real persistent blocks"* mean
 * anything. A bridge the world dissolves next chapter is a temporary platform with extra
 * steps, and a player who lost one that way would — correctly — never trust the spell
 * again. Growing something and having it be YOURS is what separates this from a movement
 * ability, and it is what lets Verdancy be a building school rather than a traversal one.
 *
 * <h2>It never replaces anything</h2>
 *
 * A span grows into air and stops at the first block it meets. Not a limitation — the
 * alternative is a spell that eats terrain, and worse, a spell that eats whatever
 * somebody built in the way. Stopping short is legible; boring through is not.
 */
public final class BridgerootSpell {
    private BridgerootSpell() {}

    /** What one cast did: how many blocks grew, what it cost, and why not. */
    public record Cast(int grew, int frayed, String refused) {
        static Cast no(String why) {
            return new Cast(0, 0, why);
        }
    }

    public static Cast cast(ServerLevel level, BlockPos from, BlockPos toward,
                            Grimoire grimoire) {
        if (!Casting.permitted(grimoire, Bridgeroot.SCHOOL)) {
            return Cast.no("unlearned");
        }
        int grew = 0;
        for (int[] step : Bridgeroot.span(toward.getX() - from.getX(),
                toward.getY() - from.getY(), toward.getZ() - from.getZ())) {
            BlockPos at = from.offset(step[0], step[1], step[2]);
            if (!level.getBlockState(at).isAir()) {
                break;                       // stop at what is already there
            }
            level.setBlockAndUpdate(at, Blocks.MANGROVE_ROOTS.defaultBlockState());
            Claims.record(level, at);        // grown, therefore yours
            grew++;
        }
        if (grew == 0) {
            return Cast.no("blocked");
        }
        if (!Casting.drawsOnTheCorpse(level.dimension() == Level.OVERWORLD)) {
            return new Cast(grew, 0, "");
        }
        ChapterSavedData data = ChapterSavedData.get(level.getServer());
        int frayed = Unraveling.frayAround(level, from, data, level.getRandom(),
                Casting.FRAY_RADIUS, Casting.FRAY_SAMPLES);
        return new Cast(grew, frayed, "");
    }
}
