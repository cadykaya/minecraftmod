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
 * Loads the Turning's ageing table from a datapack.
 *
 * One broken file leaves the table EMPTY rather than partial, exactly as the unraveling
 * and the crossing laws do. A half-loaded ageing table is the worst state available:
 * some chains would run and others would stop halfway, so a wall would age to cobble and
 * then never green, and the world would look like it had a law it had merely lost half of.
 */
public final class TurningLoader extends SimpleJsonResourceReloadListener<StepTable.File> {
    private static final Logger LOG = LogUtils.getLogger();

    private static StepTable table = StepTable.EMPTY;

    public TurningLoader() {
        super(StepTable.File.CODEC, FileToIdConverter.json("ageing"));
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
            LOG.error("The Turning's ageing table is broken; NOTHING ages rather than "
                    + "some things ageing. The Hearth-Turner's world has no law until "
                    + "this is fixed.", e);
            table = StepTable.EMPTY;
            return;
        }
        LOG.info("content: {} ageing rule(s) loaded", table.ruleCount());
    }
}
