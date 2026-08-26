package com.cadykaya.interregnum.system.dialogue;

import com.cadykaya.interregnum.core.haunt.Manifestation;
import com.cadykaya.interregnum.system.ChapterSavedData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * The one thing the dead god does that somebody else can see.
 *
 * `WORLD.md`, locked: *"Rarely, a manifestation is server-real — a bystander sees the door
 * move too. Not a sanity bar: a **credibility problem**."*
 *
 * <h2>A door, because the locked text says a door</h2>
 *
 * The list of things that could be moved is long and every addition to it makes the beat
 * worse. A door is the right object precisely because it is the most ordinary one: it
 * moves on its own for six mundane reasons, a person who reports it sounds like somebody
 * reporting a draught, and the killer is the only one in the room who knows it was not.
 * That is the credibility problem, in one block.
 *
 * Trapdoors and fence gates are deliberately not included. They would double the hit rate
 * for nothing -- the beat is not "something opened", it is that a *door* did.
 *
 * <h2>The claim ledger does not gate this, and the reason is worth stating</h2>
 *
 * Everywhere else in the mod, a volume effect nobody aimed at spares what a player placed.
 * The ledger exists so the unraveling cannot <b>unmake</b> somebody's work. Opening a door
 * unmakes nothing, and a ghost that could only move doors the player had not hung would be
 * a poltergeist with a property deed -- it would never touch the one door in the world
 * that would mean anything.
 *
 * <h2>Every decision is here, where a command can reach it</h2>
 *
 * Same arrangement as {@link TheHaunt}: a headless server has no players standing near
 * doors, so the gate lives in one method and the tick handler is a few lines over it. It
 * is also the operator tool it looks like, for the same reason the dream's is.
 */
public final class Manifest {
    private static final Logger LOG = LogUtils.getLogger();

    private Manifest() {}

    /** What the ghost did, or why it did not. */
    public enum Outcome {
        /** A door moved, and anybody standing there saw it. */
        MOVED,
        /** The god still lives. Doors stay shut. */
        NO_GHOST,
        /** It is bound to its killer and to nobody else. */
        NOT_THE_KILLER,
        /**
         * There was nothing within reach to move.
         *
         * Not a failure. A haunting in an empty field has nothing to work with, and
         * inventing something for it to move would be the mod supplying its own evidence.
         */
        NOTHING_TO_MOVE
    }

    /**
     * Have the ghost move a door near {@code near}, if it is theirs to be haunted by.
     *
     * <b>No dimension check.</b> `WORLD.md` locks the binding as *"personal, permanent"* --
     * it is not a property of the overworld, it is a property of the person, and a ghost
     * that stayed home would be a location rather than a haunting.
     */
    public static Outcome move(ServerLevel level, BlockPos near, UUID who) {
        ChapterSavedData data = ChapterSavedData.get(level.getServer());
        if (data.mechanicsDormant()) {
            return Outcome.NO_GHOST;
        }
        if (!who.equals(data.killer())) {
            return Outcome.NOT_THE_KILLER;
        }
        BlockPos door = nearestDoor(level, near);
        if (door == null) {
            return Outcome.NOTHING_TO_MOVE;
        }
        BlockState state = level.getBlockState(door);
        // Vanilla's own method, rather than a setBlock: it carries the sound and the game
        // event with it, and both are the point. A door that changed state silently would
        // be seen by nobody, which is the exact opposite of "server-real".
        ((DoorBlock) state.getBlock()).setOpen(null, level, state, door,
                !state.getValue(DoorBlock.OPEN));
        LOG.info("A door at {} moved for {}.", door, who);
        return Outcome.MOVED;
    }

    /**
     * The nearest lower half of a door within {@link Manifestation#REACH}.
     *
     * The LOWER half specifically. A door is two blocks and `setOpen` is written against
     * the half that carries the hinge; handed the upper one it would set a state the other
     * half does not agree with, and the door would render as half open forever.
     */
    private static BlockPos nearestDoor(ServerLevel level, BlockPos near) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        int r = Manifestation.REACH;
        for (BlockPos at : BlockPos.betweenClosed(near.offset(-r, -r, -r), near.offset(r, r, r))) {
            BlockState state = level.getBlockState(at);
            if (!(state.getBlock() instanceof DoorBlock)
                    || state.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) {
                continue;
            }
            double d = at.distSqr(near);
            if (d < bestDist) {
                bestDist = d;
                best = at.immutable();
            }
        }
        return best;
    }
}
