package com.cadykaya.interregnum.system.letters;

import com.cadykaya.interregnum.Interregnum;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
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

/**
 * The Post's own record: what has been transcribed, and what is on a desk right now.
 *
 * <h2>Two halves, one file, because they are one system</h2>
 *
 * The set is permanent and the map is temporary, and separating them into two saved datas
 * would mean two things that have to agree about a letter id. A letter leaves the map and
 * enters the set in the same call, which is the only moment either changes.
 *
 * <h2>Server-wide, and that is the mechanic</h2>
 *
 * A transcription belongs to the world, not to whoever paid for it — see
 * {@link com.cadykaya.interregnum.core.letters.Transcription}. So this hangs off the
 * overworld like everything else that is about the story rather than about ground.
 *
 * <h2>Desks are positions, and a desk that stops being a desk keeps its letter</h2>
 *
 * A lodged letter is stored against the block position, not against a block entity. If
 * somebody breaks the lectern the entry survives, and putting a lectern back at the same
 * spot finds the letter still there — which is the forgiving behaviour, and the right one
 * for a system whose whole point is that the letters are few and losing one is
 * unrecoverable.
 */
public final class Codex extends SavedData {
    private static final Identifier ID =
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "codex");

    /** A letter on a desk, and when it was put there. */
    public record Lodged(BlockPos where, String letter, long lodgedAt) {
        static final Codec<Lodged> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("where").forGetter(Lodged::where),
                Codec.STRING.fieldOf("letter").forGetter(Lodged::letter),
                Codec.LONG.fieldOf("lodged_at").forGetter(Lodged::lodgedAt)
        ).apply(i, Lodged::new));
    }

    private static final Codec<Codex> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().fieldOf("transcribed")
                    .forGetter(d -> List.copyOf(d.transcribed)),
            Lodged.CODEC.listOf().fieldOf("desks")
                    .forGetter(d -> List.copyOf(d.desks.values()))
    ).apply(i, (done, open) -> {
        Codex d = new Codex();
        d.transcribed.addAll(done);
        for (Lodged l : open) {
            d.desks.put(l.where(), l);
        }
        return d;
    }));

    public static final SavedDataType<Codex> TYPE =
            new SavedDataType<>(ID, Codex::new, CODEC, DataFixTypes.LEVEL);

    private final Set<String> transcribed = new LinkedHashSet<>();
    private final Map<BlockPos, Lodged> desks = new LinkedHashMap<>();

    public static Codex get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Has this letter been through a desk? */
    public boolean transcribed(String letter) {
        return transcribed.contains(letter);
    }

    /** What is on this desk, or null. */
    public Lodged on(BlockPos where) {
        return desks.get(where.immutable());
    }

    /** Put a letter down. @return whether the desk was free. */
    public boolean lodge(BlockPos where, String letter, long now) {
        BlockPos at = where.immutable();
        if (desks.containsKey(at)) {
            return false;
        }
        desks.put(at, new Lodged(at, letter, now));
        setDirty();
        return true;
    }

    /**
     * Take the letter back, and record the transcription.
     *
     * The two happen together on purpose: a letter that left the desk without being
     * recorded would be a letter the clerk did the work for and nobody kept, and a
     * transcription recorded without the letter coming back would be a letter destroyed by
     * filing.
     */
    public Lodged collect(BlockPos where) {
        Lodged was = desks.remove(where.immutable());
        if (was == null) {
            return null;
        }
        transcribed.add(was.letter());
        setDirty();
        return was;
    }

    /** How many letters the Post has copies of. For the command seam. */
    public int copies() {
        return transcribed.size();
    }

    /** How many desks have something on them. For the command seam. */
    public int working() {
        return desks.size();
    }
}
