package com.cadykaya.interregnum.system.dialogue;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.core.chapter.Chapter;
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

    /**
     * The second dream: the ghost, back, saying what it actually wanted.
     *
     * Gated on the chapter rather than on wall-clock time or on how many nights have
     * passed, because WORLD.md locks the Haunt as escalating *with chapter* -- and
     * because a beat about the Wardens still enforcing is nonsense to a player who
     * has never met one. ENFORCEMENT is exactly "the deicide happened AND a Warden
     * has spoken to somebody", so the prerequisite for the scene and the subject of
     * the scene are the same fact.
     */
    public static final Identifier DREAM_TWO =
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "dream_audience_two");

    /** Why the dream did or did not happen. Every branch below names one. */
    public enum Outcome {
        OPENED,
        /** The god still lives; there is nothing to be haunted by. */
        NO_GHOST,
        /** Only its killer can be haunted. Everyone else sleeps fine. */
        NOT_THE_KILLER,
        /** The dream that is due has already happened, and it happens once. */
        ALREADY,
        /**
         * The first dream is spent and the second is not due yet.
         *
         * Distinct from {@link #ALREADY} because it is not the same answer: nothing
         * has been used up, the world simply has not got there. Collapsing the two
         * would make an operator reading the log believe a scene had been consumed.
         */
        NOT_YET,
        /** They are mid-conversation; the dream would trample it. */
        BUSY,
        /** The scene is missing from the datapack. */
        NO_SCENE
    }

    /**
     * Offer whichever dream-audience is due to this player, if any is theirs to have.
     *
     * There is no "which dream" parameter, and there should not be: which one is due
     * is a fact about the world, not a choice the caller gets to make. A sleeping
     * player cannot pick, so neither can the command.
     *
     * @param force skip the once-only check -- the operator is deliberately
     *              re-issuing a scene that was lost. It re-issues the dream that is
     *              CURRENTLY due, which is the one that can have been lost; it is not
     *              a way to run an earlier scene again out of order. Everything else
     *              still applies: a non-killer cannot be handed the ghost's private
     *              conversation by an admin with good intentions.
     */
    public static Outcome offer(MinecraftServer server, UUID player, boolean force) {
        ChapterSavedData data = ChapterSavedData.get(server);
        if (data.mechanicsDormant()) {
            return Outcome.NO_GHOST;
        }
        if (!player.equals(data.killer())) {
            return Outcome.NOT_THE_KILLER;
        }
        // Which dream is due. The second one needs the first to have happened AND the
        // world to have reached ENFORCEMENT; short of that the first is still the
        // answer, and once it has been had there is simply nothing to offer yet.
        boolean returning = data.has(Milestone.HAUNT_OPENED)
                && data.chapter().band >= Chapter.ENFORCEMENT.band;
        Identifier scene = returning ? DREAM_TWO : DREAM;
        Milestone marks = returning ? Milestone.HAUNT_RETURNED : Milestone.HAUNT_OPENED;
        if (!force && data.has(marks)) {
            return returning ? Outcome.ALREADY : Outcome.NOT_YET;
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
            Conversations.open(server, scene, List.of(id), null);
        } catch (IllegalArgumentException e) {
            LOG.error("The dream-audience could not open: {}", e.getMessage());
            return Outcome.NO_SCENE;
        }
        // Recorded on OPENING, not on reaching an ending, and deliberately: the beat
        // is that the god got its audience. Walking out of it is an answer, and an
        // answer must not buy the scene back.
        data.record(marks);
        LOG.info("The dead god has spoken to {}: {}.", player, scene);
        return Outcome.OPENED;
    }
}
