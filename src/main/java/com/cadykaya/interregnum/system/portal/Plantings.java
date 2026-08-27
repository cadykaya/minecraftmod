package com.cadykaya.interregnum.system.portal;

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
 * Where somebody planted a door.
 *
 * <h2>Persisted, unlike every other portal's state</h2>
 *
 * The Anchorite's shaft keeps its counters in memory and says why: two seconds of altered
 * physics is not the kind of thing that should survive a crash. This is the opposite kind
 * of thing. `WORLD.md` locks the Verdant's portal as *"the only portal in the mod with a
 * lifespan"*, and a lifespan that ended at the next restart would be a life of about an
 * hour. You planted it; the world remembers you planted it, for as long as it stands.
 *
 * <h2>One remembered position, and everything else read off the world</h2>
 *
 * This holds **only** the positions. Whether a door is open, seeded or gone is decided
 * every time by {@link com.cadykaya.interregnum.core.portal.Rooting#state} from the blocks
 * actually there — so a portal cannot outlive the tree that is it, whatever happened to
 * that tree and whoever did it. Storing "open" as a flag would mean a felled tree stayed a
 * door until something remembered to say otherwise, and `WORLD.md`'s *"closes when cut"*
 * would be a promise kept by a listener rather than by the world.
 *
 * <h2>Per level, and it has to be</h2>
 *
 * A position is only a position; (0, 70, 0) means something different in every world. This
 * is stored on the Verdant's own level rather than on the overworld — the opposite of
 * {@link com.cadykaya.interregnum.system.clast.Theoclasts}, which is about people and so
 * belongs to the server.
 */
public final class Plantings extends SavedData {
    private static final Identifier ID =
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "plantings");

    private static final Codec<Plantings> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.listOf().fieldOf("planted")
                    .forGetter(d -> List.copyOf(d.planted))
    ).apply(i, raw -> {
        Plantings d = new Plantings();
        d.planted.addAll(raw);
        return d;
    }));

    public static final SavedDataType<Plantings> TYPE =
            new SavedDataType<>(ID, Plantings::new, CODEC, DataFixTypes.LEVEL);

    /** Insertion-ordered, so the save file lists them in the order they were planted. */
    private final Set<BlockPos> planted = new LinkedHashSet<>();

    /**
     * The ledger for one world.
     *
     * Takes the level, not the server: see the class javadoc. Every other saved data in
     * this mod hangs off the overworld because it is about people or about the story;
     * this one is about ground.
     */
    public static Plantings get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** @return whether this was new. */
    public boolean plant(BlockPos pos) {
        if (!planted.add(pos.immutable())) {
            return false;
        }
        setDirty();
        return true;
    }

    /**
     * Forget one.
     *
     * Called when a position is found holding neither trunk nor sapling — the tree is
     * gone, however it went. Not called from the break event: a door can end in a dozen
     * ways the game does not report as somebody breaking a block, and a ledger that only
     * heard about one of them would keep a position for a tree that burned.
     */
    public boolean forget(BlockPos pos) {
        if (!planted.remove(pos)) {
            return false;
        }
        setDirty();
        return true;
    }

    public boolean has(BlockPos pos) {
        return planted.contains(pos);
    }

    /** Every planted position, as a snapshot safe to iterate while forgetting. */
    public List<BlockPos> all() {
        return List.copyOf(planted);
    }

    /** How many stand. For the command seam. */
    public int count() {
        return planted.size();
    }
}
