package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.core.magic.HeldBreath;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * What holding your breath does to the world's attention. See {@link HeldBreath}.
 *
 * <h2>This is the one place a game event is cancelled on purpose</h2>
 *
 * {@link com.cadykaya.interregnum.system.portal.SilenceEvents} listens to the same event
 * and is explicit that it must never cancel: observing a noise must not stop the noise, or
 * a portal would deafen the sculk sensors near it and standing beside a door would be a
 * stealth ability.
 *
 * Here the cancellation IS the spell. `WORLD.md`: *"your own sound, taken away. Nothing
 * tracks you while you hold it."* The vibration a footstep would have posted is not
 * suppressed for a listener — it is never made, by the person who is not making a sound.
 * Same API, opposite intent, and the difference is which end of the noise it happens at.
 *
 * It follows that the Quiet One's door does not hear you either, which is the interlock
 * `WORLD.md` names when it calls this spell *"for the last few steps"*.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class HeldBreathEvents {
    private HeldBreathEvents() {}

    /**
     * A noise nobody makes.
     *
     * Gated on the CAUSE, not the position: a person holding their breath is silent
     * wherever they go, and everything else in the room is as loud as it was. That is the
     * difference between this and {@code Hush}, which silences a place.
     */
    @SubscribeEvent
    public static void onGameEvent(VanillaGameEvent event) {
        var cause = event.getCause();
        if (cause == null || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (Holding.holds(level, cause.getUUID())) {
            event.setCanceled(true);
        }
    }

    /**
     * Nothing acquires you.
     *
     * The same refusal {@code HushEvents} makes inside a zone, on the same event, for one
     * person instead of a room -- and checked on the TARGET here rather than on the mob,
     * which is the mirror image and the right one: a silence is somewhere you are, and this
     * is something you are.
     */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        var target = event.getNewAboutToBeSetTarget();
        if (target == null || !(target.level() instanceof ServerLevel level)) {
            return;
        }
        if (Holding.holds(level, target.getUUID())) {
            event.setCanceled(true);
        }
    }

    /** Nobody is still holding their breath when the server comes back. */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Holding.clear();
    }
}
