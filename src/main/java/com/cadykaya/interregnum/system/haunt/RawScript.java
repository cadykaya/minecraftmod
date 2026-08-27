package com.cadykaya.interregnum.system.haunt;

import com.cadykaya.interregnum.core.haunt.Script;
import com.cadykaya.interregnum.core.stele.Steles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.UUID;

/**
 * Reading the dead god's own hand, and what it costs.
 *
 * The seam every reader goes through — the carved stone's right-click, the letter's, and
 * `interregnum script read`. Same arrangement as the rite, the spoken word and the
 * crossings, and for the same reason: a decision only a right-click can reach is a decision
 * nothing can check.
 *
 * <h2>Two kinds of script, one ledger</h2>
 *
 * `WORLD.md` names *"letters, shrine inscriptions"*. They are different objects and the
 * same hazard, so they produce different marks and go to the same place. A stone is where
 * it stands; a letter is which letter it is — carry it across the world and it is still the
 * one you have read.
 *
 * <h2>What a carved stone actually says</h2>
 *
 * Nothing you can use, and that is the point. This is **raw** script: the god's own hand,
 * untranscribed, and the desk exists precisely because a person cannot read it. So the line
 * a reader gets is not information — it is the moment of looking, and the moment of being
 * looked back at. A stone that gave you lore would make the desk pointless and the hazard a
 * toll.
 */
public final class RawScript {
    private RawScript() {}

    /** What happened when somebody looked. */
    public enum Outcome {
        /** New to them. There is one more thing the god knows they have seen. */
        MARKED,
        /** They had already read this one. Knowledge is not made twice. */
        ALREADY,
        /** Not enough light to make out a band of worn carving. */
        TOO_DARK
    }

    /** The mark a carved stone leaves: where it stands. */
    public static String markOf(BlockPos pos) {
        return "stone/" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    /**
     * The mark a letter leaves: which letter it is.
     *
     * Not the stack, not a position. `WORLD.md` has the letters carried across worlds and
     * handed to gods; a mark tied to where you happened to be standing would let the same
     * letter be read four times for four times the cost.
     */
    public static String markOf(String letterId) {
        return "letter/" + letterId;
    }

    /**
     * The brightest light falling on this stone from any side.
     *
     * Borrowed wholesale from the steles, including the reason it is six-sided rather than
     * a reading at the block's own position: the inside of an opaque block is dark in every
     * world there has ever been, and a version that asked there reported open daylight as
     * unreadable. Same worn carving on the same masonry, so the same rule and the same
     * threshold -- {@link Steles#READING_LIGHT}. A second number would be a second thing
     * for a player to learn about carvings.
     */
    private static int lightOn(ServerLevel level, BlockPos pos) {
        int best = 0;
        for (Direction dir : Direction.values()) {
            best = Math.max(best, level.getMaxLocalRawBrightness(pos.relative(dir)));
        }
        return best;
    }

    /** Read a carved stone. */
    public static Outcome read(ServerLevel level, BlockPos pos, UUID who) {
        if (!Steles.legible(lightOn(level, pos))) {
            return Outcome.TOO_DARK;
        }
        return mark(level, who, markOf(pos));
    }

    /** Read a letter, raw — which is the only way anything can read one yet. */
    public static Outcome readLetter(ServerLevel level, String letterId, UUID who) {
        return mark(level, who, markOf(letterId));
    }

    private static Outcome mark(ServerLevel level, UUID who, String mark) {
        return Readings.get(level.getServer()).mark(who, mark)
                ? Outcome.MARKED : Outcome.ALREADY;
    }

    /** How many distinct pieces this person has read. */
    public static int by(ServerLevel level, UUID who) {
        return Readings.get(level.getServer()).by(who);
    }

    /**
     * What a reader is told, for a carved stone.
     *
     * The refusal and the reading are both one line, and neither explains the mechanic.
     * `WORLD.md` locks the hazard as *"no affliction bar, no debuff"*, and a message
     * announcing that your manifestation rate has risen would be an affliction bar made of
     * text. A player finds out the way the fiction says they do: by noticing, later, that
     * things have been happening near them.
     */
    public static List<Component> saidTo(Outcome outcome) {
        return switch (outcome) {
            case TOO_DARK -> List.of(Component.translatable("interregnum.script.dark")
                    .withStyle(ChatFormatting.DARK_GRAY));
            case MARKED -> List.of(Component.translatable("interregnum.script.read")
                    .withStyle(ChatFormatting.GRAY));
            case ALREADY -> List.of(Component.translatable("interregnum.script.again")
                    .withStyle(ChatFormatting.DARK_GRAY));
        };
    }
}
