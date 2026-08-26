package com.cadykaya.interregnum.core.dialogue;

import com.cadykaya.interregnum.core.chapter.Milestone;
import com.cadykaya.interregnum.core.magic.School;
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
 *
 * <h2>A node may mark a milestone, and an OPTION may not</h2>
 *
 * {@code milestone} is null on almost every node. Where it is set, reaching that node
 * records the milestone in the world's chapter state -- which is how the four delivery
 * scenes make `LETTER_DELIVERED` mean anything, and therefore how chapters 3 to 5 become
 * reachable at all.
 *
 * It hangs on the NODE rather than on the option that leads there, and the distinction is
 * not stylistic. "The letter was delivered" is a fact about the conversation having
 * ARRIVED somewhere, not about which sentence somebody picked to get there: a scene with
 * three routes into its accepting ending should record one delivery, not three, and it
 * should record none at all down the route where the players refuse the errand. Putting
 * it on the option would make the milestone a property of a choice, and then every new
 * branch into the same ending would be a chance to forget it.
 */
public record DialogueNode(String id, String speaker, String textKey,
                           ResolutionRule rule, List<DialogueOption> options,
                           List<TextVariant> textVariants, Milestone milestone,
                           School teaches) {
    public DialogueNode {
        Objects.requireNonNull(id);
        Objects.requireNonNull(speaker);
        Objects.requireNonNull(textKey);
        Objects.requireNonNull(rule);
        options = List.copyOf(options);
        textVariants = List.copyOf(textVariants);
    }

    /** A node that marks nothing. Almost every node is this. */
    public DialogueNode(String id, String speaker, String textKey,
                        ResolutionRule rule, List<DialogueOption> options,
                        List<TextVariant> textVariants) {
        this(id, speaker, textKey, rule, options, textVariants, null, null);
    }

    /** A node that marks the world but teaches nothing. */
    public DialogueNode(String id, String speaker, String textKey,
                        ResolutionRule rule, List<DialogueOption> options,
                        List<TextVariant> textVariants, Milestone milestone) {
        this(id, speaker, textKey, rule, options, textVariants, milestone, null);
    }

    /** A node whose line is the same for everybody. Most nodes are this. */
    public DialogueNode(String id, String speaker, String textKey,
                        ResolutionRule rule, List<DialogueOption> options) {
        this(id, speaker, textKey, rule, options, List.of(), null, null);
    }

    /** Does arriving here record something permanent about the world? */
    public boolean marks() {
        return milestone != null;
    }

    /**
     * Does arriving here teach the listeners a school?
     *
     * `WORLD.md` locks schools as *"learned in their worlds"*, and a conversation is how
     * a god does the teaching. Like {@link #milestone} this hangs on the NODE rather than
     * on an option, for the same reason: being taught is a fact about where the
     * conversation ARRIVED, not about which sentence somebody picked on the way.
     */
    public boolean teachesSomething() {
        return teaches != null;
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
