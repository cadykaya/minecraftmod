package com.cadykaya.interregnum.system.dialogue;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.core.chapter.Milestone;
import com.cadykaya.interregnum.system.ChapterSavedData;

import java.util.List;
import java.util.UUID;

/**
 * The dead god, speaking to the one who killed it.
 *
 * It gets its own class rather than a few lines in an event handler because the
 * gate is the whole feature and every clause in it is a way the beat goes wrong:
 * firing for the wrong player, firing before there is a ghost to do the haunting,
 * firing twice, or firing into a conversation somebody is already having.
 *
 * The handler that calls this needs a sleeping player and a headless server has
 * neither, so `/interregnum haunt dream` is the second legitimate caller -- the same
 * arrangement as {@link com.cadykaya.interregnum.system.Deicide}, and for the same
 * reason. It is also a real operator tool: a player who slept through a crash has
 * lost the only scripted delivery this scene has, and there is no other way to
 * hand it back to them.
 */
public final class TheHaunt {
    private static final Logger LOG = LogUtils.getLogger();

    private TheHaunt() {}

    public static final Identifier DREAM =
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "dream_audience");

    /** Why the dream did or did not happen. Every branch below names one. */
    public enum Outcome {
        OPENED,
        /** The god still lives; there is nothing to be haunted by. */
        NO_GHOST,
        /** Only its killer can be haunted. Everyone else sleeps fine. */
        NOT_THE_KILLER,
        /** It has already happened once, and once is what "first" means. */
        ALREADY,
        /** They are mid-conversation; the dream would trample it. */
        BUSY,
        /** The scene is missing from the datapack. */
        NO_SCENE
    }

    /**
     * Offer the first dream-audience to this player, if it is theirs to have.
     *
     * @param force skip the once-only check -- the operator is deliberately
     *              re-issuing a scene that was lost. Everything else still applies:
     *              a non-killer cannot be handed the ghost's private conversation by
     *              an admin with good intentions.
     */
    public static Outcome offer(MinecraftServer server, UUID player, boolean force) {
        ChapterSavedData data = ChapterSavedData.get(server);
        if (data.mechanicsDormant()) {
            return Outcome.NO_GHOST;
        }
        if (!player.equals(data.killer())) {
            return Outcome.NOT_THE_KILLER;
        }
        if (!force && data.has(Milestone.HAUNT_OPENED)) {
            return Outcome.ALREADY;
        }
        String id = player.toString();
        if (Conversations.of(id) != null) {
            // Waking up inside a conversation is possible -- somebody can be at a
            // table and get into a bed -- and the dream must not evict it. The
            // milestone is NOT recorded here, so the god simply tries again the
            // next time they sleep.
            return Outcome.BUSY;
        }
        try {
            Conversations.open(server, DREAM, List.of(id), null);
        } catch (IllegalArgumentException e) {
            LOG.error("The dream-audience could not open: {}", e.getMessage());
            return Outcome.NO_SCENE;
        }
        data.record(Milestone.HAUNT_OPENED);
        LOG.info("The dead god has spoken to {} for the first time.", player);
        return Outcome.OPENED;
    }
}
