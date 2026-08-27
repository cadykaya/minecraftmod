"""Every registered thing must say how a player reaches it. Exits non-zero otherwise.

THREE SYSTEMS SHIPPED UNREACHABLE before this existed -- the Haunt's dream, the sealed
letter, the warning steles -- and every one of them was green in CI the whole time. That
is not a gap in any of those checks: `haunt_check.sh` drove the command seam and was right
to, `mail_check.sh` read the letters out of the data and was right to. **A check that
covers a path says nothing about whether anything else reaches it**, and no test this
project knows how to write would have told the difference.

So the route is written down in docs/REACHABILITY.md and this checks the writing:

  * every id registered in ModBlocks, ModItems and ModEntities appears in the table
  * nothing appears in the table that is not registered (the other direction, which is
    how a table goes stale: content is deleted and its row is left behind saying a player
    can reach something that no longer exists)
  * every row carries a status from the fixed set, so "reached by" cannot quietly become
    prose that means nothing

What it deliberately does NOT do is decide whether a status is TRUE. Nothing static can:
`PLAY` is a claim about a right-click handler somewhere, and a wrong one is exactly the
bug this file is about. What it enforces is that somebody had to write the claim down --
which is the whole of what was missing all three times.
"""
import os, re, sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOC = os.path.join(REPO, "docs/REACHABILITY.md")
REG = os.path.join(REPO, "src/main/java/com/cadykaya/interregnum/registry")

SOURCES = {
    "ModBlocks.java": r'BLOCKS\.register(?:SimpleBlock)?\(\s*\n?\s*"([a-z_]+)"',
    "ModItems.java": r'ITEMS\.register(?:SimpleItem)?\(\s*\n?\s*"([a-z_]+)"',
    "ModEntities.java": r'ENTITIES\.registerEntityType\("([a-z_]+)"',
}

STATUSES = ("PLAY", "OP", "SCENERY", "BLOCKED:")

fails = []


def registered():
    out = set()
    for name, pattern in SOURCES.items():
        text = open(os.path.join(REG, name)).read()
        found = set(re.findall(pattern, text))
        if not found:
            fails.append(f"{name}: no registrations matched. Either the file was renamed "
                         f"or the registration idiom changed -- and this check would then "
                         f"pass forever while covering nothing")
        out |= found
    return out


def documented():
    out = {}
    for line in open(DOC):
        m = re.match(r"\|\s*`([a-z_]+)`\s*\|(.+?)\|(.+?)\|\s*$", line)
        if m:
            out[m.group(1)] = (m.group(2).strip(), m.group(3).strip())
    return out


def main():
    reg = registered()
    doc = documented()

    for name in sorted(reg - set(doc)):
        fails.append(f"{name} is registered and is not in docs/REACHABILITY.md. Say how a "
                     f"player reaches it, or say which question in HANDOFF's 'Waiting on "
                     f"owner' blocks it -- three systems have shipped unreachable and green")
    for name in sorted(set(doc) - reg):
        fails.append(f"{name} is in docs/REACHABILITY.md and is not registered anywhere. "
                     f"The table is telling a reader they can reach something that does "
                     f"not exist")

    for name, (reached, status) in sorted(doc.items()):
        if not any(status.startswith(s) or f"`{s}" in status for s in STATUSES):
            fails.append(f"{name}: status {status!r} is not one of {STATUSES}. A free-text "
                         f"status is a row that cannot be read at a glance, which is the "
                         f"only thing this table is for")
        if not reached:
            fails.append(f"{name}: nothing in the 'reached by' column")

    if fails:
        print(f"FAIL: {len(fails)} reachability violation(s)")
        for f in fails:
            print("  -", f)
        return 1
    print(f"OK: {len(reg)} registered thing(s), every one with a route written down")
    return 0


if __name__ == "__main__":
    sys.exit(main())
