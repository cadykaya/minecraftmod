package com.cadykaya.interregnum.core.ferry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * What the keel picked up, and what a destination makes of it.
 *
 * A manifest is a census: block id to count, and nothing else. It deliberately does not
 * hold positions -- the crossing needs a bill of lading, not a blueprint, and keeping
 * the two apart means this whole class is testable with no world in existence.
 *
 * <h2>Why the checklist is sorted and counted</h2>
 *
 * A player who has built a ferry out of two hundred blocks and gets back "there is a
 * problem" has been told nothing. The checklist names the rule, the block, and **how
 * many**, in a stable order, because the actual question in their head is "where is it"
 * and the count is the closest this can get to answering it. Two note blocks is a
 * different search from twelve.
 */
public record Manifest(Map<String, Integer> blocks) {

    public Manifest {
        // TreeMap: the checklist is read by a person, and a bill of lading that
        // reorders itself between two identical crossings is one nobody can trust.
        blocks = Collections.unmodifiableMap(new TreeMap<>(blocks));
    }

    public static Manifest of(Iterable<String> blockIds) {
        Map<String, Integer> counts = new TreeMap<>();
        for (String id : blockIds) {
            counts.merge(id, 1, Integer::sum);
        }
        return new Manifest(counts);
    }

    public int total() {
        return blocks.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** One line of the checklist: a rule broken, by what, how many times. */
    public record Violation(String rule, String blockId, int count, String reasonKey) {}

    /**
     * Everything this law refuses in this cargo, in a stable order.
     *
     * Empty means the crossing is legal. Note that it reports EVERY violation rather
     * than stopping at the first: a player sent back to fix one thing, who then finds
     * a second thing, has been made to cross twice for one mistake of the design's.
     */
    public List<Violation> validate(Law law) {
        List<Violation> out = new ArrayList<>();
        for (var entry : blocks.entrySet()) {
            String rule = law.ruleAgainst(entry.getKey());
            if (rule != null) {
                out.add(new Violation(rule, entry.getKey(), entry.getValue(),
                        law.rules().get(rule).reasonKey()));
            }
        }
        return out;
    }

    public boolean admissible(Law law) {
        return validate(law).isEmpty();
    }
}
