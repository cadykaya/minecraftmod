package com.cadykaya.interregnum.core.magic;

/**
 * A patch of world where a spell is in force, and for how long.
 *
 * <h2>Why a spell has a shape at all</h2>
 *
 * `WORLD.md` locks the doctrine: *"**Every spell is a world-verb.** … A spell changes the
 * world's **state** — blocks, physics, capabilities — and its combat use falls out of its
 * world use, never the reverse."*
 *
 * *Weather* changes a block and is done. *Lighten* is the other kind: **a shared
 * low-gravity zone, mobs float too**. It does not target anything. It makes a piece of
 * the world temporarily obey a different law, and whatever walks into it is subject to
 * that law — friend, enemy, or a sheep. That is what makes its combat use fall out of its
 * world use: you cannot aim it at somebody, you can only change where they are standing.
 *
 * <h2>A cube, and deliberately not a sphere</h2>
 *
 * Chebyshev distance, matching how this mod measures every other region. A player has to
 * be able to tell where a zone ENDS — stepping out of it is how you learn it has an edge,
 * the same reasoning that gives band 3's leaks a boundary — and a square edge is one a
 * person can find by walking. A sphere's is not.
 *
 * <h2>It does not survive a restart, on purpose</h2>
 *
 * Zones are held in memory. A spell whose effect outlived the server would be a spell
 * that could strand somebody: log in tomorrow inside a low-gravity field cast by a player
 * who has since left, with no way to know what it is or when it ends. Everything
 * permanent in this mod — regard, what you know, what the world has forgotten — is
 * persisted, and a minute of altered physics is deliberately not in that category.
 */
public record Zone(int x, int y, int z, int radius, long expiresAtTick) {

    /** Is this position inside? Chebyshev, so the region is a cube. */
    public boolean covers(int px, int py, int pz) {
        return Math.abs(px - x) <= radius
                && Math.abs(py - y) <= radius
                && Math.abs(pz - z) <= radius;
    }

    /**
     * Is this column inside, at any height?
     *
     * The zone's FOOTPRINT, which is a different question from {@link #covers} and is
     * asked by exactly one caller: the Anchorite's shaft. `WORLD.md` locks that portal as
     * *"a shaft you do not build but let go into"*, and a shaft is vertical — the cube is
     * where the spell's physics apply, and the column beneath and above it is where the
     * world has stopped having a floor.
     *
     * Kept here rather than computed at the call site because the two questions have to
     * agree about the edge. A shaft one block wider than the field it comes from would be
     * a door you could fall through from outside the spell that opened it.
     */
    public boolean coversColumn(int px, int pz) {
        return Math.abs(px - x) <= radius && Math.abs(pz - z) <= radius;
    }

    /**
     * Has it lapsed?
     *
     * Strictly greater, so a zone is still in force on the exact tick it expires. The
     * alternative loses the last tick to an off-by-one nobody would ever see, and
     * "expires at" reading as "expired at" is the kind of quiet wrongness that only shows
     * up as a spell feeling a moment shorter than it should.
     */
    public boolean expired(long nowTick) {
        return nowTick > expiresAtTick;
    }
}
