"""Every band a player can cross into has a line, and not one of them says a number.

`docs/WORLD.md` bans the karma bar. The ban is not "say nothing" -- a system that is
recorded, persisted and invisible is one a player cannot know exists -- it is **no
number**. That rule is easy to state, easy to agree with, and easy to break with one
line reading "your standing with the villages fell by 25", so it is checked here
rather than remembered.

Two things are asserted:

  1. COVERAGE. Every (institution, band, direction) a player can actually reach has a
     line. A missing key does not fail loudly at runtime -- it renders as the raw key
     in somebody's chat -- so it has to fail here instead. The institutions and bands
     are read out of the Java enums, which means adding an institution breaks this
     check until its lines exist. That coupling is the point.

  2. NO DIGITS. Not in any regard line, ever.

Impossible crossings are excluded rather than demanded: you cannot RISE into the
lowest band or FALL into the highest, because there is nowhere to have come from.
Demanding those would be demanding text for an event that cannot happen, and text
nobody can reach is text nobody will maintain.

    python3 tools/regard_lines_check.py
"""
import json
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CORE = os.path.join(REPO, "core/src/main/java/com/cadykaya/interregnum/core/regard")
LANG = os.path.join(REPO, "src/main/resources/assets/interregnum/lang/en_us.json")
PREFIX = "interregnum.regard."


def enum_constants(path, name):
    """The constants of a Java enum, in declaration order.

    Parsed rather than hardcoded so that this check cannot drift from the code it is
    checking -- a hardcoded copy of a list is a check that silently stops covering
    whatever was added after it was written.
    """
    src = open(path).read()
    body = src.split(f"enum {name} {{", 1)[1].split("\n}", 1)[0]
    body = re.sub(r"/\*.*?\*/", "", body, flags=re.S)
    body = re.sub(r"//.*", "", body)
    body = body.split(";")[0]
    out = []
    for token in body.split(","):
        token = token.strip()
        if re.fullmatch(r"[A-Z][A-Z_]*", token):
            out.append(token)
    if not out:
        print(f"  could not parse any constants out of {name}")
        sys.exit(1)
    return out


def main():
    institutions = enum_constants(os.path.join(CORE, "Institution.java"), "Institution")
    bands = enum_constants(os.path.join(CORE, "Standing.java"), "Standing")
    lang = json.load(open(LANG))

    lowest, highest = bands[0], bands[-1]
    wanted = []
    for inst in institutions:
        for band in bands:
            # You cannot rise into the bottom band or fall out of the top one: there
            # is no band below the first to have come from, and none above the last.
            for direction in ("rise", "fall"):
                if direction == "rise" and band == lowest:
                    continue
                if direction == "fall" and band == highest:
                    continue
                wanted.append(
                    f"{PREFIX}{inst.lower()}.{band.lower()}.{direction}")

    missing = [k for k in wanted if k not in lang]
    if missing:
        print(f"FAIL: {len(missing)} reachable band crossing(s) have no line:")
        for k in missing[:12]:
            print(f"  {k}")
        if len(missing) > 12:
            print(f"  ... and {len(missing) - 12} more")
        print("  A missing key renders as the raw key in a player's chat.")
        return 1

    # The rule the whole system exists to keep. A band is a relationship; a number is
    # a meter, and one line with a figure in it turns the first back into the second.
    numeric = []
    for key, text in lang.items():
        if key.startswith(PREFIX) and re.search(r"\d", text):
            numeric.append((key, text))
    if numeric:
        print(f"FAIL: {len(numeric)} regard line(s) state a number:")
        for key, text in numeric[:8]:
            print(f"  {key}: {text}")
        print("  docs/WORLD.md: bands, never numbers. There is no karma bar.")
        return 1

    # Lines that exist but no crossing can ever reach. Not fatal -- but they are dead
    # text that will be maintained forever by someone who assumes it ships.
    extra = [k for k in lang if k.startswith(PREFIX) and k not in set(wanted)]
    if extra:
        print(f"FAIL: {len(extra)} regard line(s) no crossing can reach:")
        for k in extra[:8]:
            print(f"  {k}")
        return 1

    print(f"OK: {len(wanted)} reachable band crossing(s), all written, none numeric")
    return 0


if __name__ == "__main__":
    sys.exit(main())
