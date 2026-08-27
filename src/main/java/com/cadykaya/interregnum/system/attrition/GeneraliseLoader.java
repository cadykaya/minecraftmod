package com.cadykaya.interregnum.system.attrition;

import com.cadykaya.interregnum.system.convert.StepTable;
import com.cadykaya.interregnum.system.unraveling.UnravelingDefs.ConversionDef;
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
 * Loads band 4's generalisation table from a datapack.
 *
 * One broken file leaves the table EMPTY rather than partial, exactly as the unraveling,
 * the crossing laws and the Turning do. A half-loaded table here would be the worst state
 * available: some distinctions would go and others would not, so a world would end up
 * with oak logs under birch leaves and look like a bug rather than like a world losing
 * its categories.
 */
public final class GeneraliseLoader extends SimpleJsonResourceReloadListener<StepTable.File> {
    private static final Logger LOG = LogUtils.getLogger();

    private static StepTable table = StepTable.EMPTY;

    public GeneraliseLoader() {
        super(StepTable.File.CODEC, FileToIdConverter.json("attrition"));
    }

    public static StepTable table() {
        return table;
    }

    @Override
    protected void apply(Map<Identifier, StepTable.File> files,
                         ResourceManager manager, ProfilerFiller profiler) {
        try {
            List<ConversionDef> all = new ArrayList<>();
            for (var entry : files.entrySet()) {
                all.addAll(entry.getValue().conversions());
            }
            table = new StepTable(all);          // validates, and throws if wrong
        } catch (RuntimeException e) {
            LOG.error("Band 4's generalisation table is broken; the world keeps ALL its "
                    + "distinctions rather than losing some of them. Attrition has no "
                    + "table until this is fixed.", e);
            table = StepTable.EMPTY;
            return;
        }
        LOG.info("content: {} generalisation rule(s) loaded", table.ruleCount());
    }
}
