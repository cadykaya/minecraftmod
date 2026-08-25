#!/bin/bash
# Do the Warden statues open their eyes?
#
# Two paths and they need separate proof:
#   NEAR -- statues around the site wake at the instant the god dies.
#   FAR  -- statues in chunks that were not loaded wake when they load, so a player
#           who was underground climbs out and finds the one in their garden
#           already watching.
#
# The FAR path needs a statue in a chunk that is genuinely NOT loaded when the god
# dies, which is why this takes two server runs: run 1 places it and lets the world
# save, run 2 boots without that chunk, kills the god, and only then loads it.
#
# Every setup step is asserted. An earlier version of this test placed a statue at
# an unloaded position, got "That position is not loaded", never checked, and then
# proved nothing at all while looking like it had run.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }

echo "1/2 place statues -- near the site, and far away in its own chunk"
COMMANDS='forceload add -32 -32 31 31
forceload add 384 384 415 415
setblock 20 -60 20 interregnum:warden_statue[facing=north,woken=false] replace
setblock 400 -60 400 interregnum:warden_statue[facing=south,woken=false] replace
execute if block 20 -60 20 interregnum:warden_statue[woken=false] run say SETUP_NEAR_ASLEEP
execute if block 400 -60 400 interregnum:warden_statue[woken=false] run say SETUP_FAR_ASLEEP
forceload remove 384 384 415 415' LOG=/tmp/statue_a.log ./tools/server_smoke.sh > /tmp/sa.txt 2>&1 \
    || { tail -20 /tmp/sa.txt; fail "run 1 did not complete"; }

# Assert the SETUP, not just the outcome: setblock answers "That position is not
# loaded" and carries on, and a test built on a block that was never placed will
# happily report whatever the absence of that block implies.
grep -q 'SETUP_NEAR_ASLEEP' /tmp/statue_a.log || fail "the near statue was never placed"
grep -q 'SETUP_FAR_ASLEEP'  /tmp/statue_a.log || fail "the far statue was never placed"

echo "2/2 restart, kill the god, then load the far chunk"
KEEP_WORLD=1 COMMANDS='forceload add -32 -32 31 31
execute if block 20 -60 20 interregnum:warden_statue[woken=false] run say PRE_NEAR_ASLEEP
interregnum record deicide
execute if block 20 -60 20 interregnum:warden_statue[woken=true] run say NEAR_WOKE
execute if block 20 -60 20 interregnum:warden_statue[facing=north] run say NEAR_FACING_KEPT
forceload add 384 384 415 415
execute if block 400 -60 400 interregnum:warden_statue[woken=true] run say FAR_WOKE_ON_LOAD
execute if block 400 -60 400 interregnum:warden_statue[facing=south] run say FAR_FACING_KEPT' \
    LOG=/tmp/statue_b.log ./tools/server_smoke.sh > /tmp/sb.txt 2>&1 \
    || { tail -20 /tmp/sb.txt; fail "run 2 did not complete"; }

grep -q 'PRE_NEAR_ASLEEP' /tmp/statue_b.log \
    || fail "the statue was already awake before the deicide -- the test proves nothing"
grep -q 'NEAR_WOKE' /tmp/statue_b.log || fail "a statue at the site did NOT wake"
grep -q 'NEAR_FACING_KEPT' /tmp/statue_b.log || fail "waking rotated the statue"
grep -q 'FAR_WOKE_ON_LOAD' /tmp/statue_b.log \
    || fail "a statue in a chunk loaded AFTER the deicide did not wake"
grep -q 'FAR_FACING_KEPT' /tmp/statue_b.log || fail "waking on load rotated the statue"

echo
grep -oE '[0-9]+ Warden statue.*' /tmp/statue_b.log | head -1 | sed 's/^/    /'
echo
echo "OK: statues at the site wake with the god's death, statues elsewhere wake"
echo "    when their chunk loads, and neither turns around doing it"
