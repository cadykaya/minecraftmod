package com.cadykaya.interregnum.system.hearth;

import com.cadykaya.interregnum.system.convert.StepTable;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads what a thing becomes past its end.
 *
 * <h2>A second table, in the same shape, and separate on purpose</h2>
 *
 * `WORLD.md`'s locked reuse note is *"the block-aging registry powering the Turning **is
 * the same system that runs the unraveling.** One mechanism; a school and an
 * apocalypse."* This is the third caller of that one mechanism and costs almost nothing
 * because of it — the same {@link StepTable}, the same `ConversionDef`, the same loud
 * failure on a typo.
 *
 * It is a **separate table** rather than more rows in the Turning's, and that is the whole
 * character of the spell. *Rewind* reads the ageing table backwards, so anything in it can
 * be undone — which is what *keeping every past* means when it is a block. Nothing here is
 * in that table, so nothing here can be rewound. Past a thing's end there is no past left
 * to keep.
 *
 * One broken file leaves it EMPTY rather than partial, exactly as the ageing table, the
 * unraveling and the crossing laws do.
 */
public final class RottingLoader extends SimpleJsonResourceReloadListener<StepTable.File> {
    private static final Logger LOG = LogUtils.getLogger();

    private static StepTable table = StepTable.EMPTY;

    public RottingLoader() {
        super(StepTable.File.CODEC, FileToIdConverter.json("rotting"));
    }

    public static StepTable table() {
        return table;
    }

    @Override
    protected void apply(Map<Identifier, StepTable.File> files,
                         ResourceManager manager, ProfilerFiller profiler) {
        try {
            List<com.cadykaya.interregnum.system.unraveling.UnravelingDefs.ConversionDef> all =
                    new ArrayList<>();
            for (var entry : files.entrySet()) {
                all.addAll(entry.getValue().conversions());
            }
            table = new StepTable(all);       // validates, and throws if wrong
        } catch (RuntimeException e) {
            LOG.error("The rotting table is broken; NOTHING rots rather than some things "
                    + "rotting. Rot is a spell with no effect until this is fixed.", e);
            table = StepTable.EMPTY;
            return;
        }
        LOG.info("content: {} rotting rule(s) loaded", table.ruleCount());
    }
}
