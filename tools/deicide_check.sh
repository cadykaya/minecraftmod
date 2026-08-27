#!/bin/bash
# The moment the god dies, and what the world does about it.
#
# Asserts the whole beat rather than just the flag:
#   * before, the world turns (advance_time is true) and is DORMANT;
#   * the deicide moves it to VIGIL and STOPS THE SUN -- the day cycle was the
#     god's, and nobody is left to turn it;
#   * a second deicide is a no-op, because a world can only lose its god once;
#   * and both survive a restart, because otherwise the catastrophe un-happens
#     the first time an operator reboots.
#
# NB: the gamerule is `advance_time` in 26.x. `doDaylightCycle` was renamed along
# with most of the gamerule set.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }

echo "1/2 fresh world: before, during and after"
COMMANDS='gamerule advance_time
interregnum status
interregnum record deicide
interregnum status
interregnum record deicide
gamerule advance_time' LOG=/tmp/deicide1.log ./tools/server_smoke.sh > /tmp/d1.txt 2>&1 \
    || { cat /tmp/d1.txt; fail "run 1 did not complete"; }

grep -q 'chapter=DORMANT' /tmp/d1.txt || fail "the world did not start dormant"
grep -q 'set to: true'    /tmp/d1.txt || fail "the sun was not moving before the deicide"
grep -q 'recorded DEICIDE; chapter=VIGIL' /tmp/d1.txt || fail "the deicide did not reach VIGIL"
grep -q 'already recorded DEICIDE' /tmp/d1.txt \
    || { grep -A8 'commands' /tmp/d1.txt; fail "a second deicide was NOT a no-op"; }
grep -q 'set to: false'   /tmp/d1.txt || fail "the sun did not stop"

echo "2/2 restart: the catastrophe must not un-happen"
KEEP_WORLD=1 COMMANDS='interregnum status
gamerule advance_time' LOG=/tmp/deicide2.log ./tools/server_smoke.sh > /tmp/d2.txt 2>&1 \
    || { cat /tmp/d2.txt; fail "run 2 did not complete"; }
grep -q 'chapter=VIGIL' /tmp/d2.txt || fail "the chapter reverted on restart"
grep -q 'set to: false'  /tmp/d2.txt || fail "the sun started moving again after a restart"

echo
echo "OK: the god dies once, the sun stops, and neither un-happens on restart"
