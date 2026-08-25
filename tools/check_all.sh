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

echo "== ci claims =="
python3 tools/ci_claims_check.py

echo "== failure paths =="
python3 tools/failpath_check.py

echo "== renamed APIs =="
python3 tools/renames_check.py

echo "== dimension laws =="
python3 tools/dimension_check.py

echo "== the mail =="
python3 tools/letters_check.py

echo "== regard lines =="
python3 tools/regard_lines_check.py

echo "== advancement =="
python3 tools/advancement_check.py

echo "== unraveling =="
python3 tools/unraveling_check.py

echo "== entity specs =="
# Validates on import: no two box nets may overlap and none may run off the sheet.
# Overlapping nets do not crash -- they render, wrongly, with one box wearing a
# slice of another's paint.
python3 -c "import sys; sys.path.insert(0,'tools'); import entity_specs as e; \
print(f'OK: {len(e.SPECS)} entity spec(s), nets do not collide')"

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
# The client package is watched because entity GEOMETRY is generated Java: a box
# resized in the spec without regenerating leaves the model and its texture net
# disagreeing, which garbles the skin and raises nothing anywhere.
WATCH="src/main/resources src/generated/resources assets/palette.json
       src/main/java/com/cadykaya/interregnum/client"
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
python3 tools/doclink_check.py

echo
echo "ALL CHECKS PASSED"
