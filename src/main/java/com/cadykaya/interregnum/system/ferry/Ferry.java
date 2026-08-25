package com.cadykaya.interregnum.system.ferry;

import com.cadykaya.interregnum.core.ferry.Law;
import com.cadykaya.interregnum.core.ferry.Manifest;
import com.cadykaya.interregnum.registry.ModBlocks;
import com.cadykaya.interregnum.system.claim.Claims;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The dead god's mail-ferry.
 *
 * `WORLD.md`: *built and furnished from real blocks; a keel block captures the
 * structure, validates it against the destination's law, and re-places it at the far
 * pad.* Every hard problem in that sentence is in the first clause.
 *
 * <h2>The problem: how do you capture a boat without capturing the planet?</h2>
 *
 * A flood-fill from the keel through connected solid blocks eats the seabed, the
 * mountain the seabed is attached to, and eventually the world. The usual answers are
 * all bad: a fixed bounding box means the ferry is a template rather than something you
 * built; a "boat blocks only" whitelist means you furnish it from a catalogue.
 *
 * <h2>The answer was already in the codebase</h2>
 *
 * **The ferry takes only what a player PLACED.** {@link Claims} has recorded every
 * player-placed block since the unraveling needed to know what not to eat, so the
 * capture walks the claimed set and stops dead at natural terrain. A hull resting on
 * the seabed lifts off it. A hull carved *out of* the seabed does not float, and that
 * is correct — this is a thing you build, not a thing you dig.
 *
 * One system, three jobs now: it stops the apocalypse eating your house, it will decide
 * what attrition may generalise, and it is the hull of the ferry.
 *
 * <h2>The cap is real and it is named</h2>
 *
 * {@link #MAX_HULL} bounds the walk. When it bites the capture FAILS rather than
 * sailing with part of the boat: half a ferry arriving at a pad, with the other half
 * still at the dock, is the single worst outcome available and it would look like a
 * dupe bug rather than a limit.
 */
public final class Ferry {
    private static final Logger LOG = LogUtils.getLogger();

    private Ferry() {}

    /** The most blocks one hull may contain. */
    public static final int MAX_HULL = 4096;

    /** A captured hull: what it is, and where each piece sat relative to the keel. */
    public record Hull(Map<BlockPos, BlockState> blocks, Manifest manifest) {
        public int size() {
            return blocks.size();
        }
    }

    /** Why a capture produced nothing. Named, because "it didn't work" is not a reason. */
    public enum Refusal {
        /** The keel is fine and the hull is captured. */
        NONE,
        /** Nothing player-placed touches the keel: there is no boat here, only ground. */
        NOTHING_BUILT,
        /** The hull ran past {@link #MAX_HULL}. Better to refuse than to halve it. */
        TOO_LARGE,
        /**
         * There is no keel at that position.
         *
         * Without this the capture would happily start anywhere and teleport any
         * structure a player had built, which is a different mod. The keel is the
         * consent: you crossed when you laid one.
         */
        NOT_A_KEEL
    }

    public record Capture(Refusal refusal, Hull hull) {
        public boolean ok() {
            return refusal == Refusal.NONE;
        }
    }

    /**
     * Walk the player-placed blocks connected to the keel.
     *
     * Six-way adjacency, breadth-first, and the keel's own position is included --
     * a ferry that left its keel behind would strand the thing that moves it. The
     * keel is also the one block admitted without a claim: it is the origin of the
     * walk, so demanding a claim on it would make an unclaimable ferry unbuildable.
     */
    public static Capture capture(ServerLevel level, BlockPos keel) {
        if (!level.getBlockState(keel).is(ModBlocks.FERRY_KEEL.get())) {
            return new Capture(Refusal.NOT_A_KEEL, null);
        }
        Map<BlockPos, BlockState> hull = new LinkedHashMap<>();
        List<String> census = new ArrayList<>();
        Deque<BlockPos> frontier = new ArrayDeque<>();
        frontier.add(keel.immutable());

        while (!frontier.isEmpty()) {
            BlockPos pos = frontier.poll();
            if (hull.containsKey(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            // The whole trick: only what somebody built. Natural terrain is where the
            // boat stops, which is why a hull can rest on the ground and still lift.
            if (!pos.equals(keel) && !Claims.isClaimed(level, pos)) {
                continue;
            }
            if (hull.size() >= MAX_HULL) {
                LOG.warn("Ferry capture at {} exceeded {} blocks and was refused; a hull "
                        + "that sailed in halves would look like a dupe, not a limit.",
                        keel, MAX_HULL);
                return new Capture(Refusal.TOO_LARGE, null);
            }
            hull.put(pos, state);
            census.add(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            for (Direction d : Direction.values()) {
                BlockPos next = pos.relative(d);
                if (!hull.containsKey(next)) {
                    frontier.add(next);
                }
            }
        }

        if (hull.size() <= 1) {
            // Just the keel. Somebody has put it on the ground and pressed it.
            return new Capture(Refusal.NOTHING_BUILT, null);
        }
        return new Capture(Refusal.NONE, new Hull(hull, Manifest.of(census)));
    }

    /**
     * Move a captured hull so the keel lands on {@code pad}.
     *
     * Cleared before it is written, in two passes over the whole hull rather than
     * block-by-block. A one-pass move overwrites its own destination when origin and
     * destination overlap -- and a player who nudges a ferry three blocks sideways is
     * the most ordinary thing anybody will do with this.
     *
     * `tools/ferry_check.sh` nudges a hull two blocks along and asserts it arrives
     * whole. That leg has been watched failing against a deliberately one-pass
     * version of this method: it deletes the keel. Twice, in fact -- the first
     * version of the assertion could not fail at all (docs/LESSONS.md #24).
     */
    public static void place(ServerLevel level, Hull hull, BlockPos keel, BlockPos pad) {
        int dx = pad.getX() - keel.getX();
        int dy = pad.getY() - keel.getY();
        int dz = pad.getZ() - keel.getZ();

        for (BlockPos pos : hull.blocks().keySet()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            Claims.forget(level, pos);
        }
        for (var e : hull.blocks().entrySet()) {
            BlockPos to = e.getKey().offset(dx, dy, dz);
            level.setBlock(to, e.getValue(), 3);
            // It is still a thing somebody built. Losing that on the crossing would
            // hand the arriving ferry to the unraveling as scenery.
            Claims.record(level, to);
        }
        LOG.info("Ferry of {} block(s) crossed from {} to {}.", hull.size(), keel, pad);
    }

    /** The checklist a destination hands back, in the order a person reads it. */
    public static List<Manifest.Violation> checklist(Hull hull, Law law) {
        return hull.manifest().validate(law);
    }
}
