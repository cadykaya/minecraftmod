package com.cadykaya.interregnum.system.ferry;

import com.cadykaya.interregnum.core.ferry.Law;
import com.cadykaya.interregnum.core.ferry.Manifest;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The page the ferry hands back when somebody asks what it is carrying.
 *
 * <h2>Why this is not in the block</h2>
 *
 * `WORLD.md` locks *"the validation checklist teaches each world's rule before arrival"*,
 * and until now that checklist was reachable only from a command — so the beat the locked
 * text is about, a player learning what a god is like by being refused by its paperwork,
 * had never happened in play. Touching the keel produces it now.
 *
 * The docket itself lives here rather than in {@link
 * com.cadykaya.interregnum.content.block.FerryKeelBlock} for the reason every other
 * command seam in this mod exists: a right-click cannot be driven from a headless server,
 * so a thing only a block could produce is a thing no check can read. Both the block and
 * `interregnum ferry inspect` call this, which is the same arrangement `interregnum learn`
 * has with the dialogue node that teaches a school — one path, two callers, and the one a
 * check can reach is the one the player uses.
 *
 * <h2>All four destinations, every time</h2>
 *
 * A player told only about the crossing they asked for learns one rule. A player handed
 * the whole page learns that the four gods refuse <b>different</b> things, which is the
 * reconnaissance band 3 exists to begin and the reason the letters are worth reading. It
 * costs four map lookups over a census, so there is no argument for the narrower version.
 */
public final class FerryDocket {
    private FerryDocket() {}

    /**
     * @return the docket, in the order a person reads it. Never empty: a keel that
     *         cannot be inspected says so on one line rather than answering with
     *         nothing, because silence from a desk is indistinguishable from a mod that
     *         has stopped working.
     */
    public static List<Component> of(ServerLevel level, BlockPos keel) {
        List<Component> out = new ArrayList<>();
        Ferry.Capture capture = Ferry.capture(level, keel);
        if (!capture.ok()) {
            out.add(Component.translatable("interregnum.ferry.inspection."
                            + capture.refusal().name().toLowerCase(java.util.Locale.ROOT))
                    .withStyle(ChatFormatting.GRAY));
            return out;
        }

        out.add(Component.translatable("interregnum.ferry.inspection.header")
                .withStyle(ChatFormatting.GOLD));
        out.add(Component.translatable("interregnum.ferry.inspection.hull",
                        capture.hull().size())
                .withStyle(ChatFormatting.GRAY));

        // Stable order, for the same reason Manifest is a TreeMap: a docket that
        // reorders itself between two identical inspections is one nobody can trust.
        for (Map.Entry<String, Law> entry : FerryLaws.all().entrySet()) {
            List<Manifest.Violation> refusals =
                    Ferry.checklist(capture.hull(), entry.getValue());
            Component where = Component.translatable(
                    "interregnum.ferry.destination." + entry.getKey());
            if (refusals.isEmpty()) {
                out.add(Component.translatable("interregnum.ferry.inspection.cleared", where)
                        .withStyle(ChatFormatting.GREEN));
                continue;
            }
            out.add(Component.translatable("interregnum.ferry.inspection.refused", where)
                    .withStyle(ChatFormatting.RED));
            // EVERY violation, not the first. A player sent back to fix one thing who
            // then finds a second has been made to cross twice for one mistake of the
            // design's -- see Manifest.validate, which reports them all for that reason.
            for (Manifest.Violation v : refusals) {
                out.add(Component.translatable("interregnum.ferry.inspection.line",
                                v.count(), v.blockId())
                        .withStyle(ChatFormatting.DARK_RED));
                out.add(Component.translatable(v.reasonKey())
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        return out;
    }

    /**
     * What the desk says back when somebody holds a letter out to a keel.
     *
     * Every outcome gets a line, including the ones that are nobody's fault: a keel that
     * answered nothing would be indistinguishable from a mod that had stopped working,
     * which is the same argument the inspection docket runs on.
     *
     * The one worth reading twice is {@code UNADDRESSED}. The dead god's last document is
     * addressed `To --`, and a ferry that sails where the letter says cannot be told where
     * to go with it in hand. That line is written as a clerk's shrug rather than an error,
     * because the endgame's opening question is *who was it for* and the mechanism asking
     * it should sound like the rest of the institution.
     */
    public static List<Component> report(Sailing.Sail done) {
        List<Component> out = new ArrayList<>();
        if (done.ok()) {
            out.add(Component.translatable("interregnum.ferry.sailed",
                            Component.translatable("interregnum.ferry.destination." + done.law()),
                            done.blocks())
                    .withStyle(ChatFormatting.GOLD));
            return out;
        }
        if (done.outcome() == Sailing.Outcome.HELD) {
            Component where = Component.translatable(
                    "interregnum.ferry.destination." + done.law());
            out.add(Component.translatable("interregnum.ferry.inspection.refused", where)
                    .withStyle(ChatFormatting.RED));
            for (Manifest.Violation v : done.held()) {
                out.add(Component.translatable("interregnum.ferry.inspection.line",
                                v.count(), v.blockId())
                        .withStyle(ChatFormatting.DARK_RED));
                out.add(Component.translatable(v.reasonKey())
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            return out;
        }
        out.add(Component.translatable("interregnum.ferry.refused."
                        + done.outcome().name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(ChatFormatting.GRAY));
        return out;
    }
}
