package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.core.magic.Hedge;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * A hedge, cut. See {@link Hedge} for why this is the spell and the drawing of the wall is
 * a bridge stood on its end.
 *
 * <h2>Not gated on the breaker being a player</h2>
 *
 * The claim ledger's own break handler makes the same choice and says why: *however the
 * block went, it is gone*. A hedge does not care who cut it — `WORLD.md` says *"thickens
 * where it is **struck**"*, not where a person struck it, and a wall that only answered
 * players would be a wall an enderman could quietly dismantle.
 *
 * <h2>It is not cancelled</h2>
 *
 * The block the attacker struck really is gone. `WORLD.md`'s promise is *improved by being
 * attacked*, not *unattackable* — a hedge that closed its own wound would be a wall that
 * cannot be got through, which is a much less interesting thing to have built and a much
 * worse thing to be on the wrong side of.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class HedgeEvents {
    private HedgeEvents() {}

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            HedgeSpell.struck(level, event.getPos());
        }
    }
}
