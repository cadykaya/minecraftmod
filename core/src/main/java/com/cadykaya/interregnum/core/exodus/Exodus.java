package com.cadykaya.interregnum.core.exodus;

/**
 * Band 3: <b>the overworld starts leaking other gods' law.</b>
 *
 * `WORLD.md`, locked: *"Not their blocks. Their **rules**. The dead god's policy was what
 * held the systems apart — the Isolation was a policy, not a wall — and with nobody
 * enforcing it, patches of the overworld begin obeying somebody else's law."*
 *
 * <h2>Why the patches are at the shrines</h2>
 *
 * The shrines are already the mod's map of where the dead god's attention was, and the
 * unraveling already thins the world there. Making them the leaks too says the thing the
 * band is about: **the places its authority was strongest are where its absence shows
 * first.** The Isolation was enforced from the shrines; the shrines are where it fails.
 *
 * It also means the patches are contiguous by construction. A per-chunk roll would
 * scatter single chunks of foreign law across the map, and *"a hollow where nothing makes
 * a sound"* is one place you can stand in and walk out of, not confetti.
 *
 * <h2>Which god, and why it must never change its mind</h2>
 *
 * A shrine's leak is a pure function of where the shrine is. Nothing is stored, nothing
 * is rolled at runtime, and a player who walks away from a silent hollow and comes back
 * next week finds the same silent hollow.
 *
 * That matters more than it looks. `WORLD.md`: *"Each patch is shaped like exactly one
 * god, and it is **the same law you will meet in their world**. So band 3 is
 * reconnaissance: the apocalypse is teaching you the curriculum."* A patch that changed
 * god between visits would teach nothing — it would be weather.
 */
public final class Exodus {
    private Exodus() {}

    /** The band at which the overworld starts leaking. Below this, nothing here applies. */
    public static final int BAND = 3;

    /**
     * The four laws, in the order the pantheon table lists them.
     *
     * Deliberately a core enum with no Minecraft in it: which god leaks where is a fact
     * about coordinates, and the machinery that *applies* a law already lives in the
     * game module next to the dimension that shares it.
     */
    public enum Law {
        VERDANT,
        ANCHORITE,
        HEARTH_TURNER,
        QUIET_ONE
    }

    private static final Law[] LAWS = Law.values();

    /**
     * Which god's law leaks at the shrine in this chunk.
     *
     * A finalising integer hash rather than {@code (x + z) % 4} or
     * {@code Objects.hash(x, z)}. The cheap versions look fine until you plot them:
     * {@code (x + z) % 4} makes diagonal stripes, so a player walking one direction meets
     * the same god's law over and over and the curriculum has three quarters missing.
     * The mixing below is the standard two-round finaliser; what matters here is only
     * that it decorrelates neighbouring coordinates, which the self-test asserts by
     * sampling rather than by trusting this comment.
     */
    public static Law lawAt(int chunkX, int chunkZ) {
        int h = chunkX * 0x9E3779B9 ^ chunkZ * 0x85EBCA6B;
        h ^= h >>> 16;
        h *= 0x7FEB352D;
        h ^= h >>> 15;
        h *= 0x846CA68B;
        h ^= h >>> 16;
        return LAWS[Math.floorMod(h, LAWS.length)];
    }

    /** Whether the world has unravelled far enough for any of this to happen. */
    public static boolean leaking(int band) {
        return band >= BAND;
    }
}
