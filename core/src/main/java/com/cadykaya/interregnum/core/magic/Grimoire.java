package com.cadykaya.interregnum.core.magic;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * What one person has been taught.
 *
 * <h2>Nothing is known by default, and that is the progression</h2>
 *
 * `WORLD.md` locks schools as *"learned in their worlds"*. A player who has not been
 * taught cannot cast, full stop — so the reason to cross is not a stat bonus, it is that
 * the verbs themselves are over there. Every spell in the mod is behind a journey and a
 * conversation, and this class is the reason.
 *
 * <h2>It only ever grows</h2>
 *
 * There is no unlearning, and no method to do it. A school is a thing you understand
 * about how the world works; the Wardenate can make casting a citable offence and a god
 * can refuse to teach you the rest, but neither can reach into your head and take back
 * what you already know. The consequences of casting are enforced where casting happens —
 * see {@link Casting} — not by confiscating the knowledge.
 *
 * Serialised as lowercase names rather than ordinals, because an enum's order is an
 * implementation detail and a save that survives a reordering by accident is a save that
 * will not survive the next one.
 */
public final class Grimoire {
    private final Set<School> known = EnumSet.noneOf(School.class);

    public Grimoire() {}

    public Grimoire(Iterable<School> schools) {
        for (School s : schools) {
            known.add(s);
        }
    }

    /** @return whether this was new. */
    public boolean learn(School school) {
        return known.add(school);
    }

    public boolean knows(School school) {
        return known.contains(school);
    }

    public boolean empty() {
        return known.isEmpty();
    }

    public int size() {
        return known.size();
    }

    /** Stable, lowercase, sorted -- so a save file diff is readable and deterministic. */
    public List<String> serialize() {
        return known.stream().map(s -> s.name().toLowerCase(Locale.ROOT)).sorted().toList();
    }

    /**
     * Rebuild from what was written.
     *
     * An unrecognised name is DROPPED rather than throwing. A school removed in a later
     * version must not make a save unloadable: the player simply no longer knows a thing
     * that no longer exists, which is the correct outcome and the only one that does not
     * cost somebody their world.
     */
    public static Grimoire deserialize(List<String> names) {
        Grimoire g = new Grimoire();
        for (String n : names) {
            for (School s : School.values()) {
                if (s.name().equalsIgnoreCase(n)) {
                    g.learn(s);
                }
            }
        }
        return g;
    }
}
