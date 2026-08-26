package com.cadykaya.interregnum.system.unraveling;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

import com.cadykaya.interregnum.registry.ModBlocks;
import com.cadykaya.interregnum.system.ChapterSavedData;
import com.cadykaya.interregnum.system.claim.Claims;
import com.cadykaya.interregnum.system.unraveling.UnravelingDefs.Scope;

import java.util.List;

/**
 * How the overworld spends itself once its god is dead.
 *
 * The design constraint that shapes every line of this class is in docs/WORLD.md
 * and is not negotiable: **the unraveling never destroys player-placed blocks.**
 * The moment the apocalypse eats somebody's house the mod has turned hateful and
 * lost them. So every conversion goes through {@link Claims}, which fails closed.
 *
 * Three further decisions worth knowing before changing anything here:
 *
 * **It works on the surface column.** Samples are taken at the top of a column and
 * the two blocks under it, not uniformly through the world. Partly that is where
 * the table's blocks actually live -- grass, flowers, leaves are one layer thick
 * and uniform sampling would essentially never hit them, so band 1 would be
 * invisible and nobody would ever know it was broken. Mostly it is that a world
 * decaying where nobody can see it is not decaying at all.
 *
 * **It only runs near players.** Not a compromise: {@link Claims} answers "claimed"
 * for an unloaded chunk, so an unloaded chunk could never be converted anyway.
 * Sampling around players is the honest version of the reachable set.
 *
 * **It never places a block that cannot stand there.** A dead bush in mid-air pops
 * off on the next tick, so the rule would look like it worked and change nothing.
 * A conversion that immediately falls apart is not a conversion.
 */
public final class Unraveling {
    private Unraveling() {}

    /** Ticks between sampling passes. */
    private static final int PERIOD = 20;
    /** Columns examined per player per pass. */
    private static final int SAMPLES_PER_PLAYER = 16;
    /** How far around a player the world can fray, in blocks. */
    private static final int SAMPLE_RADIUS = 32;
    /** How far below the surface a sample may land. */
    private static final int SAMPLE_DEPTH = 2;
    /** How far the crater makes the world thin, in blocks. */
    static final int CRATER_THIN_RADIUS = 48;
    /** How far a shrine makes the world thin, in chunks around the one holding it. */
    static final int SHRINE_THIN_CHUNK_RADIUS = 1;

    /**
     * Why a position did or did not change. Every branch below names one of these.
     *
     * `progress` is how many gates the position got through before something
     * stopped it, and it is a written-down number rather than the declaration order
     * because reporting the wrong near-miss would send whoever is debugging this to
     * the wrong end of the file.
     */
    public enum Outcome {
        CONVERTED(0),
        /** The god still lives. Chapter 0 changes nothing, ever. */
        DORMANT(0),
        /** Other gods' worlds keep their own laws. */
        WRONG_DIMENSION(0),
        UNLOADED(0),
        /** Nothing in the table converts this block. */
        NO_RULE(0),
        /** A rule exists but the world has not gone far enough yet. */
        BAND_TOO_LOW(1),
        /** In band, but this is not one of the places that band reaches. */
        OUT_OF_SCOPE(2),
        /** Somebody built this. Untouchable. */
        CLAIMED(3),
        /** The replacement could not stand here, so nothing was placed. */
        UNSUPPORTED(4),
        /** Everything allowed it; the dice said not this time. */
        DID_NOT_ROLL(5);

        final int progress;

        Outcome(int progress) {
            this.progress = progress;
        }
    }

    /** @param rule the conversion that fired, or that came closest to firing. */
    public record Decision(Outcome outcome, String rule) {
        static Decision of(Outcome outcome) {
            return new Decision(outcome, "none");
        }
    }

    // ---------------------------------------------------------------- the tick --

    /**
     * How many overworld ticks and sampling passes this server run has seen.
     *
     * Reported by `/interregnum status`, and not only for tests. Everything else
     * about this system is reachable through commands, which means a version where
     * the tick handler was never registered at all would look completely healthy
     * from the outside -- the exact shape of failure docs/VERIFICATION.md rule 3 is
     * about. `ticks` rising is the only evidence that the clock is connected;
     * `passes` rising is the only evidence it is doing anything with it.
     *
     * Deliberately not persisted: the question these answer is "is it running
     * right now", and a number carried over from a previous boot would answer it
     * wrongly.
     */
    private static long ticks;
    private static long passes;

    public static long ticksObserved() {
        return ticks;
    }

    public static long passesRun() {
        return passes;
    }

