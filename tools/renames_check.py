"""No shell check hands the server a name 26.x renamed.

The Java half of an API rename takes care of itself: `javac` refuses to compile it. The
half that does not is a **command string**. `tools/*.sh` drive a live server over RCON,
and a renamed gamerule or command there is not an error anyone sees — the server answers
"Incorrect argument for command" into a log, the command does nothing, and every
assertion after it keeps running against a world that was never set up.

That is how `exodus_check.sh` came to carry a failure message reading "in a world with
randomTickSpeed at zero" about a world where the rule had been rejected two minutes
earlier. The check was red, which felt like the system working, and the diagnosis pointed
at the mod. `deicide_check.sh` had carried a comment about this exact rename since the day
it was written.

So the known-dead names are listed once, here, and enforced. `docs/PLATFORM.md` carries
the same table with what each one broke.

Deliberately NOT a general "is this a real gamerule" check: that would need the game's
registry, which means booting a server, which is the twelve-minute job this fast gate
exists to stay out of. A denylist of names we have actually been bitten by is small,
exact, and has no false positives.

    python3 tools/renames_check.py
"""
import glob
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# name -> (replacement, what it broke)
DEAD = {
    "randomTickSpeed": ("random_tick_speed",
                        "vanilla's ticking never switched off in exodus_check.sh"),
    "doDaylightCycle": ("advance_time", "the sun would not stop in deicide_check.sh"),
    "doFireTick": ("fire_spread", "not yet hit here; renamed with the rest of the set"),
    "doMobSpawning": ("spawn_monsters", "not yet hit here; renamed with the rest of the set"),
    "keepInventory": ("keep_inventory", "not yet hit here; renamed with the rest of the set"),
    "mobGriefing": ("mob_griefing", "not yet hit here; renamed with the rest of the set"),
    "doWeatherCycle": ("advance_weather", "not yet hit here; renamed with the rest of the set"),
}

fails = []
scripts = sorted(glob.glob(os.path.join(REPO, "tools", "*.sh")))

for path in scripts:
    rel = os.path.relpath(path, REPO)
    for n, line in enumerate(open(path).read().splitlines(), 1):
        # Comments are where these names legitimately appear -- both this table's own
        # documentation and the "NB: it was renamed" notes that are the point of writing
        # them down. Flagging those would train the reader to ignore this check.
        if line.lstrip().startswith("#"):
            continue
        for dead, (live, bit) in DEAD.items():
            if re.search(r"\b" + re.escape(dead) + r"\b", line):
                fails.append(
                    f"{rel}:{n} uses `{dead}`, which 26.x renamed to `{live}`. A server "
                    f"rejects it and carries on, so nothing fails where the mistake is -- "
                    f"last time: {bit}.\n      {line.strip()[:100]}")

if fails:
    print()
    for f in fails:
        print("  - " + f)
    print(f"\nFAIL: {len(fails)} name(s) the game no longer knows")
    sys.exit(1)

print(f"\nOK: {len(scripts)} shell check(s), none hands the server a name 26.x renamed")
