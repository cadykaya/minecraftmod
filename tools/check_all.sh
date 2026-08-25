#!/bin/bash
# The single gate. Run before every commit; the heartbeat runs it every tick.
# Fails loud and first -- each stage exits non-zero on violation, and the flags
# below stop at the first one so the output ends AT the failure, not past it.
#
# bash, and -euo pipefail, on purpose: a /bin/sh version of this file once hit a
# syntax error on a bashism, kept going, and still printed "ALL CHECKS PASSED".
# A gate that can silently stop checking is worse than no gate at all.
# See docs/LESSONS.md #9.
set -euo pipefail
cd "$(dirname "$0")/.."

echo "== palette =="
python3 tools/palette_check.py --textures src/main/resources/assets

echo "== dialogue =="
python3 tools/dialogue_check.py

echo "== registry =="
python3 tools/registry_check.py

echo "== unraveling =="
python3 tools/unraveling_check.py

echo "== client leak =="
python3 tools/client_leak_check.py

echo "== core selftest =="
rm -rf build/core && mkdir -p build/core
javac -d build/core $(find core/src -name '*.java') 2>/dev/null
java -cp build/core com.cadykaya.interregnum.core.SelfTest 2>/dev/null

echo "== core mutation check =="
python3 tools/mutate_check.py 2>/dev/null | tail -1

echo "== generated assets current =="
# Distinguish the two things a bare `git diff` conflates. Note what was ALREADY
# dirty before regenerating: that is uncommitted work, not staleness. Only files
# that regeneration itself changes are stale committed output. Reporting "stale"
# when the real cause is "you have not committed yet" is how a check earns a
# reputation for crying wolf, and a check people ignore is worse than none.
WATCH="src/main/resources assets/palette.json"
TMPB=$(mktemp); TMPA=$(mktemp)
trap 'rm -f "$TMPB" "$TMPA"' EXIT
git status --porcelain -- $WATCH | awk '{print $2}' | sort > "$TMPB"
python3 tools/build_textures.py >/dev/null
python3 tools/gen_resources.py >/dev/null
git status --porcelain -- $WATCH | awk '{print $2}' | sort > "$TMPA"
STALE=$(comm -13 "$TMPB" "$TMPA")
if [ -n "$STALE" ]; then
    echo "FAIL: committed generated output is stale -- regenerate and commit:"
    echo "$STALE" | sed 's/^/  /'
    exit 1
fi
if [ -s "$TMPB" ]; then
    echo "  note: uncommitted changes present (not staleness):"
    sed 's/^/    /' "$TMPB"
else
    echo "  generated output matches its source"
fi

echo "== doc links =="
python3 - <<'EOF'
import os, re, sys
bad = 0
for root, _, fs in os.walk('docs'):
    for f in fs:
        if f.endswith('.md'):
            p = os.path.join(root, f)
            for m in re.finditer(r'\]\(([^)#]+?\.md)(?:#[^)]*)?\)', open(p).read()):
                t = os.path.normpath(os.path.join(root, m.group(1)))
                if not os.path.exists(t):
                    print('BROKEN', p, '->', m.group(1)); bad += 1
sys.exit(1 if bad else 0)
EOF

echo
echo "ALL CHECKS PASSED"
