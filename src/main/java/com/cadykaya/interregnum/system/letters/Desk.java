package com.cadykaya.interregnum.system.letters;

import com.cadykaya.interregnum.core.letters.Transcription;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/**
 * The codex desk: the safe way to read the dead god's hand.
 *
 * <h2>It is a lectern, and that is an argument this codebase has already made</h2>
 *
 * `WORLD.md` names *"the ferry's own desk"* and *"the codex desk"* and never says what one
 * looks like. A lectern is a reading desk, vanilla has one, and {@link
 * com.cadykaya.interregnum.system.ferry.FerryPad} already argues the case in blocks:
 * *"an institution does not redesign its dock for each god. It has a standard dock."* The
 * Post used what was to hand, in every world, and that is the joke the rest of the mod's
 * bureaucracy runs on said once more.
 *
 * A lectern does nothing unusual unless somebody offers it one of four letters. Books,
 * quills and every other use of one are untouched.
 *
 * <h2>What the desk is for, and what it is not</h2>
 *
 * It does not read the letter to you and it does not tell you anything. It makes a copy,
 * and after that copy exists nobody who reads that letter is reading the god's own hand any
 * more. The hazard is not in the words — it is in whose handwriting they are in.
 */
public final class Desk {
    private Desk() {}

    /** What happened at a desk. */
    public enum Outcome {
        /** The letter is with the clerk. */
        LODGED,
        /** Something else is already on this desk. */
        OCCUPIED,
        /** Taken back, and there is a copy in the Post now. */
        COLLECTED,
        /** The clerk is not finished. */
        NOT_YET,
        /** Nothing is on this desk. */
        EMPTY,
        /** Not a desk. */
        NO_DESK
    }

    /**
     * Whether there is a desk here.
     *
     * The block is checked at the moment of use rather than remembered, so a lectern
     * somebody breaks stops being a desk immediately — while the letter on it survives in
     * the ledger, because losing one of four letters to a broken block would be
     * unrecoverable. See {@link Codex}.
     */
    public static boolean isDesk(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.LECTERN);
    }

    /** Leave a letter with the clerk. */
    public static Outcome lodge(ServerLevel level, BlockPos pos, String letter) {
        if (!isDesk(level, pos)) {
            return Outcome.NO_DESK;
        }
        return Codex.get(level.getServer()).lodge(pos, letter, level.getGameTime())
                ? Outcome.LODGED : Outcome.OCCUPIED;
    }

    /**
     * Take it back, if the work is done.
     *
     * @return the letter id on success, or null. The outcome is asked for separately by
     *         {@link #state} so a caller that only wants to look does not have to risk
     *         collecting.
     */
    public static String collect(ServerLevel level, BlockPos pos) {
        Codex codex = Codex.get(level.getServer());
        var lodged = codex.on(pos);
        if (lodged == null || !Transcription.done(lodged.lodgedAt(), level.getGameTime())) {
            return null;
        }
        var taken = codex.collect(pos);
        return taken == null ? null : taken.letter();
    }

    /** What this desk would say if asked, without changing anything. */
    public static Outcome state(ServerLevel level, BlockPos pos) {
        if (!isDesk(level, pos)) {
            return Outcome.NO_DESK;
        }
        var lodged = Codex.get(level.getServer()).on(pos);
        if (lodged == null) {
            return Outcome.EMPTY;
        }
        return Transcription.done(lodged.lodgedAt(), level.getGameTime())
                ? Outcome.COLLECTED : Outcome.NOT_YET;
    }

    /** How long this desk still has, in ticks. Zero for a desk with nothing on it. */
    public static long remaining(ServerLevel level, BlockPos pos) {
        var lodged = Codex.get(level.getServer()).on(pos);
        return lodged == null ? 0
                : Transcription.remaining(lodged.lodgedAt(), level.getGameTime());
    }

    /** Whether this letter has a copy in the Post. */
    public static boolean transcribed(ServerLevel level, String letter) {
        return Codex.get(level.getServer()).transcribed(letter);
    }
}
