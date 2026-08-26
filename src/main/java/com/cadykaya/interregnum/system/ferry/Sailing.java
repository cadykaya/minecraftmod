package com.cadykaya.interregnum.system.ferry;

import com.cadykaya.interregnum.core.ferry.Law;
import com.cadykaya.interregnum.core.ferry.Manifest;
import com.cadykaya.interregnum.core.ferry.Routing;
import com.cadykaya.interregnum.system.letters.Letters;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.List;

/**
 * One crossing, from the decision to the arrival.
 *
 * <h2>Why this exists as a class</h2>
 *
 * The whole crossing used to live inside the `interregnum ferry sail` command handler,
 * where an operator typed the law by name. `WORLD.md` now locks the player-facing
 * affordance — *the ferry sails where the letter in your hand is addressed* — and a keel
 * being touched has to do the identical thing. (On what "addressed" turns out to mean, and
 * the reading of it that would have made a whole god unreachable, see {@link Routing}.) Two callers, one sequence, and the sequence
 * has eight ways to refuse: leaving it in the command and copying it into the block would
 * have produced two ferries that disagreed about what a berth is.
 *
 * <h2>The docket is still the empty-handed answer</h2>
 *
 * Touching a keel with nothing in hand hands back the checklist for all four crossings,
 * which is `WORLD.md`'s *"the validation checklist teaches each world's rule before
 * arrival"*. Touching it with a letter sails. **Ask with an empty hand, go with a full
 * one** — and neither affordance had to be explained to build the other.
 */
public final class Sailing {
    private static final Logger LOG = LogUtils.getLogger();

    private Sailing() {}

    /** Why a crossing did not happen, or that it did. */
    public enum Outcome {
        SAILED,
        /** The hull did not clear the destination's checklist. See {@link Sail#held}. */
        HELD,
        /** Nothing held, an unmarked stack, or a letter the post does not have. */
        NOT_A_LETTER,
        /** A letter naming a crossing the datapack does not have. */
        NO_SUCH_CROSSING,
        /** The keel, the hull, or the size of it. Carries {@link Ferry.Refusal}. */
        NO_HULL,
        /** The law names a dimension this server does not have. */
        NO_DESTINATION,
        /** There is no dock at the far end and none could be built. */
        NO_PAD,
        /** A ferry is already sitting in the berth. */
        BERTH_OCCUPIED
    }

    /**
     * What one crossing did. {@code held} is empty unless the outcome is {@link
     * Outcome#HELD}, in which case it is the checklist a player is owed.
     */
    public record Sail(Outcome outcome, String law, ResourceKey<Level> to, int blocks,
                       List<Manifest.Violation> held, String detail) {
        static Sail no(Outcome why, String detail) {
            return new Sail(why, "", null, 0, List.of(), detail);
        }

        public boolean ok() {
            return outcome == Outcome.SAILED;
        }
    }

    /**
     * Sail the hull at {@code keel} on the crossing the letter {@code letterId} names.
     *
     * The player-facing entry point. Everything about *which* crossing comes from the
     * letter and nothing from the caller, which is the locked rule: a keel that could be
     * told where to go by any other means would make the mail decorative.
     */
    public static Sail byLetter(ServerLevel from, BlockPos keel, String letterId) {
        var letter = letterId == null ? null : Letters.forGod(letterId);
        var lawId = Routing.lawFor(letter);
        if (lawId.isEmpty()) {
            return Sail.no(Outcome.NOT_A_LETTER, String.valueOf(letterId));
        }
        return sail(from, keel, lawId.get(), null);
    }

    /**
     * Sail the hull at {@code keel} on the named crossing.
     *
     * @param pad where to put it down, or {@code null} to use the destination's own dock.
     *            A named pad is the nudge case -- a hull moved a few blocks inside one
     *            world, where there is no dock involved and no berth to be occupied.
     */
    public static Sail sail(ServerLevel from, BlockPos keel, String lawId, BlockPos pad) {
        Law law = FerryLaws.of(lawId);
        if (law == null) {
            return Sail.no(Outcome.NO_SUCH_CROSSING, lawId);
        }
        var cap = Ferry.capture(from, keel);
        if (!cap.ok()) {
            return Sail.no(Outcome.NO_HULL, String.valueOf(cap.refusal()));
        }
        var bad = Ferry.checklist(cap.hull(), law);
        if (!bad.isEmpty()) {
            return new Sail(Outcome.HELD, lawId, null, cap.hull().manifest().total(), bad, "");
        }
        // Where the law says this crossing goes. Never a parameter: the destination is a
        // property of the law the hull was cleared against, so a hull cleared for the
        // Quiet One cannot be sailed anywhere else.
        ResourceKey<Level> target = FerryLaws.destinationOf(lawId);
        ServerLevel to = from.getServer().getLevel(target);
        if (to == null) {
            // A law may legitimately name a dimension another datapack supplies, so this
            // is refused here rather than at load, where it would take down every law in
            // the file instead of naming the one that is missing.
            return Sail.no(Outcome.NO_DESTINATION, target.identifier().toString());
        }
        BlockPos arrival = pad != null ? pad : FerryPad.ensure(to);
        if (arrival == null) {
            return Sail.no(Outcome.NO_PAD, target.identifier().toString());
        }
        if (pad == null && FerryPad.occupied(to, arrival)) {
            return Sail.no(Outcome.BERTH_OCCUPIED, target.identifier().toString());
        }
        Ferry.place(from, cap.hull(), keel, to, arrival);
        // The return leg, filed on departure. A mail service knows where its vessels came
        // from; see Voyages for why the way home is a record rather than a fifth law.
        Voyages.get(from.getServer()).departed(from.dimension(), keel, target, arrival);
        LOG.info("A ferry of {} block(s) sailed on the {} crossing to {}.",
                cap.hull().manifest().total(), lawId, target.identifier());
        return new Sail(Outcome.SAILED, lawId, target, cap.hull().manifest().total(),
                List.of(), "");
    }
}
