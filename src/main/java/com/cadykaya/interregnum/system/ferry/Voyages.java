package com.cadykaya.interregnum.system.ferry;

import com.cadykaya.interregnum.Interregnum;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Where each ferry came from, so it can be sent back.
 *
 * <h2>Why the return is not a fifth crossing law</h2>
 *
 * The obvious shape was another entry in `laws.json` — a `home` law with the overworld as
 * its destination. It is wrong twice.
 *
 * {@link com.cadykaya.interregnum.core.ferry.Law} refuses a law with no rules, and says
 * why: *"a crossing that refuses nothing is not a law"* — a checklist that can never
 * refuse anything can never be seen to be broken. So a home law would need something to
 * refuse, and the overworld has **nobody left to refuse it**. Every other checklist is a
 * god's policy about its own world. Inventing one for a world whose god this player killed
 * would be inventing an authority the fiction has spent the whole game removing.
 *
 * And `WORLD.md` says what the checklist is *for*: *"the validation checklist teaches each
 * world's rule before arrival."* The overworld's rule is the one the player already lives
 * under. There is nothing to teach.
 *
 * So the return leg is not a crossing to a destination. It is a **mail service returning a
 * vessel to the depot it left**, which is the plainest possible reading of the thing being
 * a mail-ferry, and it needs no rule and no new authority — only a record of where the
 * ferry came from. Which a mail service would have.
 *
 * <h2>Keyed by where the keel is now</h2>
 *
 * The record is written when a hull lands, against the keel's arrival position, and read
 * when somebody asks that keel to go home. A ferry that is nudged three blocks along
 * inside one world writes a new record over the old one — correct, and the reason the key
 * is the keel rather than a voyage id: what a player points at is a keel, and the question
 * they are asking is always "where did THIS come from".
 *
 * It is deleted on use. A ferry that has gone home has no return leg on file, and asking
 * for a second one gets the answer a desk would give.
 */
public final class Voyages extends SavedData {
    private static final Identifier ID =
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "voyages");

    /** Where one ferry sailed from. */
    public record Origin(ResourceKey<Level> level, BlockPos keel) {}

    private static final Codec<Voyages> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("legs")
                    .forGetter(d -> {
                        Map<String, String> out = new LinkedHashMap<>();
                        d.legs.forEach((k, v) -> out.put(k, write(v)));
                        return out;
                    })
    ).apply(i, raw -> {
        Voyages d = new Voyages();
        raw.forEach((k, v) -> {
            Origin o = read(v);
            if (o != null) {
                d.legs.put(k, o);
            }
        });
        return d;
    }));

    public static final SavedDataType<Voyages> TYPE =
            new SavedDataType<>(ID, Voyages::new, CODEC, DataFixTypes.LEVEL);

    private final Map<String, Origin> legs = new HashMap<>();

    public static Voyages get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * Flat strings on both sides, rather than a nested record codec.
     *
     * A dimension id and three integers is exactly what this is, and the file is read by
     * people debugging a stuck ferry as often as by the game. A malformed value is
     * dropped on load rather than taking the whole file down with it: losing one return
     * leg strands one ferry, and refusing to load strands every one of them.
     */
    private static String write(Origin o) {
        return o.level().identifier() + " " + o.keel().getX() + " "
                + o.keel().getY() + " " + o.keel().getZ();
    }

    private static Origin read(String s) {
        String[] parts = s.split(" ");
        if (parts.length != 4) {
            return null;
        }
        try {
            return new Origin(
                    ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                            Identifier.parse(parts[0])),
                    new BlockPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                            Integer.parseInt(parts[3])));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String key(ResourceKey<Level> level, BlockPos keel) {
        return level.identifier() + " " + keel.getX() + " " + keel.getY() + " " + keel.getZ();
    }

    /** Record that the ferry now at `arrival` in `to` came from `keel` in `from`. */
    public void departed(ResourceKey<Level> from, BlockPos keel,
                         ResourceKey<Level> to, BlockPos arrival) {
        legs.put(key(to, arrival), new Origin(from, keel.immutable()));
        setDirty();
    }

    /** Where the ferry at this keel came from, or null if there is no leg on file. */
    public Origin originOf(ResourceKey<Level> level, BlockPos keel) {
        return legs.get(key(level, keel));
    }

    /** Forget a leg, once it has been travelled. */
    public void arrivedHome(ResourceKey<Level> level, BlockPos keel) {
        if (legs.remove(key(level, keel)) != null) {
            setDirty();
        }
    }
}
