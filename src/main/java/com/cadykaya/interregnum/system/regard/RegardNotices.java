package com.cadykaya.interregnum.system.regard;

import com.cadykaya.interregnum.core.regard.BandChange;
import com.cadykaya.interregnum.core.regard.Institution;
import com.cadykaya.interregnum.core.regard.Standing;
import com.cadykaya.interregnum.core.regard.Standings;
import com.cadykaya.interregnum.system.RegardSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Telling a player that somebody changed their mind about them.
 *
 * `docs/WORLD.md` bans the karma bar, and the ban is easy to misread as "say
 * nothing". Saying nothing is what the mod did until now, and it means the regard
 * system is invisible: recorded, persisted, and with no way for a player to learn it
 * exists short of reading the source. The rule is not *no feedback*. It is **no
 * number** -- you are told that the Wardenate has noticed you, never that it has
 * noticed you by five.
 *
 * So the event is a band CROSSING, computed in `core/regard/Standings` where it is
 * tested without a game, and most changes are not one. A conversation that moves four
 * institutions a little says nothing at all, which is correct: nobody's opinion of
 * you actually changed, it just moved.
 *
 * <h2>Why this wraps the mutation instead of being called after it</h2>
 *
 * Regard moves from three unrelated places today -- a conversation settling, a god
 * dying, a keeper murdered -- and will move from more. A helper that each of them
 * calls afterwards is a helper one of them will eventually forget, silently, and the
 * only symptom is a player not being told something. {@link #around} takes the
 * mutation itself, so the snapshot cannot be skipped without deleting the call that
 * does the work.
 */
public final class RegardNotices {
    private static final Logger LOG = LoggerFactory.getLogger(RegardNotices.class);

    private RegardNotices() {}

    /**
     * Run a change to these players' regard, and tell each of them what crossed.
     *
     * The snapshot is taken for every player named BEFORE the change runs, because a
     * conversation resolves everyone at once and a snapshot taken lazily afterwards
     * would be of a state that has already moved.
     */
    public static void around(MinecraftServer server, Collection<UUID> whom, Runnable change) {
        RegardSavedData data = RegardSavedData.get(server);
        Map<UUID, Map<Institution, Standing>> before = new LinkedHashMap<>();
        for (UUID who : whom) {
            before.put(who, Standings.snapshot(data.of(server, who)));
        }

        change.run();

        // The change has happened. Everything past this point is TELLING somebody
        // about it, which is cosmetic, and cosmetic work is not allowed to undo it.
        //
        // Found by mutation: a deliberate bug in the crossing logic threw out of the
        // middle of `Deicide.commit`, which had already recorded the milestone and
        // set the killer and had not yet stopped the sun. A god half-killed because a
        // line of chat could not be composed is a far worse failure than a missing
        // line of chat, and the ordering above is not a defence -- the mutation
        // proved the throw reaches the caller. So it is caught here, loudly, and the
        // world moves on without the message.
        try {
            for (var entry : before.entrySet()) {
                List<BandChange> crossed =
                        Standings.since(entry.getValue(), data.of(server, entry.getKey()));
                if (!crossed.isEmpty()) {
                    tell(server, entry.getKey(), crossed);
                }
            }
        } catch (RuntimeException e) {
            LOG.error("Regard changed, but the notice could not be delivered. "
                    + "The change stands; the message is lost.", e);
        }
    }

    /** The single-player shape, which is most of the callers. */
    public static void around(MinecraftServer server, UUID who, Runnable change) {
        around(server, List.of(who), change);
    }

    private static void tell(MinecraftServer server, UUID who, List<BandChange> crossed) {
        ServerPlayer player = server.getPlayerList().getPlayer(who);
        for (BandChange change : crossed) {
            String key = key(change);
            // Logged as well as sent, and the key is logged rather than the rendered
            // sentence on purpose: the key is the stable identity of the event and
            // the sentence is the writer's business, free to be reworded at any time.
            // A check that asserted the prose would break every time somebody
            // improved a line, which is the fastest way to teach people to stop
            // improving lines. `regard_keys_check.py` reads exactly these keys.
            //
            // (An earlier version of this comment claimed a dedicated server cannot
            // resolve translations and that this was the only way to observe them.
            // That is false in this setup -- the mod's `assets/` are on the
            // classpath and `talk show` prints fully rendered English.)
            LOG.info("Regard crossing for {}: {} {} -> {} [{}]",
                    who, change.institution(), change.from(), change.to(), key);
            if (player != null) {
                // Grey italic: this is the world observing, not a character speaking.
                // The same styling every ambient line in the mod uses, so a player
                // learns to read it as narration rather than as somebody addressing
                // them -- which matters here, because an institution changing its
                // mind about you is precisely NOT it coming to talk to you.
                player.sendSystemMessage(Component.translatable(key)
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
        }
    }

    /**
     * `interregnum.regard.villages.known.rise`.
     *
     * The direction is in the key rather than being left to the reader, because
     * falling into WARY from KNOWN and rising into WARY from RESENTED are opposite
     * events that share a band. A key without the direction would need one sentence
     * to cover both, and the only sentence that covers both says nothing.
     */
    public static String key(BandChange change) {
        return "interregnum.regard."
                + change.institution().name().toLowerCase(Locale.ROOT) + "."
                + change.to().name().toLowerCase(Locale.ROOT) + "."
                + (change.rose() ? "rise" : "fall");
    }
}
