package com.cadykaya.interregnum.core.regard;

/**
 * The player-facing form of regard. Bands, never numbers: the UI says how someone
 * feels about you, never a score, because a visible score turns a relationship
 * back into the meter this system exists to avoid.
 */
public enum Standing {
    HATED, RESENTED, WARY, KNOWN, TRUSTED, BELOVED;

    static Standing of(int value) {
        if (value <= -60) return HATED;
        if (value <= -25) return RESENTED;
        if (value < 10) return WARY;
        if (value < 40) return KNOWN;
        if (value < 75) return TRUSTED;
        return BELOVED;
    }
}
