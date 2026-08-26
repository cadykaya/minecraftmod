package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.core.magic.Casting;
import com.cadykaya.interregnum.core.magic.Grimoire;
import com.cadykaya.interregnum.core.magic.Rewind;
import com.cadykaya.interregnum.system.ChapterSavedData;
import com.cadykaya.interregnum.system.hearth.TurningLoader;
import com.cadykaya.interregnum.system.unraveling.UnravelingDefs.ConversionDef;
import com.cadykaya.interregnum.system.unraveling.Unraveling;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Casting {@link Rewind}. See that class for the two decisions in it — that this spell may
 * touch what a player built, and that some blocks have more than one past.
 *
 * The ageing table read backwards, through {@code StepTable.stepTo}. *Weather* and this
 * are one table in two directions, which is why the Turning's second spell cost almost no
 * new machinery — and it is the same doctrine that made the Turning and the unraveling
 * share a registry to begin with.
 */
public final class RewindSpell {
    private RewindSpell() {}

    /** What one cast did: the block it restored, what it cost, and why not. */
    public record Cast(BlockState became, int frayed, String refused) {
        public boolean worked() {
            return became != null;
        }

        static Cast no(String why) {
            return new Cast(null, 0, why);
        }
    }

    public static Cast cast(ServerLevel level, BlockPos pos, Grimoire grimoire) {
        if (!Casting.permitted(grimoire, Rewind.SCHOOL)) {
            return Cast.no("unlearned");
        }
        BlockState state = level.getBlockState(pos);
        ConversionDef rule = TurningLoader.table().stepTo(state.getBlock());
        if (rule == null) {
            // Either nothing ages into this, or more than one thing does. The table
            // cannot tell the two apart and neither can this -- see Rewind's javadoc for
            // why refusing is the characterful answer rather than the cautious one.
            return Cast.no("no-single-past");
        }
        BlockState was = rule.from().defaultBlockState();
        if (!was.canSurvive(level, pos)) {
            return Cast.no("unsupported");
        }
        // NOTE the absence of a claim check, and see Rewind's javadoc. The ledger stops
        // the WORLD eating your work; it is not there to stop you working on it, and a
        // Rewind that refused your own wall would be useless at the one thing it is for.
        level.setBlockAndUpdate(pos, was);
        if (!Casting.drawsOnTheCorpse(level.dimension() == Level.OVERWORLD)) {
            return new Cast(was, 0, "");
        }
        ChapterSavedData data = ChapterSavedData.get(level.getServer());
        int frayed = Unraveling.frayAround(level, pos, data, level.getRandom(),
                Casting.FRAY_RADIUS, Casting.FRAY_SAMPLES);
        return new Cast(was, frayed, "");
    }

    /** The block id a cast produced, for the command seam. */
    public static String describe(Cast cast) {
        return cast.worked()
                ? BuiltInRegistries.BLOCK.getKey(cast.became().getBlock()).toString()
                : cast.refused();
    }
}
