package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.core.magic.Graft;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The clock a graft lives on. See {@link GraftSpell#tend} for what it does.
 *
 * <h2>A timer, and the only one in the magic kit</h2>
 *
 * Every other spell here is an event: something is cast, something is struck, something
 * walks into a zone. This one has to *keep* being true, and the world will undo it whenever
 * it happens to look — so the graft has to look too.
 *
 * Every ten ticks rather than every one, which is {@link Graft#TENDED_EVERY} and is a feel
 * decision as much as a cost one: a scion is restored on the next look, so something that
 * cuts one sees it gone for up to half a second. That reads as the plant being **held**
 * there by something rather than as an invulnerable block.
 *
 * <h2>Per level, and it costs nothing in a world with no grafts</h2>
 *
 * `LevelTickEvent.Post` fires for every loaded level. The due check is arithmetic and
 * `tend` returns on an empty ledger, so the ordinary cost of this feature in a world nobody
 * has cast it in is one modulo and one map lookup every ten ticks.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class GraftEvents {
    private GraftEvents() {}

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level
                && Graft.due(level.getGameTime())) {
            GraftSpell.tend(level);
        }
    }
}
