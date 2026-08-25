package com.cadykaya.interregnum.system;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;
import org.slf4j.Logger;

import java.util.UUID;

import com.cadykaya.interregnum.core.chapter.Milestone;

/**
 * The moment the overworld's god dies.
 *
 * Everything that happens when the heart is taken happens HERE, in one method, so
 * that the thing which fires it can be swapped without the consequences drifting.
 * That matters more than it looks: the pickup handler needs a real player, and a
 * headless server has none, so `/interregnum record deicide` calls the same method.
 * The command is not a test hook bolted on -- it is the second legitimate caller,
 * and having exactly one implementation is what lets the untestable path be three
 * lines of adapter over a path that IS tested.
 */
public final class Deicide {
    private static final Logger LOG = LogUtils.getLogger();

    private Deicide() {}

    /**
     * Kill the god. Idempotent: a world can only lose its god once, and a second
     * call is a no-op rather than a second catastrophe.
     *
     * @param killer the player responsible, or null when no one is (a command).
     * @return true if this call is what did it.
     */
    public static boolean commit(MinecraftServer server, UUID killer) {
        ChapterSavedData data = ChapterSavedData.get(server);
        if (!data.record(Milestone.DEICIDE)) {
            return false;
        }
        data.setKiller(killer);

        // The sun stops.
        //
        // The day cycle was never the sky's; it was the god's, and with nobody left
        // to turn it the light stays exactly where it was at the moment of death.
        // This is the whole announcement. Per docs/WORLD.md the mod never says who
        // did it and never posts a message -- there is simply a world that has
        // stopped moving, and a player who has gone very quiet.
        //
        // NB: the rule is ADVANCE_TIME in 26.x. `doDaylightCycle` was renamed along
        // with most of the gamerule set, so every pre-2026 reference to it is wrong.
        for (ServerLevel level : server.getAllLevels()) {
            level.getGameRules().set(GameRules.ADVANCE_TIME, false, server);
        }

        LOG.info("Deicide committed{}; the daylight cycle has stopped.",
                killer == null ? " (no killer recorded)" : " by " + killer);
        return true;
    }

    /** Where the heart was taken from, for the crater. Unused until the crater exists. */
    public static void markSite(ServerLevel level, BlockPos pos) {
        LOG.info("Deicide site recorded at {} in {}", pos, level.dimension().identifier());
    }
}
