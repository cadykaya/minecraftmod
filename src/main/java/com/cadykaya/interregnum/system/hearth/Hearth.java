package com.cadykaya.interregnum.system.hearth;

import com.cadykaya.interregnum.system.convert.StepTable;
import com.cadykaya.interregnum.system.claim.Claims;
import com.cadykaya.interregnum.system.unraveling.UnravelingDefs.ConversionDef;
import com.cadykaya.interregnum.worldgen.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * The Hearth-Turner's law: <b>everything here keeps every past it has had.</b>
 *
 * <h2>Not the Verdant with different blocks</h2>
 *
 * The danger with a fourth god is that its world becomes the third world in another
 * colour, which is precisely the failure `AESTHETIC.md` names: *could I replace it with
 * a different random weird thing without changing anything?* So the two mechanisms are
 * deliberately different in kind, not in table.
 *
 * The Verdant asks vanilla for **more of what it already does** — extra random ticks,
 * so its world grows the way any world grows, faster. Nothing new happens there; it
 * happens sooner.
 *
 * Here, something happens that does not happen anywhere else. Vanilla has no notion of
 * stone acquiring moss with age, and the overworld will never do it in a hundred hours.
 * This world applies an explicit **ageing table** — the same block-aging registry that
 * runs the unraveling, which `WORLD.md` locks as one mechanism with two uses.
 *
 * <h2>Accumulating, not coming apart</h2>
 *
 * The unraveling's rules carry a band and a scope because it escalates and has a
 * frontier. Ageing has neither: it is what time does, everywhere, always, at the rate
 * it did yesterday. The overworld is coming apart because nobody is holding it. This
 * world is not coming apart at all — it is *accumulating*, and it will not let any of
 * it go. That is the same god who *"has never let a grievance become past tense"*,
 * expressed in masonry.
 *
 * <h2>The claim promise, stated at the width it is actually kept</h2>
 *
 * Unlike the Verdant's, this one IS categorical, and for a structural reason worth
 * naming: ageing is applied to the block being aged, not to a source that reaches a
 * neighbour. There is no indirect path by which a claimed block can be aged. So
 * <b>a block somebody placed is never aged here, full stop</b> — the same guarantee, and
 * the same ledger, that stops the apocalypse eating a house.
 */
public final class Hearth {
    private Hearth() {}

    /**
     * Positions sampled per chunk section per tick.
     *
     * Exactly vanilla's random tick budget, spent on memory instead of on growth. Not
     * more: ageing is not meant to be watchable. A wall that visibly crawls through its
     * states while you look at it is a special effect; a wall that is mossy when you come
     * back is a world with a history. The 0.35 on each rule does the rest of the slowing,
     * and the chain means a stone needs two separate landings to reach moss.
     */
    public static final int SAMPLES_PER_SECTION = 3;

