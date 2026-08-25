package com.cadykaya.interregnum.core.regard;

import java.util.EnumMap;
import java.util.Map;

/**
 * Per-institution regard for one player.
 *
 * Values live in [-100, 100] so no amount of grinding buys immunity and no single
 * atrocity can be out-farmed. On top of that each institution carries a **ceiling**:
 * a permanent cap that atrocities lower and nothing raises. Regard can always fall;
 * it can only rise as far as the ceiling allows. That one asymmetry is what makes a
 * deicide a scar instead of a debt.
 *
 * Design rules, all tested:
 *  - **No global morality.** There is no shared pool; pleasing the Wardenate costs
 *    you with the gods through explicit, declared couplings.
 *  - **The ghost is private.** Only a killer has a THE_GHOST relationship; for
 *    anyone else it is permanently zero and immovable.
 */
public final class RegardState {
    public static final int MIN = -100, MAX = 100;

    private final Map<Institution, Integer> values = new EnumMap<>(Institution.class);
    private final Map<Institution, Integer> ceilings = new EnumMap<>(Institution.class);
    private final boolean isKiller;

    public RegardState(boolean isKiller) { this.isKiller = isKiller; }

    public boolean isKiller() { return isKiller; }
    public int value(Institution i) { return values.getOrDefault(i, 0); }
    public int ceiling(Institution i) { return ceilings.getOrDefault(i, MAX); }
    public Standing standing(Institution i) { return Standing.of(value(i)); }

    /**
     * The single mutator. Everything else in this class is expressed through it.
     *
     * @return the delta actually applied after clamping to [MIN, ceiling].
     */
    public int adjust(Institution i, int delta) {
        if (i == Institution.THE_GHOST && !isKiller) return 0;
        int before = value(i);
        int after = Math.max(MIN, Math.min(ceiling(i), before + delta));
        values.put(i, after);
        return after - before;
    }

    /** Lower an institution's permanent cap. Never raises one. */
    public void lowerCeiling(Institution i, int cap) {
        ceilings.put(i, Math.min(ceiling(i), cap));
        if (value(i) > ceiling(i)) values.put(i, ceiling(i));   // the cap applies at once
    }

    /**
     * Killing a god. Permanent, coupled, and capped: the victim's own regard bottoms
     * out and is locked there, every surviving god learns what you are, and none of
     * them will fully trust you again.
     *
     * The Wardenate is deliberately NOT coupled here beyond a flat hit -- to them a
     * second deicide is not worse than the first, it is simply more evidence.
     */
    public void recordDeicide(Institution victim) {
        for (Institution i : Institution.values()) {
            if (i == Institution.WARDENATE || i == Institution.THE_GHOST) continue;
            if (i == victim) {
                lowerCeiling(i, MIN);
            } else {
                adjust(i, -45);
                lowerCeiling(i, -10);
            }
        }
        adjust(Institution.WARDENATE, -30);
    }
}
