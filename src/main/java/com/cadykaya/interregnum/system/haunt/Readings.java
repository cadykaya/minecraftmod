package com.cadykaya.interregnum.system.haunt;

import com.cadykaya.interregnum.Interregnum;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What each person has read of the dead god's own hand.
 *
 * <h2>Persisted, and never cleared</h2>
 *
 * `WORLD.md` locks reading as *knowledge-as-hazard*. Knowledge does not expire, there is no
 * cure, and nothing in the mod removes an entry from here — which is the whole difference
 * between this and an affliction. A debuff you wait out is a debuff; a thing you now know
 * is a thing you now know.
 *
 * <h2>Distinct marks, not a tally</h2>
 *
 * A set, not a count. Re-reading a stone you have already read is not more knowledge, so it
 * changes nothing — see {@link com.cadykaya.interregnum.core.haunt.Script}. Storing the
 * marks rather than the number is what makes that true without a second mechanism, and it
 * bounds the total by how much script a world actually contains.
 *
 * <h2>What a mark looks like</h2>
 *
 * An opaque token, built by {@link RawScript}. A carved stone is its position and a letter
 * is its id, and this class deliberately knows neither — a ledger that understood the
 * difference would be a ledger that had to be taught about the next kind of script.
 *
 * <h2>It records everybody, and only one person's total is ever consulted</h2>
 *
 * The Haunt reaches the killer and nobody else, so a reader who did not kill the god is
 * marked and nothing looks at them. That is not an oversight, and extending the Haunt to
 * other people would be a different feature: the god has a line to the person who killed
 * it. Everybody else can read all they like, and nothing is listening.
 *
 * Recorded anyway, because who has been reading is a fact about the world, and the killer
 * is not always the same person as the one who was reading when the world started.
 */
public final class Readings extends SavedData {
    private static final Identifier ID =
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "readings");

    private record Reader(String who, List<String> marks) {
        static final Codec<Reader> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("who").forGetter(Reader::who),
                Codec.STRING.listOf().fieldOf("marks").forGetter(Reader::marks)
        ).apply(i, Reader::new));
    }

    private static final Codec<Readings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Reader.CODEC.listOf().fieldOf("readers").forGetter(d -> d.read.entrySet().stream()
                    .map(e -> new Reader(e.getKey(), List.copyOf(e.getValue()))).toList())
    ).apply(i, raw -> {
        Readings d = new Readings();
        for (Reader r : raw) {
            d.read.put(r.who(), new LinkedHashSet<>(r.marks()));
        }
        return d;
    }));

    public static final SavedDataType<Readings> TYPE =
            new SavedDataType<>(ID, Readings::new, CODEC, DataFixTypes.LEVEL);

    /** Insertion-ordered both ways, so the save file reads as a history of who read what. */
    private final Map<String, Set<String>> read = new LinkedHashMap<>();

    public static Readings get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * Record one piece of raw script.
     *
     * @return whether this was new to them. False means they had already read it, and
     *         nothing about their standing with the dead god has changed.
     */
    public boolean mark(UUID who, String mark) {
        if (!read.computeIfAbsent(who.toString(), k -> new LinkedHashSet<>()).add(mark)) {
            return false;
        }
        setDirty();
        return true;
    }

    /** How many distinct pieces this person has read. */
    public int by(UUID who) {
        Set<String> marks = read.get(who.toString());
        return marks == null ? 0 : marks.size();
    }

    /** Whether this person has read this particular piece. */
    public boolean has(UUID who, String mark) {
        Set<String> marks = read.get(who.toString());
        return marks != null && marks.contains(mark);
    }

    /** How many people have read anything. For the command seam. */
    public int readers() {
        return read.size();
    }
}
