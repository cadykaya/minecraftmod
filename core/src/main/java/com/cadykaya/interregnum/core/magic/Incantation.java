package com.cadykaya.interregnum.core.magic;

import java.util.Locale;

/**
 * The word you say to cast a spell.
 *
 * `WORLD.md`, locked: *"Casting is a spoken word. Not a focus item, not a keybind. **You
 * say the word, out loud, in chat, and everyone in earshot sees you say it.**"*
 *
 * <h2>The word is the spell's own name, and that is the design</h2>
 *
 * No invented incantation, no fantasy Latin. You say *weather*, and the block ages. You
 * say *hush*, and the room goes quiet. Three reasons, in order of how much they matter:
 *
 * <ol>
 *   <li><b>It is the register.</b> This is a world whose institutions run on dockets,
 *       statements and correspondence. Magic here is not mystical, it is <i>stated</i> —
 *       and a Warden's citation can quote you verbatim because what you said was a plain
 *       word in a language the docket is already written in.</li>
 *   <li><b>An invented word would need a language, and the only language this world has
 *       is the dead god's script — which is now a hazard.</b> `WORLD.md` locks reading raw
 *       god-script as something that marks the reader. An incantation drawn from it would
 *       mean every cast was also an exposure, which is a collision nobody designed and a
 *       rule nobody could keep track of.</li>
 *   <li><b>It is discoverable.</b> A player who has been taught the Turning has been told
 *       the school's verbs. There is nothing else to learn and nothing to look up.</li>
 * </ol>
 *
 * <h2>Derived from {@link Spell}, never a hand-kept list</h2>
 *
 * A copy of a list is a claim that rots silently, and this repository has spent whole
 * sessions finding out what silently-rotted claims cost. Adding a spell and forgetting to
 * add its word would leave a verb nobody could say, with nothing anywhere reporting it —
 * so the word is computed from the enum constant and a new spell is speakable the moment
 * it exists.
 *
 * <h2>The whole message, or nothing</h2>
 *
 * {@link #of} matches the entire trimmed message and never a substring. This is not
 * fussiness: chat is where players talk about the game, and *"I hushed the room and it
 * still blew up"* must not silence anybody. A magic system that fires on a substring turns
 * every conversation about magic into a hazard, and the first thing anyone would learn is
 * to stop discussing it.
 */
public final class Incantation {
    private Incantation() {}

    /**
     * The word for a spell: its name, lowercased, underscores as hyphens.
     *
     * `DROP_FORGE` becomes *drop-forge* and `HELD_BREATH` would become *held-breath*,
     * which is how `WORLD.md` writes both of them. The hyphen is kept rather than dropped
     * so the word a player types is the word the design document uses.
     */
    public static String wordFor(Spell spell) {
        return spell.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /**
     * The spell this message casts, or {@code null} if it is just something somebody said.
     *
     * Case-insensitive and trimmed, because *"Hush"* at the start of a sentence and a
     * trailing space are both the player meaning it. Nothing else is forgiven.
     */
    public static Spell of(String spoken) {
        if (spoken == null) {
            return null;
        }
        String word = spoken.trim().toLowerCase(Locale.ROOT);
        if (word.isEmpty()) {
            return null;
        }
        for (Spell s : Spell.values()) {
            if (wordFor(s).equals(word)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Does this spell reach for what the caster is looking at, rather than around them?
     *
     * The division is not arbitrary and it is not per-spell taste. <b>A spell that makes a
     * place you are standing in centres on you; a spell you do something to centres on
     * what you are aiming at.</b>
     *
     * <ul>
     *   <li><i>Hush</i>, <i>Still</i> and <i>Lighten</i> are rooms you are inside. A
     *       silence you had to aim would be a bubble you could stand outside and shoot
     *       into, which is the one thing a silence must not be.</li>
     *   <li><i>Drop-forge</i> is the exception among the zones, and earns it: it is ground
     *       you <i>prepare</i> and then go and fetch a weight for. A forge centred on the
     *       caster would be a forge you are standing in the middle of.</li>
     *   <li>Everything else names a thing — a block to age, a creature to quell, a
     *       building to pick up, a volume to hurry.</li>
     * </ul>
     */
    public static boolean aimed(Spell spell) {
        return switch (spell) {
            // ...and Held-breath, which is the plainest case of all: the subject is the
            // speaker, and there is nothing to look at.
            case HUSH, STILL, LIGHTEN, HELD_BREATH -> false;
            // Moor IS aimed, and is the pair to Quell in the other school: a place you
            // change against a thing you change. It falls through to the default below
            // rather than being listed, which is the point of having a default at all.
            default -> true;
        };
    }
}
