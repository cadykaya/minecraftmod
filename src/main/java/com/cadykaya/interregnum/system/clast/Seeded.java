package com.cadykaya.interregnum.system.clast;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

/**
 * Whether this chunk has already given up its piece of the god.
 *
 * One boolean, saved with the chunk, and it exists because the pool is finite. The
 * statues next door need no equivalent: a woken statue is a different block, so waking is
 * self-marking and re-running it costs nothing. Scattering is not -- a shrine chunk that
 * loaded, unloaded and loaded again would take a second clast, and a player who walked
 * back and forth could drain a world's whole allowance at one shrine.
 *
 * Recorded whether or not anything was actually handed over, and that is deliberate: a
 * shrine that loaded after the pool ran dry is DONE with, not waiting. Marking only the
 * successful ones would leave every empty shrine asking again forever, which costs a
 * block scan per load for an answer that cannot change.
 */
public class Seeded implements ValueIOSerializable {
    private boolean seeded;

    /** @return whether this call is what marked it, so callers can skip a dirty mark. */
    public boolean mark() {
        if (seeded) {
            return false;
        }
        seeded = true;
        return true;
    }

    public boolean isSeeded() {
        return seeded;
    }

    @Override
    public void serialize(ValueOutput output) {
        if (seeded) {
            output.putBoolean("seeded", true);
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        seeded = input.getBooleanOr("seeded", false);
    }
}
