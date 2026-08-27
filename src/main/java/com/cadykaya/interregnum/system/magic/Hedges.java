package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.Interregnum;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Which leaves are a hedge.
 *
 * <h2>Why a ledger at all</h2>
 *
 * A hedge is made of an ordinary vanilla block, because inventing a registered one for it
 * would cost a texture, a model, a palette entry and a reachability row for something whose
 * whole job is to be leaves. So the mod needs some way to tell **the wall somebody grew**
 * from the forest it was grown next to — and it has to, or hitting an oak tree would make
 * it thicker, which is a different spell and a much worse one.
 *
 * <h2>Persisted, like the plantings and unlike the zones</h2>
 *
 * A hedge is a thing somebody built that stands until it is cut down. Losing the ledger on
 * a restart would leave a wall that looks like a hedge, is made of hedge, and has stopped
 * being one — the failure nobody would report as a bug and everybody would feel.
 *
 * <h2>Per level, because a position is only a position</h2>
 *
 * Same reason {@link com.cadykaya.interregnum.system.portal.Plantings} hangs off its own
 * world rather than the overworld: (0, 70, 0) means something different in each of the
 * nine.
 */
public final class Hedges extends SavedData {
    private static final Identifier ID =
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "hedges");

    private static final Codec<Hedges> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.listOf().fieldOf("grown").forGetter(d -> List.copyOf(d.grown))
    ).apply(i, raw -> {
        Hedges d = new Hedges();
        d.grown.addAll(raw);
        return d;
    }));

    public static final SavedDataType<Hedges> TYPE =
            new SavedDataType<>(ID, Hedges::new, CODEC, DataFixTypes.LEVEL);

    private final Set<BlockPos> grown = new LinkedHashSet<>();

    public static Hedges get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** @return whether this position was new to the hedge. */
    public boolean grow(BlockPos pos) {
        if (!grown.add(pos.immutable())) {
            return false;
        }
        setDirty();
        return true;
    }

    /** @return whether this position was part of a hedge. */
    public boolean cut(BlockPos pos) {
        if (!grown.remove(pos)) {
            return false;
        }
        setDirty();
        return true;
    }

    public boolean is(BlockPos pos) {
        return grown.contains(pos);
    }

    /**
     * How many blocks of hedge stand in this world.
     *
     * ONE number for the whole level rather than one per wall, and the difference is worth
     * saying: there is no such thing as "a hedge" in this ledger, only hedge blocks. Giving
     * each wall an identity would mean deciding when two walls that touch become one, which
     * is a question with no good answer and no player-visible consequence. The cap in
     * {@link com.cadykaya.interregnum.core.magic.Hedge#MAX_BLOCKS} is a budget for the
     * world, and a world with a hundred and twenty-eight blocks of hedge in it has enough.
     */
    public int count() {
        return grown.size();
    }
}
