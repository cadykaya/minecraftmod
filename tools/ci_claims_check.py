"""The counts the docs advertise are the counts CI actually runs.

`docs/HANDOFF.md` opens with a table a reader takes on trust, and the live-world
check count is the number in it that carries the most weight: it is the difference
between "this mod is asserted against a running server" and "this mod compiles".

That number was wrong. The workflow ran fifteen live-world checks and the table
said seventeen -- drift accumulated over several sessions of adding a check and
bumping a number by hand, in a repo whose entire doctrine is that an unchecked
claim rots. Nobody would have caught it by reading, because the only way to catch
it is to count the workflow, and nobody counts a workflow.

So it is counted here, on every push, and the docs may not disagree with it.

    python3 tools/ci_claims_check.py
"""
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WORKFLOW = os.path.join(REPO, ".github/workflows/checks.yml")
HANDOFF = os.path.join(REPO, "docs/HANDOFF.md")

fails = []

wf = open(WORKFLOW).read()
# Every step that drives a booted server. check_all.sh is excluded deliberately:
# it is the FAST gate and runs in a different job with no Minecraft on it at all,
# so counting it here would inflate exactly the number this file exists to defend.
live = [s for s in re.findall(r"run:\s*\./tools/([a-z_0-9]+\.sh)", wf)
        if s != "check_all.sh"]
if not live:
    fails.append("no live-world check steps found in the workflow -- has the `run:` "
                 "shape changed? This check would then be blind, and blind is worse "
                 "than absent because it still prints OK.")

# The fast gate's own stages, counted from the file rather than from memory.
gate = re.findall(r'^echo "== (.+) =="', open(os.path.join(REPO, "tools/check_all.sh")).read(),
                  re.M)

hand = open(HANDOFF).read()
m = re.search(r"\| Live-world checks \| \*\*(\d+)\*\*", hand)
if not m:
    fails.append("docs/HANDOFF.md: no `| Live-world checks | **N**` row -- the table "
                 "this check defends has been renamed or removed.")
elif int(m.group(1)) != len(live):
    fails.append(f"docs/HANDOFF.md advertises {m.group(1)} live-world checks; the "
                 f"workflow runs {len(live)}. Adding a check and forgetting the number "
                 f"is how a status table stops being status.")

for name in live:
    if not os.path.exists(os.path.join(REPO, "tools", name)):
        fails.append(f"workflow runs tools/{name}, which does not exist")

if fails:
    print()
    for f in fails:
        print("  - " + f)
    print(f"\nFAIL: {len(fails)} stale claim(s)")
    sys.exit(1)

print(f"\nOK: {len(live)} live-world check(s) in CI, {len(gate)} fast gate stage(s); "
      f"HANDOFF agrees")
