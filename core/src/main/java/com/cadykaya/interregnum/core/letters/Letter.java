package com.cadykaya.interregnum.core.letters;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One of the dead god's unanswered letters.
 *
 * `WORLD.md`: *the route to a successor is the dead god's unanswered correspondence to
 * its estranged family, and you are the only one left carrying their mail.* Four letters,
 * one per god, each opening a world's questline when it is delivered.
 *
 * <h2>The addressee is an Optional and that is the entire design</h2>
 *
 * **Three letters open with a name. The fourth opens `To —`.** That is locked, and it is
 * the whole character of the Quiet One: whether the dead god never got close enough to
 * have a name for it, or had one and struck it out, is never answered. The letter is the
 * only evidence and it is ambiguous on purpose. *"The one who never wrote back"* stops
 * being a fact about the Quiet One and becomes a question about the dead god.
 *
 * So absence is modelled as {@link Optional#empty()} and an EMPTY STRING is refused. A
 * blank addressee would render as `To ` with nothing after it, which is a typo; `To —`
 * is a decision. The two look nearly identical in a JSON file and mean opposite things,
 * which is exactly why the type has to tell them apart rather than the reader.
 *
 * <h2>Why this is here and not in the game module</h2>
 *
 * Nothing about a letter is Minecraft-shaped. It is an addressee, a subject line and
 * some lines of text, and the rule that matters about it is a rule about a *set* of
 * letters. All of that is checkable with no game running, so it is.
 */
public record Letter(String id, Optional<String> addressee, String subjectKey,
                     List<String> bodyKeys) {

    public Letter {
        Objects.requireNonNull(id, "a letter needs an id");
        Objects.requireNonNull(addressee, "addressee must be an Optional, never null");
        Objects.requireNonNull(subjectKey, "a letter needs a subject");
        if (id.isBlank()) {
            throw new IllegalArgumentException("a letter needs an id");
        }
        if (addressee.isPresent() && addressee.get().isBlank()) {
            throw new IllegalArgumentException(
                    "letter " + id + " has a blank addressee. An unaddressed letter is "
                            + "Optional.empty() and renders as `To --`; a blank string is "
                            + "a typo that renders as `To ` and looks like a bug.");
        }
        if (subjectKey.isBlank()) {
            throw new IllegalArgumentException("letter " + id + " has no subject");
        }
        bodyKeys = List.copyOf(bodyKeys);
        if (bodyKeys.isEmpty()) {
            throw new IllegalArgumentException(
                    "letter " + id + " has no body; a letter nobody wrote anything in "
                            + "cannot open a questline");
        }
    }

    /** Whether this letter names the god it is for. */
    public boolean named() {
        return addressee.isPresent();
    }
}
