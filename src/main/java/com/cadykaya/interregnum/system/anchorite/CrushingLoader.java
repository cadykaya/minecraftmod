package com.cadykaya.interregnum.system.anchorite;

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
 * Loads what a landing weight does, from a datapack.
 *
 * The fourth {@link StepTable}. The unraveling loosens, the Turning weathers, attrition
 * generalises — and this one <b>crushes</b>, which is the first of the four that is not
 * something the world does on its own. Nothing here happens on a clock or a random tick;
 * every entry waits for somebody to drop something.
 *
 * One broken file leaves the table EMPTY rather than partial, exactly as the other three
 * do. A half-loaded crushing table is the usual worst case — some drops work, some do
 * nothing — and here it would be read as the spell being unreliable rather than as the
 * data being wrong.
 */
public final class CrushingLoader extends SimpleJsonResourceReloadListener<StepTable.File> {
    private static final Logger LOG = LogUtils.getLogger();

    private static StepTable table = StepTable.EMPTY;

    public CrushingLoader() {
        super(StepTable.File.CODEC, FileToIdConverter.json("crushing"));
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
            LOG.error("The crushing table is broken; NOTHING crushes rather than some "
                    + "things crushing. Drop-forge does nothing until this is fixed.", e);
            table = StepTable.EMPTY;
            return;
        }
        LOG.info("content: {} crushing rule(s) loaded", table.ruleCount());
    }
}
