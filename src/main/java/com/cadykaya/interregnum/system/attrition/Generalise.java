package com.cadykaya.interregnum.system.attrition;

import com.cadykaya.interregnum.core.attrition.Attrition;
import com.cadykaya.interregnum.system.ChapterSavedData;
import com.cadykaya.interregnum.system.claim.Claims;
import com.cadykaya.interregnum.system.convert.StepTable;
import com.cadykaya.interregnum.system.unraveling.UnravelingDefs.ConversionDef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Band 4 applied: ground nobody tends stops being anywhere in particular.
 *
 * See {@link Attrition} for the design and {@link Tending} for the signal this reads.
 *
 * <h2>Four gates, and every one of them is load-bearing</h2>
 *
 * <ol>
 *   <li><b>The overworld only.</b> The gods' worlds have their own laws and are not
 *       losing their categories; theirs are being kept, insisted on, or ignored.</li>
 *   <li><b>Band 4.</b> Below it the world is coming apart but has not started
 *       forgetting.</li>
 *   <li><b>Stale ground only.</b> The one with the idea in it — see {@link Attrition}.
 *       Ground somebody tends keeps its definition, and that is the player's
 *       counter-move rather than a difficulty setting.</li>
 *   <li><b>Never a block somebody placed.</b> {@link Claims}, which fails closed. The
 *       moment an apocalypse eats somebody's house the mod has turned hateful and lost
 *       them, and that promise is older than this system.</li>
 * </ol>
 *
 * <h2>Why this samples whole sections rather than the surface column</h2>
 *
 * The unraveling works the surface, because its table is grass and flowers and leaves —
 * one layer thick, and uniform sampling would essentially never hit them. Attrition's
 * table is mostly <b>ore</b>, which lives everywhere from the surface to the deepslate,
 * so a surface-only sweep would miss the bulk of what band 4 is for and the ores would
 * quietly never generalise.
 *
 * Palette-gated per section, like the ageing tick: a section holding no block this table
 * has an opinion about costs one predicate pass and is skipped whole.
 */
public final class Generalise {
    private Generalise() {}

    /** Sample points per section per pass. */
    public static final int SAMPLES_PER_SECTION = 3;

    /**
     * Generalise one chunk, if it has gone unattended.
     *
     * Everything expensive sits behind the cheap gates, and the staleness read is
     * deliberately the last of them: it touches the chunk's attachment, which is more
     * work than a reference comparison or a field read.
     */
    public static void sweep(ServerLevel level, LevelChunk chunk, ChapterSavedData data) {
        if (level.dimension() != Level.OVERWORLD || !Attrition.fraying(data.band())) {
            return;
        }
        StepTable table = GeneraliseLoader.table();
        if (table.ruleCount() == 0 || !Tending.stale(level, chunk.getPos())) {
            return;
        }

        RandomSource random = level.getRandom();
        int minY = level.getMinY();
        LevelChunkSection[] sections = chunk.getSections();
        for (int s = 0; s < sections.length; s++) {
            LevelChunkSection section = sections[s];
            if (section.hasOnlyAir()
                    || !section.maybeHas(state -> table.stepFrom(state.getBlock()) != null)) {
                continue;
            }
            int baseY = minY + (s << 4);
            for (int n = 0; n < SAMPLES_PER_SECTION; n++) {
                BlockPos pos = new BlockPos(
                        chunk.getPos().getMinBlockX() + random.nextInt(16),
                        baseY + random.nextInt(16),
                        chunk.getPos().getMinBlockZ() + random.nextInt(16));
                step(level, pos, data, random);
            }
        }
    }

    /**
     * One block, one step plainer. <b>Every gate lives here.</b>
     *
     * The gates were briefly split between this method and the command that calls it --
     * the command tested the dimension, the band and the staleness so it could report a
     * precise reason, and this method tested only the claim ledger. That is the shape of
     * a check that tests its own harness: `attrition_check.sh` asserts that tended ground
     * is spared, and with the gate living in the command, deleting it from the sweep
     * would have left the check green and the law gone.
     *
     * So all four are here, the command reports what this returns, and the sweep and the
     * command are the same law with different callers. The sweep also tests staleness
     * once per chunk before it starts sampling; that is a cheap early-out, not a second
     * copy of the rule, and this method would refuse anything it let through anyway.
     *
     * `random` may be null to skip ONLY the chance roll. Band 4's rates are slow on
     * purpose and a check that waited for a roll to land would be testing the random
     * number generator rather than the law.
     *
     * @return what happened, for the command seam to report.
     */
    public static String step(ServerLevel level, BlockPos pos, ChapterSavedData data,
                              RandomSource random) {
        if (level.dimension() != Level.OVERWORLD) {
            return "not-overworld";
        }
        if (!Attrition.fraying(data.band())) {
            return "not-fraying";
        }
        BlockState state = level.getBlockState(pos);
        ConversionDef rule = GeneraliseLoader.table().stepFrom(state.getBlock());
        if (rule == null) {
            return "nothing";
        }
        if (!Tending.stale(level, net.minecraft.world.level.ChunkPos.containing(pos))) {
            return "tended";
        }
        if (Claims.isClaimed(level, pos)) {
            return "claimed";
        }
        if (random != null && random.nextFloat() >= rule.chance()) {
            return "unlucky";
        }
        level.setBlockAndUpdate(pos, rule.to().defaultBlockState());
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getKey(rule.to()).toString();
    }
}
