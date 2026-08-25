package com.cadykaya.interregnum.system.ferry;

import com.cadykaya.interregnum.core.ferry.Law;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
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

    /**
     * Where each law's crossing goes.
     *
     * Held here rather than on {@link Law} because `core/` is deliberately free of
     * Minecraft: a dimension key is the most Minecraft-shaped thing there is, and the
     * checklist logic that makes a law worth having does not need to know it exists.
     */
    private static Map<String, ResourceKey<Level>> destinations = Map.of();

    public FerryLaws() {
        super(FerryDefs.LawsFile.CODEC, net.minecraft.resources.FileToIdConverter.json("ferry"));
    }

    public static Map<String, Law> all() {
        return laws;
    }

    public static Law of(String id) {
        return laws.get(id);
    }

    /** The level this law's crossing arrives in, or null if the law is unknown. */
    public static ResourceKey<Level> destinationOf(String id) {
        return destinations.get(id);
    }

    @Override
    protected void apply(Map<Identifier, FerryDefs.LawsFile> files,
                         ResourceManager manager, ProfilerFiller profiler) {
        Map<String, Law> built = new LinkedHashMap<>();
        Map<String, ResourceKey<Level>> where = new LinkedHashMap<>();
        try {
            for (var entry : files.entrySet()) {
                for (FerryDefs.LawDef def : entry.getValue().laws()) {
                    Law law = def.toLaw();           // validates, and throws if wrong
                    if (built.put(law.id(), law) != null) {
                        throw new IllegalArgumentException(
                                "two crossing laws share the id " + law.id());
                    }
                    where.put(law.id(), ResourceKey.create(
                            Registries.DIMENSION, def.destination()));
                }
            }
        } catch (RuntimeException e) {
            LOG.error("The crossing laws are broken; NO law is loaded rather than some. "
                    + "Every destination will refuse until this is fixed.", e);
            laws = Map.of();
            destinations = Map.of();
            return;
        }
        laws = Map.copyOf(built);
        destinations = Map.copyOf(where);
        LOG.info("content: {} crossing law(s) loaded", laws.size());
    }
}