    /**
     * Age everything in one loaded chunk.
     *
     * Palette-gated per section like the Verdant's growth, but on a different question:
     * there the section is asked whether anything in it random-ticks, and here it is
     * asked whether it holds any block the table has an opinion about. Most sections
     * are air and answer no to both.
     */
    public static void age(ServerLevel level, LevelChunk chunk) {
        StepTable table = TurningLoader.table();
        if (table.ruleCount() == 0) {
            return;
        }
        int minY = chunk.getMinY();
        var sections = chunk.getSections();
        var origin = chunk.getPos();

        for (int i = 0; i < sections.length; i++) {
            var section = sections[i];
            if (section.hasOnlyAir()) {
                continue;
            }
            // A palette-level reject before any position is rolled: if nothing in this
            // section can age, the whole section costs one predicate pass.
            if (!section.maybeHas(state -> table.stepFrom(state.getBlock()) != null)) {
                continue;
            }
            int sectionBottom = minY + (i << 4);
            for (int n = 0; n < SAMPLES_PER_SECTION; n++) {
                BlockPos pos = level.getBlockRandomPos(
                        origin.getMinBlockX(), sectionBottom, origin.getMinBlockZ(), 15);
                BlockState state = section.getBlockState(
                        pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
                ConversionDef rule = table.stepFrom(state.getBlock());
                if (rule == null) {
                    continue;
                }
                // Before the roll, so a block somebody placed is never even a
                // candidate -- the same order the unraveling uses, for the same reason.
                if (Claims.isClaimed(level, pos)) {
                    continue;
                }
                if (level.getRandom().nextFloat() >= rule.chance()) {
                    continue;
                }
                BlockState next = rule.to().defaultBlockState();
                if (!next.canSurvive(level, pos)) {
                    continue;
                }
                // Flag 3: neighbours and clients. No drops -- nothing is broken here,
                // the block has simply been standing a while.
                level.setBlock(pos, next, 3);
            }
        }
    }

    /**
     * Age exactly one block, ignoring the roll. The seam CI reaches through.
     *
     * Ageing is deliberately slow -- see {@link #SAMPLES_PER_SECTION} -- which makes it
     * a poor thing to observe passively inside a check's patience. Worse, the property
     * that matters most is the CHAIN (stone, then cobble, then mossy cobble), and
     * waiting for two independent rolls to land on the same block turns a categorical
     * fact into a statistical one for no benefit.
     *
     * So `interregnum turning age <pos>` applies the table at a position with the chance
     * bypassed, the same seam `unravel at` and `warden post` open for the same reason.
     * Every other gate still applies: wrong dimension, no rule, claimed, unsupported.
     * A check driving this is testing the law, not a special path written for it.
     *
     * @return what the block became, or null if nothing applied.
     */
    public static BlockState ageOnce(ServerLevel level, BlockPos pos) {
        if (!holds(level)) {
            return null;
        }
        return step(level, pos);
    }

    /**
     * One block, one step older, <b>with no opinion about which world it is in</b>.
     *
     * Split out of {@link #ageOnce} when the Turning's school got its first spell.
     * `WORLD.md` locks the reuse — *"the block-aging registry powering the Turning **is
     * the same system that runs the unraveling.** One mechanism; a school and an
     * apocalypse"* — and *Weather* is that mechanism pointed at a block on purpose rather
     * than by the passing of time. A spell you can only cast in the god's own world is
     * not "magic as a builder's palette"; it is scenery.
     *
     * So the dimension gate stays where it belongs, on the two callers that need it, and
     * the law itself is one method. Every OTHER gate is here and applies to both: no rule
     * in the table, a block somebody placed, a result that cannot stand where it is put.
     *
     * @return what the block became, or null if nothing applied.
     */
    public static BlockState step(ServerLevel level, BlockPos pos) {
        return step(level, pos, true);
    }

    /**
     * @param sparePlacedBlocks whether to refuse a block somebody put there.
     *
     * <b>True for the world, false for a caster</b>, and the distinction turned out to be
     * a real one rather than a convenience.
     *
     * The claim ledger exists so the world cannot eat your work: the unraveling, attrition
     * and this world's own clock all pass true, and none of them will ever touch a wall
     * you built. But `WORLD.md` calls *Weather* *"magic as a builder's palette"*, and a
     * palette you cannot use on your own building is not one — the first thing anybody
     * will try is ageing a wall they just made, and it would silently do nothing.
     *
     * Weather inherited the refusal by reusing this method wholesale, and it took writing
     * *Rewind* — where refusing your own wall is obviously absurd, since the spell is
     * literally *repair* — to notice that the same absurdity had already shipped in the
     * spell next to it. The principle, now explicit: <b>the ledger gates the world, not
     * the caster.</b>
     */
    public static BlockState step(ServerLevel level, BlockPos pos, boolean sparePlacedBlocks) {
        BlockState state = level.getBlockState(pos);
        ConversionDef rule = TurningLoader.table().stepFrom(state.getBlock());
        if (rule == null || (sparePlacedBlocks && Claims.isClaimed(level, pos))) {
            return null;
        }
        BlockState next = rule.to().defaultBlockState();
        if (!next.canSurvive(level, pos)) {
            return null;
        }
        level.setBlock(pos, next, 3);
        return next;
    }

    /** Whether this level is the Hearth-Turner's. */
    public static boolean holds(ServerLevel level) {
        return level.dimension() == ModDimensions.TEMPORAL_AUTHORITY;
    }
}
