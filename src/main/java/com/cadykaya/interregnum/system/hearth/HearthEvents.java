package com.cadykaya.interregnum.system.hearth;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import com.cadykaya.interregnum.Interregnum;

/** Game-bus wiring for the Turning: the datapack listener, and the clock. */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class HearthEvents {
    private HearthEvents() {}

    private static final Identifier AGEING_LISTENER =
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "ageing");

    /** Chunks around a player that count as theirs, for ageing purposes. */
    private static final int REACH = 4;

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(AGEING_LISTENER, new TurningLoader());
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !Hearth.holds(level)) {
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
            // getChunkNow, never getChunk: an ageing tick must never be the thing that
            // causes a chunk to load.
            LevelChunk chunk = level.getChunkSource()
                    .getChunkNow(ChunkPos.getX(packed), ChunkPos.getZ(packed));
            if (chunk != null) {
                Hearth.age(level, chunk);
            }
        }
    }
}