    public static void tick(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        ticks++;
        if (level.getGameTime() % PERIOD != 0) {
            return;
        }
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }
        ChapterSavedData data = ChapterSavedData.get(level.getServer());
        if (data.mechanicsDormant()) {
            return;
        }
        RandomSource random = level.getRandom();
        for (ServerPlayer player : players) {
            passes++;
            sampleAround(level, player.blockPosition(), data, random);
        }
    }

    /** One pass of columns around a point. Returns how many blocks changed. */
    public static int sampleAround(ServerLevel level, BlockPos centre,
                                   ChapterSavedData data, RandomSource random) {
        return sampleAround(level, centre, data, random, SAMPLE_RADIUS, SAMPLES_PER_PLAYER);
    }

    public static int sampleAround(ServerLevel level, BlockPos centre, ChapterSavedData data,
                                   RandomSource random, int radius, int samples) {
        int converted = 0;
        for (int i = 0; i < samples; i++) {
            int x = centre.getX() + random.nextInt(radius * 2 + 1) - radius;
            int z = centre.getZ() + random.nextInt(radius * 2 + 1) - radius;
            BlockPos surface = surfaceOf(level, x, z);
            if (surface == null) {
                continue;
            }
            BlockPos pos = surface.below(random.nextInt(SAMPLE_DEPTH + 1));
            if (apply(level, pos, data, random, false).outcome() == Outcome.CONVERTED) {
                converted++;
            }
        }
        return converted;
    }

    /**
     * The topmost non-air block of a column, or null if the chunk is not loaded.
     *
     * The heightmap gives the answer and then we walk down to confirm it, rather
     * than trusting it. A heightmap that is stale by a block or two would not throw
     * -- it would just make every sample land in air, and the whole system would do
     * nothing quietly forever. (docs/LESSONS.md #11 is this exact failure, one
     * heightmap over.)
     */
    private static BlockPos surfaceOf(ServerLevel level, int x, int z) {
        ChunkAccess chunk = level.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, false);
        if (chunk == null) {
            return null;
        }
        int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = top; y >= top - 4 && y > level.getMinY(); y--) {
            p.set(x, y, z);
            if (!level.getBlockState(p).isAir()) {
                return p.immutable();
            }
        }
        return null;
    }

    // ----------------------------------------------------------- the decision --

    /**
     * Consider one position, and change it if everything allows.
     *
     * @param certain skip the probability roll -- the operator asked what this rule
     *                does HERE, and an answer of "usually nothing" helps nobody.
     *                Every other guard still applies, which is the point: this is
     *                how the guards get exercised without waiting on dice.
     */
    public static Decision apply(ServerLevel level, BlockPos pos, ChapterSavedData data,
                                RandomSource random, boolean certain) {
        return apply(level, pos, data, random, certain, false);
    }

    /**
     * What one cast costs the world around it.
     *
     * `WORLD.md`, locked: *"With the god dead, all overworld casting draws on the corpse
     * — the residue still holding the world together. Heavy casting visibly frays its
     * surroundings."* The corpse is what this class is already spending, so casting
     * spends it through the same table rather than through a number invented for spells.
     *
     * <b>It does not consult the scope, and that is the one real difference.</b> A band's
     * scope describes where the world is thin enough to come apart <i>on its own</i> —
     * near the crater and the shrines in band 1, everywhere after. Fraying from a cast is
     * not the frontier arriving, it is the caster drawing on the residue where they
     * stand, so it happens wherever they are. Made to obey the scope, band-1 casting
     * would be visibly free anywhere away from a shrine, a player would reasonably
     * conclude the Wardens' ban was arbitrary, and *"the player can discover it is
     * right"* would be false.
     *
     * Certain rather than rolled: a cost that usually does not happen is a cost nobody
     * connects to the spell.
     *
     * @return how many places this cast spent.
     */
    public static int frayAround(ServerLevel level, BlockPos centre, ChapterSavedData data,
                                 RandomSource random, int radius, int samples) {
        int frayed = 0;
        for (int i = 0; i < samples; i++) {
            // THE SURFACE COLUMN, not a cube around the cast. The first version sampled a
            // box and almost always spent nothing: a thirteen-cube is mostly air, so the
            // hit rate was under a tenth and the cost of a cast came out as zero more
            // often than not. That is the same defect as a threshold on a random count
            // (docs/LESSONS.md #31) with the randomness hidden one level down, in where
            // the samples land rather than in what they roll.
            //
            // The surface is also what the sentence means. "Frays its surroundings" is
            // about the ground a caster is standing on and can see, which is the whole
            // reason the cost teaches anything.
            int x = centre.getX() + random.nextInt(radius * 2 + 1) - radius;
            int z = centre.getZ() + random.nextInt(radius * 2 + 1) - radius;
            BlockPos surface = surfaceOf(level, x, z);
            if (surface == null) {
                continue;
            }
            // THE SURFACE BLOCK ITSELF, with no random depth. Depth sampling belongs to
            // the passive unraveling, which is eating a whole world and should reach
            // under it. A cast's cost is local and immediate and has to be RELIABLE: with
            // a random depth only the top layer had a band-1 rule, so one cast in
            // fourteen spent nothing at all, and "casting costs the overworld something"
            // would have been a 93%-true assertion shipped as if it were categorical.
            if (apply(level, surface, data, random, true, true).outcome() == Outcome.CONVERTED) {
                frayed++;
            }
        }
        return frayed;
    }

    private static Decision apply(ServerLevel level, BlockPos pos, ChapterSavedData data,
                                RandomSource random, boolean certain, boolean anywhere) {
        if (level.dimension() != Level.OVERWORLD) {
            return Decision.of(Outcome.WRONG_DIMENSION);
        }
        if (data.mechanicsDormant()) {
            return Decision.of(Outcome.DORMANT);
        }
        if (level.isOutsideBuildHeight(pos)
                || level.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false) == null) {
            return Decision.of(Outcome.UNLOADED);
        }

        BlockState state = level.getBlockState(pos);
        List<UnravelingTable.Rule> rules = UnravelingLoader.table().rulesFor(state.getBlock());
        if (rules.isEmpty()) {
            return Decision.of(Outcome.NO_RULE);
        }

        Outcome nearest = Outcome.BAND_TOO_LOW;
        String nearestRule = "none";
        for (UnravelingTable.Rule rule : rules) {
            if (rule.band() > data.band()) {
                continue;
            }
            nearest = worse(nearest, Outcome.OUT_OF_SCOPE);
            nearestRule = rule.id();
            if (!anywhere && !inScope(level, pos, data, rule.scope())) {
                continue;
            }
            // THE guarantee. Before the roll, so that a block somebody placed is
            // never even a candidate -- not merely usually spared.
            if (Claims.isClaimed(level, pos)) {
                return new Decision(Outcome.CLAIMED, rule.id());
            }
            BlockState next = rule.conversion().to().defaultBlockState();
            if (!next.canSurvive(level, pos)) {
                nearest = worse(nearest, Outcome.UNSUPPORTED);
                continue;
            }
            nearest = worse(nearest, Outcome.DID_NOT_ROLL);
            if (!certain && random.nextFloat() >= rule.conversion().chance()) {
                continue;
            }
            // Flag 3: update neighbours and tell clients. No drops -- nothing is
            // being broken here. The world is simply no longer holding this up.
            level.setBlock(pos, next, 3);
            return new Decision(Outcome.CONVERTED, rule.id());
        }
        return new Decision(nearest, nearestRule);
    }

    /**
     * Which of two near-misses to report. Ordered by how far the position got, so
     * an operator asking "why did nothing happen here" is told the last gate that
     * stopped it rather than the first.
     */
    private static Outcome worse(Outcome a, Outcome b) {
        return b.progress > a.progress ? b : a;
    }

    // -------------------------------------------------------------- the scopes --

    private static boolean inScope(ServerLevel level, BlockPos pos,
                                   ChapterSavedData data, Scope scope) {
        return switch (scope) {
            case OVERWORLD -> true;
            case THIN_PLACES -> isThinPlace(level, pos, data);
        };
    }

    /**
     * Is the world thin here?
     *
     * Two ways to be thin, and they are the two places the god was: where it died,
     * and where it was addressed. Everything else in band 1 is still ordinary
     * ground, which is the whole beat -- for one chapter the wrongness has a
     * boundary you can stand outside of.
     */
    public static boolean isThinPlace(ServerLevel level, BlockPos pos, ChapterSavedData data) {
        GlobalPos site = data.site();
        if (site != null && site.isCloseEnough(level.dimension(), pos, CRATER_THIN_RADIUS)) {
            return true;
        }
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        for (int dx = -SHRINE_THIN_CHUNK_RADIUS; dx <= SHRINE_THIN_CHUNK_RADIUS; dx++) {
            for (int dz = -SHRINE_THIN_CHUNK_RADIUS; dz <= SHRINE_THIN_CHUNK_RADIUS; dz++) {
                if (chunkHoldsShrine(level, cx + dx, cz + dz)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Does this chunk contain shrine masonry?
     *
     * Asked of the section PALETTES, not of 65,536 blocks: a section that has never
     * held shrine stone cannot have it in its palette, so the common answer costs
     * a handful of reference comparisons.
     *
     * `maybeHas` is honest about being a maybe -- a section whose palette has spilled
     * to the global one answers yes to everything. That over-reports, and
     * over-reporting is the safe direction here: the cost is that band 1's gentle
     * conversions reach a little further than intended, while under-reporting would
     * make band 1 invisible. It is also vanishingly rare; a section needs hundreds
     * of distinct block states before its palette goes global.
     */
    private static boolean chunkHoldsShrine(ServerLevel level, int cx, int cz) {
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
}
