package com.cadykaya.interregnum.system.unraveling;

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
 * Loads data/&lt;namespace&gt;/unraveling/*.json into a validated {@link UnravelingTable}.
 *
 * Datapack-driven for the same reason the dialogue is: how the world spends itself
 * is content, and a pack should be able to retune it without a recompile.
 *
 * A file that fails validation takes down the WHOLE table, unlike a broken dialogue
 * graph which only loses one conversation. That is deliberate. A half-loaded
 * unraveling is a world that decays in some ways and not others with nothing on
 * screen to say which, and the only honest failure is to stop unravelling entirely
 * and say so loudly.
 */
public final class UnravelingLoader extends SimpleJsonResourceReloadListener<UnravelingDefs.TableDef> {
    private static final Logger LOG = LogUtils.getLogger();
    private static final FileToIdConverter LISTER = FileToIdConverter.json("unraveling");

    private static UnravelingTable table = UnravelingTable.EMPTY;

    public UnravelingLoader() {
        super(UnravelingDefs.TableDef.CODEC, LISTER);
    }

    /** The table in force. Never null; empty until the first successful load. */
    public static UnravelingTable table() {
        return table;
    }

    @Override
    protected void apply(Map<Identifier, UnravelingDefs.TableDef> parsed,
                         ResourceManager manager, ProfilerFiller profiler) {
        List<UnravelingDefs.BandDef> bands = new ArrayList<>();
        for (var entry : parsed.entrySet()) {
            bands.addAll(entry.getValue().bands());
        }
        try {
            table = new UnravelingTable(bands);
            LOG.info("Unraveling: {} band(s), {} conversion(s) in force.",
                    table.bandCount(), table.ruleCount());
        } catch (IllegalArgumentException e) {
            table = UnravelingTable.EMPTY;
            LOG.error("Unraveling table is invalid and NOTHING will unravel: {}", e.getMessage());
        }
    }
}
