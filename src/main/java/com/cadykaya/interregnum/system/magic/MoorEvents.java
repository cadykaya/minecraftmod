package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.core.magic.Moor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * What a mooring does to everything that would push. See {@link Moor}.
 *
 * <h2>One rule for water and for the Anchorite's law, and a second for pistons</h2>
 *
 * A moored thing's position does not change, and holding that every tick is enough to
 * refuse anything that moves an entity — a current, a lifting law, a piston shoving what
 * stands in front of it. Two of `WORLD.md`'s three named forces need no special case at all
 * and are never mentioned in this file, which is the sign the rule is the right one.
 *
 * A piston gets a second handler because it does something the other two do not: it moves
 * **blocks**, and a block being carried out from under a moored thing would leave the
 * mooring holding air. Cancelling the push is the honest answer — *"not pistons"* is in the
 * locked text, and a piston that shoved the floor away while the spell held the thing above
 * it would satisfy the letter and lose the sentence.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class MoorEvents {
    private MoorEvents() {}

    /**
     * Pre, not Post, for the reason {@code AnchoriteEvents} gives: the position has to be
     * restored BEFORE the entity moves, or it spends every tick being pushed and dragged
     * back — which looks far worse than either behaviour on its own.
     *
     * The delta is zeroed as well as the position restored. Position alone would leave a
     * thing accumulating speed it never gets to use, and the instant the mooring lapsed it
     * would depart at whatever the current had been building for a minute.
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        var entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 anchor = Moored.anchorOf(level, entity.getUUID());
        if (anchor == null) {
            return;
        }
        entity.setDeltaMovement(Vec3.ZERO);
        if (!entity.position().equals(anchor)) {
            entity.setPos(anchor);
        }
        entity.resetFallDistance();
    }

    /**
     * A piston will not move a moored thing, nor the block it is standing on.
     *
     * The structure helper lists every block the push would take. Any of them being the
     * block under a moored thing is enough to refuse the whole push — pistons move a
     * connected structure or nothing, so there is no partial answer available and no
     * sensible one to invent.
     */
    @SubscribeEvent
    public static void onPiston(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || Moored.count(level) == 0) {
            return;
        }
        var helper = event.getStructureHelper();
        if (helper == null || !helper.resolve()) {
            return;
        }
        for (var pos : helper.getToPush()) {
            // Above the block, which is where a thing standing on it is. Cheap because it
            // only runs in a world where something is actually moored.
            var above = new net.minecraft.world.phys.AABB(pos.above()).inflate(0.5);
            for (var entity : level.getEntities((net.minecraft.world.entity.Entity) null,
                    above, e -> Moored.holds(level, e.getUUID()))) {
                if (entity != null) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }

    /** Nothing is still moored when the server comes back. */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Moored.clear();
    }
}
