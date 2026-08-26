package com.cadykaya.interregnum.system.anchorite;

import com.cadykaya.interregnum.system.unraveling.UnravelingDefs.ConversionDef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What one impact does to the block underneath it.
 *
 * <h2>Force, and what different things do under it</h2>
 *
 * The table is not a recipe list with a theme; it answers one question, the way every
 * other {@link com.cadykaya.interregnum.system.convert.StepTable} here does. The question
 * is <b>what does this do when something heavy lands on it</b>, and there are exactly two
 * answers in the world: rock <i>shatters</i>, and loose or soft matter <i>packs</i>.
 *
 * Stone becomes cobble becomes gravel becomes sand. Snow becomes ice becomes packed ice
 * becomes blue ice. Both are one step at a time, both go somewhere a player wanted to
 * get, and neither is a crafting grid with extra steps: you can watch the intermediate
 * states happen, and stop at any of them.
 *
 * <h2>The ledger does not apply here</h2>
 *
 * A crush only ever happens because somebody cast a spell on this ground and then dropped
 * something into it. That is a caster acting, not the world acting, and the claim ledger
 * gates the world — it is the promise that the unraveling, attrition and the Turning's
 * clock will never take a block you placed. It was never a promise that you may not
 * crush your own cobblestone into your own gravel on purpose.
 *
 * Compare {@link com.cadykaya.interregnum.system.hearth.Hearth#step(ServerLevel,
 * BlockPos, boolean)}, which has the same split for the same reason, and had it wrong
 * once. See `LESSONS.md` #35.
 */
public final class Crush {
    private Crush() {}

    /**
     * Crush the block at {@code pos}, if the table has an opinion about it.
     *
     * @return what it became, or null if nothing crushes it.
     */
    public static BlockState crush(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        ConversionDef rule = CrushingLoader.table().stepFrom(state.getBlock());
        if (rule == null) {
            return null;
        }
        BlockState next = rule.to().defaultBlockState();
        level.setBlock(pos, next, 3);
        return next;
    }
}
