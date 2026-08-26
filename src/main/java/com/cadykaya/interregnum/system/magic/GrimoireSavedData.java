package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.core.magic.Grimoire;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Who has been taught what, persisted with the world.
 *
 * Follows {@link com.cadykaya.interregnum.system.RegardSavedData} exactly -- a map from
 * player id to a `core` value object, stored on the OVERWORLD, because what somebody
 * knows is a fact about them and not about which world they happen to be standing in. A
 * player who learns the Turning in the Hearth-Turner's world still knows it at home;
 * that is the entire premise of the overworld ban being a *choice* rather than a wall.
 *
 * The logic is all in {@link Grimoire}, tested with no game running. This is the storage
 * seam and is deliberately thin.
 */
public final class GrimoireSavedData extends SavedData {
    private static final Identifier ID =
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "grimoires");

    private static final Codec<GrimoireSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()).fieldOf("players")
                    .forGetter(d -> {
                        Map<String, List<String>> out = new LinkedHashMap<>();
                        d.known.forEach((k, v) -> out.put(k.toString(), v.serialize()));
                        return out;
                    })
    ).apply(i, raw -> {
        GrimoireSavedData d = new GrimoireSavedData();
        raw.forEach((k, v) -> d.known.put(UUID.fromString(k), Grimoire.deserialize(v)));
        return d;
    }));

    public static final SavedDataType<GrimoireSavedData> TYPE = new SavedDataType<>(
            ID, GrimoireSavedData::new, CODEC, DataFixTypes.LEVEL);

    private final Map<UUID, Grimoire> known = new HashMap<>();

    public static GrimoireSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * What this player knows, created empty on first ask.
     *
     * Creating on read is safe here in a way it is not for regard: an empty grimoire is
     * indistinguishable from no grimoire -- {@link Grimoire#knows} is false either way --
     * so asking about somebody cannot accidentally give them anything.
     */
    public Grimoire of(UUID player) {
        return known.computeIfAbsent(player, k -> new Grimoire());
    }

    /** Read without creating, for callers that only want to look. */
    public Grimoire peek(UUID player) {
        return known.get(player);
    }

    public void touch() {
        setDirty();
    }
}
