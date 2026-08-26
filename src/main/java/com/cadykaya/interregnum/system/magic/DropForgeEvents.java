package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.core.magic.Spell;
import com.cadykaya.interregnum.system.anchorite.Crush;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * The moment a weight lands, inside a drop-forge.
 *
 * <h2>Post, and this is the one place in the mod where that is forced</h2>
 *
 * Every other falling-block handler here uses `EntityTickEvent.Pre` — {@link
 * com.cadykaya.interregnum.system.anchorite.AnchoriteEvents} must set the delta before
 * the entity moves, and {@link StillEvents} must cancel the tick before it happens. This
 * one cannot, because of how {@code FallingBlockEntity.tick} is written: it calls
 * {@code move}, which is what sets {@code onGround}, and then lands and {@code discard}s
 * itself — all inside the same tick. So {@code Pre} of the landing tick still sees a
 * block in the air, and {@code Pre} of the tick after never comes.
 *
 * {@code Post} is fired from {@code ServerLevel.tickNonPassenger} immediately after
 * {@code tick()} returns, unconditionally, including for an entity that removed itself
 * during it. That is the only frame in which a landing exists to be noticed.
 *
 * <h2>The block below, not the block that landed</h2>
 *
 * A falling block that lands places itself at its own position, so the thing it landed ON
 * is one below. Crushing the entity's own block instead would be a spell that destroys
 * the hammer, and no forge works that way — the anvil you dropped is still an anvil, and
 * you can pick it up and do it again.
 *
 * Which also means the cost of a crush is a climb. That is the whole economy of the
 * spell and it is deliberately not in this file: nothing here charges anything.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class DropForgeEvents {
    private DropForgeEvents() {}

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        // Most selective test first, same ordering and same reasoning as the two
        // handlers above: an instanceof against a class almost nothing in a running
        // world is, so the zone lookup behind it is paid for by falling blocks only.
        if (!(event.getEntity() instanceof FallingBlockEntity falling)
                || !(falling.level() instanceof ServerLevel level)) {
            return;
        }
        // `onGround` distinguishes a landing from the other way a falling block ends:
        // the ten-minute timeout, and the out-of-world discard, both of which happen in
        // mid-air. A forge that fired on those would crush whatever was under a block
        // that never actually arrived.
        if (!falling.onGround()) {
            return;
        }
        BlockPos landed = falling.blockPosition();
        if (!Zones.covering(level, Spell.DROP_FORGE, landed)) {
            return;
        }
        Crush.crush(level, landed.below());
    }
}
