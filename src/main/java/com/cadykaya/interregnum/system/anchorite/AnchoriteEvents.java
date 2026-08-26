package com.cadykaya.interregnum.system.anchorite;

import com.cadykaya.interregnum.core.exodus.Exodus;
import com.cadykaya.interregnum.system.exodus.Leaks;
import com.cadykaya.interregnum.system.magic.Zones;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import com.cadykaya.interregnum.Interregnum;

/**
 * Game-bus wiring for the Anchorite's law. See {@link Anchorite} for what it is, and
 * {@link Leaks} for why band 3 routes an overworld patch through this same handler
 * rather than through a second one that would only look the same.
 *
 * `EntityTickEvent.Pre` rather than `Post`: the delta has to be set BEFORE the entity
 * moves, or the block spends every tick falling one step and being pushed back, which
 * is a different and much worse-looking bug than either behaviour on its own.
 *
 * Server-side only. A client that disagreed with the server about which way things fall
 * would show a player sand rising and then snapping back down every time the position
 * was corrected -- the same reason nothing about the unraveling is decided on a client.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class AnchoriteEvents {
    private AnchoriteEvents() {}

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        var entity = event.getEntity();
        // MOST SELECTIVE TEST FIRST, and the order changed when the leak arrived.
        //
        // It used to ask about the dimension first, on the reasoning that a reference
        // comparison is the cheapest instruction here. True, and beside the point: what
        // matters is how many entity ticks each test throws away. `unanchored` is an
        // instanceof against a class that essentially nothing in a running world is, so
        // it rejects almost every tick in the game -- and everything after it, including
        // the leak lookup's saved-data read and chunk scan, is then paid for only by the
        // occasional falling block.
        if (!(entity.level() instanceof ServerLevel level) || !Anchorite.unanchored(entity)) {
            return;
        }
        // The god's own world, or a patch of the overworld obeying it. `Anchorite.lift`
        // either way: the whole claim of band 3 is that the patch runs the same law, and
        // it can only be the same law if it is the same method.
        // Three ways to be under the Anchorite's law, and ONE method that applies it.
        //
        //   * the god's own world, where it is simply how things are;
        //   * a band-3 patch of overworld that has forgotten whose it is;
        //   * a Lighten zone -- somebody who learned how to ask.
        //
        // That progression is the school system's argument: you meet the law as a place,
        // meet it again as a wrongness leaking into your own world, and the third time
        // you are the one doing it. It is only the same law because it is the same
        // `Anchorite.lift`, which is why all three are clauses here rather than three
        // handlers that would drift.
        if (Anchorite.holds(entity)
                || Leaks.leaks(level, entity.blockPosition(), Exodus.Law.ANCHORITE)
                || Zones.covering(level, com.cadykaya.interregnum.core.magic.School.WEIGHT,
                        entity.blockPosition())) {
            Anchorite.lift(entity);
        }
    }
}
