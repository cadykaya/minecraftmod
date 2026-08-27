package com.cadykaya.interregnum.system.portal;

import com.cadykaya.interregnum.Interregnum;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Game-bus wiring for the Hearth-Turner's door. {@link com.cadykaya.interregnum.core.portal.Hour}
 * is what it is; {@link Doorway} is the stone; this is only when.
 *
 * Post, like the other two, and here for a plainer reason than either: the question is
 * where something ended up, and asking before the move answers about where it was.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class DoorwayEvents {
    private DoorwayEvents() {}

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        var entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !Doorway.holds(level)) {
            return;
        }
        if (Passing.tick(entity.getUUID(), Doorway.inGap(level, entity.blockPosition()))) {
            Doorway.take(level, entity);
        }
    }

    /** The threshold state does not outlive the server. See {@link Passing#clear}. */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Passing.clear();
    }
}
