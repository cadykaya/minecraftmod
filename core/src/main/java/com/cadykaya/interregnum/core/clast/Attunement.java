package com.cadykaya.interregnum.core.clast;

import com.cadykaya.interregnum.core.regard.Institution;
import com.cadykaya.interregnum.core.regard.RegardState;
import com.cadykaya.interregnum.core.regard.Standing;

/**
 * Who may become a Theoclast, and who decides.
 *
 * `WORLD.md`, locked: *"**a rite at a shrine, and the keeper has to agree to witness it.**
 * Not a right-click, and not the crater."*
 *
 * <h2>The keeper agreeing is a standing check, and that is the whole design</h2>
 *
 * The keeper **is** the villages — there is no separate village institution to meet, only
 * its people, and a shrine-keeper is one of them. So "will they witness this" is a
 * question the regard system has been able to answer since long before there was anything
 * to ask it about.
 *
 * That makes this **the first time regard gates something a player actually wants.**
 * Standing has decided prices, greetings and which replies are offered; none of those is
 * a door. This one is: below the bar, you are carrying a piece of a god you cannot use,
 * and the way through is to go and be somebody the villages will vouch for.
 *
 * <h2>KNOWN, not TRUSTED</h2>
 *
 * The bar is deliberately the *first* band above indifference rather than a high one.
 *
 * <ul>
 *   <li><b>A stranger cannot do it.</b> Nobody has a record until they have dealt with
 *       somebody, and an institution's opinion of a person it has never met is an absence
 *       rather than a nought — so a player who has walked past every village is refused,
 *       which is the point.</li>
 *   <li><b>It is not a grind.</b> `WORLD.md` locks clasts as *finite* and the class as *"a
 *       server negotiation"*. The scarce thing is the clast, and it already is: seven in a
 *       world, ever. Making the standing hard as well would gate a rare item behind a
 *       long errand and turn a negotiation between players into a chore against a meter,
 *       which is the one shape `docs/WORLD.md` rules out for regard.</li>
 * </ul>
 *
 * <h2>Being the killer does not help and does not hurt</h2>
 *
 * The deicide caps every god's regard and files the killer with the Wardenate forever. It
 * does nothing to the villages, who do not know. The First Theoclast is unique because of
 * what happened to them, not because anybody granted it — and the ordinary route into the
 * class is open to them on the same terms as everybody else.
 */
public final class Attunement {
    private Attunement() {}

    /** The institution whose opinion decides. The keeper is its people. */
    public static final Institution WITNESS = Institution.VILLAGES;

    /** The lowest standing a keeper will witness a rite at. */
    public static final Standing BAR = Standing.KNOWN;

    /** Why a rite did or did not happen. */
    public enum Verdict {
        /** The keeper agrees. */
        WITNESSED,
        /**
         * The keeper will not.
         *
         * Not a failure of the rite: a refusal by a person, and the only one in the mod
         * that a player can change by living differently.
         */
        REFUSED,
        /** They are already one. A clast attunes a person, and they are already attuned. */
        ALREADY
    }

    /**
     * Will this keeper witness a rite for this person?
     *
     * @param regard the player's record, or {@code null} for somebody no institution has
     *               ever dealt with. Treated as the bottom of the scale rather than as an
     *               exception -- a stranger is not outside the villages' opinion, they are
     *               at the start of it.
     */
    public static Verdict judge(RegardState regard, boolean alreadyAttuned) {
        if (alreadyAttuned) {
            return Verdict.ALREADY;
        }
        if (regard == null) {
            return Verdict.REFUSED;
        }
        return regard.standing(WITNESS).compareTo(BAR) >= 0
                ? Verdict.WITNESSED : Verdict.REFUSED;
    }
}
