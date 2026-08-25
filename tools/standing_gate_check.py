"""Does an option appear and disappear as an institution changes its mind?

The gate is the first thing in the mod that READS regard. Everything else writes it.
So the property under test is not "the option exists" but "the SAME node offers a
DIFFERENT set of replies to the same player at three different standings" -- which no
single render can show, and which is why this parses a sequence rather than grepping.

The shape it expects in a smoke log, produced by talk_check.sh:

    talk show <uuid>              -- default standing (WARY)
    interregnum regard <uuid> adjust WARDENATE 50    -> TRUSTED
    talk show <uuid>
    interregnum regard <uuid> adjust WARDENATE -90   -> RESENTED
    talk show <uuid>

Split on the adjustments, three groups of `show|` lines, and each group must contain
exactly the options that standing admits. Grepping counts alone would pass an
implementation that showed both gated options in one render and neither in the others.

Note it matches the RENDERED ENGLISH, not the translation key. A dedicated server in
this setup does resolve `Component.translatable` -- the mod's `assets/` are on the
classpath -- so what `show` prints is what a player would read.

    python3 tools/standing_gate_check.py <smoke-output>
"""
import os
import sys

# The two gated replies, by the text a player actually sees.
TRUST_ONLY = "And we'll answer for the ones who aren't."
RESENT_ONLY = "Before you ask. Yes. It's us."
# One ungated reply, present in every render. Without this the check would pass
# happily against three EMPTY renders (docs/LESSONS.md #5, #7, #10).
ALWAYS = "We're here. We're present."

# The SPEAKER's line, which changes for the same reason the replies do. Asserted
# alongside them rather than in its own check because the interesting property is
# that the two agree: a unit that offers you the resented reply and greets you as a
# stranger is worse than one that does neither.
TRUST_LINE = "Your returns have been accepted without amendment."
RESENT_LINE = "Your designation appears in three prior returns."


def main():
    if len(sys.argv) != 2 or not os.path.exists(sys.argv[1]):
        print("  usage: standing_gate_check.py <smoke-output>")
        return 1

    lines = open(sys.argv[1], errors="replace").read().splitlines()

    # Everything after the LAST pair of WARDENATE adjustments: earlier `show` calls in
    # the same run belong to other assertions and must not be swept in.
    marks = [i for i, l in enumerate(lines) if "adjust= WARDENATE" in l]
    if len(marks) != 2:
        print(f"  expected exactly two WARDENATE adjustments, found {len(marks)}")
        return 1

    # The first group starts at the `show` that precedes the first adjustment. Walk
    # back to the render boundary rather than guessing a line count.
    start = 0
    for i in range(marks[0] - 1, -1, -1):
        if ALWAYS in lines[i]:
            start = i
            break
    groups = [
        lines[start:marks[0]],
        lines[marks[0] + 1:marks[1]],
        lines[marks[1] + 1:],
    ]

    # (label, must contain, must not contain) -- replies AND the speaker's line
    expect = [
        ("WARY (default)", [], [TRUST_ONLY, RESENT_ONLY, TRUST_LINE, RESENT_LINE]),
        ("TRUSTED", [TRUST_ONLY, TRUST_LINE], [RESENT_ONLY, RESENT_LINE]),
        ("RESENTED", [RESENT_ONLY, RESENT_LINE], [TRUST_ONLY, TRUST_LINE]),
    ]

    bad = []
    for group, (label, wanted, unwanted) in zip(groups, expect):
        text = "\n".join(l for l in group if "show|" in l)
        if ALWAYS not in text:
            bad.append(f"{label}: the render is missing its UNGATED reply -- "
                       f"this render did not happen, so it proves nothing")
            continue
        for w in wanted:
            if w not in text:
                bad.append(f"{label}: should offer {w!r} and does not")
        for u in unwanted:
            if u in text:
                bad.append(f"{label}: should NOT offer {u!r} and does")

    if bad:
        print("FAIL: standing does not decide what the node offers:")
        for b in bad:
            print(f"  {b}")
        return 1

    print("  the same node greets three ways and offers three sets of replies")
    return 0


if __name__ == "__main__":
    sys.exit(main())
