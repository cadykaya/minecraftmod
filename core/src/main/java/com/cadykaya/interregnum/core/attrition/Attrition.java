package com.cadykaya.interregnum.core.attrition;

/**
 * Band 4: <b>the world forgets what it was.</b>
 *
 * `WORLD.md`, locked: *"Attrition is loss by repeated small subtraction, and what gets
 * subtracted is **distinction**. Biome-specific detail reverts to its plainest
 * equivalent. Your forest stops being a forest — not destroyed, *generalised*. … That is
 * what a dead god's world would actually do, because **biomes were its taxonomy**.
 * Nobody is maintaining the categories any more."*
 *
 * This class is the half that decides <b>where</b>. What gets generalised is a
 * conversion table like the unraveling's; which ground is eligible is the rule below,
 * and it is the part with an actual idea in it.
 *
 * <h2>It frays where nobody tends</h2>
 *
 * `WORLD.md` again: *"Regions people visit and keep hold their definition. This makes the
 * 'take the job' ending literal rather than thematic — holding the world together shrine
 * by shrine is exactly the counter-move."*
 *
 * So a chunk carries the last time anybody was near it, and attrition only touches ground
 * whose stamp has gone stale. Walking your territory is the counter-move, and it is a
 * real one rather than a gesture: the apocalypse becomes a thing you can argue with.
 *
 * <h2>The radius problem, which is the whole reason this is a separate class</h2>
 *
 * There is a contradiction sitting under band 4 and it has to be resolved deliberately
 * rather than discovered later:
 *
 * <ul>
 *   <li>Attrition must act where <b>nobody tends</b>.</li>
 *   <li>But like the unraveling it can only act on <b>loaded</b> ground, because
 *       placement tracking answers "claimed" for an unloaded chunk and so protects it
 *       absolutely. Loaded, in practice, means <b>near a player</b>.</li>
 * </ul>
 *
 * Taken naively those cancel out and band 4 can never do anything anywhere.
 *
 * The resolution is that the two radii are <b>different sizes</b>. Tending is intimate —
 * {@link #TEND_RADIUS_CHUNKS} chunks, roughly the ground you are standing on and can
 * see the detail of. Acting reaches as far as the chunks a player keeps loaded, which is
 * several times that. So the ring between them is ground that is present but unattended:
 * <b>the fringe of your world frays while its heart holds.</b>
 *
 * That is not a workaround, it is the mechanic. A base you actually live in keeps its
 * forest. The forest eight chunks out — loaded every day, walked through never — goes
 * quietly generic, and you notice one day that you cannot remember it being like that.
 *
 * <h2>Never-tended ground is not stale ground</h2>
 *
 * A chunk generated after band 4 has no stamp, and the tempting reading is that nobody
 * has ever tended it so it should arrive already generalised. That is wrong, and it is
 * wrong in a way that would read as a bug: a player exploring at band 4 would find fresh
 * land that is *already* plain, which looks like broken worldgen rather than like a world
 * forgetting. Attrition has to be something you can watch happen to a place you knew.
 *
 * So first sight counts as tending: an unstamped chunk is stamped when it is first seen,
 * and frays only if you leave it and do not come back. The world you found is the world
 * it was.
 */
public final class Attrition {
    private Attrition() {}

    /** The band at which the world starts losing its distinctions. */
    public static final int BAND = 4;

    /**
     * How near a player must be to tend a chunk, in chunks.
     *
     * Two: the chunk you are in and its neighbours. Deliberately much smaller than the
     * distance at which chunks stay loaded -- see the class javadoc, the gap between the
     * two IS the mechanic.
     */
    public static final int TEND_RADIUS_CHUNKS = 2;

    /**
     * How long a chunk holds its definition after the last visit, in ticks.
     *
     * Twenty minutes of continuous play. Long enough that ordinary movement around a
     * base keeps the whole base tended without anybody thinking about it; short enough
     * that a region genuinely abandoned shows it inside one session, because an
     * apocalypse nobody ever sees happen is not an apocalypse.
     */
    public static final long FRAY_AFTER_TICKS = 20L * 60 * 20;

    /** Whether the world has unravelled far enough for any of this to happen. */
    public static boolean fraying(int band) {
        return band >= BAND;
    }

    /**
     * Has this ground gone unattended long enough to start losing what made it itself?
     *
     * @param lastTendedTick the game time somebody was last near it
     * @param nowTick        the game time now
     */
    public static boolean stale(long lastTendedTick, long nowTick) {
        return nowTick - lastTendedTick >= FRAY_AFTER_TICKS;
    }

    /**
     * Is a player at this chunk distance close enough to tend?
     *
     * Chebyshev rather than Euclidean, matching how Minecraft measures chunk distance
     * everywhere else: a square of tended ground, not a circle with corners nobody can
     * explain.
     */
    public static boolean tends(int chunkDX, int chunkDZ) {
        return Math.max(Math.abs(chunkDX), Math.abs(chunkDZ)) <= TEND_RADIUS_CHUNKS;
    }
}
