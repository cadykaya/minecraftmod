package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.core.magic.Casting;
import com.cadykaya.interregnum.core.magic.Grimoire;
import com.cadykaya.interregnum.core.magic.Hedge;
import com.cadykaya.interregnum.system.ChapterSavedData;
import com.cadykaya.interregnum.system.claim.Claims;
import com.cadykaya.interregnum.system.unraveling.Unraveling;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Casting {@link Hedge}, and what a hedge does when it is cut.
 *
 * <h2>Persistent leaves, on purpose</h2>
 *
 * `PERSISTENT` is set on every block a hedge is made of. Without it vanilla's own leaf
 * decay would take the wall down a few seconds after it was grown, because there is no log
 * anywhere near it — a spell that quietly dismantled itself, for a reason nothing in this
 * mod would have reported.
 */
public final class HedgeSpell {
    private HedgeSpell() {}

    /** What a hedge is made of. */
    private static BlockState leaf() {
        return Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, true);
    }

    /** What one cast did. */
    public record Cast(int grew, int frayed, String refused) {
        static Cast no(String why) {
            return new Cast(0, 0, why);
        }
    }

    /**
     * Draw a wall from {@code from} toward {@code toward}.
     *
     * <b>It never replaces anything</b>, for the reason {@code BridgerootSpell} gives: a
     * spell that ate terrain would eat whatever somebody built in the way. Unlike the
     * bridge it does not stop at the first obstruction — a wall with a tree in the middle
     * of it is still a wall, and stopping there would make hedging along a hillside
     * impossible. It skips what is occupied and keeps going.
     */
    public static Cast cast(ServerLevel level, BlockPos from, BlockPos toward,
                            Grimoire grimoire) {
        if (!Casting.permitted(grimoire, Hedge.SCHOOL)) {
            return Cast.no("unlearned");
        }
        Hedges hedges = Hedges.get(level);
        int grew = 0;
        for (int[] step : Hedge.wall(toward.getX() - from.getX(), toward.getZ() - from.getZ())) {
            BlockPos at = from.offset(step[0], step[1], step[2]);
            if (!level.getBlockState(at).isAir()) {
                continue;
            }
            level.setBlockAndUpdate(at, leaf());
            hedges.grow(at);
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

    /**
     * Somebody cut a hedge block. Grow more.
     *
     * <b>The block they struck really is gone.</b> A hedge is not invulnerable — a
     * methodical person gets through — and restoring the block would make it a wall that
     * cannot be attacked, which is a different and much less interesting promise than
     * `WORLD.md`'s *improved by being attacked*.
     *
     * What grows is {@link Hedge#THICKENS_BY} blocks in air beside what is left, so the
     * wall gets denser exactly where it was hit. Ordered by how much hedge each candidate
     * already touches, so it fills the wall in rather than sprawling outward — a hedge that
     * grew into open ground on every strike would be a thicket somebody made by defending
     * themselves.
     *
     * @return how many grew. Zero when the world's hedge budget is spent, which is
     *         {@link Hedge#MAX_BLOCKS} and exists because *grows when struck* is otherwise
     *         a way to fill a world with leaves by hitting a bush.
     */
    public static int struck(ServerLevel level, BlockPos cut) {
        Hedges hedges = Hedges.get(level);
        if (!hedges.cut(cut)) {
            return 0;                        // not a hedge; an ordinary tree is safe
        }
        int room = Hedge.thickening(hedges.count());
        if (room == 0) {
            return 0;
        }
        List<BlockPos> candidates = new ArrayList<>();
        // Two rings: beside the wound, and beside those. One ring alone leaves a hedge cut
        // at its own edge with nowhere legal to grow, and the spell would look like it had
        // a budget it does not have.
        for (Direction d : Direction.values()) {
            BlockPos near = cut.relative(d);
            candidates.add(near);
            for (Direction e : Direction.values()) {
                candidates.add(near.relative(e));
            }
        }
        // THE WOUND IS NOT A CANDIDATE, and it takes a line to say so because the two-ring
        // walk finds its way back: `cut.relative(d).relative(d.getOpposite())` is `cut`.
        // Without this the hedge grows straight back into the hole, which is a wall that
        // closes its own wound -- unattackable rather than improved by being attacked, and
        // a much less interesting thing to have built.
        candidates.removeIf(p -> p.equals(cut) || !level.getBlockState(p).isAir());
        // Densest first: the most hedge already touching. A stable sort over a fixed
        // candidate order, so one strike does the same thing as the next.
        candidates.sort((a, b) -> Integer.compare(touching(level, hedges, b),
                touching(level, hedges, a)));
        int grew = 0;
        for (BlockPos at : candidates) {
            if (grew >= room) {
                break;
            }
            if (touching(level, hedges, at) == 0 || hedges.is(at)) {
                continue;                    // never grow off on its own
            }
            level.setBlockAndUpdate(at, leaf());
            hedges.grow(at);
            Claims.record(level, at);
            grew++;
        }
        return grew;
    }

    /** How many hedge blocks this position has for neighbours. */
    private static int touching(ServerLevel level, Hedges hedges, BlockPos pos) {
        int n = 0;
        for (Direction d : Direction.values()) {
            if (hedges.is(pos.relative(d))) {
                n++;
            }
        }
        return n;
    }
}
