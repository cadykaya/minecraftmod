package com.cadykaya.interregnum.core.magic;

/**
 * <b>Wildgrowth</b> — the Verdant's second spell.
 *
 * `WORLD.md`, locked: *"**Verdancy** (Verdant): *Bridgeroot* … *Hedge* · *Graft* ·
 * *Wildgrowth* — and in the Verdant's own world, accelerating growth is a **hazard**."*
 *
 * <h2>The fourth caller of one law</h2>
 *
 * This does not implement growth. It runs {@code Verdant.quicken}, which is the same
 * acceleration the Verdant's world applies to every loaded chunk it holds and the same
 * one band 3's leaks apply to a patch of overworld that has forgotten whose it is. The
 * progression is the school system's whole argument, and by now it has run four times —
 * {@link Lighten} says the same thing about the Anchorite's weight. You meet a god's law
 * as a place, meet it again as a wrongness leaking into your own world, and then you are
 * the one doing it.
 *
 * <h2>It accelerates; it does not choose</h2>
 *
 * `Verdant` refuses to keep a list of growable blocks — *"a hand-written list would be out
 * of date the first time Mojang shipped a plant"* — and so does this. What one cast does
 * is ask the world to tick, hard, in a small volume. Every crop, sapling, vine, moss and
 * mushroom is covered without being named, and so is whatever the next game drop adds.
 *
 * Which is also why the locked word is *hazard*. A surge you cannot aim at the wheat and
 * away from the jungle is a surge that closes the path behind you. In the Verdant's own
 * world this spell is nearly pointless — everything is already growing at eight times the
 * rate you know — and casting it there is free, because a living god replenishes what
 * casting spends. The spell costs most exactly where it does most.
 *
 * <h2>The ledger, and where the line actually falls</h2>
 *
 * `LESSONS.md` #35 put it as *the ledger gates the world, not the caster*, and that was
 * the fix for a spell aimed at one block. Said as flatly as that it would hand this spell
 * a licence over other people's greenhouses, so here is the sharper form the code has in
 * fact always had: <b>the ledger gates what you did not aim at.</b>
 *
 * <ul>
 *   <li>{@code Weather} and {@code Rewind} name one block, and take it — your own wall
 *       included.</li>
 *   <li>{@code Drop-forge} changes exactly the block your weight lands on, and you chose
 *       where to drop it.</li>
 *   <li>A cast's <b>fraying</b> sweeps a volume nobody pointed at, and has spared placed
 *       blocks since the day it was written.</li>
 *   <li>Wildgrowth sweeps a volume too. Same answer.</li>
 * </ul>
 *
 * So your neighbour's leaves do not decay because you wanted your wheat in early, and
 * nothing about a spell you aimed at one block has changed.
 */
public final class Wildgrowth {
    private Wildgrowth() {}

    /** The school this belongs to. */
    public static final School SCHOOL = School.VERDANCY;

    /**
     * How far the surge reaches, in blocks.
     *
     * Three: a garden bed. Smaller than a {@link Lighten}, which is a room, and for a
     * reason particular to an effect that cannot be aimed — everything inside is subject,
     * including whatever you had not thought about. A large indiscriminate spell is one
     * whose blast radius the caster stops being able to hold in their head, and the
     * moment that happens the hazard stops being a decision and becomes an accident.
     */
    public static final int RADIUS = 3;

    /**
     * How many random ticks one cast delivers to every position inside it.
     *
     * Not a rate and not a probability: every position gets exactly this many, so a cast
     * does the same thing twice and a player can learn what it is worth. The three
     * systems that grow a world on a clock all use probability, because a rate is what
     * weather is; this is an act, and an act that varies is a gamble.
     *
     * Twenty-four, calibrated against the plainest thing in the game that counts its own
     * ticks: sugar cane advances one segment on exactly sixteen. One cast has to be
     * plainly worth more than one segment of cane or nobody would spend a journey on the
     * school, and plainly less than a forest, or the hazard is a farming convenience.
     */
    public static final int PUSHES = 24;
}
