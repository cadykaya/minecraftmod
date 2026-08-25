package com.cadykaya.interregnum.core.ferry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * What a destination will not admit.
 *
 * `WORLD.md`: *the validation checklist teaches each world's rule before arrival.* That
 * sentence is the whole design of this class, and it is why a law is a list of NAMED
 * refusals rather than a boolean predicate. A crossing that answered "no" would be a
 * locked door. A crossing that answers *"no note blocks — the Quiet One's crossing is
 * silent, and a thing that can sound will sound"* has taught the player a world's law
 * before they have seen a single block of it.
 *
 * So every rule carries its own reason key. The checklist a player reads is assembled
 * from the rules they broke, which means **the law only ever explains itself at the
 * moment it is relevant** -- the cheapest possible tutorial, paid for by the player's
 * own mistake.
 *
 * Laws are pure data here. Which blocks a rule names is the game module's business
 * (block ids are Minecraft's), so this side speaks in opaque strings and stays
 * loader-independent like the rest of `core/`.
 */
public record Law(String id, Map<String, Rule> rules) {

    /**
     * One refusal.
     *
     * @param blocks   block ids this rule refuses, as opaque strings
     * @param reasonKey translation key for what the checklist tells the player
     */
    public record Rule(java.util.Set<String> blocks, String reasonKey) {
        public Rule {
            Objects.requireNonNull(reasonKey);
            blocks = java.util.Set.copyOf(blocks);
            if (blocks.isEmpty()) {
                // A rule that refuses nothing can never appear on a checklist, so it
                // teaches nothing and cannot be seen to be wrong -- the exact profile
                // of a thing that quietly stops working.
                throw new IllegalArgumentException(
                        "rule " + reasonKey + " names no blocks, so it can never refuse anything");
            }
        }
    }

    public Law {
        Objects.requireNonNull(id);
        rules = Map.copyOf(rules);
        if (rules.isEmpty()) {
            throw new IllegalArgumentException(
                    "law " + id + " has no rules; a crossing that refuses nothing is not a law");
        }
        // A block named by two rules would produce two checklist lines for one mistake,
        // and the player would reasonably read that as two separate problems.
        Map<String, String> seen = new LinkedHashMap<>();
        for (var e : rules.entrySet()) {
            for (String block : e.getValue().blocks()) {
                String other = seen.put(block, e.getKey());
                if (other != null) {
                    throw new IllegalArgumentException(
                            "law " + id + ": " + block + " is refused by both '" + other
                            + "' and '" + e.getKey() + "'; one mistake, one line");
                }
            }
        }
    }

    /** The rule refusing this block, or null if the law admits it. */
    public String ruleAgainst(String blockId) {
        for (var e : rules.entrySet()) {
            if (e.getValue().blocks().contains(blockId)) {
                return e.getKey();
            }
        }
        return null;
    }
}
