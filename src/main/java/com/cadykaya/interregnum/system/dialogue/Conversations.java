package com.cadykaya.interregnum.system.dialogue;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

import com.cadykaya.interregnum.content.dialogue.DialogueLoader;
import com.cadykaya.interregnum.content.entity.WardenEntity;
import com.cadykaya.interregnum.core.chapter.Milestone;
import com.cadykaya.interregnum.core.dialogue.Conversation;
import com.cadykaya.interregnum.core.dialogue.DialogueGraph;
import com.cadykaya.interregnum.core.dialogue.DialogueNode;
import com.cadykaya.interregnum.core.dialogue.Resolution;
import com.cadykaya.interregnum.system.ChapterSavedData;
import com.cadykaya.interregnum.system.RegardSavedData;
import com.cadykaya.interregnum.core.regard.RegardEffects;
import com.cadykaya.interregnum.system.regard.RegardNotices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * Live conversations on the server.
 *
 * The decisions -- who wins a node, what dissent does -- are all in
 * {@link Conversation} in `core/`, tested with no game running. This class is the
 * part that needs a server: who is at which table, when a node resolves, and what
 * happens when somebody walks away mid-sentence.
 *
 * **Participants are opaque string ids, not players.** That is what the core engine
 * asked for and it is load-bearing here for two reasons. A real player is their UUID
 * string; but a table that only knows ids also means the whole multiplayer state
 * machine -- votes, ties, unanimity, someone quitting -- can be driven and asserted
 * on a headless server with no client in existence. Every rule below is exercised
 * that way in `tools/talk_check.sh`.
 *
 * **Nothing here is persisted.** A conversation is a thing happening between people
 * right now; if the server stops, the table is over. Saving one would mean restoring
 * a player into a half-finished argument they no longer remember having.
 */
public final class Conversations {
    private static final Logger LOG = LogUtils.getLogger();

    private Conversations() {}

    /**
     * How long a table waits on somebody who has stopped answering.
     *
     * A minute is a long time to stare at a dialogue box and a short time to walk
     * to the kitchen. Erring long is right: the failure mode of a short timeout is
     * that a table resolves without a player who was still reading, and there is no
     * undo for that.
     */
    public static final int TIMEOUT_TICKS = 20 * 60;

    public static final class Table {
        public final UUID id;
        public final Identifier scene;
        public final Conversation conversation;
        /** The entity being spoken to, or null for a conversation with nobody. */
        public final UUID speaker;
        private final RandomGenerator roll;
        private long lastActivity;

        Table(UUID id, Identifier scene, Conversation conversation, UUID speaker,
              RandomGenerator roll, long now) {
            this.id = id;
            this.scene = scene;
            this.conversation = conversation;
            this.speaker = speaker;
            this.roll = roll;
            this.lastActivity = now;
        }

        public DialogueNode node() {
            return conversation.current();
        }
    }

    private static final Map<UUID, Table> TABLES = new LinkedHashMap<>();
    private static final Map<String, UUID> SEATED = new HashMap<>();

    /** Everything is dropped when a server starts: nothing here outlives a session. */
    public static void reset() {
        TABLES.clear();
        SEATED.clear();
    }

    public static int active() {
        return TABLES.size();
    }

