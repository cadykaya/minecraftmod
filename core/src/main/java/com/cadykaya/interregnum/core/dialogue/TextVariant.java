package com.cadykaya.interregnum.core.dialogue;

import com.cadykaya.interregnum.core.regard.RegardState;

import java.util.List;
import java.util.Objects;

/**
 * A different way of saying the same beat, to somebody with a different file.
 *
 * "A Warden's opening line depending on your file" is the oldest item on the regard
 * list, and it wants less machinery than it looks like it does. The node is the same
 * node -- same id, same rule, same replies underneath -- and only the SENTENCE
 * changes. Making it a whole alternate scene per standing would mean maintaining
 * three copies of a conversation that differ by one line, and they would drift.
 *
 * <h2>Whose standing, in a scene with three people in it</h2>
 *
 * The viewer's. Every player at the table reads the line addressed to *them*, and two
 * players can be looking at different words for the same beat.
 *
 * That sounds like a desync and is not, because the option list already works this
 * way: {@link StandingGate} means a Warden that trusts you offers you a reply it does
 * not offer your friend, and it has done since the gate landed. Personalising the line
 * is the same idea one step earlier in the paragraph. The alternative -- resolving
 * against the initiator, so everybody reads the same thing -- would make the text and
 * the replies disagree about whose file is open, which is worse than either rule on
 * its own.
 *
 * The node's own {@code textKey} is the fallback, so a scene that adds variants stays
 * correct for everybody they do not match. There is no "default variant".
 */
public record TextVariant(String textKey, StandingGate gate) {
    public TextVariant {
        Objects.requireNonNull(textKey);
        Objects.requireNonNull(gate);
        if (gate.isOpen()) {
            // A variant with no condition matches everybody, which makes it the line
            // -- and silently shadows the node's own text and every variant after it.
            // An author who wants that should change `text_key`.
            throw new IllegalArgumentException(
                    "text variant " + textKey + " has no standing condition, so it would "
                    + "always win; put it in the node's text_key instead");
        }
    }

    /**
     * The first variant this player's standing admits, or {@code fallback}.
     *
     * First-match rather than best-match: the author's order is the priority, and
     * "most specific wins" would need a definition of specific that does not exist
     * for two gates naming different institutions.
     */
    public static String choose(List<TextVariant> variants, RegardState regard, String fallback) {
        for (TextVariant v : variants) {
            if (v.gate().admits(regard)) {
                return v.textKey();
            }
        }
        return fallback;
    }
}
