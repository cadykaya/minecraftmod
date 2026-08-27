package com.cadykaya.interregnum.core.regard;

import com.cadykaya.interregnum.core.dialogue.DialogueOption;
import com.cadykaya.interregnum.core.dialogue.Resolution;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * What a conversation does to the people in it.
 *
 * One rule, and it is the whole design:
 *
 * > **Each participant is judged on what THEY said, not on what the table decided.**
 *
 * A vote you lost is still on your record with the party you sided with. Arguing
 * against your friends and losing protects your standing with whoever you argued
 * for -- and, symmetrically, going along with a group atrocity does not launder it,
 * because you still said the words.
 *
 * That is the mechanical form of the mod's stated position that dissent is content
 * rather than an error state (docs/WORLD.md, "Dialogue"). Without it the ensemble
 * system is decoration: everyone would end up with the initiator's record, and the
 * only player whose choices mattered would be whoever clicked first.
 *
 * The one exception is structural rather than moral: a REPROMPT resolved nothing, so
 * nothing was said yet and nothing is recorded. People are held to what they settle
 * on, not to their opening position in an argument they are still having.
 */
public final class RegardEffects {
    private RegardEffects() {}

    /**
     * Apply each participant's own stance to their own regard.
     *
     * @param states  participant id -> their regard, or null for someone with no
     *                record to keep (an id that is not a player).
     * @return what was actually applied, per participant, AFTER clamping -- which is
     *         not the same as what the data asked for, because ceilings and the
     *         [-100, 100] bounds both bite. Reporting the request rather than the
     *         effect is how a UI ends up promising a change that did not happen.
     */
    public static Map<String, Map<Institution, Integer>> apply(
            Resolution resolution, Function<String, RegardState> states) {
        Map<String, Map<Institution, Integer>> applied = new LinkedHashMap<>();
        if (resolution.kind() != Resolution.Kind.ADVANCED) {
            return applied;                    // nothing was settled, so nothing was said
        }
        for (String participant : resolution.stances().keySet()) {
            DialogueOption option = resolution.stanceOf(participant);
            if (option == null || option.regard().isEmpty()) {
                continue;
            }
            RegardState state = states.apply(participant);
            if (state == null) {
                continue;
            }
            Map<Institution, Integer> moved = new EnumMap<>(Institution.class);
            for (var entry : option.regard().entrySet()) {
                int delta = state.adjust(entry.getKey(), entry.getValue());
                if (delta != 0) {
                    moved.put(entry.getKey(), delta);
                }
            }
            if (!moved.isEmpty()) {
                applied.put(participant, moved);
            }
        }
        return applied;
    }
}
