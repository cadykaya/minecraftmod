package com.cadykaya.interregnum.system.warden;

import com.cadykaya.interregnum.system.claim.Claims;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

/**
 * What a Warden finds when it stops walking: <b>a return, not an accusation.</b>
 *
 * <h2>Why this files a census and does not cite anybody</h2>
 *
 * `WORLD.md` says Wardens *inspect, cite, confiscate, escalate*, and the tempting move
 * is to build all four at once. It cannot be done honestly yet, and the reason is worth
 * writing down rather than discovering again in three months.
 *
 * **A citation needs an offence, and the mod does not have one it can find.** The locked
 * countermeasures — shielded casting rooms, forged dispensations — say plainly that what
 * the Wardenate polices is *casting*, and magic does not exist yet. Of the two locked
 * offences that need no magic, one is the sleep code and the other is
 * *permitted airspace* (`WORLD.md`: **the height limit | permitted airspace**) — and the
 * second is only findable once the unraveling has loosened the limit enough for anyone
 * to break it. Picking a lower ceiling to make it findable today would be inventing a
 * rule the world does not have. Flagged for the owner in HANDOFF instead.
 *
 * So the unit does the part it can do without inventing anything, which happens to be
 * the part it was already doing in the shipped dialogue:
 *
 * <blockquote>
 * This unit is conducting a census of the living. Attendance was nine hundred and
 * forty-one. Attendance is now nine hundred and forty.
 * </blockquote>
 *
 * It counts. It files. It does not conclude.
 *
 * <h2>The count is of a SITE, never of a person</h2>
 *
 * `WORLD.md` locks *enforcement targets sites, never a single player — no player is the
 * system's butt*, and that constraint shapes this class rather than merely being obeyed
 * by it. The return is keyed to a position, records what is standing there, and has
 * nowhere to put a name even if something later wanted one.
 *
 * <h2>Why a return with no consequence is still worth shipping</h2>
 *
 * Because it is the half that can be wrong invisibly. A patrol that also filed citations
 * would make it impossible to say which of the two was broken when one of them was — and
 * the survey is the part whose failure looks exactly like success, since a unit that
 * counts nothing and a site with nothing in it file identical returns.
 */
public final class SiteReturn {
    private static final Logger LOG = LogUtils.getLogger();

    private SiteReturn() {}

    /**
     * How far around the inspection point the unit looks, in chunks.
     *
     * One: the chunk it is standing in. The claim ledger is stored per chunk, so this
     * is the radius at which the count costs nothing — and a beat of four points on a
     * ring of eight already walks a Warden across several chunks per circuit, so the
     * coverage comes from the walking rather than from the reach.
     */
    public static final int REACH_CHUNKS = 1;

    /** One filing: where the unit stood, and what was standing there. */
    public record Return(BlockPos at, int built) {}

    /**
     * Survey the site at {@code at} and file what is there.
     *
     * "What is there" is the count of blocks a PLAYER placed, taken from the same
     * ledger the unraveling reads to know what not to eat. That is deliberate reuse:
     * the two systems agree by construction on what counts as somebody's work, and a
     * Warden whose idea of a built site differed from the apocalypse's would be a
     * second definition of the same word.
     */
    public static Return survey(ServerLevel level, BlockPos at) {
        int built = 0;
        for (int dx = -REACH_CHUNKS; dx <= REACH_CHUNKS; dx++) {
            for (int dz = -REACH_CHUNKS; dz <= REACH_CHUNKS; dz++) {
                built += Claims.count(level, at.offset(dx * 16, 0, dz * 16));
            }
        }
        return new Return(at.immutable(), built);
    }

    /**
     * File it.
     *
     * In docket register, because everything this institution emits is in docket
     * register, and because the log is where a server operator meets the Wardens
     * before any player does. `RETURN FILED` rather than `inspected site`: the unit
     * does not narrate, it records.
     *
     * A count of zero is filed exactly like any other. An institution that only
     * reported when it found something would be one you could learn to read for
     * signal, and the whole point of these returns is that they are indifferent.
     */
    public static void file(Return r) {
        LOG.info("RETURN FILED  site={} {} {}  built={}",
                r.at().getX(), r.at().getY(), r.at().getZ(), r.built());
    }
}
