package com.cadykaya.interregnum.core.dialogue;

import java.util.List;
import java.util.Objects;

/**
 * One selectable reply. {@code textKey} is a translation key, never English text --
 * the same rule as every other player-facing string. {@code requiredTags} gates the
 * option to players whose tag set contains all of them (e.g. "class/theoclast",
 * "cited/3"); empty means everyone. {@code targetNodeId} of {@link DialogueGraph#END}
 * ends the conversation.
 */
public record DialogueOption(String id, String textKey, String targetNodeId,
                             List<String> requiredTags) {
    public DialogueOption {
        Objects.requireNonNull(id);
        Objects.requireNonNull(textKey);
        Objects.requireNonNull(targetNodeId);
        requiredTags = List.copyOf(requiredTags);
    }

    public boolean visibleTo(java.util.Set<String> playerTags) {
        return playerTags.containsAll(requiredTags);
    }
}
