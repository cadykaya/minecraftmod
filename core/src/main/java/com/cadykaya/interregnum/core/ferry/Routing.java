package com.cadykaya.interregnum.core.ferry;

import com.cadykaya.interregnum.core.letters.Letter;

import java.util.Optional;

/**
 * Where a ferry goes when it reads the letter in your hand.
 *
 * `WORLD.md`, locked: *"No menu on the keel, no destination written by hand. **You hold a
 * letter, and the ferry reads it.** No letter, no voyage."* This is *"the route to them is
 * its unanswered correspondence"* and *"you are the only one carrying their mail"* ceasing
 * to be flavour and becoming the navigation.
 *
 * <h2>You cannot reach a god you are not carrying mail for</h2>
 *
 * That is the entire rule, and it is the whole reason this affordance was chosen over a
 * keel that cycles four destinations. The letters ARE the map. A world you have no post
 * for is a world with no route to it, and the shrine-keeper handing over the returned
 * correspondence is therefore the moment the map exists.
 *
 * <h2>The unaddressed letter still sails, and getting this wrong nearly shipped</h2>
 *
 * `Post` enforces that exactly one letter opens `To —`, and the obvious-looking rule was:
 * the ferry sails where the letter is <i>addressed</i>, so an envelope with no name on it
 * cannot be routed and the boat does not move. It reads beautifully. It is wrong.
 *
 * <b>The unaddressed letter is the Quiet One's</b>, and `WORLD.md` says why in as many
 * words: *"The Quiet One has no name in the letters, and that is the whole character…
 * whether the dead god never got close enough to have one, or had one and struck it out,
 * is never answered."* The blank envelope is a fact about a god, not a defect in a
 * document — and a routing rule keyed on the addressee would have made that god's world
 * <b>permanently unreachable</b>, silently, by the only affordance there is.
 *
 * So routing is by the letter's <b>id</b>, which names its crossing, and the addressee is
 * never consulted. The blank line stays exactly what it was written to be: something you
 * notice while holding the mail, and cannot ask anybody about.
 *
 * This is written down at length because the wrong version is the more appealing one, and
 * nothing downstream would have caught it. `letters_check.py` now asserts the set-level
 * fact that makes the right version work — every letter names a crossing and every
 * crossing has a letter.
 */
public final class Routing {
    private Routing() {}

    /** Why a letter could not be sailed on. */
    public enum Refusal {
        /** It can. */
        NONE,
        /**
         * Nothing was held, or what was held is not one of the dead god's letters.
         *
         * The only refusal there is. An unaddressed letter is NOT one of these -- see the
         * class note, which exists to stop that being re-added.
         */
        NOT_A_LETTER
    }

    /**
     * The crossing this letter is carried on, or empty if it is not a letter.
     *
     * @param letter the letter being held, or {@code null} for an empty hand or an
     *               unmarked stack -- both of which are the same thing to a ferry.
     */
    public static Optional<String> lawFor(Letter letter) {
        return letter == null ? Optional.empty() : Optional.of(letter.id());
    }

    /** Why {@link #lawFor} came back empty. */
    public static Refusal refusalFor(Letter letter) {
        return letter == null ? Refusal.NOT_A_LETTER : Refusal.NONE;
    }
}
