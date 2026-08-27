package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.Interregnum;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Which positions are joined to which.
 *
 * <h2>The first ledger of pairs in the mod, and the reason Graft was last</h2>
 *
 * Every other thing persisted here is a **set**: positions somebody planted, positions that
 * are hedge, letters that have been copied, people who have attuned. This is a list of
 * **relationships**, and nothing else in the mod needed one. Building it earlier would have
 * been building it on speculation.
 *
 * <h2>The scion's block is stored, and it has to be</h2>
 *
 * A graft keeps a plant somewhere it could not survive, which means the world will remove
 * it and the graft will put it back — and to put a thing back you have to know what it was.
 * Reading it off the world at restore time is exactly too late: by then it is air.
 *
 * The state is stored rather than the block id, so wheat comes back at the age it was
 * grafted rather than freshly planted. A graft that quietly reset a crop every time
 * something brushed it would be a spell that undoes its own point.
 *
 * <h2>Persisted, because a graft is a thing you did to a garden</h2>
 *
 * Not a zone and not a breath. It stands until somebody cuts it, and a restart is not
 * somebody cutting it.
 */
public final class Grafts extends SavedData {
    private static final Identifier ID =
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "grafts");

    /** One join: what is feeding, what is fed, and what the fed thing is. */
    public record Join(BlockPos stock, BlockPos scion, BlockState was) {
        static final Codec<Join> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("stock").forGetter(Join::stock),
                BlockPos.CODEC.fieldOf("scion").forGetter(Join::scion),
                BlockState.CODEC.fieldOf("was").forGetter(Join::was)
        ).apply(i, Join::new));
    }

    private static final Codec<Grafts> CODEC = RecordCodecBuilder.create(i -> i.group(
            Join.CODEC.listOf().fieldOf("joins").forGetter(d -> List.copyOf(d.joins))
    ).apply(i, raw -> {
        Grafts d = new Grafts();
        d.joins.addAll(raw);
        return d;
    }));

    public static final SavedDataType<Grafts> TYPE =
            new SavedDataType<>(ID, Grafts::new, CODEC, DataFixTypes.LEVEL);

    private final List<Join> joins = new ArrayList<>();

    public static Grafts get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void join(Join join) {
        joins.add(join);
        setDirty();
    }

    /** A snapshot safe to walk while cutting. */
    public List<Join> all() {
        return List.copyOf(joins);
    }

    /** @return whether anything was cut. */
    public boolean cut(Join join) {
        if (!joins.remove(join)) {
            return false;
        }
        setDirty();
        return true;
    }

    /**
     * Is either end of any join at this position?
     *
     * Asked before making a new one, so a position cannot be two things at once — a scion
     * that was also somebody else's stock would be a chain, and a chain is a thing the
     * locked text does not describe and nobody could reason about while cutting it.
     */
    public boolean touches(BlockPos pos) {
        return joins.stream().anyMatch(j -> j.stock().equals(pos) || j.scion().equals(pos));
    }

    public int count() {
        return joins.size();
    }
}
