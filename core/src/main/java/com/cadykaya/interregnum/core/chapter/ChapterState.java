package com.cadykaya.interregnum.core.chapter;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Server-wide chapter tracking. One instance per world-save.
 *
 * Two invariants, both load-bearing and both tested:
 *
 *  1. MONOTONIC. The chapter never decreases. A milestone cannot be un-earned, and
 *     even if one somehow were, the recorded high-water mark holds. The overworld
 *     does not heal because someone reloaded a backup.
 *
 *  2. DERIVED. The chapter is computed from the milestone set rather than stored
 *     alongside it, so the two can never disagree -- the class of bug where a save
 *     says "chapter 3" while the world has done none of chapter 3's work.
 */
public final class ChapterState {
    private final EnumSet<Milestone> milestones = EnumSet.noneOf(Milestone.class);
    private int letters;
    private Chapter highWater = Chapter.DORMANT;

    /** @return true if this milestone is newly recorded (worth announcing). */
    public boolean record(Milestone m) {
        boolean isNew = milestones.add(m);
        if (m == Milestone.LETTER_DELIVERED) letters++;   // repeatable; count separately
        recompute();
        return isNew;
    }

    public boolean has(Milestone m) { return milestones.contains(m); }
    public int lettersDelivered() { return letters; }
    public Set<Milestone> milestones() { return Collections.unmodifiableSet(milestones); }
    public Chapter chapter() { return highWater; }
    public int band() { return highWater.band; }

    /** Chapter 0 is the whole point of the dormant phase: NOTHING may change here. */
    public boolean mechanicsDormant() { return highWater == Chapter.DORMANT; }

    private void recompute() {
        Chapter derived = Chapter.DORMANT;
        for (Chapter c : Chapter.values())
            if (milestones.containsAll(c.requires()) && c.band > derived.band) derived = c;
        if (derived.band > highWater.band) highWater = derived;   // monotonic
    }

    // --- save round-trip: a plain string, so the NBT/codec layer stays trivial ----

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        for (Milestone m : milestones) sb.append(m.name()).append(',');
        return sb.append('|').append(letters).append('|').append(highWater.name()).toString();
    }

    public static ChapterState deserialize(String s) {
        ChapterState st = new ChapterState();
        String[] parts = s.split("\\|", -1);
        if (parts.length != 3) throw new IllegalArgumentException("bad chapter state: " + s);
        for (String name : parts[0].split(",")) {
            if (name.isBlank()) continue;
            st.milestones.add(Milestone.valueOf(name));
        }
        st.letters = Integer.parseInt(parts[1]);
        st.highWater = Chapter.valueOf(parts[2]);   // restore the mark, then re-derive
        st.recompute();                             // never lets a save LOWER the mark
        return st;
    }
}
