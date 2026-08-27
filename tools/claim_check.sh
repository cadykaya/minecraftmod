#!/bin/bash
# Does the world remember what people built?
#
# This is the prerequisite for the unraveling. The crater gets away with a tag
# whitelist because it fires once at one spot; the unraveling runs forever over a
# whole world, and sooner or later a whitelist alone eats somebody's cobblestone
# wall on the grounds that cobblestone is natural.
#
# Three properties, each of which fails silently if wrong:
#   * claims are per-chunk and per-position, not global;
#   * they SURVIVE A RESTART -- an attachment that does not serialise looks perfect
#     all session and forgets every build overnight;
#   * a chunk past the cap saturates, which bounds memory and errs toward
#     protecting more.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }


echo "1/3 record, query, forget"
COMMANDS='forceload add -32 -32 31 31
interregnum claim at 5 -60 5
interregnum claim record 5 -60 5 7 -60 7
interregnum claim at 5 -60 5
interregnum claim at 20 -60 20
interregnum claim forget 6 -60 6 6 -60 6
interregnum claim at 6 -60 6
interregnum claim at 5 -60 5' LOG=/tmp/claim1.log ./tools/server_smoke.sh > /tmp/c1.txt 2>&1 \
    || { tail -20 /tmp/c1.txt; fail "run 1 did not complete"; }

grep -q 'claimed 9 position(s)' /tmp/c1.txt || fail "recording a 3x3x1 region did not claim 9 positions"
grep -q 'claimed=true chunkPlacements=9' /tmp/c1.txt || fail "a recorded position did not read back as claimed"
grep -q 'claimed=false chunkPlacements=0' /tmp/c1.txt \
    || fail "a position in an untouched chunk was reported claimed -- claims are leaking between chunks"
grep -q 'claimed=false chunkPlacements=8' /tmp/c1.txt \
    || fail "forget did not release the position (or released more than one)"
grep -q 'claimed=true chunkPlacements=8' /tmp/c1.txt \
    || fail "forgetting one position released its neighbours too"

echo "2/3 restart: is it remembered?"
KEEP_WORLD=1 COMMANDS='forceload add -32 -32 31 31
interregnum claim at 5 -60 5' LOG=/tmp/claim2.log ./tools/server_smoke.sh > /tmp/c2.txt 2>&1 \
    || { tail -20 /tmp/c2.txt; fail "run 2 did not complete"; }
grep -q 'claimed=true chunkPlacements=8' /tmp/c2.txt \
    || { grep -A3 'claim at' /tmp/c2.txt; fail "claims did NOT survive the restart"; }

echo "3/3 saturation: past the cap the whole chunk is theirs"
COMMANDS='forceload add -32 -32 31 31
interregnum claim record 0 -63 0 15 -45 15
interregnum claim at 3 -60 3
interregnum claim at 12 -50 12
interregnum claim forget 3 -60 3 3 -60 3
interregnum claim at 3 -60 3' LOG=/tmp/claim3.log ./tools/server_smoke.sh > /tmp/c3.txt 2>&1 \
    || { tail -20 /tmp/c3.txt; fail "run 3 did not complete"; }
grep -q 'chunkPlacements=4096' /tmp/c3.txt \
    || { grep -A2 'claim at' /tmp/c3.txt; fail "the chunk did not saturate at the cap"; }
# After saturation, forgetting must NOT re-open the chunk.
#
# Read the LAST `claimed=` reply specifically. A first version used `tail -3` on
# the whole output, which reads the smoke test's own epilogue rather than any
# command reply, and duly failed on correct behaviour.
last_claim=$(grep -oE 'claimed=(true|false)' /tmp/c3.txt | tail -1)
[ "$last_claim" = "claimed=true" ] \
    || fail "forget un-saturated a chunk ($last_claim) -- digging a hole must not make a place wilderness again"

echo
echo "OK: claims are per-position, survive restarts, and saturate safely"
