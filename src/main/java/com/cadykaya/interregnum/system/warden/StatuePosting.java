package com.cadykaya.interregnum.system.warden;

import com.cadykaya.interregnum.content.block.WardenStatueBlock;
import com.cadykaya.interregnum.content.entity.WardenEntity;
import com.cadykaya.interregnum.core.chapter.Milestone;
import com.cadykaya.interregnum.core.spatial.Facing;
import com.cadykaya.interregnum.registry.ModEntities;
import com.cadykaya.interregnum.system.ChapterSavedData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the Wardenate can reach.
 *
 * A woken statue posts a Warden. That is the whole mechanism, and it is the answer to
 * a question the mod had been carrying since the entity existed: nothing created one,
 * so `WARDEN_CONTACT` -- and therefore band 2 -- was unreachable by playing.
 *
 * <h2>Why the statue and not a spawn table</h2>
 *
 * The statues were handed out as scenery for a hundred hours before the death and they
 * all opened their eyes at once when it happened. Making the woken statue the thing
 * that CALLS a Warden turns that scenery into a map: where the statues are is where
 * enforcement reaches, players can read it off the landscape, and tearing one down
 * becomes a real decision with a real cost. A spawn table would have been invisible
 * and arbitrary; this is legible and already sitting in everybody's garden.
 *
 * <h2>The statue is permanent, the Warden is not</h2>
 *
 * A posted Warden is explicitly NOT persistence-required (see
 * {@link WardenEntity#posted()}). It stands down when nobody is there and the statue
 * posts another when somebody returns. The alternative -- one immortal Warden per
 * statue, forever -- is not an institution, it is a leak, and on a server where people
 * have built with these blocks for a hundred hours it is a very large one.
 *
 * <h2>The cap is real and it is logged</h2>
 *
 * {@link #MAX_POSTED_PER_SWEEP} bounds what one pass will do. A player who has built a
 * colonnade of forty statues is expressing something the design wants to honour, but
 * not by spawning forty mobs in one tick. When the cap bites it says so, because a
 * silent truncation reads as "everything was handled" when it was not.
 */
public final class StatuePosting {
    private static final Logger LOG = LogUtils.getLogger();

    private StatuePosting() {}

    /** How far from a player a statue can notice them, in blocks. */
    public static final int NOTICE_RADIUS = 32;

    /**
     * How close a Warden has to be for a statue to consider itself answered.
     *
     * Generous on purpose. A statue that re-posts because its Warden wandered eight
     * blocks off would breed them; the failure that matters is duplicates, and the
     * cost of being too generous is only that one Warden covers two nearby statues.
     */
    public static final int ANSWERED_RADIUS = 24;

    /** Bounded work per pass, and named when it bites. */
    public static final int MAX_POSTED_PER_SWEEP = 3;

    /** Every player's surroundings, once. */
    public static void tick(ServerLevel level) {
        if (!ChapterSavedData.get(level.getServer()).has(Milestone.DEICIDE)) {
            return;                      // nothing is awake yet; nothing posts
        }
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }
        for (ServerPlayer player : players) {
            postAround(level, player.blockPosition());
        }
    }

    /**
     * Post Wardens for woken statues near a point. Returns how many were posted.
     *
     * Takes a centre rather than a player so the whole path is reachable from a
     * command: a headless server has no players at all, so a mechanism that could
     * only be driven by proximity could never be asserted in CI. The unraveling
     * takes the same shape for the same reason.
     */
    public static int postAround(ServerLevel level, BlockPos centre) {
        List<BlockPos> statues = wokenStatuesNear(level, centre);
        int posted = 0;
        int skipped = 0;
        for (BlockPos statue : statues) {
            if (posted >= MAX_POSTED_PER_SWEEP) {
                skipped++;
                continue;
            }
            if (alreadyAnswered(level, statue)) {
                continue;
            }
            if (post(level, statue)) {
                posted++;
            }
        }
        if (skipped > 0) {
            LOG.info("Posting capped at {} this pass; {} woken statue(s) near {} not "
                    + "answered yet and will be picked up next pass.",
                    MAX_POSTED_PER_SWEEP, skipped, centre);
        }
        return posted;
    }

    /** Is there a Warden close enough that this statue's call is answered? */
    private static boolean alreadyAnswered(ServerLevel level, BlockPos statue) {
        AABB box = new AABB(statue).inflate(ANSWERED_RADIUS);
        return !level.getEntitiesOfClass(WardenEntity.class, box).isEmpty();
    }

    /**
     * Woken statues in the loaded chunks around a point.
     *
     * Reuses the palette-level rejection {@link com.cadykaya.interregnum.system.WardenWake}
     * established: ask a section's palette whether it mentions a statue at all before
     * walking 4096 blocks of it. Almost no section does, so almost every section costs
     * a handful of comparisons.
     */
    private static List<BlockPos> wokenStatuesNear(ServerLevel level, BlockPos centre) {
        List<BlockPos> found = new ArrayList<>();
        int r = NOTICE_RADIUS >> 4;
        int cx = centre.getX() >> 4;
        int cz = centre.getZ() >> 4;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                // getChunkNow, never getChunk: a posting sweep must never be the
                // thing that drags a chunk into memory. If it is not loaded, the
                // statue in it is not near anybody who matters.
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
                if (chunk == null) {
                    continue;
                }
                collectFromChunk(chunk, centre, pos, found);
            }
        }
        return found;
    }

    private static void collectFromChunk(LevelChunk chunk, BlockPos centre,
                                         BlockPos.MutableBlockPos pos, List<BlockPos> found) {
        LevelChunkSection[] sections = chunk.getSections();
        int minY = chunk.getMinY();
        for (int si = 0; si < sections.length; si++) {
            LevelChunkSection section = sections[si];
            if (section == null || section.hasOnlyAir()
                    || !section.maybeHas(s -> s.getBlock() instanceof WardenStatueBlock)) {
                continue;
            }
            int baseY = minY + (si << 4);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (!(state.getBlock() instanceof WardenStatueBlock)
                                || !state.getValue(WardenStatueBlock.WOKEN)) {
                            continue;
                        }
                        pos.set(chunk.getPos().getMinBlockX() + x, baseY + y,
                                chunk.getPos().getMinBlockZ() + z);
                        if (pos.closerThan(centre, NOTICE_RADIUS)) {
                            found.add(pos.immutable());
                        }
                    }
                }
            }
        }
    }

    /**
     * Stand one up beside its statue.
     *
     * Beside, and facing AWAY: the unit is not admiring the statue, it is standing
     * where the statue is looking. That reads correctly from across a field, which is
     * the only distance most players will ever see one from.
     */
    private static boolean post(ServerLevel level, BlockPos statue) {
        int[][] spots = {{1, 0}, {-1, 0}, {0, 1}, {0, -1},
                         {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
        for (int[] d : spots) {
            BlockPos stand = statue.offset(d[0], 0, d[1]);
            if (!level.getBlockState(stand.below()).isSolid()
                    || !level.getBlockState(stand).isAir()
                    || !level.getBlockState(stand.above()).isAir()) {
                continue;
            }
            WardenEntity warden = ModEntities.WARDEN.get()
                    .create(level, EntitySpawnReason.EVENT);
            if (warden == null) {
                LOG.warn("The statue at {} could not post a Warden: the entity type "
                        + "refused to make one.", statue);
                return false;
            }
            warden.setPosted(true);
            warden.snapTo(stand, Facing.yawToward(d[0], d[1]), 0.0F);
            warden.setHomeTo(statue, ANSWERED_RADIUS);
            if (!level.addFreshEntity(warden)) {
                LOG.warn("The statue at {} could not post a Warden: the level refused "
                        + "the entity at {}.", statue, stand);
                return false;
            }
            LOG.info("The statue at {} posted a Warden at {}.", statue, stand);
            return true;
        }
        LOG.info("The statue at {} has nowhere to post a Warden: no clear tile beside it.",
                statue);
        return false;
    }
}
