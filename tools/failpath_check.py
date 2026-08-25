"""A failure path must not contain anything that can fail.

`docs/LESSONS.md` #23, and this file exists because I broke that rule three separate
times in three separate checks, each time the same way:

    fail "`interregnum turning age` aged a block in the OVERWORLD -- ..."

Backticks inside a DOUBLE-quoted bash string are command substitution. Bash runs the
text, prints `interregnum: command not found`, and delivers a failure message with its
own subject missing -- on the failure path, so only when somebody most needed to read
it. The habit comes from writing markdown all day, which is exactly why noticing it by
eye does not work: it looks correct.

The guard is blunt on purpose. This repository substitutes with `$(...)` everywhere and
has no legitimate backtick outside a comment in any shell script -- verified before this
check was written -- so ANY backtick on a non-comment line is the bug. A narrower rule
that tried to parse quoting would be a small bash parser, and a small bash parser is a
thing that can fail on the failure path.

Also asserted, from the same family: a script that calls `fail` defines it. An undefined
`fail` is `command not found` and exit 127 -- which `set -e` turns into a red build with
no message at all, so the check would be right and mute.

    python3 tools/failpath_check.py
"""
import glob
import os
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

fails = []
scripts = sorted(glob.glob(os.path.join(REPO, "tools", "*.sh")))

for path in scripts:
    rel = os.path.relpath(path, REPO)
    with open(path) as fh:
        lines = fh.read().splitlines()

    calls_fail = False
    defines_fail = False
    for n, line in enumerate(lines, 1):
        if line.lstrip().startswith("#"):
            continue
        if "`" in line:
            fails.append(
                f"{rel}:{n} has a backtick outside a comment. In a double-quoted string "
                f"that is command substitution, so bash RUNS the text and the message "
                f"arrives with its own subject missing -- and only on the failure path. "
                f"Use single quotes inside the message, or $(...) if substitution was "
                f"actually meant:\n      {line.strip()[:100]}")
        if line.lstrip().startswith("fail()"):
            defines_fail = True
        elif "fail " in line or line.lstrip().startswith("fail "):
            calls_fail = True

    if calls_fail and not defines_fail:
        fails.append(
            f"{rel} calls `fail` but never defines it. Undefined means exit 127 with "
            f"'command not found', which set -e turns into a red build carrying no "
            f"diagnosis at all -- the check would be right and mute.")

if fails:
    print()
    for f in fails:
        print("  - " + f)
    print(f"\nFAIL: {len(fails)} problem(s) on a failure path")
    sys.exit(1)

print(f"\nOK: {len(scripts)} shell check(s), no backtick outside a comment, "
      f"every `fail` defined where it is called")
