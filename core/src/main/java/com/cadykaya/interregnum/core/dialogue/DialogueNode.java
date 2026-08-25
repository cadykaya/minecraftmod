package com.cadykaya.interregnum.core.dialogue;

import com.cadykaya.interregnum.core.regard.RegardState;

import java.util.List;
import java.util.Objects;

/**
 * One beat: a speaker line and the replies it accepts. A node with no options is
 * terminal prose (the conversation ends after it is shown).
 *
 * {@code textVariants} let the same beat be worded differently for a player an
 * institution has an opinion about -- see {@link TextVariant}. The node keeps a single
 * {@code textKey} as the line everybody else reads, so a node with no variants behaves
 * exactly as it always did.
 */
public record DialogueNode(String id, String speaker, String textKey,
                           ResolutionRule rule, List<DialogueOption> options,
                           List<TextVariant> textVariants) {
    public DialogueNode {
        Objects.requireNonNull(id);
        Objects.requireNonNull(speaker);
        Objects.requireNonNull(textKey);
        Objects.requireNonNull(rule);
        options = List.copyOf(options);
        textVariants = List.copyOf(textVariants);
    }

    /** A node whose line is the same for everybody. Most nodes are this. */
    public DialogueNode(String id, String speaker, String textKey,
                        ResolutionRule rule, List<DialogueOption> options) {
        this(id, speaker, textKey, rule, options, List.of());
    }

    public boolean terminal() {
        return options.isEmpty();
    }

    /**
     * The line this player reads.
     *
     * A null record -- somebody with no history, or a caller with none to hand --
     * reads as DEFAULT standing rather than as "no variants apply", exactly as an
     * option gate treats it. A fresh player is not outside the institutions' opinion;
     * they are at the bottom of it, and a variant written for people nobody has
     * formed a view about should reach them.
     */
    public String textFor(RegardState regard) {
        return TextVariant.choose(textVariants, regard, textKey);
    }
}
