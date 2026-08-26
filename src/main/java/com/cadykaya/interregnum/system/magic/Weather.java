package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.core.magic.Casting;
import com.cadykaya.interregnum.core.magic.Grimoire;
import com.cadykaya.interregnum.core.magic.School;
import com.cadykaya.interregnum.system.ChapterSavedData;
import com.cadykaya.interregnum.system.hearth.Hearth;
import com.cadykaya.interregnum.system.unraveling.Unraveling;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * <b>Weather</b> — the Turning's first spell, and the mod's first.
 *
 * `WORLD.md`, locked: *"**Weather** — age blocks: instant mossy/cracked/oxidized — magic
 * as a builder's palette."* And the doctrine it has to satisfy: *"**Every spell is a
 * world-verb.** No damage buttons with particle effects. A spell changes the world's
 * state — blocks, physics, capabilities — and its combat use falls out of its world use,
 * never the reverse."*
 *
 * Weather is the cleanest possible first case for that rule, because it has <i>no</i>
 * combat use at all. It makes stone into cobble into mossy cobble. A player will use it
 * to make a new build look old, which is the entire point: the first thing the mod
 * teaches you to do with magic is decorate.
 *
 * <h2>It is the ageing table, aimed</h2>
 *
 * `WORLD.md` locks the reuse: *"the block-aging registry powering the Turning **is the
 * same system that runs the unraveling.** One mechanism; a school and an apocalypse."*
 * So this calls {@link Hearth#step}, which is the same method the Hearth-Turner's world
 * runs on its own clock — not a copy of it, and not a table tuned to look similar. The
 * spell is that law performed on purpose instead of by the passage of time.
 *
 * Which means every promise the ageing table already makes holds here for free, including
 * the oldest one in the mod: <b>a block somebody placed is never touched.</b> You cannot
 * Weather your neighbour's wall.
 *
 * <h2>And in the overworld it costs</h2>
 *
 * See {@link Casting}. Casting here draws on the corpse, so a successful cast frays the
 * ground around the caster — through {@link Unraveling}, the same machinery spending the
 * same residue, so the cost is legible in a currency the player has been reading since
 * chapter one.
 *
 * <b>Only a successful cast costs anything.</b> Aiming at a block the table has no rule
 * for spends nothing: the residue is drawn on by the world changing, not by the attempt.
 * A spell that frayed the world for a miss would make experimenting with it feel like
 * being punished for curiosity, and the ban would read as arbitrary — which is the one
 * reading `WORLD.md` explicitly rules out.
 */
public final class Weather {
    private Weather() {}

    /** What one cast did: the new block state, how many places it cost, and why not. */
    public record Cast(BlockState became, int frayed, String refused) {
        public boolean worked() {
            return became != null;
        }

        static Cast no(String why) {
            return new Cast(null, 0, why);
        }
    }

    /**
     * Age one block a step, and pay for it if this world makes you.
     *
     * @param pos the block to age
     */
    public static Cast cast(ServerLevel level, BlockPos pos, Grimoire grimoire) {
        // Knowing comes first, and not merely for tidiness: a caster who has never been
        // taught should be told they cannot do this, not told that the stone they aimed
        // at happened to have no rule. Those are different answers and only one of them
        // points at what to do next.
        if (!Casting.permitted(grimoire, School.TURNING)) {
            return Cast.no("unlearned");
        }
        BlockState became = Hearth.step(level, pos);
        if (became == null) {
            return Cast.no("nothing");       // nothing happened; nothing is owed
        }
        if (!Casting.drawsOnTheCorpse(level.dimension() == Level.OVERWORLD)) {
            // A living god replenishes what this spent.
            //
            // `Unraveling` independently refuses to touch anything outside the overworld,
            // so this gate is not the only thing enforcing the rule -- and it is kept
            // anyway, because the rule is a fact about CASTING and belongs where casting
            // is described, in `core`, where it is stated once and testable without a
            // game. Two independent guards on the mod's economics is the right number.
            return new Cast(became, 0, "");
        }
        ChapterSavedData data = ChapterSavedData.get(level.getServer());
        int frayed = Unraveling.frayAround(level, pos, data, level.getRandom(),
                Casting.FRAY_RADIUS, Casting.FRAY_SAMPLES);
        return new Cast(became, frayed, "");
    }
}
