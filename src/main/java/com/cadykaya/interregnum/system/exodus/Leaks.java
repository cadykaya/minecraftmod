package com.cadykaya.interregnum.system.exodus;

import com.cadykaya.interregnum.core.exodus.Exodus;
import com.cadykaya.interregnum.registry.ModBlocks;
import com.cadykaya.interregnum.system.ChapterSavedData;
import com.cadykaya.interregnum.system.hearth.Hearth;
import com.cadykaya.interregnum.system.verdant.Verdant;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * Band 3 in the overworld: a patch of ground obeying somebody else's law.
 *
 * See {@link Exodus} for the design and for why the patches sit on the shrines. This is
 * the half that needs a world.
 *
 * <h2>The laws are not reimplemented here, and that is the whole point</h2>
 *
 * A leak calls the SAME code the god's own dimension calls — {@link Verdant#grow} for the
 * Verdant, {@link Hearth#age} for the Hearth-Turner. Not a copy, not a table tuned to
 * feel similar: the same method.
 *
 * `WORLD.md` is explicit about why: *"Each patch is shaped like exactly one god, and it
 * is **the same law you will meet in their world**. So band 3 is reconnaissance: the
 * apocalypse is teaching you the curriculum. By the time the ferry's checklist tells you
 * the Quiet One's crossing forbids note blocks, you have already stood in a silent hollow
 * and worked out why."*
 *
 * A curriculum that taught a slightly different lesson than the exam would be worse than
 * no curriculum. So if the two ever diverge it will be because somebody changed one
 * method, and there is only one method to change.
 *
 * <h2>Two of four, and the file says which</h2>
 *
 * Only the Verdant's growth and the Hearth-Turner's ageing leak today, because those are
 * the two laws that are per-chunk operations on blocks and therefore mean something
 * applied to a patch of ground.
 *
 * The Anchorite's is per-ENTITY (falling blocks rise) and would need the handler to ask
 * whether an entity is standing in a leak, which is a different shape and a separate
 * increment. The Quiet One's is per-DIMENSION — bed rules, respawn anchors, ambient
 * sound — and none of those can apply to a region at all; *"a hollow where nothing makes
 * a sound"* needs client-side audio suppression this container cannot verify. Both are
 * recorded in HANDOFF rather than half-built.
 */
public final class Leaks {
    private Leaks() {}

    /**
     * How far a leak reaches from the shrine's chunk, in chunks.
     *
     * One: a three-by-three of chunks, about 48 blocks across. Big enough to be a place
     * you stand in rather than a block you notice, small enough that walking out of it
     * is the obvious thing to try -- which is how a player finds out it has edges, and
     * therefore that it is a *rule* rather than the world being broken.
     */
    public static final int REACH_CHUNKS = 1;

    /** Does this chunk sit inside a leak, and whose? Null if the world is not leaking. */
    public static Exodus.Law lawFor(ServerLevel level, ChunkPos at, ChapterSavedData data) {
        if (level.dimension() != Level.OVERWORLD || !Exodus.leaking(data.band())) {
            return null;
        }
        for (int dx = -REACH_CHUNKS; dx <= REACH_CHUNKS; dx++) {
            for (int dz = -REACH_CHUNKS; dz <= REACH_CHUNKS; dz++) {
                int cx = at.x() + dx;
                int cz = at.z() + dz;
                if (holdsShrine(level, cx, cz)) {
                    // The law belongs to the SHRINE's chunk, not to the chunk being
                    // asked about. Otherwise a nine-chunk patch would be nine different
                    // gods' laws in a grid, which is not a patch, it is static.
                    return Exodus.lawAt(cx, cz);
                }
            }
        }
        return null;
    }

    /** Apply whatever law leaks here. Does nothing where nothing leaks. */
    public static void apply(ServerLevel level, LevelChunk chunk, ChapterSavedData data) {
        Exodus.Law law = lawFor(level, chunk.getPos(), data);
        if (law == null) {
            return;
        }
        switch (law) {
            // The same method the god's own world calls. See the class javadoc.
            case VERDANT -> Verdant.grow(level, chunk);
            case HEARTH_TURNER -> Hearth.age(level, chunk);
            // Not leakable as a region yet -- see the class javadoc and HANDOFF. Silence
            // is deliberate: a log line per chunk per tick for a law that is merely not
            // built would be noise on every server that ever reaches band 3.
            case ANCHORITE, QUIET_ONE -> { }
        }
    }

    /**
     * Does this chunk hold shrine masonry?
     *
     * `getChunk(..., false)` so asking never LOADS a chunk -- the same rule the statue
     * sweep and the ageing tick follow. Palette-gated per section, so a chunk of stone
     * and air costs one predicate pass.
     */
    private static boolean holdsShrine(ServerLevel level, int cx, int cz) {
        ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.FULL, false);
        if (chunk == null) {
            return false;
        }
        for (LevelChunkSection section : chunk.getSections()) {
            if (section.hasOnlyAir()) {
                continue;
            }
            if (section.maybeHas(s -> s.is(ModBlocks.SHRINE_STONE.get())
                    || s.is(ModBlocks.SHRINE_STONE_CARVED.get()))) {
                return true;
            }
        }
        return false;
    }

    /** For the command seam: name the law leaking at a position, or "none". */
    public static String describe(ServerLevel level, BlockPos pos, ChapterSavedData data) {
        Exodus.Law law = lawFor(level, ChunkPos.containing(pos), data);
        return law == null ? "none" : law.name().toLowerCase(java.util.Locale.ROOT);
    }
}
