package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.core.magic.Casting;
import com.cadykaya.interregnum.core.magic.Grimoire;
import com.cadykaya.interregnum.core.magic.Loft;
import com.cadykaya.interregnum.system.ChapterSavedData;
import com.cadykaya.interregnum.system.claim.Claims;
import com.cadykaya.interregnum.system.unraveling.Unraveling;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Casting {@link Loft}: picking a small structure up, and putting it down again.
 *
 * <h2>The capture is the ferry's, and that is the whole reason this was cheap</h2>
 *
 * {@link com.cadykaya.interregnum.system.ferry.Ferry} solved *"how do you capture a boat
 * without capturing the planet"* by walking only what a player PLACED, so a hull resting
 * on the seabed lifts off it. A shed standing on a hillside is the same problem and takes
 * the same answer. The walk is repeated here rather than shared because the two differ in
 * the one place that matters -- a ferry's walk starts at a keel and admits it without a
 * claim, since the keel is the consent; a loft has no keel, so the block the caster names
 * must itself be something somebody built. Sharing the method would mean a flag, and the
 * flag would be "is this the ferry", which is not a property of a flood fill.
 *
 * <h2>Setting down refuses rather than overwrites</h2>
 *
 * The ferry writes onto its pad, which the ferry built. This lands wherever a player is
 * standing, and a shed set down through somebody's wall would eat the wall silently. So
 * every destination position is checked for air first and the whole cast is refused if any
 * is not -- all or nothing, for the same reason the ferry refuses an over-large hull
 * rather than sailing with half a boat.
 */
public final class LoftSpell {
    private LoftSpell() {}

    /** Why a lift or a set-down did or did not happen. */
    public enum Refusal {
        NONE,
        /** Schools are learned in their worlds; this one has not been. */
        UNLEARNED,
        /** Nothing player-placed within reach: there is no structure here, only ground. */
        NOTHING_BUILT,
        /** Past {@link Loft#MAX_BLOCKS}. "Small" is the locked word. */
        TOO_LARGE,
        /** Already carrying something. One structure, two hands. */
        HANDS_FULL,
        /** Nothing is being carried, so there is nothing to put down. */
        CARRYING_NOTHING,
        /** A load may only be set down in the world it was lifted from. */
        WRONG_WORLD,
        /** Something is already there, and a set-down must not eat it. */
        BLOCKED
    }

    public record Cast(Refusal refusal, int blocks, int frayed) {
        static Cast no(Refusal why) {
            return new Cast(why, 0, 0);
        }

        public boolean ok() {
            return refusal == Refusal.NONE;
        }
    }

    /**
     * Take up the structure the caster is standing at.
     *
     * The origin is the nearest player-placed block within {@link Loft#REACH} of
     * {@code pos}, so a caster does not have to name a corner exactly. From there the
     * walk is the ferry's: six-way, breadth-first, claimed blocks only.
     */
    public static Cast lift(ServerLevel level, BlockPos pos, UUID who, Grimoire grimoire) {
        if (!Casting.permitted(grimoire, Loft.SCHOOL)) {
            return Cast.no(Refusal.UNLEARNED);
        }
        Lofted held = Lofted.get(level.getServer());
        if (held.held(who) != null) {
            return Cast.no(Refusal.HANDS_FULL);
        }
        BlockPos origin = nearestBuilt(level, pos);
        if (origin == null) {
            return Cast.no(Refusal.NOTHING_BUILT);
        }

        Map<BlockPos, BlockState> found = new LinkedHashMap<>();
        Deque<BlockPos> frontier = new ArrayDeque<>();
        frontier.add(origin);
        while (!frontier.isEmpty()) {
            BlockPos at = frontier.poll();
            if (found.containsKey(at)) {
                continue;
            }
            BlockState state = level.getBlockState(at);
            if (state.isAir() || !Claims.isClaimed(level, at)) {
                continue;
            }
            if (Loft.tooLarge(found.size() + 1)) {
                // Refused whole, never truncated. A structure that came up in halves
                // would read as the spell eating part of a building, which is what it
                // most needs never to look like.
                return Cast.no(Refusal.TOO_LARGE);
            }
            found.put(at, state);
            for (Direction d : Direction.values()) {
                BlockPos next = at.relative(d);
                if (!found.containsKey(next)) {
                    frontier.add(next);
                }
            }
        }
        if (found.isEmpty()) {
            return Cast.no(Refusal.NOTHING_BUILT);
        }

        List<Lofted.Piece> pieces = new ArrayList<>(found.size());
        for (var e : found.entrySet()) {
            pieces.add(new Lofted.Piece(e.getKey().subtract(origin), e.getValue()));
        }
        for (BlockPos at : found.keySet()) {
            level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
            Claims.forget(level, at);
        }
        held.take(who, new Lofted.Load(level.dimension(), pieces));
        return new Cast(Refusal.NONE, pieces.size(), fray(level, pos));
    }

    /** Put down what is being carried, with the lift's own block landing on {@code pos}. */
    public static Cast place(ServerLevel level, BlockPos pos, UUID who, Grimoire grimoire) {
        if (!Casting.permitted(grimoire, Loft.SCHOOL)) {
            return Cast.no(Refusal.UNLEARNED);
        }
        Lofted store = Lofted.get(level.getServer());
        Lofted.Load load = store.held(who);
        if (load == null) {
            return Cast.no(Refusal.CARRYING_NOTHING);
        }
        if (!load.level().equals(level.dimension())) {
            return Cast.no(Refusal.WRONG_WORLD);
        }
        // Air, everywhere, before anything is written. All or nothing: a set-down that
        // overwrote what was already there would delete somebody's wall without a word.
        Set<BlockPos> targets = new HashSet<>();
        for (Lofted.Piece p : load.pieces()) {
            BlockPos at = pos.offset(p.offset());
            if (!level.getBlockState(at).isAir()) {
                return Cast.no(Refusal.BLOCKED);
            }
            targets.add(at);
        }
        for (Lofted.Piece p : load.pieces()) {
            BlockPos at = pos.offset(p.offset());
            level.setBlock(at, p.state(), 3);
            // Still somebody's work, and the ledger is what keeps the unraveling off it.
            Claims.record(level, at);
        }
        store.release(who);
        return new Cast(Refusal.NONE, load.size(), fray(level, pos));
    }

    /** The nearest player-placed block to the cast, or null if there is none in reach. */
    private static BlockPos nearestBuilt(ServerLevel level, BlockPos pos) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos at : BlockPos.betweenClosed(pos.offset(-Loft.REACH, -Loft.REACH, -Loft.REACH),
                                                  pos.offset(Loft.REACH, Loft.REACH, Loft.REACH))) {
            if (level.getBlockState(at).isAir() || !Claims.isClaimed(level, at)) {
                continue;
            }
            double d = at.distSqr(pos);
            if (d < bestDist) {
                bestDist = d;
                best = at.immutable();
            }
        }
        return best;
    }

    /**
     * What the cast spent.
     *
     * Both halves fray, and that is deliberate: carrying is the spell, so putting a thing
     * down is a cast rather than the end of one. A version that charged only for the lift
     * would make a loft cheaper the longer it was held, which is exactly backwards from
     * what the word weightless is doing.
     */
    private static int fray(ServerLevel level, BlockPos pos) {
        if (!Casting.drawsOnTheCorpse(level.dimension() == Level.OVERWORLD)) {
            return 0;
        }
        ChapterSavedData data = ChapterSavedData.get(level.getServer());
        return Unraveling.frayAround(level, pos, data, level.getRandom(),
                Casting.FRAY_RADIUS, Casting.FRAY_SAMPLES);
    }
}
