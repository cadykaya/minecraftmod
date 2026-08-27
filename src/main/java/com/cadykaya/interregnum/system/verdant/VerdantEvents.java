package com.cadykaya.interregnum.system.verdant;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import com.cadykaya.interregnum.Interregnum;

/**
 * Game-bus wiring for the Verdant's law. See {@link Verdant} for what it is.
 *
 * The dimension test comes first and is the cheap one: this handler fires for every
 * level on every tick, and on a server with the overworld, the Nether, the End and four
 * god-worlds loaded, six of seven calls should cost a reference comparison and nothing
 * else.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class VerdantEvents {
    private VerdantEvents() {}

    /** Chunks around a player that count as theirs, for growth purposes. */
    private static final int REACH = 4;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !Verdant.holds(level)) {
            return;
        }
        // Chunks the server is already keeping loaded, by the two ways a chunk gets
        // that way: somebody forceloaded it, or somebody is standing near it. Growing
        // only near players would make the world grow differently depending on where
        // people happened to walk, which is the kind of invisible inconsistency that
        // becomes a bug report nobody can reproduce; growing everywhere would be work
        // on chunks that are not loaded to be worked on.
        //
        // Forceloaded chunks are listed first and deliberately: they are the only ones
        // a headless server has, so `tools/verdant_check.sh` exercises the same code
        // path a played server does rather than a special case written for it.
        LongSet seen = new LongOpenHashSet(level.getForceLoadedChunks());
        for (ServerPlayer player : level.players()) {
            ChunkPos at = player.chunkPosition();
            for (int dx = -REACH; dx <= REACH; dx++) {
                for (int dz = -REACH; dz <= REACH; dz++) {
                    seen.add(ChunkPos.pack(at.x() + dx, at.z() + dz));
                }
            }
        }
        for (long packed : seen) {
            LevelChunk chunk = level.getChunkSource()
                    .getChunkNow(ChunkPos.getX(packed), ChunkPos.getZ(packed));
            // getChunkNow, never getChunk: a growth tick must never be the thing that
            // causes a chunk to load. See docs/LESSONS.md on the same rule in the
            // statue sweep.
            if (chunk != null) {
                Verdant.grow(level, chunk);
            }
        }
    }
}
