package com.cadykaya.interregnum.system.portal;

import com.cadykaya.interregnum.Interregnum;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Game-bus wiring for the Verdant's grown door. {@link com.cadykaya.interregnum.core.portal.Rooting}
 * is what it is; {@link Grove} is where it happens; this is only when.
 *
 * <h2>Planting is a second listener on an event the claim ledger already watches</h2>
 *
 * A separate handler rather than a clause inside {@code ClaimEvents}, because the two are
 * unrelated questions that happen to share a trigger: one is *whose work is this*, asked
 * everywhere about everything, and the other is *is this a door*, asked in one world about
 * saplings. Folding this into the ledger would make the apocalypse's guarantee and the
 * Verdant's portal share a method, and the next person to change either would be changing
 * both.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class GroveEvents {
    private GroveEvents() {}

    /**
     * Somebody planted something in the Verdant's world.
     *
     * Gated on a player for the same reason the claim ledger is: a sapling that a villager
     * dropped, a structure generated, or a dispenser placed is not somebody deciding to
     * grow a door. `WORLD.md`'s verb is *plant*, and planting is a thing a person does.
     */
    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player)
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = event.getPos();
        if (Grove.isPlanting(level, pos)) {
            Plantings.get(level).plant(pos);
        }
    }

    /**
     * Post, not Pre, and for the reason the Anchorite's handler gives in reverse: this
     * reads where something ended up this tick, and asking before the move gets last
     * tick's answer.
     *
     * The world test rejects every entity tick in every level but the Verdant's two, so
     * nothing below it is ever paid for anywhere else.
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        var entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !Grove.holds(level)) {
            return;
        }
        BlockPos at = entity.blockPosition();
        boolean under = Grove.openNear(level, at) != null;
        if (Resting.tick(entity.getUUID(), under, at)) {
            Grove.take(level, entity);
        }
    }

    /** The counts do not outlive the server. See {@link Resting#clear}. */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Resting.clear();
    }
}
