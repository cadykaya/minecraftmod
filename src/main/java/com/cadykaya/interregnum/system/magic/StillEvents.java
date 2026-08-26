package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.core.magic.Spell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Things already in motion, stopped where they are.
 *
 * <h2>Cancelling the tick is the whole implementation, and here it is right</h2>
 *
 * `Hush` cancels an ignited creeper's tick too, and there it is a compromise — silence
 * should not freeze a walking mob, and the class says so. Here it is exactly the spell:
 * `WORLD.md` says *"freeze primed TNT / falling block **mid-state**"*, and a cancelled
 * tick is precisely a thing holding the state it was in. Nothing is deleted, nothing is
 * defused, the fuse does not advance. When the zone lapses it all resumes.
 *
 * <b>Only these two kinds.</b> Not mobs, not players, not projectiles — `WORLD.md` names
 * primed TNT and falling blocks and this does not extend the list. A Still that stopped
 * everything would be a stasis field, which is a much bigger spell than the one that was
 * locked, and it would make the Anchorite's *Moor* and half the Weight kit redundant
 * before either is built.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class StillEvents {
    private StillEvents() {}

    /** The two kinds of thing this spell has an opinion about. */
    public static boolean inMidState(Entity entity) {
        return entity instanceof FallingBlockEntity || entity instanceof PrimedTnt;
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        // Most selective test first: an instanceof against two classes almost nothing in
        // a running world is, so the zone lookup behind it is paid for only by the
        // handful of entities that could possibly care. Same ordering, same reasoning as
        // AnchoriteEvents.
        if (!inMidState(event.getEntity())
                || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        if (Zones.covering(level, Spell.STILL, event.getEntity().blockPosition())) {
            event.setCanceled(true);
        }
    }
}