    /**
     * The online player behind a participant id, or null.
     *
     * Null is ordinary, not exceptional: participants are opaque ids, so a table may
     * legitimately contain someone who has logged off, or an id that was never a
     * player at all. Every push below simply skips those.
     */
    private static ServerPlayer playerFor(MinecraftServer server, String id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return null;                       // not a player id; nothing to show
        }
        return server.getPlayerList().getPlayer(uuid);
    }

    /**
     * Write what was said into the record.
     *
     * The rule -- each participant judged on their OWN stance, not on what the table
     * decided -- lives in {@link com.cadykaya.interregnum.core.regard.RegardEffects}
     * where it is tested without a game. This is the seam that hands it the states.
     *
     * No number is ever announced. There is no karma bar and no "+5 Villages"
     * (docs/WORLD.md) -- but there IS a line when somebody's opinion of you crosses
     * into a different band, because a system that is recorded, persisted and utterly
     * invisible is one a player cannot know exists. {@link RegardNotices} draws that
     * distinction; most conversations move regard without crossing anything and say
     * nothing at all, which is the correct outcome and the reason this is safe.
     *
     * Every participant is snapshotted before the effects run, not one at a time
     * afterwards: a table resolves everyone at once, and a snapshot taken after the
     * fact is of a state that has already moved.
     */
    private static void record(MinecraftServer server, Resolution r) {
        RegardSavedData regard = RegardSavedData.get(server);
        List<UUID> players = new ArrayList<>();
        for (String id : r.stances().keySet()) {
            try {
                players.add(UUID.fromString(id));
            } catch (IllegalArgumentException e) {
                // not a player; nothing keeps their record and nothing to tell them
            }
        }
        RegardNotices.around(server, players, () -> {
            var applied = RegardEffects.apply(r, id -> {
                UUID uuid;
                try {
                    uuid = UUID.fromString(id);
                } catch (IllegalArgumentException e) {
                    return null;           // not a player; nothing keeps their record
                }
                return regard.of(server, uuid);
            });
            if (!applied.isEmpty()) {
                regard.touch();
                LOG.info("Regard moved for {} participant(s): {}", applied.size(), applied);
            }
        });
    }

    /** Show every participant where the table stands. */
    public static void push(MinecraftServer server, Table table) {
        for (String p : table.conversation.participants()) {
            ServerPlayer player = playerFor(server, p);
            if (player == null) {
                continue;
            }
            for (Component line : ConversationView.render(table, p, PlayerTags.of(player))) {
                player.sendSystemMessage(line);
            }
        }
    }

    /** Show every participant what the table just said, losing stances included. */
    private static void pushStances(MinecraftServer server, Table table, Resolution r) {
        List<Component> stances = ConversationView.stances(r);
        for (String p : table.conversation.participants()) {
            ServerPlayer player = playerFor(server, p);
            if (player == null) {
                continue;
            }
            for (Component line : stances) {
                player.sendSystemMessage(line);
            }
        }
    }

    /** The table this participant is at, or null. */
    public static Table of(String participant) {
        UUID id = SEATED.get(participant);
        return id == null ? null : TABLES.get(id);
    }

    /**
     * Sit a group down in front of a scene. The first id is the initiator.
     *
     * @param speaker the entity being addressed, or null.
     * @throws IllegalArgumentException with a message meant to be shown, not logged.
     */
    public static Table open(MinecraftServer server, Identifier scene,
                             List<String> participants, Entity speaker) {
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("nobody is at the table");
        }
        DialogueGraph graph = DialogueLoader.get(scene);
        if (graph == null) {
            throw new IllegalArgumentException("no such conversation: " + scene);
        }
        for (String p : participants) {
            if (SEATED.containsKey(p)) {
                throw new IllegalArgumentException(p + " is already in a conversation");
            }
        }

        UUID id = UUID.randomUUID();
        // Seeded from the world and the table, per the core engine's contract: two
        // tables never share dice, and one table's dice are reproducible from its id.
        long seed = server.overworld().getSeed()
                ^ id.getMostSignificantBits() ^ id.getLeastSignificantBits();
        Table table = new Table(id, scene,
                new Conversation(graph, participants.get(0), participants),
                speaker == null ? null : speaker.getUUID(),
                new Random(seed), server.overworld().getGameTime());
        TABLES.put(id, table);
        for (String p : participants) {
            SEATED.put(p, id);
        }
        push(server, table);

        // The world answers back.
        //
        // This is what ends Chapter 1: not seeing a Warden, not being near one, but
        // being *addressed* by one. It belongs here rather than in the entity because
        // this is the single place a conversation with a Warden can begin, however it
        // was started -- by a right-click, or by a command on a headless server.
        if (speaker instanceof WardenEntity) {
            ChapterSavedData data = ChapterSavedData.get(server);
            var before = data.chapter();
            if (data.record(Milestone.WARDEN_CONTACT)) {
                // Reported as a transition rather than a state, because it is often
                // NOT one: ENFORCEMENT needs the deicide as well, so contact before
                // the god dies records the milestone and moves nothing. A log line
                // saying "the interregnum is now DORMANT" reads like a bug.
                LOG.info("First Warden contact recorded; chapter {} -> {}.",
                        before, data.chapter());
            }
        }
        return table;
    }

    /** How far a bystander can stand and still be pulled into a conversation. */
    public static final double TABLE_RADIUS = 8.0;

    /**
     * Somebody addresses an entity, and everyone nearby is in it.
     *
     * **Everyone within {@link #TABLE_RADIUS} with line of sight is pulled in**, not
     * just whoever clicked. That is the Star Wars: The Old Republic beat the owner
     * asked for by name, and it is the only version where the resolution rules mean
     * anything -- a VOTE node with one player at the table is an INITIATOR node with
     * extra steps. Standing back, or being behind a wall, is a real way to decline.
     *
     * Lives here rather than in either mob because both of them need it and the rule
     * is about conversations, not about who is speaking.
     *
     * **[NEEDS PLAYTEST]** the radius, and the AFK case: somebody pulled in who then
     * does nothing makes the rest wait out the timeout.
     */
    public static void address(ServerPlayer initiator, LivingEntity speaker, Identifier scene) {
        MinecraftServer server = initiator.level().getServer();
        if (server == null || of(initiator.getUUID().toString()) != null) {
            return;
        }
        List<String> table = new ArrayList<>();
        table.add(initiator.getUUID().toString());
        for (ServerPlayer other : speaker.level().getEntitiesOfClass(ServerPlayer.class,
                speaker.getBoundingBox().inflate(TABLE_RADIUS))) {
            String id = other.getUUID().toString();
            if (other != initiator && speaker.hasLineOfSight(other) && of(id) == null) {
                table.add(id);
            }
        }
        try {
            open(server, scene, table, speaker);
        } catch (IllegalArgumentException e) {
            initiator.sendSystemMessage(Component.literal(e.getMessage()));
        }
    }

    /**
     * Record a pick, and resolve the node if that was the last one outstanding.
     *
     * @return the resolution if the node resolved, or null if the table is still
     *         waiting on somebody.
     */
    public static Resolution submit(MinecraftServer server, String participant, String optionId) {
        Table table = of(participant);
        if (table == null) {
            throw new IllegalArgumentException(participant + " is not in a conversation");
        }
        table.conversation.submit(participant, optionId);
        table.lastActivity = server.overworld().getGameTime();
        if (!table.conversation.allSubmitted()) {
            return null;
        }
        return resolveAndShow(server, table);
    }

    /**
     * Resolve the node, show everyone what happened, and only then tidy up.
     *
     * The order matters and the first version had it wrong: closing the table as
     * soon as the conversation ended meant the TERMINAL NODE WAS NEVER SHOWN. Every
     * scene's last line -- the payoff of every branch, the line the whole
     * conversation was walking toward -- was resolved, recorded, and thrown away
     * without ever reaching a player. The state machine was perfectly correct and
     * the experience was missing its ending.
     *
     * Found by playing a scene through and reading the output, which is the only
     * way it could have been found: nothing about it is wrong from the inside.
     */
    private static Resolution resolveAndShow(MinecraftServer server, Table table) {
        Resolution r = table.conversation.resolve(table.roll);
        record(server, r);
        pushStances(server, table, r);
        push(server, table);                   // the next beat -- or the last line
        if (table.conversation.ended()) {
            close(table);
        }
        return r;
    }

    /**
     * Take somebody off a table.
     *
     * If the initiator leaves, the whole conversation ends -- there is no sensible
     * INITIATOR node without one, and the fiction agrees: the Warden was addressing
     * them. Otherwise the table shrinks, and shrinking can complete it, so a leave
     * may resolve the node on its way out. Without that, one player alt-tabbing
     * leaves everyone else staring at a box forever.
     *
     * @return the resolution if their departure completed the node, else null.
     */
    public static Resolution leave(MinecraftServer server, String participant) {
        Table table = of(participant);
        if (table == null) {
            return null;
        }
        if (participant.equals(table.conversation.initiator())) {
            close(table);
            return null;
        }
        table.conversation.remove(participant);
        SEATED.remove(participant);
        if (table.conversation.participants().isEmpty()) {
            close(table);
            return null;
        }
        return table.conversation.allSubmitted() ? resolveAndShow(server, table) : null;
    }

    public static void close(Table table) {
        TABLES.remove(table.id);
        SEATED.values().removeIf(id -> id.equals(table.id));
    }

    /**
     * Drop tables nobody is answering.
     *
     * Silence from the initiator ends the conversation; silence from anybody else
     * takes them off the table and lets the rest carry on. A table that can deadlock
     * on one absent player is a griefing tool in multiplayer, and this mod is
     * deliberately full of conversations several people are in at once.
     */
    public static void tick(MinecraftServer server) {
        if (TABLES.isEmpty()) {
            return;
        }
        long now = server.overworld().getGameTime();
        for (Table table : List.copyOf(TABLES.values())) {
            if (now - table.lastActivity < TIMEOUT_TICKS) {
                continue;
            }
            List<String> silent = new ArrayList<>();
            Map<String, String> picks = table.conversation.picks();
            for (String p : table.conversation.participants()) {
                if (!picks.containsKey(p)) {
                    silent.add(p);
                }
            }
            if (silent.contains(table.conversation.initiator()) || picks.isEmpty()) {
                LOG.info("Conversation {} timed out with nobody answering.", table.scene);
                close(table);
                continue;
            }
            table.lastActivity = now;
            for (String p : silent) {
                leave(server, p);
            }
        }
    }
}
