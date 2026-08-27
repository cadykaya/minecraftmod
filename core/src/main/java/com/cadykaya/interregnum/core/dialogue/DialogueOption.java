package com.cadykaya.interregnum.core.dialogue;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.cadykaya.interregnum.core.regard.Institution;
import com.cadykaya.interregnum.core.regard.RegardState;

/**
 * One selectable reply. {@code textKey} is a translation key, never English text --
 * the same rule as every other player-facing string. {@code requiredTags} gates the
 * option to players whose tag set contains all of them (e.g. "class/theoclast",
 * "cited/3"); empty means everyone. {@code standing} gates it on what an institution
 * thinks of the player. {@code targetNodeId} of {@link DialogueGraph#END} ends the
 * conversation.
 *
 * The two gates are ANDed, and deliberately kept separate rather than merged into one
 * condition list: a tag is something you ARE and a standing is something somebody
 * else DECIDED, they fail for different reasons, and a UI that wants to explain why
 * an option is missing needs to tell them apart.
 */
public record DialogueOption(String id, String textKey, String targetNodeId,
                             List<String> requiredTags, Map<Institution, Integer> regard,
                             StandingGate standing) {
    public DialogueOption {
        Objects.requireNonNull(id);
        Objects.requireNonNull(textKey);
        Objects.requireNonNull(targetNodeId);
        Objects.requireNonNull(standing);
        requiredTags = List.copyOf(requiredTags);
        regard = Map.copyOf(regard);
    }

    /** An option gated only by tags, with no standing requirement. */
    public DialogueOption(String id, String textKey, String targetNodeId,
                          List<String> requiredTags, Map<Institution, Integer> regard) {
        this(id, textKey, targetNodeId, requiredTags, regard, StandingGate.OPEN);
    }

    /** An option with no consequences. Most options are this. */
    public DialogueOption(String id, String textKey, String targetNodeId, List<String> requiredTags) {
        this(id, textKey, targetNodeId, requiredTags, Map.of());
    }

    /**
     * Tags only -- for callers that genuinely have no regard to consult.
     *
     * Kept because a great deal of the engine legitimately does not know about
     * regard, and because an option with no standing gate must not become invisible
     * merely because it was asked the short question.
     */
    public boolean visibleTo(java.util.Set<String> playerTags) {
        return visibleTo(playerTags, null);
    }

    /** Both gates. A null {@code regard} means somebody with no history yet. */
    public boolean visibleTo(java.util.Set<String> playerTags, RegardState regard) {
        return playerTags.containsAll(requiredTags) && standing.admits(regard);
    }
}
