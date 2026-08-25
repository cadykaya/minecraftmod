package com.cadykaya.interregnum.core.dialogue;

/**
 * How a node decides which submitted option wins. Declared per node by the author,
 * never global -- see docs/WORLD.md "Dialogue".
 */
public enum ResolutionRule {
    /** The initiator's pick wins. Everyone else's pick is shown as a stance. */
    INITIATOR,
    /** Most-picked option wins; ties break toward the initiator's pick. */
    VOTE,
    /** Uniform roll among all submitted picks (the SWTOR dice). Seeded by caller. */
    ROLL,
    /** Every participant must pick the same option, or the node re-prompts. */
    UNANIMOUS
}
