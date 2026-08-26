package com.cadykaya.interregnum.system.stele;

import com.cadykaya.interregnum.core.stele.Steles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * What a warning stele says, and whether there is light to read it by.
 *
 * The seam both callers go through — the block's right-click and
 * `interregnum stele read` — for the reason every seam here exists: a right-click cannot
 * be driven from a headless server, so a text only the block could produce is a text no
 * check can read.
 *
 * <h2>Every notice is the Wardenate explaining a rule that is about to stop being true</h2>
 *
 * The five inscriptions are the four locked vanilla-rules-as-policy entries — permitted
 * airspace, the sleep code, incineration at dawn, the world's floor — and one that says
 * what to do if any of them ever fails. `WORLD.md` calls the steles *"Chapter 0 dressing
 * that players read as ruin flavour for hours, and which after the death is the only
 * instruction anyone left behind."* The fifth notice is that instruction, and it was
 * written by somebody who did not believe they were writing it.
 *
 * Not one word of any of them changes at the deicide. See {@link Steles}: what re-reads
 * differently is the reader.
 */
public final class SteleReading {
    private SteleReading() {}

    /**
     * The brightest light falling on this stele from any side.
     *
     * NOT the light at the stele's own position, which is what the first version asked
     * for and is always zero: a stele is an opaque block, and the inside of an opaque
     * block is dark in every world there has ever been. A stele in open daylight reported
     * itself unreadable and one buried in stone reported itself fine, which is the shape
     * of a reading taken from the wrong place — wrong everywhere, and wrong in a way that
     * looks like a rule rather than a bug.
     *
     * Six sides rather than just the top, because a stele can be laid on its side, built
     * into a wall, or lit by a torch on one face — and the question a reader is really
     * asking is "is there light on this thing anywhere", not "is there sky above it".
     */
    private static int lightOn(ServerLevel level, BlockPos pos) {
        int best = 0;
        for (var dir : net.minecraft.core.Direction.values()) {
            best = Math.max(best, level.getMaxLocalRawBrightness(pos.relative(dir)));
        }
        return best;
    }

    /** @return the notice, or the one line about not being able to see it. */
    public static List<Component> of(ServerLevel level, BlockPos pos) {
        if (!Steles.legible(lightOn(level, pos))) {
            return List.of(Component.translatable("interregnum.stele.dark")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        int which = Steles.inscriptionAt(pos.getX(), pos.getY(), pos.getZ());
        return List.of(
                Component.translatable("interregnum.stele.header")
                        .withStyle(ChatFormatting.GOLD),
                Component.translatable("interregnum.stele." + which)
                        .withStyle(ChatFormatting.GRAY));
    }
}
