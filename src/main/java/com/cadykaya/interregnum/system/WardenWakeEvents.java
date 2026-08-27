package com.cadykaya.interregnum.system;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

import com.cadykaya.interregnum.Interregnum;

/**
 * Statues in chunks that were not loaded when the god died.
 *
 * The deicide can only reach loaded chunks, so the rest wake as they come into the
 * world. A player who was mining when it happened climbs out and finds the statue
 * in their garden already watching -- which is the better version of the beat
 * anyway, because they get to notice it themselves.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class WardenWakeEvents {
    private WardenWakeEvents() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (ChapterSavedData.isDormant(level)) {
            return;                       // the god still lives: nothing wakes
        }
        if (event.getChunk() instanceof LevelChunk chunk) {
            WardenWake.wakeChunk(level, chunk);
        }
    }
}
