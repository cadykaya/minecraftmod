package com.cadykaya.interregnum.core.magic;

/**
 * <b>Loft</b> — the Anchorite's third spell.
 *
 * `WORLD.md`, locked: *"**Weight** (Anchorite): … *Loft* — make a small structure
 * weightless and carry it."*
 *
 * <h2>Two casts, because "carry" is a verb with a middle</h2>
 *
 * A single cast that named where the shed came from and where it was going would be a
 * *move*, and moving is not what the locked line says. Carrying is picking a thing up,
 * walking, and putting it down — so Loft is <b>lift</b> and <b>set down</b>, and the
 * distance is whatever the caster's own legs covered in between. There is no range on the
 * setting down and there should not be: the range IS the walk.
 *
 * <h2>Weightless has no timer</h2>
 *
 * Nothing expires. Every other spell in the mod lapses, and this one must not: a loft that
 * ran out would drop a house wherever its owner happened to be standing, or worse, delete
 * it. Weightless means it costs nothing to hold, so holding it costs nothing — including
 * across a logout, which is why {@code Lofted} is saved with the world rather than kept in
 * memory the way spell zones are.
 *
 * <h2>One at a time, and it stays in its own world</h2>
 *
 * A caster carries one structure. Two would need a way to say which, and "the shed" is not
 * a thing a player can disambiguate at a command prompt or in a hand.
 *
 * And a load may only be set down in the world it was lifted from. `WORLD.md` locks
 * *"travel between systems is only by ferry"*, and a spell that walked a workshop into
 * another god's world through a portal would be a second way to do the one thing the ferry
 * exists to be. The ferry is how buildings cross. This is how they move around the yard.
 */
public final class Loft {
    private Loft() {}

    /** The school this belongs to. */
    public static final School SCHOOL = School.WEIGHT;

    /**
     * The most blocks one loft may contain.
     *
     * <b>Small</b> is the locked word, and this number is what makes it mean something.
     * The ferry's hull cap is {@code 4096} because a ferry is a vessel you furnish; this
     * is a four-by-four-by-four of solid blocks, or a shed with room inside it, and a
     * caster can picture it without counting.
     *
     * Keeping the two caps far apart is the point rather than a side effect. At a ferry's
     * cap this spell would be a ferry that needs no keel, no dock and no checklist, and
     * the crossing laws would be a thing you could simply decline to use.
     */
    public static final int MAX_BLOCKS = 64;

    /**
     * How far from the cast a block may be and still be part of the structure lifted.
     *
     * This is not a radius on the structure -- the walk decides that, the way the ferry's
     * does. It is how far the caster may be standing from the thing they mean, and it is
     * short for the same reason {@link Quell}'s is: at a range where you could not say
     * which building you meant, "that one" stops being what the spell does.
     */
    public static final int REACH = 3;

    /** Is a structure of this size past what a caster may carry? */
    public static boolean tooLarge(int blocks) {
        return blocks > MAX_BLOCKS;
    }
}
