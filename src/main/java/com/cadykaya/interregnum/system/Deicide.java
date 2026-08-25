package com.cadykaya.interregnum.system;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
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
        return commit(server, killer, null, null);
    }

    /**
     * @param level where it happened, or null if nowhere in particular.
     * @param site  the block it happened at, or null.
     */
    public static boolean commit(MinecraftServer server, UUID killer,
                                 ServerLevel level, BlockPos site) {
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
        for (ServerLevel each : server.getAllLevels()) {
            each.getGameRules().set(GameRules.ADVANCE_TIME, false, server);
        }

        if (level != null && site != null) {
            formCrater(level, site);
        }

        int woken = wakeWitnesses(server, level, site);
        if (woken > 0) {
            LOG.info("{} Warden statue(s) opened their eyes.", woken);
        }

        LOG.info("Deicide committed{}; the daylight cycle has stopped.",
                killer == null ? " (no killer recorded)" : " by " + killer);
        return true;
    }

    /** How far around a witness statues wake immediately, in chunks. */
    private static final int WAKE_RADIUS_CHUNKS = 8;

    /**
     * Statues near anyone who could see it happen open their eyes at once.
     *
     * Deliberately NOT every loaded chunk in the world: walking a whole server's
     * loaded set means touching the internals of ChunkMap, and walking its region
     * files would take minutes and lock the server. Everything outside this radius
     * wakes on chunk load instead (see WardenWakeEvents), which is the better beat
     * anyway -- a player who was underground when it happened climbs out and finds
     * the statue in their garden already watching, and gets to notice it themselves.
     *
     * Only chunks that are ALREADY loaded are touched: `getChunk(..., false)`
     * returns null rather than generating, so this can never drag new terrain into
     * existence as a side effect of the god dying.
     */
    private static int wakeNear(ServerLevel level, ChunkPos centre) {
        int count = 0;
        for (int dx = -WAKE_RADIUS_CHUNKS; dx <= WAKE_RADIUS_CHUNKS; dx++) {
            for (int dz = -WAKE_RADIUS_CHUNKS; dz <= WAKE_RADIUS_CHUNKS; dz++) {
                ChunkAccess access = level.getChunk(centre.x() + dx, centre.z() + dz,
                        ChunkStatus.FULL, false);
                if (access instanceof LevelChunk chunk) {
                    count += WardenWake.wakeChunk(level, chunk);
                }
            }
        }
        return count;
    }

    private static int wakeWitnesses(MinecraftServer server, ServerLevel level, BlockPos site) {
        int count = 0;
        if (level != null && site != null) {
            count += wakeNear(level, ChunkPos.containing(site));
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            count += wakeNear(player.level(), player.chunkPosition());
        }
        return count;
    }

    /** How wide and deep the ground gives up. */
    private static final int CRATER_RADIUS = 6;

    /**
     * The ground gives up where the heart was taken.
     *
     * NOT an explosion -- nothing detonated. A god died and the world stopped being
     * held up in that spot, so it **subsides**: a quiet bowl, no fire, no scorching,
     * no sound. Subsidence is also far cheaper than an explosion and does not throw
     * blocks or hurt anything standing nearby, which matters because the person
     * standing nearby is the one who just did it and the mod is not punishing them.
     *
     * **Only natural ground moves.** Anything a player put there stays exactly where
     * it is -- see {@link #isNaturalGround}. That is `WORLD.md`'s standing guarantee
     * that the world may warp but a player's work may not, and it produces the image
     * this beat wants anyway: a house at the shrine is left hanging over a pit,
     * untouched and no longer resting on anything.
     */
    public static void formCrater(ServerLevel level, BlockPos centre) {
        int removed = 0, spared = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -CRATER_RADIUS; dx <= CRATER_RADIUS; dx++) {
            for (int dz = -CRATER_RADIUS; dz <= CRATER_RADIUS; dz++) {
                for (int dy = -CRATER_RADIUS; dy <= 1; dy++) {
                    // A bowl: the lower half of a sphere, plus one course above so
                    // the lip is clean rather than a ring of half-buried blocks.
                    if (dx * dx + dy * dy + dz * dz > CRATER_RADIUS * CRATER_RADIUS) {
                        continue;
                    }
                    p.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    if (level.isOutsideBuildHeight(p)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(p);
                    if (state.isAir()) {
                        continue;
                    }
                    if (!isNaturalGround(state)) {
                        spared++;
                        continue;
                    }
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    removed++;
                }
            }
        }
        LOG.info("The ground gave way at {}: {} blocks subsided, {} left standing.",
                centre, removed, spared);
    }

    /**
     * Is this block part of the world, rather than part of somebody's work?
     *
     * Tag-based and deliberately narrow. Minecraft does not record who placed a
     * block, so this errs toward sparing: an unlisted block is left alone. Sparing
     * a natural block leaves a slightly lumpy crater; removing a placed one deletes
     * something a person made, and only one of those is recoverable.
     */
    private static boolean isNaturalGround(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.SAND)
                || state.is(BlockTags.TERRACOTTA)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS)
                || state.is(BlockTags.REPLACEABLE);
    }
}
