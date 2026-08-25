package com.cadykaya.interregnum.core.letters;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The whole of the dead god's outgoing mail, and the rule that holds it together.
 *
 * <h2>The rule is about the SET, which is why it cannot live on {@link Letter}</h2>
 *
 * `WORLD.md` locks it in two sentences: *"Three letters open with a name. The fourth
 * opens `To --`."* No individual letter can be checked against that. A letter with no
 * addressee is perfectly legal — exactly one of them must be — and a letter with an
 * addressee is legal too. The invariant only exists once you have all four.
 *
 * That makes it precisely the kind of thing that rots silently. Somebody later decides
 * the Quiet One should have a name after all, or writes a fifth letter and leaves the
 * addressee out because they have not written it yet, and nothing anywhere fails. The
 * reveal — you have called it The Verdant for a hundred hours and the mail says
 * **"Rill —"** — quietly stops working, and it stops working for a reader who will never
 * know it was supposed to.
 */
public record Post(Map<String, Letter> letters) {

    /** How many letters may open without a name. Exactly one, forever. */
    public static final int UNADDRESSED = 1;

    public Post {
        Map<String, Letter> copy = new LinkedHashMap<>();
        for (var e : letters.entrySet()) {
            copy.put(e.getKey(), e.getValue());
        }
        letters = Map.copyOf(copy);
        if (letters.isEmpty()) {
            throw new IllegalArgumentException("there is no mail");
        }
        long unnamed = letters.values().stream().filter(l -> !l.named()).count();
        if (unnamed != UNADDRESSED) {
            throw new IllegalArgumentException(
                    "exactly " + UNADDRESSED + " letter must open unaddressed, and "
                            + unnamed + " do. WORLD.md: three letters open with a name, "
                            + "the fourth opens `To --`. If the Quiet One has been given "
                            + "a name, or another god has lost one, the reveal that the "
                            + "dead god's mail uses names nobody has heard is gone -- and "
                            + "nothing else in the mod will notice.");
        }
    }

    /** The letter for this god, or null. */
    public Letter forGod(String id) {
        return letters.get(id);
    }

    /** Every name the mail uses, in file order. The Quiet One contributes nothing. */
    public List<String> namesUsed() {
        List<String> out = new ArrayList<>();
        for (Letter l : letters.values()) {
            l.addressee().ifPresent(out::add);
        }
        return List.copyOf(out);
    }

    public int size() {
        return letters.size();
    }
}
