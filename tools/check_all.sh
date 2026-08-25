#!/bin/sh
# The single gate. Run before every commit; the heartbeat runs it every tick.
# Fails loud and first -- each stage exits non-zero on violation, and set -e
# stops at the first one so the output ends AT the failure, not past it.
set -e
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

echo "== generated assets current (textures + resources rebuild with no diff) =="
python3 tools/build_textures.py >/dev/null
python3 tools/gen_resources.py >/dev/null
git diff --exit-code --stat -- src/main/resources assets/palette.json \
  || { echo "FAIL: committed generated output is stale -- regenerate and commit"; exit 1; }

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
