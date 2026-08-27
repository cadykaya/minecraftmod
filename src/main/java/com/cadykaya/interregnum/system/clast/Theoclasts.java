package com.cadykaya.interregnum.system.clast;

import com.cadykaya.interregnum.Interregnum;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Who has attuned a clast.
 *
 * <h2>The first class the mod has, and the only per-player permanent thing in it</h2>
 *
 * `WORLD.md` locks clasts as **finite** — seven in a world, ever — and the class as *"a
 * server negotiation"*. That negotiation only means anything if the outcome sticks, so
 * this is saved with the world and never expires.
 *
 * <h2>It only grows, like a grimoire and for the same reason</h2>
 *
 * There is no un-attuning and no method to do one. A Theoclast is somebody carrying a
 * piece of a god; the Wardenate can cite them for it and a god can refuse to speak to
 * them, and neither can reach into them and take the piece back out. The consequences are
 * enforced where they happen, not by revoking the class.
 *
 * <h2>What it feeds</h2>
 *
 * {@link com.cadykaya.interregnum.system.dialogue.PlayerTags}, which is the seam every
 * scene already reads. Options gated `class/theoclast` have been shipping since before
 * anything could be attuned -- correctly hiding themselves, because nobody could
 * truthfully hold the tag. This is the class existing, and they light up with no edit to
 * any scene.
 */
public final class Theoclasts extends SavedData {
    private static final Identifier ID =
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "theoclasts");

    private static final Codec<Theoclasts> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().fieldOf("attuned")
                    .forGetter(d -> List.copyOf(d.attuned))
    ).apply(i, raw -> {
        Theoclasts d = new Theoclasts();
        d.attuned.addAll(raw);
        return d;
    }));

    public static final SavedDataType<Theoclasts> TYPE =
            new SavedDataType<>(ID, Theoclasts::new, CODEC, DataFixTypes.LEVEL);

    /**
     * Insertion-ordered, so the save file lists them in the order they were attuned.
     *
     * That order is the class's own history on a server -- who was first, and who came in
     * after -- and it costs nothing to keep. A `HashSet` would scramble it on every load
     * for no gain.
     */
    private final Set<String> attuned = new LinkedHashSet<>();

    public static Theoclasts get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean is(UUID player) {
        return attuned.contains(player.toString());
    }

    /** @return whether this was new. */
    public boolean attune(UUID player) {
        if (!attuned.add(player.toString())) {
            return false;
        }
        setDirty();
        return true;
    }

    /** How many exist. For the command seam, and for anybody counting against the seven. */
    public int count() {
        return attuned.size();
    }
}
