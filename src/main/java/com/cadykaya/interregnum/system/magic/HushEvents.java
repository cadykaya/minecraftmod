package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.core.magic.Hush;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Creeper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * What silence does to things that kill by announcing themselves.
 *
 * See {@link Hush}. Two clauses of the locked description are server-side facts and are
 * enforced here; the audible silence is client-side and is not claimed.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class HushEvents {
    private HushEvents() {}

    /**
     * <b>Mobs cannot alert.</b>
     *
     * Cancelled at the moment of acquisition rather than by clearing targets afterwards:
     * a mob that acquires and then loses a target has already turned, already pathed,
     * already made the noise. Refusing the acquisition is what "cannot alert" means.
     *
     * Checked on the MOB's position, not the target's. A silence you can stand outside of
     * and shoot into is a bubble, not a silence -- and the interesting use is walking
     * into a room and having nothing notice, which is the mob's side.
     */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity().level() instanceof ServerLevel level
                && Zones.covering(level, Hush.SCHOOL, event.getEntity().blockPosition())) {
            event.setCanceled(true);
        }
    }

    /**
     * <b>A creeper that cannot hiss cannot detonate.</b>
     *
     * Two cases, because a creeper reaches a detonation two different ways.
     *
     * <b>The ordinary one</b> is `SwellGoal` winding the fuse up as you get close. Setting
     * the swell direction to -1 before the creeper ticks means its own tick decrements
     * instead of incrementing, so the fuse never accumulates. It still chases. It still
     * looms. It simply never arrives -- which is the right feel, and much better than a
     * creeper that stands inert.
     *
     * <b>The deliberate one</b> is a player striking it with flint and steel, which sets
     * an `ignited` flag with a public setter and no public way to clear it. For that case
     * the tick is cancelled outright: a creeper already committed to exploding is stopped
     * where it stands. That freezes it, which is more than silence would do -- and it is
     * the honest trade, because the alternative is a hole in a locked promise. A lit
     * creeper standing perfectly still in a silent field is also, as it happens, exactly
     * what this god should look like.
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Creeper creeper)
                || !(creeper.level() instanceof ServerLevel level)) {
            return;
        }
        if (!Zones.covering(level, Hush.SCHOOL, creeper.blockPosition())) {
            return;
        }
        if (creeper.isIgnited()) {
            event.setCanceled(true);
            return;
        }
        creeper.setSwellDir(-1);
    }
}
