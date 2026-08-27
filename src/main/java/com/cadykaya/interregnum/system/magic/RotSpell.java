package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.core.magic.Casting;
import com.cadykaya.interregnum.core.magic.Grimoire;
import com.cadykaya.interregnum.core.magic.Rot;
import com.cadykaya.interregnum.system.ChapterSavedData;
import com.cadykaya.interregnum.system.hearth.RottingLoader;
import com.cadykaya.interregnum.system.unraveling.Unraveling;
import com.cadykaya.interregnum.system.unraveling.UnravelingDefs.ConversionDef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Casting {@link Rot}. See that class for why the locked constraint needs no enforcing.
 *
 * The rotting table, aimed. Structurally the twin of {@link Weather} — one block, one
 * table, one step — and the difference is which table, which is the whole of the
 * difference between ageing a thing and finishing it.
 */
public final class RotSpell {
    private RotSpell() {}

    /** What one cast did. */
    public record Cast(Rot.Subject subject, String what, int frayed, String refused) {
        static Cast no(String why) {
            return new Cast(Rot.Subject.NOTHING, "", 0, why);
        }

        public boolean worked() {
            return subject != Rot.Subject.NOTHING;
        }
    }

    /**
     * Rot the block at {@code pos}.
     *
     * <b>The claim ledger is not consulted</b>, and that is `LESSONS.md` #35 rather than an
     * oversight: the ledger gates what you did not aim at, and this is one block somebody
     * pointed at. It is the same answer *Weather* and *Rewind* arrive at — a builder's
     * palette you cannot use on your own wall is not one, and neither is a demolition verb
     * that refuses your own ruin.
     *
     * <b>`canSurvive` is checked</b>, exactly as the ageing step checks it. A rule that put
     * a block somewhere it cannot stand would produce a block that pops off on the next
     * update, which is a spell that appears to work and then undoes itself.
     */
    public static Cast cast(ServerLevel level, BlockPos pos, Grimoire grimoire) {
        if (!Casting.permitted(grimoire, Rot.SCHOOL)) {
            return Cast.no("unlearned");
        }
        Cast done = rot(level, pos);
        if (!Casting.drawsOnTheCorpse(level.dimension() == Level.OVERWORLD)) {
            return done;
        }
        ChapterSavedData data = ChapterSavedData.get(level.getServer());
        int frayed = Unraveling.frayAround(level, pos, data, level.getRandom(),
                Casting.FRAY_RADIUS, Casting.FRAY_SAMPLES);
        return new Cast(done.subject(), done.what(), frayed, done.refused());
    }

    private static Cast rot(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        ConversionDef rule = RottingLoader.table().stepFrom(state.getBlock());
        if (rule == null) {
            return new Cast(Rot.Subject.NOTHING, "", 0, "");
        }
        BlockState next = rule.to().defaultBlockState();
        if (!next.canSurvive(level, pos)) {
            return new Cast(Rot.Subject.NOTHING, "", 0, "");
        }
        level.setBlock(pos, next, 3);
        return new Cast(Rot.Subject.THING,
                BuiltInRegistries.BLOCK.getKey(next.getBlock()).toString(), 0, "");
    }
}
