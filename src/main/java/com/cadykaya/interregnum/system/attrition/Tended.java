package com.cadykaya.interregnum.system.attrition;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

/**
 * When anybody was last near this chunk.
 *
 * One long, saved with the chunk. See {@link com.cadykaya.interregnum.core.attrition.Attrition}
 * for why band 4 needs it and why the tending radius is deliberately smaller than the
 * distance at which chunks stay loaded.
 *
 * <h2>Unstamped is not stale</h2>
 *
 * {@link #UNSTAMPED} means nobody has ever recorded a visit -- a chunk generated after
 * band 4 arrived, or one that existed before this attachment did. The tempting reading is
 * that such ground has gone untended forever and should already be generalised, and it is
 * wrong for a reason worth writing down: a player exploring at band 4 would find fresh
 * land that was *already* plain, which reads as broken worldgen rather than as a world
 * forgetting. Attrition has to be something you can watch happen to a place you knew.
 *
 * So first sight counts as tending, and {@link #tend} is called on load as well as on
 * approach. The state is the same either way; only the caller differs.
 */
public class Tended implements ValueIOSerializable {
    /** No visit has ever been recorded here. */
    public static final long UNSTAMPED = Long.MIN_VALUE;

    private long lastTended = UNSTAMPED;

    /** @return whether this actually changed anything, so callers can skip a dirty mark. */
    public boolean tend(long gameTime) {
        if (lastTended == gameTime) {
            return false;
        }
        lastTended = gameTime;
        return true;
    }

    public boolean isUnstamped() {
        return lastTended == UNSTAMPED;
    }

    public long lastTended() {
        return lastTended;
    }

    @Override
    public void serialize(ValueOutput output) {
        if (lastTended != UNSTAMPED) {
            output.putLong("tended", lastTended);
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        lastTended = input.getLongOr("tended", UNSTAMPED);
    }
}
