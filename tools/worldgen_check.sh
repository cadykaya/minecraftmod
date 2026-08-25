#!/bin/bash
# Prove the shrine actually builds in a live world, and that it contains what it
# should. This is the only way worldgen gets verified without a client.
#
# Every assertion is an `execute if block` that prints a marker on success, so a
# missing marker is a failure -- the check cannot pass by finding nothing, which is
# the trap that caught this project three times (docs/LESSONS.md #5, #7, #10).
set -euo pipefail
cd "$(dirname "$0")/.."

LOG=${LOG:-/tmp/interregnum-worldgen.log}
export LOG

# A flat world (server_smoke.sh writes level-type=flat and deletes run/world) makes
# the ground level everywhere, so a placement failure means the FEATURE is wrong
# rather than the terrain being unlucky.
#
# Coordinates: flat-world surface is grass at y=-61. findSurface returns the topmost
# SOLID block, so paving replaces the grass AT -61 -- it does not sit on top of it.
# Steles are 1 or 2 tall, so -60 is the only course guaranteed to be there.
COMMANDS='forceload add -16 -16 47 47
place feature interregnum:shrine 8 -60 8
execute if block 8 -61 8 interregnum:shrine_stone_carved run say A_CENTRE_CARVED
execute if block 6 -61 6 interregnum:shrine_stone run say B_CORNER_PAVING
execute if block 6 -60 6 interregnum:warning_stele run say C_STELE_STANDS
execute if block 8 -60 8 air run say D_CENTRE_CLEAR' \
    ./tools/server_smoke.sh > /tmp/wg_smoke_out.txt 2>&1 || {
        echo "FAIL: the server smoke run itself failed"; tail -20 /tmp/wg_smoke_out.txt; exit 1; }

grep -q 'Placed "interregnum:shrine"' /tmp/wg_smoke_out.txt || {
    echo "FAIL: the shrine feature did not place on flat ground"
    grep -A3 'place feature' /tmp/wg_smoke_out.txt; exit 1; }

missing=""
for marker in A_CENTRE_CARVED B_CORNER_PAVING C_STELE_STANDS D_CENTRE_CLEAR; do
    grep -q "$marker" "$LOG" || missing="$missing $marker"
done
if [ -n "$missing" ]; then
    echo "FAIL: the shrine placed but is missing:$missing"
    exit 1
fi
echo "OK: shrine places on flat ground with carved centre, paving, a standing stele"
echo "    and clear air above the centre"
