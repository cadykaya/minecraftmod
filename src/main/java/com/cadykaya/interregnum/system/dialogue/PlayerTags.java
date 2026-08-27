package com.cadykaya.interregnum.system.dialogue;

import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;

/**
 * What a player is, for the purpose of what they may say.
 *
 * Dialogue options can be gated on tags (`class/theoclast`, and later things like
 * `cited/3`), and this is the one place that answers what a given player carries.
 *
 * **It returned nothing for a long time, and that was correct rather than unfinished.**
 * The only tag any written scene used was `class/theoclast`, and the Theoclast class did
 * not exist -- no clast could be attuned, so no player could truthfully hold it. Hiding
 * those options was the honest answer, and the note here said: *when attunement lands, it
 * lands here, and every scene already written starts offering its gated lines with no edit
 * to the scenes.*
 *
 * <b>Attunement has landed, and that is exactly what happened.</b> The Warden's intake
 * scene has carried a `class/theoclast` reply since long before anybody could see it; it
 * appears now because somebody can hold the tag, and not one line of that scene changed.
 *
 * Kept as a named seam rather than an inline `Set.of()` at the call site so that
 * "where do tags come from" has an answer a reader can find.
 */
public final class PlayerTags {
    private PlayerTags() {}

    /** What this player is. Empty for almost everybody, which is the usual answer. */
    public static Set<String> of(ServerPlayer player) {
        var server = player.level().getServer();
        return server == null ? Set.of() : of(server, player.getUUID());
    }

    /**
     * The same question, asked about an id rather than a body.
     *
     * <b>This is the primary form and the {@link ServerPlayer} one is a wrapper</b>, which
     * is the opposite of how it started. What a player IS turns out to be a property of
     * their record: `Theoclasts` is keyed by uuid, saved with the world, and true of them
     * whether or not they are online.
     *
     * It matters because of who asks. A headless server has no players, so `talk show` --
     * the only way anything can see what a player would see -- has an id and nothing else.
     * A tag lookup that needed a body would have made the whole class invisible to every
     * check, exactly as it was invisible to this one until the signature changed.
     */
    public static Set<String> of(net.minecraft.server.MinecraftServer server, UUID player) {
        return com.cadykaya.interregnum.system.clast.Theoclasts.get(server).is(player)
                ? Set.of("class/theoclast") : Set.of();
    }
}
