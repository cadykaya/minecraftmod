package com.cadykaya.interregnum.system.exodus;

import com.cadykaya.interregnum.core.exodus.Exodus;
import com.cadykaya.interregnum.system.ChapterSavedData;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import com.cadykaya.interregnum.Interregnum;

/**
 * Band 3 in the overworld, on the clock.
 *
 * Three gates before any work happens, cheapest first, because this handler fires for
 * every level on every tick: the overworld test is a reference comparison, the band test
 * is a field read, and only then does anything look at a chunk.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class LeakEvents {
    private LeakEvents() {}

    /** Chunks around a player that count as theirs. */
    private static final int REACH = 4;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD) {
            return;
        }
        ChapterSavedData data = ChapterSavedData.get(level.getServer());
        if (!Exodus.leaking(data.band())) {
            return;
        }

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
            // getChunkNow, never getChunk: a leak must never be the thing that causes a
            // chunk to load.
            LevelChunk chunk = level.getChunkSource()
                    .getChunkNow(ChunkPos.getX(packed), ChunkPos.getZ(packed));
            if (chunk != null) {
                Leaks.apply(level, chunk, data);
            }
        }
    }
}
