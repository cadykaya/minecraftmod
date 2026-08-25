package com.cadykaya.interregnum.core.dialogue;

import java.util.List;
import java.util.Objects;

/**
 * One beat: a speaker line and the replies it accepts. A node with no options is
 * terminal prose (the conversation ends after it is shown).
 */
public record DialogueNode(String id, String speaker, String textKey,
                           ResolutionRule rule, List<DialogueOption> options) {
    public DialogueNode {
        Objects.requireNonNull(id);
        Objects.requireNonNull(speaker);
        Objects.requireNonNull(textKey);
        Objects.requireNonNull(rule);
        options = List.copyOf(options);
    }

    public boolean terminal() {
        return options.isEmpty();
    }
}
