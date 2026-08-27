package com.cadykaya.interregnum.system.portal;

import com.cadykaya.interregnum.Interregnum;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Game-bus wiring for the Quiet One's door.
 *
 * <h2>Listening the way sculk listens</h2>
 *
 * {@link VanillaGameEvent} fires on the server for every vanilla game event — the vibration
 * a sculk sensor would hear. That is the whole reason this portal can exist: a door keyed
 * on <em>audible</em> sound would be keyed on something a headless server never has, and
 * `Hush` already refuses to claim the audible half of its own spell for exactly that
 * reason.
 *
 * <b>The event is never cancelled.</b> Listening to a noise must not stop the noise, or the
 * door would make the world quieter by being watched — sculk sensors near a portal would go
 * deaf, and a player would find that standing by a door is a stealth ability. It is
 * observed and nothing more.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class SilenceEvents {
    private SilenceEvents() {}

    @SubscribeEvent
    public static void onGameEvent(VanillaGameEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !Silence.holds(level)) {
            return;
        }
        Silence.broke(level, BlockPos.containing(event.getEventPosition()));
    }

    /**
     * Post, like the other three portals.
     *
     * The order against the noise listener does not matter and is worth saying so: a step
     * posts its event during the move, so by the time this runs the silence is already
     * broken for this tick. Something walking through a door it opened by standing still
     * closes it on the step that would have carried it in, which is the mechanic rather
     * than a race.
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        var entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !Silence.holds(level)) {
            return;
        }
        if (Silence.open(level, entity.blockPosition())) {
            Silence.take(level, entity);
        }
    }

    /** The record of broken silences does not outlive the server. */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Hushed.clear();
    }
}
