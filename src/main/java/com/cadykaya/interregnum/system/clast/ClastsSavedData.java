package com.cadykaya.interregnum.system.clast;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.core.clast.Clasts;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * How many pieces of the god this world has already given up.
 *
 * One number, and it is the whole of `WORLD.md`'s locked *"clasts are finite — the class
 * is a server negotiation"*. Everything else the mod produces falls out of a rule applied
 * to whatever is there; this is a fixed allowance, spent once, per world.
 *
 * Kept per world rather than per site for the reason {@link Clasts} gives at length: a
 * world with forty shrines would otherwise hand out forty classes, and the negotiation
 * would never happen. The arithmetic is in `core/` and tested with no game running; this
 * is the storage seam and is deliberately thin.
 */
public final class ClastsSavedData extends SavedData {
    private static final Identifier ID =
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "clasts");

    private static final Codec<ClastsSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("issued").forGetter(d -> d.issued)
    ).apply(i, raw -> {
        ClastsSavedData d = new ClastsSavedData();
        d.issued = raw;
        return d;
    }));

    public static final SavedDataType<ClastsSavedData> TYPE =
            new SavedDataType<>(ID, ClastsSavedData::new, CODEC, DataFixTypes.LEVEL);

    private int issued;

    public static ClastsSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * Take up to `want` from the pool.
     *
     * @return how many were actually available, which may be none. A site that asks after
     *         the pool is empty gets nothing and that is not an error -- shrines keep
     *         loading long after the last clast has gone, and a world that has been picked
     *         over is supposed to look like one.
     */
    public int take(int want) {
        int got = Clasts.issue(issued, want);
        if (got > 0) {
            issued += got;
            setDirty();
        }
        return got;
    }

    public int issued() {
        return issued;
    }

    public int remaining() {
        return Clasts.remaining(issued);
    }
}
