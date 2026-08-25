package com.cadykaya.interregnum.system;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.core.chapter.ChapterState;
import com.cadykaya.interregnum.core.chapter.Chapter;
import com.cadykaya.interregnum.core.chapter.Milestone;

/**
 * The interregnum's progress, persisted with the world.
 *
 * The logic lives in {@link ChapterState} in `core/` -- tested, mutation-checked,
 * and knowing nothing about Minecraft. This class is only the storage seam, and it
 * is deliberately thin: it holds one string.
 *
 * Stored on the OVERWORLD's storage, not per-dimension. The interregnum is a fact
 * about the world, not about a place in it, and every god's world will need to read
 * the same number.
 */
public final class ChapterSavedData extends SavedData {
    private static final Identifier ID =
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "chapter");

    /**
     * Serialised as the single string `ChapterState` already round-trips, rather
     * than as a bespoke NBT shape. That round-trip is covered by the core
     * self-test (including the case where a save claims a lower chapter than its
     * milestones justify), so reusing it means the persistence format is tested by
     * tests that need no game to run.
     */
    private static final Codec<ChapterSavedData> CODEC = Codec.STRING.xmap(
            s -> new ChapterSavedData(ChapterState.deserialize(s)),
            d -> d.state.serialize());

    // VERIFY: DataFixTypes has no general-purpose member; LEVEL is the conventional
    // choice for mod saved data. Revisit if a datafixer ever complains.
    public static final SavedDataType<ChapterSavedData> TYPE = new SavedDataType<>(
            ID, () -> new ChapterSavedData(new ChapterState()), CODEC, DataFixTypes.LEVEL);

    private final ChapterState state;

    private ChapterSavedData(ChapterState state) {
        this.state = state;
    }

    public static ChapterSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Chapter chapter() {
        return state.chapter();
    }

    public int band() {
        return state.band();
    }

    public boolean mechanicsDormant() {
        return state.mechanicsDormant();
    }

    public int lettersDelivered() {
        return state.lettersDelivered();
    }

    public boolean has(Milestone m) {
        return state.has(m);
    }

    /**
     * Record a milestone. Returns true if it was new.
     *
     * Always marks dirty on a new milestone: forgetting setDirty() is the classic
     * way saved data silently fails to persist, and it fails in the worst possible
     * place -- only after a restart, only sometimes.
     */
    public boolean record(Milestone m) {
        boolean isNew = state.record(m);
        if (isNew) {
            setDirty();
        }
        return isNew;
    }
}
