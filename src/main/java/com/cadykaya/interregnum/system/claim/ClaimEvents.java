package com.cadykaya.interregnum.system.claim;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import com.cadykaya.interregnum.Interregnum;

/**
 * Watching what people build.
 *
 * Only PLAYER placements are recorded. A creeper crater, a falling sand block, or
 * a piston shoving terrain around is the world moving, not a person working, and
 * treating it as a claim would slowly protect the entire map from the unraveling.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class ClaimEvents {
    private ClaimEvents() {}

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (event.getLevel() instanceof ServerLevel level) {
            Claims.record(level, event.getPos());
        }
    }

    // NB: BlockEvent.BreakEvent no longer exists in 26.2 -- it is
    // net.neoforged.neoforge.event.level.block.BreakBlockEvent, in its own package.
    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            // Not gated on the breaker being a player: however the block went, it
            // is gone, and a remembered position with nothing in it is just a hole
            // the unraveling will never touch again.
            Claims.forget(level, event.getPos());
        }
    }
}
