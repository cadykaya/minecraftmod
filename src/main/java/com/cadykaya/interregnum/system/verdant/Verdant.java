package com.cadykaya.interregnum.system.verdant;

import com.cadykaya.interregnum.system.claim.Claims;
import com.cadykaya.interregnum.worldgen.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * The Verdant's law: <b>everything grows, and that is the hazard.</b>
 *
 * <h2>Locked, and it says hazard</h2>
 *
 * `WORLD.md` on the Verdancy school: *"and in the Verdant's own world, accelerating
 * growth is a **hazard**."* That word is the whole brief. Fast growth as a convenience
 * is a farming mod; fast growth as a hazard is a place where you cannot keep ground
 * clear, where the path you cut closes behind you, and where standing still is a
 * decision. The mechanism is the same either way — what makes it a hazard is that it
 * applies to everything, everywhere, and not to the things you wanted.
 *
 * <h2>Growth has no attribute either</h2>
 *
 * Like the Anchorite's weight, and unlike the Quiet One's silence, there is nothing in
 * 26.2's `dimension_type` attributes for this: the closest entries are about bees
 * staying in hives and eyeblossoms opening. So the law is a tick handler, and the score
 * is now two gods costing code to one costing data.
 *
 * <h2>How: more random ticks, not a special case per block</h2>
 *
 * Vanilla already grows things through random ticking, so this does not reimplement
 * growth — it asks for more of the thing that already happens. Every crop, sapling,
 * vine, moss and mushroom in the game is covered without naming any of them, and
 * anything a future version adds is covered too. A hand-written list of growable blocks
 * would be out of date the first time Mojang shipped a plant.
 *
 * <h2>What the claim check does, stated narrowly, because the wide version is false</h2>
 *
 * The ledger is consulted before every tick, so <b>this system never applies its extra
 * ticks to a block somebody placed.</b> That is the whole promise, and it is deliberately
 * narrower than it first looks.
 *
 * It is <em>not</em> "a block you placed can never change here". Random ticking is how
 * vanilla grows things, and vanilla grows things by ticking a SOURCE which then reaches
 * to a neighbour — grass spreads by ticking the grass, not the dirt. So an unclaimed
 * grass block can still turn a claimed dirt block beside it, and no check on the ticked
 * position can prevent that. Writing the wide promise into a comment and shipping the
 * narrow one is how a guarantee becomes a lie in the same commit that adds it.
 *
 * The narrow promise is the honest one, and it is enough: <b>everything the mod itself
 * accelerates, it accelerates only on the world's own blocks.</b> What vanilla's ordinary
 * spread reaches is what it reaches at home too — a dirt block turning to grass beside
 * grass is Minecraft, not the apocalypse — and the `WORLD.md` guarantee is about the
 * mod's destructive systems eating somebody's house, which this is not.
 */
public final class Verdant {
    private Verdant() {}

    /**
     * Random tick attempts per chunk section per tick.
     *
     * Vanilla's `randomTickSpeed` is 3 per section per tick. This is deliberately a
     * multiple of that rather than a number chosen by feel, so the world can be
     * described honestly as *growth at N times the rate you know* — which is the only
     * way a player can calibrate against a hundred hours of overworld intuition.
     */
    public static final int VANILLA_RATE = 3;

    /** The multiple. Growth here runs at this many times the overworld's rate. */
    public static final int TIMES = 8;

    /**
     * Grow everything in one loaded chunk.
     *
     * Section-at-a-time and palette-gated: {@link LevelChunkSection#isRandomlyTicking()}
     * is a cheap answer to "is there anything in here that could grow at all", and
     * skipping a section on it costs one check instead of {@link #TIMES} times
     * {@link #VANILLA_RATE} wasted position rolls. Most sections of most chunks are
     * stone or air and answer no.
     */
    public static void grow(ServerLevel level, LevelChunk chunk) {
        int minY = chunk.getMinY();
        LevelChunkSection[] sections = chunk.getSections();
        var origin = chunk.getPos();

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (!section.isRandomlyTicking()) {
                continue;
            }
            int sectionBottom = minY + (i << 4);
            for (int n = 0; n < VANILLA_RATE * TIMES; n++) {
                BlockPos pos = level.getBlockRandomPos(
                        origin.getMinBlockX(), sectionBottom, origin.getMinBlockZ(), 15);
                BlockState state = section.getBlockState(
                        pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
                if (!state.isRandomlyTicking()) {
                    continue;
                }
                // Somebody built this. See the class javadoc: one definition of
                // "somebody's work", shared with the apocalypse.
                if (Claims.isClaimed(level, pos)) {
                    continue;
                }
                state.randomTick(level, pos, level.getRandom());
            }
        }
    }

    /** Whether this level is the Verdant's. */
    public static boolean holds(ServerLevel level) {
        return level.dimension() == ModDimensions.GREEN_AUTHORITY;
    }
}
