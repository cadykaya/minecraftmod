"""Find problems the MOD caused in a server log, and only those.

A line-based grep gets this wrong in both directions. Ignoring 'authlib' by line
still flags that same exception's `Caused by:` lines, which carry no package name
-- and the tempting fix is to widen the ignore list until the output is green,
which is how a check stops being able to express failure at all.

So this groups the log into exception BLOCKS (a header plus its stack frames and
causes) and judges each block as a unit: if any line in the block is attributable
to a known-not-ours source, the whole block is ignored. Nothing is ignored by
shape alone.

    python3 tools/server_log_check.py <logfile>
"""
import re
import sys

# Not ours, and not fixable from here: this sandbox has no Mojang auth and the
# server runs offline. Each entry names WHY, so nobody widens the list casually.
NOT_OURS = [
    ("authlib",                    "Mojang auth unavailable in the sandbox"),
    ("yggdrasil",                  "Mojang auth unavailable in the sandbox"),
    ("minecraftservices.com",      "Mojang services host, offline sandbox"),
    ("api.mojang.com",             "Mojang services host, offline sandbox"),
    ("No key layers",              "vanilla server.properties noise, offline"),
    ("Advanced terminal",          "JLine terminal unavailable, cosmetic"),
    ("Unable to authenticate",     "offline sandbox"),
]

# A new log record starts with a timestamp; everything after it, until the next
# timestamped line, belongs to that record. Grouping this way rather than by stack
# shape is what makes attribution correct: a gson "See https://..." advice line in
# the middle of a stack was splitting one exception into two, and the second half
# -- a bare `Caused by:` carrying no package name -- looked unattributable.
RECORD = re.compile(r'^\[\d\d:\d\d:\d\d\]')
PROBLEM = re.compile(r'(ERROR\]|FATAL\]|Exception|Failed to )')


def records(lines):
    """Yield (line_number, [lines]) for each timestamped log record."""
    cur, start = [], 0
    for n, line in enumerate(lines):
        if RECORD.match(line):
            if cur:
                yield start, cur
            cur, start = [line], n
        elif cur:
            cur.append(line)
    if cur:
        yield start, cur


def main(path):
    lines = open(path, errors="replace").read().splitlines()
    ours, ignored, scanned = [], 0, 0
    for idx, rec in records(lines):
        text = "\n".join(rec)
        if not PROBLEM.search(text):
            continue
        scanned += 1
        why = next((w for k, w in NOT_OURS if k.lower() in text.lower()), None)
        if why:
            ignored += 1
            continue
        ours.append((idx, rec))

    print(f"{len(lines)} lines, {scanned} problem record(s); "
          f"{ignored} attributed to known sandbox limitations")
    if ours:
        print(f"\nFAIL: {len(ours)} unattributed problem record(s):")
        for idx, rec in ours[:5]:
            print(f"\n  line {idx + 1}:")
            for l in rec[:8]:
                print("   ", l.strip()[:160])
        return 1
    print("OK: no unattributed problems in the server log")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else "/tmp/interregnum-server.log"))
