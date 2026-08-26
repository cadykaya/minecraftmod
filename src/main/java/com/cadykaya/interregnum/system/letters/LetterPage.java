package com.cadykaya.interregnum.system.letters;

import com.cadykaya.interregnum.core.letters.Letter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * A letter, as it reads when somebody opens it.
 *
 * <h2>Why this exists</h2>
 *
 * The four letters have been written, loaded, validated and checked since early on —
 * `mail_check.sh` asserts that three open with a name and the fourth opens `To —`, which
 * `WORLD.md` makes the mid-game's best reveal. And the item that carries them was
 * `registerSimpleItem`: it had no behaviour at all. **Nobody could read one.**
 *
 * The affordance was never an open question the way casting or attuning are. You read a
 * letter by opening it. So the item opens it, and this is where the page is built — the
 * same seam arrangement as {@link com.cadykaya.interregnum.system.ferry.FerryDocket},
 * because a right-click cannot be driven from a headless server and a page only the item
 * could produce is a page no check can read.
 *
 * <h2>The salutation is IN the letter, and this page does not add one</h2>
 *
 * The obvious shape was to render `To <addressee>` from {@link Letter#addressee} above
 * the body. It is wrong here: every letter's first body line already IS its salutation —
 * *"Ballast —"*, *"Rill —"*, and, for the fourth, *"To —"*. Adding one would print the
 * name twice, and print it above a `To —` that is the whole point of the set.
 *
 * `addressee` is the machine-readable half of the same fact, and it earns its place
 * elsewhere: `Post` enforces that exactly one letter in the set is unaddressed, which is
 * an invariant about a SET and cannot be read off any single letter's text. So the two
 * are not a duplication to be resolved — one is the writing and one is the rule about the
 * writing, and the rule is checked where a rule can be.
 */
public final class LetterPage {
    private LetterPage() {}

    /**
     * @return the letter, in the order it is read, or null if no such letter exists —
     *         which is a broken datapack rather than a player's mistake, so the caller
     *         decides what to say about it.
     */
    public static List<Component> of(String letterId) {
        Letter letter = Letters.forGod(letterId);
        if (letter == null) {
            return null;
        }
        List<Component> out = new ArrayList<>();
        out.add(Component.translatable(letter.subjectKey()).withStyle(ChatFormatting.GOLD));
        // The first body line is the salutation -- see above. Nothing is inserted.
        boolean first = true;
        for (String key : letter.bodyKeys()) {
            out.add(Component.translatable(key)
                    .withStyle(first ? ChatFormatting.WHITE : ChatFormatting.GRAY));
            first = false;
        }
        return out;
    }
}
