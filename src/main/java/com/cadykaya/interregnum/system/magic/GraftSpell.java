package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.core.magic.Casting;
import com.cadykaya.interregnum.core.magic.Graft;
import com.cadykaya.interregnum.core.magic.Grimoire;
import com.cadykaya.interregnum.system.ChapterSavedData;
import com.cadykaya.interregnum.system.claim.Claims;
import com.cadykaya.interregnum.system.unraveling.Unraveling;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Casting {@link Graft}, and keeping what it joined alive.
 *
 * <h2>The spell is the tending, not the cast</h2>
 *
 * Casting it puts one block down and writes a line in a ledger. Everything that makes it a
 * spell happens afterwards, on a timer: the world removes a plant that has no business
 * being where it is, and the graft puts it back, for as long as the thing feeding it is
 * still there.
 */
public final class GraftSpell {
    private GraftSpell() {}

    /** What one cast did. */
    public record Cast(Graft.Outcome outcome, int frayed) {
        static Cast no(Graft.Outcome why) {
            return new Cast(why, 0);
        }

        public boolean took() {
            return outcome == Graft.Outcome.TAKEN;
        }
    }

    /**
     * Join {@code stock} to {@code scion}.
     *
     * <b>The stock must be growing and the scion must be free.</b> Both are asked of the
     * world rather than of the caster: `WORLD.md` says *"join two growing things, or a
     * growing thing to a block"*, and the growing thing is the one doing the feeding.
     */
    public static Cast cast(ServerLevel level, BlockPos stock, BlockPos scion,
                            Grimoire grimoire) {
        if (!Casting.permitted(grimoire, Graft.SCHOOL)) {
            return Cast.no(Graft.Outcome.UNLEARNED);
        }
        if (!Graft.reaches(scion.getX() - stock.getX(), scion.getY() - stock.getY(),
                scion.getZ() - stock.getZ())) {
            return Cast.no(Graft.Outcome.OUT_OF_REACH);
        }
        BlockState growing = level.getBlockState(stock);
        // The same question `Ripen` and `Verdant.quicken` ask, and for the same reason:
        // every crop, sapling, vine and mushroom is covered without being named, and so is
        // whatever the next game drop adds.
        if (!growing.isRandomlyTicking()) {
            return Cast.no(Graft.Outcome.NOTHING_TO_GRAFT);
        }
        Grafts grafts = Grafts.get(level);
        if (grafts.count() >= Graft.MAX_JOINS) {
            return Cast.no(Graft.Outcome.TOO_MANY);
        }
        if (!level.getBlockState(scion).isAir() || grafts.touches(scion)
                || grafts.touches(stock)) {
            return Cast.no(Graft.Outcome.OCCUPIED);
        }
        place(level, scion, growing);
        Claims.record(level, scion);          // grown, therefore yours
        grafts.join(new Grafts.Join(stock.immutable(), scion.immutable(), growing));
        if (!Casting.drawsOnTheCorpse(level.dimension() == Level.OVERWORLD)) {
            return new Cast(Graft.Outcome.TAKEN, 0);
        }
        ChapterSavedData data = ChapterSavedData.get(level.getServer());
        int frayed = Unraveling.frayAround(level, stock, data, level.getRandom(),
                Casting.FRAY_RADIUS, Casting.FRAY_SAMPLES);
        return new Cast(Graft.Outcome.TAKEN, frayed);
    }

    /**
     * Put the scion down without asking its neighbours.
     *
     * <b>Flag 2 — clients, no neighbour update — and it is the whole spell working.</b>
     * `setBlockAndUpdate` makes the block it just placed ask *can I survive here?*, and the
     * answer for a plant somewhere it could not live is no, in the same instant. A graft
     * built on it would spend its life re-placing a block that removes itself: a flicker
     * rather than a plant.
     *
     * Placed this way the scion stands until something **else** looks — a neighbour
     * changing, a random tick, somebody walking into it — and then the graft puts it back
     * on its next round. That is the difference between a plant nothing is holding up and a
     * plant that is being held up, and it is the sentence `WORLD.md` locks: *a plant lives
     * somewhere it could not.*
     */
    private static void place(ServerLevel level, BlockPos at, BlockState what) {
        level.setBlock(at, what, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }

    /**
     * Look at every join in this world, and act on what has changed.
     *
     * Two things can have happened since the last look:
     *
     * <ul>
     *   <li><b>The stock is gone.</b> The graft ends — the scion is released, and because
     *       it is a plant somewhere it could not survive, the world removes it on its own
     *       within a tick or two. Nothing here removes it, which is the point: the spell
     *       stops holding, and what happens next is ordinary.</li>
     *   <li><b>The scion is gone.</b> Something removed it, which for a plant with no
     *       business being there is what the world does the moment anything makes it look.
     *       It goes back.</li>
     * </ul>
     *
     * @return how many scions were put back.
     */
    public static int tend(ServerLevel level) {
        Grafts grafts = Grafts.get(level);
        if (grafts.count() == 0) {
            return 0;
        }
        int restored = 0;
        for (Grafts.Join join : grafts.all()) {
            if (!level.getBlockState(join.stock()).isRandomlyTicking()) {
                // Cut, aged out, grown into something else, or eaten by the apocalypse.
                // However it went, it is not feeding anything now.
                grafts.cut(join);
                continue;
            }
            if (level.getBlockState(join.scion()).equals(join.was())) {
                continue;
            }
            if (!level.getBlockState(join.scion()).isAir()) {
                // Somebody has built there. The graft loses -- a spell that overwrote a
                // block a person put down would be a spell that eats other people's work
                // on a timer, which is the one shape this mod refuses everywhere else.
                grafts.cut(join);
                continue;
            }
            place(level, join.scion(), join.was());
            restored++;
        }
        return restored;
    }
}
