"""Do the keys a running server actually emits exist in the lang file?

`regard_lines_check.py` proves the lang file covers every crossing the enums allow.
That is only half of it: it checks a set of keys it BUILDS ITSELF, from the same rule
the Java uses, written out twice. Two copies of a naming rule agree right up until one
of them is edited.

This closes the loop from the other end. It reads the keys a live server logged while
crossing real bands and requires each to resolve. If `RegardNotices.key()` ever drifts
from what the lang file is indexed by -- a separator, a case, a renamed band -- the
static check stays green and this one goes red, which is the only arrangement where
the two checks are not just the same check twice.

    python3 tools/regard_keys_check.py <server-log> [<server-log> ...]
"""
import json
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LANG = os.path.join(REPO, "src/main/resources/assets/interregnum/lang/en_us.json")

# RegardNotices logs: "Regard crossing for <uuid>: VILLAGES WARY -> KNOWN [<key>]"
EMITTED = re.compile(r"Regard crossing for \S+: (\w+) (\w+) -> (\w+) \[([^\]]+)\]")


def main():
    if len(sys.argv) < 2:
        print("  usage: regard_keys_check.py <server-log> [...]")
        return 1
    lang = json.load(open(LANG))

    seen = []
    for path in sys.argv[1:]:
        if not os.path.exists(path):
            continue
        for line in open(path, errors="replace"):
            m = EMITTED.search(line)
            if m:
                seen.append(m.groups())

    if not seen:
        # Not "nothing to check" -- the caller only runs this after making a band
        # cross on purpose, so finding none means the notice never fired and the
        # assertion below would pass by being empty (docs/LESSONS.md #5, #7, #10).
        print("  no crossing was emitted at all, so this check verified nothing")
        return 1

    bad = [(i, f, t, k) for (i, f, t, k) in seen if k not in lang]
    if bad:
        print(f"FAIL: {len(bad)} emitted crossing key(s) have no line:")
        for inst, frm, to, key in bad[:8]:
            print(f"  {inst} {frm} -> {to} emitted {key!r}, which is not in en_us.json")
        print("  RegardNotices.key() and the lang file disagree about the naming rule.")
        return 1

    print(f"  {len(seen)} emitted crossing(s), every key resolves")
    return 0


if __name__ == "__main__":
    sys.exit(main())
