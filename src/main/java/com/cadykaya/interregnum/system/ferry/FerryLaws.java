package com.cadykaya.interregnum.system.ferry;

import com.cadykaya.interregnum.core.ferry.Law;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The crossing laws, loaded from a datapack.
 *
 * One broken file takes down **every** law, exactly as the unraveling table does. A
 * half-loaded set of crossing rules is the worst possible state: some destinations
 * would refuse correctly and others would admit anything, and the difference would be
 * invisible until a player crossed and found a world that had stopped being itself.
 */
public final class FerryLaws extends SimpleJsonResourceReloadListener<FerryDefs.LawsFile> {
    private static final Logger LOG = LogUtils.getLogger();

    private static Map<String, Law> laws = Map.of();

    public FerryLaws() {
        super(FerryDefs.LawsFile.CODEC, net.minecraft.resources.FileToIdConverter.json("ferry"));
    }

    public static Map<String, Law> all() {
        return laws;
    }

    public static Law of(String id) {
        return laws.get(id);
    }

    @Override
    protected void apply(Map<Identifier, FerryDefs.LawsFile> files,
                         ResourceManager manager, ProfilerFiller profiler) {
        Map<String, Law> built = new LinkedHashMap<>();
        try {
            for (var entry : files.entrySet()) {
                for (FerryDefs.LawDef def : entry.getValue().laws()) {
                    Law law = def.toLaw();           // validates, and throws if wrong
                    if (built.put(law.id(), law) != null) {
                        throw new IllegalArgumentException(
                                "two crossing laws share the id " + law.id());
                    }
                }
            }
        } catch (RuntimeException e) {
            LOG.error("The crossing laws are broken; NO law is loaded rather than some. "
                    + "Every destination will refuse until this is fixed.", e);
            laws = Map.of();
            return;
        }
        laws = Map.copyOf(built);
        LOG.info("content: {} crossing law(s) loaded", laws.size());
    }
}
