package com.cadykaya.interregnum.system.anchorite;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import com.cadykaya.interregnum.Interregnum;

/**
 * Game-bus wiring for the Anchorite's law. See {@link Anchorite} for what it is.
 *
 * `EntityTickEvent.Pre` rather than `Post`: the delta has to be set BEFORE the entity
 * moves, or the block spends every tick falling one step and being pushed back, which
 * is a different and much worse-looking bug than either behaviour on its own.
 *
 * Server-side only. A client that disagreed with the server about which way things fall
 * would show a player sand rising and then snapping back down every time the position
 * was corrected -- the same reason nothing about the unraveling is decided on a client.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class AnchoriteEvents {
    private AnchoriteEvents() {}

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        var entity = event.getEntity();
        // Cheapest test first: almost every entity tick in the game is in some other
        // level, and this runs for every entity in every level on every tick.
        if (entity.level() instanceof ServerLevel
                && Anchorite.holds(entity)
                && Anchorite.unanchored(entity)) {
            Anchorite.lift(entity);
        }
    }
}
