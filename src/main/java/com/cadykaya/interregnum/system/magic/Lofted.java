package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.Interregnum;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What each caster is currently carrying.
 *
 * <h2>Saved with the world, and it is the only spell state that is</h2>
 *
 * {@link Zones} and {@link Quelled} are deliberately in memory: half a minute of altered
 * physics is not worth a save file, and a field that outlived the server would strand
 * somebody inside a spell nobody cast. This is the opposite case in every respect.
 *
 * A load is <b>somebody's building</b>, and while it is held those blocks are not in the
 * world. Losing the map loses the shed — not a spell effect, the shed. So it persists, and
 * it persists across a restart, a logout, and a death, because none of those is a reason
 * for a player's workshop to stop existing.
 *
 * It also never expires, which is the same argument from the other side: see
 * {@link com.cadykaya.interregnum.core.magic.Loft}. A timer here would drop a house.
 *
 * <h2>Offsets, not positions</h2>
 *
 * Each piece is stored relative to where it was lifted from, so setting down is one
 * addition and the load knows nothing about where it came from. The level IS recorded, and
 * only to refuse a set-down in a different world -- `WORLD.md` locks *"travel between
 * systems is only by ferry"*, and a structure walked through a portal would be a second
 * way to do the one thing the ferry exists for.
 */
public final class Lofted extends SavedData {
    private static final Identifier ID =
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "lofted");

    /** One block of a carried structure, and where it sat relative to the lift. */
    public record Piece(BlockPos offset, BlockState state) {
        static final Codec<Piece> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("at").forGetter(Piece::offset),
                BlockState.CODEC.fieldOf("state").forGetter(Piece::state)
        ).apply(i, Piece::new));
    }

    /** Everything one caster is holding. */
    public record Load(ResourceKey<Level> level, List<Piece> pieces) {
        static final Codec<Load> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.xmap(id -> ResourceKey.create(Registries.DIMENSION, id),
                                      ResourceKey::identifier)
                        .fieldOf("level").forGetter(Load::level),
                Piece.CODEC.listOf().fieldOf("pieces").forGetter(Load::pieces)
        ).apply(i, Load::new));

        public int size() {
            return pieces.size();
        }
    }

    private static final Codec<Lofted> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Load.CODEC).fieldOf("carrying")
                    .forGetter(d -> d.carrying)
    ).apply(i, raw -> {
        Lofted d = new Lofted();
        d.carrying.putAll(raw);
        return d;
    }));

    public static final SavedDataType<Lofted> TYPE =
            new SavedDataType<>(ID, Lofted::new, CODEC, DataFixTypes.LEVEL);

    private final Map<String, Load> carrying = new HashMap<>();

    public static Lofted get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** What this caster is holding, or null. */
    public Load held(UUID who) {
        return carrying.get(who.toString());
    }

    /** Take up a structure. The caller has already checked that their hands are empty. */
    public void take(UUID who, Load load) {
        carrying.put(who.toString(), load);
        setDirty();
    }

    /** Put it down. Returns what was held, or null if nothing was. */
    public Load release(UUID who) {
        Load was = carrying.remove(who.toString());
        if (was != null) {
            setDirty();
        }
        return was;
    }

    /** How many casters are carrying something. For the command seam. */
    public int count() {
        return carrying.size();
    }
}
