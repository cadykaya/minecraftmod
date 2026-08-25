package com.cadykaya.interregnum.system.letters;

import com.cadykaya.interregnum.core.letters.Letter;
import com.cadykaya.interregnum.core.letters.Post;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The dead god's mail, loaded from a datapack.
 *
 * One broken file leaves NO mail rather than some, the same rule the crossing laws and
 * the ageing table follow. Here it matters more than usual: {@link Post}'s invariant is
 * about the whole set — exactly one letter opens unaddressed — so a partially loaded
 * post is not merely incomplete, it is a set whose defining rule cannot be evaluated.
 */
public final class Letters extends SimpleJsonResourceReloadListener<LetterDefs.PostFile> {
    private static final Logger LOG = LogUtils.getLogger();

    private static Post post;

    public Letters() {
        super(LetterDefs.PostFile.CODEC, FileToIdConverter.json("letters"));
    }

    /** The mail, or null if it is broken or not yet loaded. */
    public static Post post() {
        return post;
    }

    public static Letter forGod(String id) {
        return post == null ? null : post.forGod(id);
    }

    @Override
    protected void apply(Map<Identifier, LetterDefs.PostFile> files,
                         ResourceManager manager, ProfilerFiller profiler) {
        try {
            Map<String, Letter> built = new LinkedHashMap<>();
            for (var entry : files.entrySet()) {
                for (LetterDefs.LetterDef def : entry.getValue().letters()) {
                    Letter letter = def.toLetter();     // validates, throws if wrong
                    if (built.put(letter.id(), letter) != null) {
                        throw new IllegalArgumentException(
                                "two letters are addressed to " + letter.id());
                    }
                }
            }
            post = new Post(built);                     // validates the SET
        } catch (RuntimeException e) {
            LOG.error("The dead god's mail is broken; NO letter is loaded rather than "
                    + "some. Every world's questline is unopenable until this is fixed.", e);
            post = null;
            return;
        }
        LOG.info("content: {} letter(s) loaded, {} named", post.size(),
                post.namesUsed().size());
    }
}
