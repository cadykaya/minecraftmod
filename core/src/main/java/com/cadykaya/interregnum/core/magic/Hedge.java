package com.cadykaya.interregnum.core.magic;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Hedge</b> — the Verdant's third spell, and the only defence in the mod improved by
 * being attacked.
 *
 * `WORLD.md`, locked: *"A living wall that grows where you draw it and **thickens where it
 * is struck**. The only defence in the mod improved by being attacked."*
 *
 * <h2>The second clause is the spell; the first is a bridge stood on its end</h2>
 *
 * Growing a line of blocks toward your gaze is {@link Bridgeroot}, and this reuses that
 * geometry rather than inventing a second one — a span is a span, and the school should
 * not have two ways of drawing the same line. What makes this a different spell is what
 * happens **after**: cut a hedge and it comes back thicker.
 *
 * <h2>Why "improved by being attacked" is worth the trouble</h2>
 *
 * Every other defensive thing in this mod is a refusal that runs out. {@code Hush} lasts
 * twenty seconds; {@code Still} holds what is already moving; {@code Moor} lets go after a
 * minute. They are all versions of *wait*, and a patient attacker beats all three.
 *
 * A hedge does the opposite: patience makes it worse for you. Two blocks grow for every one
 * cut, so a wall somebody has been hacking at for a while is a **denser** wall than the one
 * they started on. It is still not invulnerable — the block you struck really is gone, and
 * a methodical person gets through eventually — but they arrive at a thicket they built
 * themselves.
 *
 * That is the Verdant's law read as a defence rather than as a hazard. `WORLD.md` calls
 * accelerating growth a *hazard* in that god's own world, where the path you cut closes
 * behind you. This is the same sentence pointed at somebody else.
 */
public final class Hedge {
    private Hedge() {}

    /** The school this belongs to. */
    public static final School SCHOOL = School.VERDANCY;

    /**
     * How tall a hedge grows, in blocks.
     *
     * Three: over a player's head, and not by much. A wall you can step over is not a wall,
     * and one you cannot see across turns every fight into a room — this is a hedge, which
     * is a thing you go around.
     */
    public static final int HEIGHT = 3;

    /**
     * The longest wall one cast draws, in blocks.
     *
     * Eight, against {@link Bridgeroot#MAX_SPAN}'s twelve, and shorter on purpose. A bridge
     * is measured against a ravine and has to be able to cross one. A hedge is measured
     * against a doorway, and a spell that fenced off a valley in one word would make the
     * cost of casting a rounding error — which is the same argument the bridge makes about
     * spanning any distance, at the length this spell is for.
     */
    public static final int MAX_LENGTH = 8;

    /**
     * How many blocks grow for every one cut.
     *
     * Two — the smallest number that is more than one, and the number matters more than it
     * looks. At one, cutting is free and the hedge is merely stubborn. At three or more, a
     * few strikes bury the attacker in wall and the spell stops being a hedge and becomes a
     * trap that punishes touching it.
     *
     * Two is *this costs you more than it costs me*, said as arithmetic, and it leaves the
     * hole you actually cut open: you are getting through, and the getting through is
     * getting harder.
     */
    public static final int THICKENS_BY = 2;

    /**
     * The most blocks one hedge may ever hold.
     *
     * A wall of eight by three is twenty-four, so this is room to be cut at about a hundred
     * times before it stops answering. Past that a strike takes a block and grows nothing,
     * and the hedge is an ordinary wall being dismantled.
     *
     * The cap exists because *grows when struck* is otherwise unbounded, and an unbounded
     * one is a way to fill a world with leaves by hitting a bush — which is a griefing tool
     * wearing a spell's clothes.
     */
    public static final int MAX_BLOCKS = 128;

    /**
     * Where a wall stands, given the line it is drawn along.
     *
     * The span's own offsets, each repeated up the height. Ordered nearest-first and
     * bottom-up within each step, so a wall that runs out of room stops at a sensible place
     * rather than in a checkerboard.
     *
     * The origin column is skipped for {@link Bridgeroot}'s reason, said about a different
     * spell: the caster is standing there, and a wall grown into the space you occupy is a
     * wall that suffocates you the first time you use it correctly.
     */
    public static List<int[]> wall(int dx, int dz) {
        List<int[]> out = new ArrayList<>();
        int steps = Math.max(Math.abs(dx), Math.abs(dz));
        if (steps == 0) {
            return out;
        }
        int reach = Math.min(steps, MAX_LENGTH);
        for (int i = 1; i <= reach; i++) {
            int x = Math.round((float) dx * i / steps);
            int z = Math.round((float) dz * i / steps);
            for (int y = 0; y < HEIGHT; y++) {
                out.add(new int[] {x, y, z});
            }
        }
        return out;
    }

    /** How many blocks a strike grows, given how big the hedge already is. */
    public static int thickening(int already) {
        return already >= MAX_BLOCKS ? 0 : THICKENS_BY;
    }
}
