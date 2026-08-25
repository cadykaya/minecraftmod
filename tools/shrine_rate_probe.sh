#!/bin/bash
# How often does a shrine actually get built, on real terrain?
#
# Two numbers decide shrine density and only one of them is known:
#
#   * the RarityFilter says one attempt per 90 chunks, by construction;
#   * ShrineFeature then REJECTS ground with more than MAX_RELIEF of relief, and
#     nobody has ever measured how much natural terrain that throws away.
#
# If the acceptance rate is 10%, the real density is 1 shrine per 900 chunks and
# a player will never meet one -- "a structure nobody finds is a structure you did
# not build" (docs/WORLDGEN.md). This measures it instead of guessing.
#
# Method: generate a real (non-flat) world at a fixed seed, then attempt a
# placement at the centre of every chunk in a grid and count the answers. This
# isolates terrain acceptance from rarity, which is the unknown half.
set -euo pipefail
cd "$(dirname "$0")/.."

GRID=${GRID:-8}                  # GRID x GRID chunks
SEED=${SEED:-interregnum}
LOG=/tmp/interregnum-shrine-rate.log

half=$(( GRID / 2 ))
lo=$(( -half * 16 ))
hi=$(( (GRID - half) * 16 - 1 ))

cmds="forceload add $lo $lo $hi $hi"
points=0
for (( cx = -half; cx < GRID - half; cx++ )); do
  for (( cz = -half; cz < GRID - half; cz++ )); do
    x=$(( cx * 16 + 8 ))
    z=$(( cz * 16 + 8 ))
    cmds="$cmds
place feature interregnum:shrine $x 64 $z"
    points=$(( points + 1 ))
  done
done

echo "probing $points chunk centres on seed '$SEED' (real terrain)..."
LEVEL_TYPE=minecraft:normal LEVEL_SEED="$SEED" COMMANDS="$cmds" LOG="$LOG" \
    ./tools/server_smoke.sh > /tmp/shrine_rate_out.txt 2>&1 || true

placed=$(grep -c 'Placed "interregnum:shrine"' /tmp/shrine_rate_out.txt || true)
failed=$(grep -c 'Failed to place feature' /tmp/shrine_rate_out.txt || true)
total=$(( placed + failed ))

if [ "$total" -eq 0 ]; then
    echo "FAIL: no placement attempts were answered -- the probe measured nothing."
    tail -20 /tmp/shrine_rate_out.txt
    exit 1
fi

pct=$(( placed * 100 / total ))
echo
echo "attempts:   $total"
echo "built:      $placed"
echo "refused:    $failed  (terrain too uneven)"
echo "acceptance: ${pct}%"
echo
python3 - "$placed" "$total" <<'PY'
import sys, json
placed, total = int(sys.argv[1]), int(sys.argv[2])
acc = placed / total if total else 0
# Read the rarity out of the GENERATED data rather than keeping a second copy
# here. A probe carrying its own idea of a number it is meant to be measuring
# against will quietly report the old answer forever after the number changes.
d = json.load(open("src/generated/resources/data/interregnum/worldgen/placed_feature/shrine.json"))
RARITY = next(m["chance"] for m in d["placement"] if m["type"] == "minecraft:rarity_filter")
print(f"rarity filter (from generated data): 1 attempt per {RARITY} chunks")
if acc == 0:
    print("=> no shrine would ever generate. MAX_RELIEF is too strict.")
    raise SystemExit
per = RARITY / acc
print(f"=> one shrine per {per:.0f} chunks of overworld")
# A player walking normally covers very roughly 1 chunk of NEW ground every few
# seconds; 20 chunks/minute is a deliberately rough working figure, stated so the
# number below is read as an order of magnitude and not as a measurement.
print(f"=> roughly {per/20:.0f} minutes of walking to the first one (at ~20 new chunks/min)")
PY
