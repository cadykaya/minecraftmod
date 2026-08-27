package com.cadykaya.interregnum.system;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.core.regard.Institution;
import com.cadykaya.interregnum.core.regard.RegardState;
import com.cadykaya.interregnum.core.regard.Standing;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * What every institution thinks of every player, persisted with the world.
 *
 * Stored server-wide rather than per-player-file for the same reason the chapter is:
 * regard is a fact about relationships, several of which the player is not the only
 * party to, and a village's opinion should not vanish because somebody's playerdata
 * was reset.
 *
 * The logic -- clamping, ceilings, the ghost's privacy, what a deicide does -- is all
 * in {@link RegardState} in `core/`, tested with no game running. This class is only
 * the storage seam.
 */
public final class RegardSavedData extends SavedData {
    private static final Identifier ID =
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "regard");

    /**
     * One player's record, flattened.
     *
     * Values AND ceilings are both stored. Storing only the values would lose the
     * permanent caps a deicide leaves behind, and those caps are the entire reason
     * the system is a scar rather than a debt -- they would silently come back as
     * MAX on the next restart and the atrocity would launder itself overnight.
     */
    private record Record(boolean killer, Map<String, Integer> values, Map<String, Integer> ceilings) {
        static final Codec<Record> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.fieldOf("killer").forGetter(Record::killer),
                Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("values").forGetter(Record::values),
                Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("ceilings").forGetter(Record::ceilings)
        ).apply(i, Record::new));

        static Record of(RegardState s) {
            Map<String, Integer> v = new LinkedHashMap<>();
            Map<String, Integer> c = new LinkedHashMap<>();
            for (Institution inst : Institution.values()) {
                if (s.value(inst) != 0) {
                    v.put(inst.name(), s.value(inst));
                }
                if (s.ceiling(inst) != RegardState.MAX) {
                    c.put(inst.name(), s.ceiling(inst));
                }
            }
            return new Record(s.isKiller(), v, c);
        }

        RegardState toState() {
            RegardState s = new RegardState(killer);
            // Ceilings first: `adjust` clamps to the ceiling, so restoring a value
            // before its cap would silently truncate every capped institution back
            // to whatever the cap allows -- losing the difference forever, one
            // restart at a time.
            ceilings.forEach((k, val) -> s.lowerCeiling(Institution.valueOf(k), val));
            // ...and then restore values as a DELTA FROM WHERE THE CEILING LEFT THEM,
            // not as if from zero. `adjust` is relative, and `lowerCeiling` has
            // already pulled every capped institution down to its cap -- so feeding
            // it the stored value added the cap to it a second time. A god saved at
            // -45 under a -10 cap came back at -55, then -65, drifting further every
            // single restart until it hit the floor. Nothing threw, nothing logged,
            // and the record looked plausible at every step. See docs/LESSONS.md #20.
            values.forEach((k, val) -> {
                Institution inst = Institution.valueOf(k);
                s.adjust(inst, val - s.value(inst));
            });
            return s;
        }
    }

    private static final Codec<RegardSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Record.CODEC).fieldOf("players")
                    .forGetter(d -> {
                        Map<String, Record> out = new LinkedHashMap<>();
                        d.states.forEach((k, v) -> out.put(k.toString(), Record.of(v)));
                        return out;
                    })
    ).apply(i, raw -> {
        RegardSavedData d = new RegardSavedData();
        raw.forEach((k, v) -> d.states.put(UUID.fromString(k), v.toState()));
        return d;
    }));

    public static final SavedDataType<RegardSavedData> TYPE = new SavedDataType<>(
            ID, RegardSavedData::new, CODEC, DataFixTypes.LEVEL);

    private final Map<UUID, RegardState> states = new HashMap<>();

    public static RegardSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * This player's record, created on first ask.
     *
     * Whether they are the killer is read from the chapter data rather than passed
     * in, so there is exactly one answer to that question in the whole mod and no
     * caller can create a second Theoclast by getting an argument wrong.
     */
    public RegardState of(MinecraftServer server, UUID player) {
        return states.computeIfAbsent(player, id -> {
            setDirty();
            return new RegardState(id.equals(ChapterSavedData.get(server).killer()));
        });
    }

    /** Present, or null: for reading without bringing a record into existence. */
    public RegardState peek(UUID player) {
        return states.get(player);
    }

    /** Call after anything mutates a state obtained from {@link #of}. */
    public void touch() {
        setDirty();
    }

    /**
     * A readable line for `/interregnum regard`.
     *
     * **Band AND number, and that is not the karma bar coming back.** The no-numbers
     * rule protects the PLAYER-facing surface: a score people can see is a score
     * people optimise, and regard exists to be a relationship instead. This command
     * is gamemaster-only and its audience is somebody asking "did that scene actually
     * do anything" -- a question bands cannot answer, because the whole point of a
     * band is that most changes do not cross one. The first version printed bands
     * only, and a conversation that moved five institutions read identically to one
     * that moved none.
     *
     * THE_GHOST prints `none` for a non-killer rather than a band, because they do
     * not have that relationship at all; showing them WARY implies a nought where
     * there is an absence.
     */
    public static String describe(RegardState s) {
        StringBuilder sb = new StringBuilder("killer=").append(s.isKiller());
        for (Institution i : Institution.values()) {
            sb.append(' ').append(i.name()).append('=');
            if (i == Institution.THE_GHOST && !s.isKiller()) {
                sb.append("none");
                continue;
            }
            Standing standing = s.standing(i);
            sb.append(standing).append('(').append(s.value(i)).append(')');
            if (s.ceiling(i) != RegardState.MAX) {
                sb.append("cap").append(s.ceiling(i));
            }
        }
        return sb.toString();
    }
}
