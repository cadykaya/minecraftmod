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

import java.util.Optional;
import java.util.UUID;

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
    private static final Codec<ChapterSavedData> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(
            i -> i.group(
                    Codec.STRING.fieldOf("state").forGetter(d -> d.state.serialize()),
                    Codec.STRING.optionalFieldOf("killer").forGetter(
                            d -> Optional.ofNullable(d.killer).map(UUID::toString))
            ).apply(i, (s, k) -> {
                ChapterSavedData d = new ChapterSavedData(ChapterState.deserialize(s));
                d.killer = k.map(UUID::fromString).orElse(null);
                return d;
            }));

    // VERIFY: DataFixTypes has no general-purpose member; LEVEL is the conventional
    // choice for mod saved data. Revisit if a datafixer ever complains.
    public static final SavedDataType<ChapterSavedData> TYPE = new SavedDataType<>(
            ID, () -> new ChapterSavedData(new ChapterState()), CODEC, DataFixTypes.LEVEL);

    private final ChapterState state;
    private UUID killer;

    private ChapterSavedData(ChapterState state) {
        this.state = state;
    }

    /**
     * Is the world still dormant? Convenience for callers that hold a level rather
     * than a server, and which must behave sanely on the logical client (where
     * there is no server and no saved data): a client assumes dormant, because the
     * server is the only thing entitled to say otherwise.
     */
    public static boolean isDormant(net.minecraft.world.level.LevelReader level) {
        if (!(level instanceof net.minecraft.world.level.Level lvl)) {
            return true;
        }
        MinecraftServer server = lvl.getServer();
        return server == null || get(server).mechanicsDormant();
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

    /** The First Theoclast: whoever took the heart. Null if a command did it. */
    public UUID killer() {
        return killer;
    }

    void setKiller(UUID uuid) {
        this.killer = uuid;
        setDirty();
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
