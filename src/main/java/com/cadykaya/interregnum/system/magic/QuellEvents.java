package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.core.magic.Quell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * What a quelling does: the thing never leaves the hand.
 *
 * See {@link Quell}. One rule, applied uniformly -- a projectile whose owner is quelled is
 * refused entry to the world.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class QuellEvents {
    private QuellEvents() {}

    /**
     * <b>A blaze that cannot ignite.</b>
     *
     * Cancelled at the moment the projectile would join the level, which is the earliest
     * point at which the thing exists and the only one where it can be stopped without
     * being seen. The alternatives are both worse: discarding it on its first tick means
     * a fireball that flashes into existence and vanishes, and blocking the mob's attack
     * goal means picking apart a different AI class for every mob that throws anything.
     * One event, every projectile in the game, no per-mob knowledge at all.
     *
     * <b>Reloads are skipped.</b> A chunk coming back from disk re-joins every projectile
     * inside it, and cancelling those would delete arrows in flight belonging to a mob
     * quelled after they were already loosed. The spell takes the throwing arm; it does
     * not reach out and unmake what is already in the air.
     */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk()
                || !(event.getEntity() instanceof Projectile projectile)
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity owner = projectile.getOwner();
        if (owner != null && Quelled.holds(level, owner.getUUID())) {
            event.setCanceled(true);
        }
    }

    /**
     * Quellings do not outlive the server that held them.
     *
     * {@link Quelled} keeps its map statically -- correct for something deliberately not
     * persisted, and a leak if nothing ever empties it. Two worlds in one JVM is the
     * development case; a map that only grows is the one that matters even without it.
     */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Quelled.clear();
    }
}
