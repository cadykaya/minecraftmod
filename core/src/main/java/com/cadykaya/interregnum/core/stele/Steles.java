package com.cadykaya.interregnum.core.stele;

/**
 * Which inscription a stele carries, and whether there is light to read it by.
 *
 * <h2>They were blank</h2>
 *
 * `WORLD.md` on chapter 0: *"the mod adds only content: shrines, **warning steles**,
 * sealed sanctums…"* — *"Chapter 0 dressing that players read as ruin flavour for hours,
 * and which after the death is the only instruction anyone left behind."* The block
 * existed, the texture existed, and there was **no text on it anywhere**. A shipped line
 * of the shrine-keeper's says *"the steles are readable if you have the light for it;
 * most people don't bother"*, which was not true of anything.
 *
 * <h2>The words never change, and that is the joke</h2>
 *
 * `WORLD.md`'s locked comedy list names *"steles that re-read differently"*. The tempting
 * build is text that swaps at the deicide. It is the weaker one and it throws the joke
 * away: what re-reads differently is **the reader**. A stele is civic safety boilerplate
 * for a hundred hours and then, one afternoon, the last instruction anybody left — without
 * a single word of it changing, because nobody was left to change it.
 *
 * So an inscription is a constant. Nothing here takes a chapter.
 *
 * <h2>Which one, from where it is</h2>
 *
 * A pure function of the stele's coordinates, the same idiom band 3 uses to decide which
 * god leaks where: *"a hollow you walk out of is the same hollow next week."* A stele you
 * read yesterday says the same thing today, two steles in one ruin can say different
 * things, and none of it costs a block state or a saved field.
 */
public final class Steles {
    private Steles() {}

    /**
     * The light a stele needs before its band of script can be made out.
     *
     * Seven, which is one below the level a torch throws at two blocks and comfortably
     * under open daylight: outdoors in the day is always enough, a lit room is enough, and
     * a ruin at night or a buried stele is not.
     *
     * The number exists because the keeper says it does — *"if you have the light for
     * it"* — and a line of shipped dialogue that describes a rule nothing implements is a
     * worse kind of wrong than a missing feature. It also gets sharper after the death,
     * with nobody left to turn the sun: a world whose god died at night has steles it can
     * no longer read.
     */
    public static final int READING_LIGHT = 7;

    /** Whether there is enough light here to make out a band of worn script. */
    public static boolean legible(int light) {
        return light >= READING_LIGHT;
    }

    /**
     * How many distinct inscriptions there are.
     *
     * Small on purpose. These are civic notices from one office, and an office does not
     * write a different notice for every post — the repetition IS the worldbuilding, and
     * a player who has read all of them has learned the whole of what the Wardenate had
     * to say, which is the point.
     */
    public static final int COUNT = 5;

    /**
     * Which inscription stands at these coordinates.
     *
     * Deterministic and stable: the same stele always reads the same. The mix is the
     * cheap integer hash this project already uses for the leaks, and `Math.floorMod`
     * rather than `%` because a negative coordinate would otherwise index backwards off
     * the end of the list — which is the sort of thing that works everywhere the author
     * happened to test and breaks west of zero.
     */
    public static int inscriptionAt(int x, int y, int z) {
        int h = x * 73_856_093 ^ y * 19_349_663 ^ z * 83_492_791;
        h ^= h >>> 16;
        return Math.floorMod(h, COUNT);
    }
}
