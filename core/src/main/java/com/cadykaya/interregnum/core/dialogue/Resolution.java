package com.cadykaya.interregnum.core.dialogue;

import java.util.Map;

/**
 * The outcome of resolving one node.
 *
 * {@code kind} ADVANCED: {@code chosen} won and the conversation moved to its
 * target (or ended, if the target was END). REPROMPT: a UNANIMOUS node did not
 * get unanimity; {@code stances} carries everyone's picks so the table can argue
 * and try again -- dissent is content, not an error.
 */
public record Resolution(Kind kind, DialogueNode node, DialogueOption chosen,
                        Map<String, String> stances) {
    public enum Kind { ADVANCED, REPROMPT }

    /**
     * The option a given participant actually picked, or null.
     *
     * The node is carried so this is answerable at all: by the time a caller sees a
     * Resolution the conversation has already moved on, and `stances` alone is a map
     * of bare option ids with nothing left that can interpret them. Consequences and
     * the interface both need the option, not its id.
     */
    public DialogueOption stanceOf(String participant) {
        String id = stances.get(participant);
        if (id == null) return null;
        for (DialogueOption o : node.options()) {
            if (o.id().equals(id)) return o;
        }
        return null;
    }
}
