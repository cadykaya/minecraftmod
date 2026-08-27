#!/bin/bash
# Is the heart reachable before the deicide, and unreachable after it?
#
# The second half is the one that matters and it is DETERMINISTIC: once the god is
# dead, `interregnum:god_lives` is false, so the probability of a heart is exactly
# zero. If a single one appears in N rolls, the uniqueness of the heart is broken
# and with it the whole premise -- there would be a second god to kill.
#
# The first half is probabilistic: at a 12% chance, P(no heart in 60 rolls) is
# about 0.05%, which is a flake rate worth accepting for the information. It is
# stated here rather than hidden so nobody later reads a red build as a real bug.
#
# Method: rebuild a chest at a fixed spot, roll the table into it, read it back.
# `/loot insert` + `/data get block` needs no player, which is the only reason any
# of this is checkable on a headless server.
set -euo pipefail
cd "$(dirname "$0")/.."

ROLLS=${ROLLS:-60}
POS="8 -59 8"

build_rolls() {
    # Clear to AIR first. `setblock <pos> chest` where a chest already stands is a
    # no-op, so without this the same chest survives every iteration and `loot
    # insert` keeps ADDING to it -- once a heart lands, every later read still sees
    # it. The first version of this probe did exactly that and reported 47 hearts in
    # 60 rolls of a 12% pool. The pass/fail was still correct; the NUMBER was
    # nonsense, and a nonsense number in a passing test is how a wrong rate ends up
    # quoted in a design document.
    local n=$1
    for (( i = 0; i < n; i++ )); do
        echo "setblock $POS minecraft:air replace"
        echo "setblock $POS minecraft:chest replace"
        echo "loot insert $POS loot interregnum:chests/shrine"
        echo "data get block $POS Items"
    done
}

echo "1/2 while the god lives: $ROLLS rolls, expect at least one heart"
echo "    (the count is also the measured rate -- it should land near 12%)"
COMMANDS="forceload add 0 0 15 15
$(build_rolls "$ROLLS")" LOG=/tmp/heart1.log ./tools/server_smoke.sh > /tmp/h1.txt 2>&1 \
    || { tail -20 /tmp/h1.txt; echo "FAIL: run 1 did not complete"; exit 1; }

before=$(grep -c 'interregnum:god_heart' /tmp/h1.txt || true)
echo "    hearts found: $before"
[ "$before" -gt 0 ] || {
    echo "FAIL: no heart in $ROLLS rolls while the god lives."
    echo "      At 12% that is a 1-in-2000 fluke -- far more likely the condition"
    echo "      or the pool is wrong. Check the generated loot table."
    exit 1; }

echo "2/2 after the deicide: $ROLLS rolls, expect exactly zero"
COMMANDS="forceload add 0 0 15 15
interregnum record deicide
$(build_rolls "$ROLLS")" LOG=/tmp/heart2.log ./tools/server_smoke.sh > /tmp/h2.txt 2>&1 \
    || { tail -20 /tmp/h2.txt; echo "FAIL: run 2 did not complete"; exit 1; }

after=$(grep -c 'interregnum:god_heart' /tmp/h2.txt || true)
echo "    hearts found: $after"
[ "$after" -eq 0 ] || {
    echo "FAIL: $after heart(s) appeared AFTER the god was already dead."
    echo "      The heart must be unique; a second one is a second god to kill."
    exit 1; }

echo
echo "OK: the heart is findable while the god lives and impossible afterwards"
