package com.cadykaya.interregnum.core.magic;

/**
 * What a spell costs, and where.
 *
 * <h2>The ban is correct, and the player can find that out</h2>
 *
 * `WORLD.md`, locked: *"**The overworld ban is correct.** With the god dead, all overworld
 * casting draws on the corpse — the residue still holding the world together. Heavy
 * casting visibly frays its surroundings. The Wardens' law is right, and the player can
 * *discover* it is right. Off-world, living gods replenish what casting spends: legal,
 * sustainable. The ban forces travel by law **and** economics."*
 *
 * That is unusual enough to be worth stating plainly: **the enforcement agency is not
 * wrong.** Every instinct a player brings to a game like this says the law banning your
 * powers is arbitrary and the fun is in breaking it. Here it is a correct reading of a
 * real hazard, written by people who could see what casting does. A player who ignores it
 * is not rebelling; they are spending the thing holding their world together, and the
 * evidence is the ground around them.
 *
 * Nobody ever says any of this. The player casts in the overworld, sees the ground go,
 * casts off-world, sees it not, and works it out. Hence "can *discover*".
 *
 * <h2>Why the cost is the unraveling and not a mana bar</h2>
 *
 * The corpse is what the unraveling is already spending. Making casting spend the same
 * thing means the cost is legible in a currency the player has been reading since chapter
 * one — the world visibly coming apart — rather than in a number invented for spells. It
 * also means the cost RISES with the band without anyone tuning it: a more unravelled
 * world has less left to draw on, and the same spell takes more from it.
 */
public final class Casting {
    private Casting() {}

    /**
     * Does casting here draw on the dead god's residue?
     *
     * True in the overworld, false in a living god's world. The whole economics of the
     * mid-game is this one boolean: the ban forces travel by law and by cost, and a
     * player who has felt the second one stops needing to be told the first.
     */
    public static boolean drawsOnTheCorpse(boolean inTheOverworld) {
        return inTheOverworld;
    }

    /**
     * May this person cast this school's spells at all?
     *
     * `WORLD.md` locks schools as *"learned in their worlds"*, so the answer is no until
     * somebody has been taught — and the check belongs here, in the one class that
     * describes what casting costs and requires, rather than inside each spell. A spell
     * that enforced its own prerequisite would be a spell that could forget to.
     */
    public static boolean permitted(Grimoire grimoire, School school) {
        return grimoire != null && grimoire.knows(school);
    }

    /**
     * How far the fraying reaches from a cast, in blocks.
     *
     * Close. `WORLD.md` says casting frays *"its surroundings"*, and the word is doing
     * work: this has to land where the caster is standing and looking, or the cost is
     * something that happens to somebody else's chunk and teaches nothing.
     */
    public static final int FRAY_RADIUS = 6;

    /**
     * How many places one cast spends.
     *
     * Enough to be visible from where you cast. A cost the player has to go looking for
     * is a cost they will never connect to the spell, and then the ban really is
     * arbitrary -- which is the one reading `WORLD.md` rules out.
     */
    public static final int FRAY_SAMPLES = 12;
}
