package com.cadykaya.interregnum.system.claim;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

/**
 * Which blocks in this chunk a person put there.
 *
 * Minecraft does not record this, and the unraveling needs it: `WORLD.md` promises
 * the world may warp but a player's work may not. The crater gets away with a tag
 * whitelist because it fires once at one spot; the unraveling runs forever over a
 * whole world, so sooner or later a whitelist alone would eat somebody's cobblestone
 * wall on the grounds that cobblestone is natural.
 *
 * **Positions are chunk-relative and packed into one int.** A BlockPos long would
 * work and cost twice the memory for information already implied by which chunk the
 * attachment is on: x and z are 4 bits each, y is 9 (a 384-tall world fits in 512).
 *
 * **Saturation is the memory bound.** Past {@link #SATURATION_CAP} placements the
 * set is dropped and the whole chunk is marked claimed. That degrades in the safe
 * direction -- it protects MORE, never less -- and it is the right answer anyway: a
 * chunk somebody has put four thousand blocks into is theirs, and arguing about
 * which individual block is not is missing the point.
 */
public class PlacedBlocks implements ValueIOSerializable {
    /** Beyond this many placements the chunk is simply theirs. ~32KB before the swap. */
    public static final int SATURATION_CAP = 4096;

    private final IntOpenHashSet packed = new IntOpenHashSet();
    private boolean saturated;

    public static int pack(BlockPos pos, int minY) {
        int y = pos.getY() - minY;
        return ((pos.getX() & 15) << 13) | ((pos.getZ() & 15) << 9) | (y & 511);
    }

    /** @return true if this position is now recorded (or the chunk is claimed). */
    public boolean add(BlockPos pos, int minY) {
        if (saturated) {
            return true;
        }
        packed.add(pack(pos, minY));
        if (packed.size() > SATURATION_CAP) {
            saturated = true;
            packed.clear();
        }
        return true;
    }

    /**
     * Forget a position, because the block there is gone.
     *
     * Without this, mining through your own wall leaves the empty space protected
     * forever and the unraveling develops permanent invisible holes shaped like
     * everywhere anyone has ever built. A saturated chunk stays saturated: once a
     * place is somebody's, digging a hole in it does not make it the wilderness
     * again.
     */
    public void remove(BlockPos pos, int minY) {
        if (!saturated) {
            packed.remove(pack(pos, minY));
        }
    }

    public boolean contains(BlockPos pos, int minY) {
        return saturated || packed.contains(pack(pos, minY));
    }

    public boolean isSaturated() {
        return saturated;
    }

    public int size() {
        return saturated ? SATURATION_CAP : packed.size();
    }

    public boolean isEmpty() {
        return !saturated && packed.isEmpty();
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putBoolean("saturated", saturated);
        if (!saturated && !packed.isEmpty()) {
            output.putIntArray("placed", packed.toIntArray());
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        packed.clear();
        saturated = input.getBooleanOr("saturated", false);
        if (!saturated) {
            input.getIntArray("placed").ifPresent(a -> {
                for (int v : a) {
                    packed.add(v);
                }
            });
        }
    }
}
