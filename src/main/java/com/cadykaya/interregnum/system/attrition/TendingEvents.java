package com.cadykaya.interregnum.system.attrition;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.core.attrition.Attrition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.cadykaya.interregnum.registry.ModAttachments;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Somebody being somewhere is what tending IS.
 *
 * No ritual, no item, no button. `WORLD.md` says *"regions people visit and keep hold
 * their definition"*, and the most honest reading of that is the plainest one: you tend
 * ground by being on it. A player who lives in their base keeps their base without ever
 * learning that this system exists, which is the right way for a counter-move to a slow
 * apocalypse to work.
 *
 * <h2>Not every tick</h2>
 *
 * The stamp only needs to be young enough to beat {@link Attrition#FRAY_AFTER_TICKS},
 * which is twenty minutes. Re-stamping twenty-five chunks per player twenty times a
 * second to move a number that is compared against a twenty-minute threshold is pure
 * waste, so this runs on an interval -- and the interval is small enough relative to the
 * threshold that a player walking through at any speed still stamps what they crossed.
 *
 * <b>Deliberately NOT gated on the band.</b> Tending is bookkeeping, not a consequence:
 * if it only started at band 4, then the moment the world reached band 4 every chunk in
 * it would be unstamped, every one would be stamped on first sight, and nobody's history
 * of actually living somewhere would count for anything. The stamp has to have been
 * accruing all along for band 4 to have something true to read.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class TendingEvents {
    private TendingEvents() {}

    /**
     * Ticks between passes. Ten seconds: a hundred and twenty times inside the fray
     * window, so the stamp is never close to stale for anyone actually present.
     */
    public static final int INTERVAL = 200;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.getGameTime() % INTERVAL != 0) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            Tending.tendAround(level, player.chunkPosition());
        }
    }

    /**
     * First sight counts as tending.
     *
     * Ground that has never carried a stamp is not ancient ground, it is ground nobody
     * has looked at yet -- see {@link com.cadykaya.interregnum.system.attrition.Tended}.
     * Stamping it here, where first sight actually happens, is what keeps a player
     * exploring at band 4 from finding fresh land that has already gone generic, which
     * would read as broken worldgen rather than as a world forgetting.
     *
     * Only unstamped chunks are touched: loading a chunk you have not visited in a week
     * must NOT count as tending it, or leaving and coming back would launder the whole
     * mechanic away.
     */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Tended tended = event.getChunk().getData(ModAttachments.TENDED);
        if (tended.isUnstamped() && tended.tend(level.getGameTime())) {
            event.getChunk().markUnsaved();
        }
    }
}
