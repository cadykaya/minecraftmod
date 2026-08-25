package com.cadykaya.interregnum.content.dialogue;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

import com.cadykaya.interregnum.core.dialogue.DialogueGraph;

/**
 * Loads data/&lt;namespace&gt;/dialogue/*.json into validated engine graphs.
 *
 * Datapack-driven on purpose: every conversation in the game is data, so writing
 * dialogue never requires a recompile and a pack can add or replace scenes.
 */
public final class DialogueLoader extends SimpleJsonResourceReloadListener<DialogueDefs.GraphDef> {
    private static final Logger LOG = LogUtils.getLogger();
    private static final FileToIdConverter LISTER = FileToIdConverter.json("dialogue");

    private static Map<Identifier, DialogueGraph> graphs = Map.of();

    public DialogueLoader() {
        super(DialogueDefs.GraphDef.CODEC, LISTER);
    }

    /** @return the loaded graph, or null if no such conversation exists. */
    public static DialogueGraph get(Identifier id) {
        return graphs.get(id);
    }

    public static int count() {
        return graphs.size();
    }

    @Override
    protected void apply(Map<Identifier, DialogueDefs.GraphDef> parsed,
                         ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, DialogueGraph> built = new HashMap<>();
        int failed = 0;
        for (var entry : parsed.entrySet()) {
            try {
                built.put(entry.getKey(), entry.getValue().toGraph());
            } catch (IllegalArgumentException e) {
                // One broken conversation must not take the others down with it,
                // but it must be loud: a silently-missing scene is a quest that
                // simply never starts, which is the worst kind of bug to chase.
                failed++;
                LOG.error("Dialogue {} is invalid and was not loaded: {}",
                        entry.getKey(), e.getMessage());
            }
        }
        graphs = Map.copyOf(built);
        LOG.info("Loaded {} dialogue graph(s){}", built.size(),
                failed > 0 ? " (" + failed + " rejected)" : "");
    }
}
