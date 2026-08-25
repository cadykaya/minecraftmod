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
public record Resolution(Kind kind, DialogueOption chosen, Map<String, String> stances) {
    public enum Kind { ADVANCED, REPROMPT }
}
