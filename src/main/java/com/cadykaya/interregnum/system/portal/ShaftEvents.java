package com.cadykaya.interregnum.system.portal;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.core.portal.Descent;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Game-bus wiring for the Anchorite's shaft. {@link Descent} is what it is; {@link Shaft}
 * is where it happens; this is only when.
 *
 * <h2>Post, not Pre</h2>
 *
 * The opposite of {@link com.cadykaya.interregnum.system.anchorite.AnchoriteEvents}, and
 * for the mirrored reason. That handler sets a delta and must run BEFORE the entity moves
 * or the block is pushed back every tick. This one READS whether the entity is on the
 * ground, which is only true after the move has happened — asking first gets last tick's
 * answer, and last tick's answer is wrong exactly once per landing.
 *
 * <h2>The order of the tests is the cost of the feature</h2>
 *
 * This runs on every entity tick in the game. The layer check is a reference comparison
 * against two dimension keys and rejects every tick in every world but two, so nothing
 * below it is ever paid for at home. Inside those two worlds the ground test is a field
 * read, and only what is genuinely airborne reaches the zone lookup.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class ShaftEvents {
    private ShaftEvents() {}

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        var entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        Descent.Layer layer = Shaft.layerOf(level);
        if (layer == null) {
            return;
        }
        // Anything that has hold of something is anchored, and the shaft takes the
        // unanchored. `onGround` is the whole of it deliberately: a passenger, a boat, a
        // ladder and a scaffold all report it, so "holding on" does not need a list of
        // the ways a person can hold on -- which is the list that would go stale.
        boolean holding = entity.onGround() || entity.isPassenger();
        boolean inShaft = Shaft.open(level, entity.blockPosition());

        // UP, on the far side. Below the surface down does not hold, so the shaft there
        // has to supply the movement the surface gets from gravity for free -- and it
        // supplies it WITHOUT turning gravity off, which is the one way this differs from
        // the god's own handler. See Shaft.buoy for why a flag is the wrong tool when the
        // thing being lifted is not discarded at the build height.
        //
        // `holding` is not consulted, and Descent.lifts says why at length: below, the
        // shaft picks things up OFF the floor, because there is no cliff down there to
        // step off and nothing to let go of.
        if (Descent.lifts(layer, inShaft)) {
            Shaft.buoy(entity);
        }

        if (Descending.tick(entity.getUUID(), inShaft, holding)) {
            Shaft.take(level, entity);
        }
    }

    /**
     * The counts do not outlive the server that held them.
     *
     * See {@link Descending#clear}, and {@link com.cadykaya.interregnum.system.magic.ZoneEvents}
     * for the same handler on the zones these count against. Both are needed: a count
     * without its zone would never complete, and a zone without its counts would restart
     * everybody's fall.
     */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Descending.clear();
    }
}
