package com.cadykaya.interregnum.core.portal;

import com.cadykaya.interregnum.core.magic.Spell;

/**
 * The Verdant's portal: <b>you plant it and wait.</b>
 *
 * `WORLD.md`, locked: *"You **plant** it and wait. It opens when mature and closes when
 * cut: the only portal in the mod with a lifespan. Opened with **Bridgeroot** /
 * **Wildgrowth** — or patience, which is worse there."*
 *
 * <h2>The door has a life, and that is the whole of it</h2>
 *
 * Every other portal in this mod is a state of the world you can put back: a lightened
 * shaft lapses, an hour comes round again. This one is **a thing that was born and can
 * die**, and it is the only one somebody else can take away from you — a portal you
 * planted is a portal a stranger can fell.
 *
 * <h2>There is no timer, and there was nearly one</h2>
 *
 * The obvious build is *planted at tick T, opens at T + N*. It is wrong, and wrong in a
 * way that would have quietly deleted the locked clause *"or patience, which is worse
 * there"*: with a timer, the school does nothing that waiting does not, and *Wildgrowth*
 * becomes a stopwatch you are allowed to skip.
 *
 * So maturity is not a duration. It is **a fact about the world**: did the thing you
 * planted become a tree? Vanilla's own growth answers that, *Wildgrowth* is the school's
 * way of making the answer yes now, and patience is the alternative that genuinely works
 * — the Verdant's world already grows at eight times the rate you know. Patience is
 * *worse* there for the reason `WORLD.md` gives elsewhere: in a world where growth is the
 * hazard, standing about is not free.
 *
 * <h2>Passage is stillness, and that is not the Anchorite's rule wearing green</h2>
 *
 * The Anchorite's shaft asks you to stop holding on. This one asks you to stop moving,
 * and the two are opposite readings of the same idea rather than one mechanism twice: one
 * is a thing you do to yourself in mid-air, the other is a thing you allow the world to do
 * to you. `WORLD.md` locks that growth here is a *hazard* — the path you cut closes behind
 * you — so the price of this door is letting that catch up with you.
 *
 * It also solves a geometry problem honestly. A mature trunk is a solid block; there is no
 * "walking into" it. Standing under what you grew is a thing a player can actually do.
 */
public final class Rooting {
    private Rooting() {}

    /**
     * The spell that opens it.
     *
     * `WORLD.md` names *Bridgeroot* / *Wildgrowth*. *Wildgrowth* is the one that opens
     * the door: it is the school's verb for hurrying what is already there, which is
     * exactly what a planted sapling is. *Bridgeroot* creates blocks that were not
     * growing and never will — it is how you reach a place, not how you ripen one.
     *
     * Same shape of choice as {@link Descent#KEY}, where the school had two verbs and
     * only one of them was about the thing the door is made of.
     */
    public static final Spell KEY = Spell.WILDGROWTH;

    /**
     * How far from the trunk the doorway reaches, in blocks.
     *
     * Three — a tree's own footprint and a step. Deliberately smaller than the
     * Anchorite's five: that one is a shaft you have to be able to fall down, and this
     * one is a place you have to be able to stand still in on purpose. A wide one would
     * take people who were merely nearby, and a door that opens for a passer-by is not a
     * door you decided to use.
     */
    public static final int REACH = 3;

    /**
     * How long you must not move, in ticks.
     *
     * Three seconds, and longer than the Anchorite's two on purpose. Falling is
     * frightening and self-limiting; standing still costs nothing to start and is only
     * expensive in a world where things grow over you. The number has to be long enough
     * that it is a decision rather than a pause, and it is measured against a much lower
     * bar than a jump — so it is set by what it costs, not by what it excludes.
     */
    public static final int STILL_TICKS = 60;

    /** What a planted position is now. Read off the world, never off a clock. */
    public enum State {
        /** Planted, still a sapling. The door exists and is not open yet. */
        SEEDED,
        /** It grew. The door is open. */
        OPEN,
        /**
         * Cut, burnt, eaten, or never there.
         *
         * `WORLD.md`: *"closes when cut"*. There is no separate closing mechanism —
         * the trunk stops being a trunk, and the position stops being a door in the
         * same instant, because the door was never anything but the tree.
         */
        GONE
    }

    /**
     * What is standing at a planted position.
     *
     * Both booleans come from the world's own blocks, which is the design: this rule
     * cannot be satisfied by anything remembered, so a portal cannot outlive the tree
     * that is it. The order matters — a log answers OPEN even if something has also put a
     * sapling there, because the tree is what the door is.
     */
    public static State state(boolean trunk, boolean sapling) {
        if (trunk) {
            return State.OPEN;
        }
        return sapling ? State.SEEDED : State.GONE;
    }

    /**
     * One tick of standing still.
     *
     * @param held   how long this thing has been still, from the last call
     * @param under  whether it is within {@link #REACH} of an open door
     * @param moved  whether it changed block since the last tick
     *
     * Measured in whole blocks rather than in distance, so turning on the spot, shuffling
     * and the ordinary drift of standing on a slope all count as still. The thing being
     * asked is *did you go anywhere*, and a rule strict enough to notice a mob's idle
     * fidget would be a door nobody could hold open.
     */
    public static int rest(int held, boolean under, boolean moved) {
        return !under || moved ? 0 : held + 1;
    }

    /** Has it been still for long enough to go through? */
    public static boolean opens(int held) {
        return held >= STILL_TICKS;
    }
}
