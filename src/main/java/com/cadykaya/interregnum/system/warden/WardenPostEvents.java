package com.cadykaya.interregnum.system.warden;

import com.cadykaya.interregnum.Interregnum;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The clock the statues keep.
 *
 * `LevelTickEvent.Post` fires on both logical sides, so the handler establishes it is
 * on the server first -- a client that posted its own Warden would show a player a
 * mob nobody else can see.
 *
 * Every tick is deliberate rather than a timer: the work is a palette rejection over
 * a handful of loaded sections, and {@link StatuePosting#alreadyAnswered} means the
 * overwhelmingly common outcome is "there is already a Warden here, do nothing".
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class WardenPostEvents {
    private WardenPostEvents() {}

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            StatuePosting.tick(level);
        }
    }
}
