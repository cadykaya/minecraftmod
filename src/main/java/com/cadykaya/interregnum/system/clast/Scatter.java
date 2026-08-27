package com.cadykaya.interregnum.system.clast;

import com.cadykaya.interregnum.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Pieces of the god, coming to rest.
 *
 * `WORLD.md`, locked: *"The overflow detonates outward, scattering splinters at shrines
 * and the crater."*
 *
 * <h2>They do not despawn, and that is design rather than convenience</h2>
 *
 * An ordinary dropped item lasts five minutes. A clast that vanished because the player
 * who caused the death happened to be underground when a shrine loaded would make the
 * whole class a matter of who was standing where — and there are only {@link
 * com.cadykaya.interregnum.core.clast.Clasts#TOTAL} of them in a world, ever. <b>A finite
 * thing that can be lost to a timer is not finite, it is random.</b>
 *
 * So {@code setUnlimitedLifetime}, and it reads correctly too: a piece of a god does not
 * rot. It is lying where it landed, and it will still be lying there.
 *
 * <h2>Thrown, not placed</h2>
 *
 * Each gets a small random push, because the locked word is *detonates* and a neat stack
 * of items in a bowl is not that. The push is small enough that they stay within the
 * crater or the shrine's own ground: scattered a hundred blocks apart, finding the second
 * one would be a search rather than a discovery.
 */
public final class Scatter {
    private Scatter() {}

    /** How hard the overflow throws one. Small: a scattering, not a launch. */
    private static final double PUSH = 0.18;

    /**
     * Drop `count` clasts around `centre`.
     *
     * @return how many entities were actually spawned, so a caller that has already taken
     *         from the pool can report what became of them rather than assuming.
     */
    public static int drop(ServerLevel level, BlockPos centre, int count) {
        int made = 0;
        for (int i = 0; i < count; i++) {
            ItemEntity entity = new ItemEntity(level,
                    centre.getX() + 0.5, centre.getY() + 1.0, centre.getZ() + 0.5,
                    new ItemStack(ModItems.CLAST.get()));
            entity.setDeltaMovement(
                    (level.getRandom().nextDouble() - 0.5) * 2 * PUSH,
                    PUSH,
                    (level.getRandom().nextDouble() - 0.5) * 2 * PUSH);
            // The whole reason this class exists rather than a `spawnAtLocation` call.
            entity.setUnlimitedLifetime();
            if (level.addFreshEntity(entity)) {
                made++;
            }
        }
        return made;
    }
}
