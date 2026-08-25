#!/bin/bash
# Does a woken statue actually put a Warden in the world?
#
# This is the mechanism that makes `WARDEN_CONTACT` -- and therefore band 2 --
# reachable by PLAYING rather than by command. Until it existed the entity had two
# scenes, a tether and a renderer, and nothing whatsoever created one.
#
# The rules being asserted, all of which are design decisions rather than plumbing:
#
#   * an UNWOKEN statue posts nobody -- before the death these are garden ornaments
#   * a WOKEN statue posts exactly one Warden
#   * a statue whose call is already answered does not post a second
#   * a posted Warden is NOT persistence-required, so it can stand down; the statue
#     is the permanent thing
#   * tearing a woken statue down costs the breaker with the Wardenate
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
# `|| true` on every dump: under set -e -o pipefail a dump that matches nothing kills
# the script before the fail message it exists to explain. See docs/LESSONS.md #23.
want() { grep -qF "$2" "$1" || { echo "--- looked for: $2"; grep -E 'posted=' "$1" || true; fail "$3"; }; }

SEL='@e[type=interregnum:warden]'

# `wait` after the forceload, then a marker probe: a statue posts an ENTITY, and an
# entity added before the chunk's entity storage has arrived is accepted and then
# invisible to every selector for the rest of the run (docs/LESSONS.md #22).
COMMANDS="forceload add -32 -32 31 31
wait 5
summon minecraft:marker 8 -60 8
execute if entity @e[type=minecraft:marker,limit=1] run say E_CHUNK_TAKES_ENTITIES
kill @e[type=minecraft:marker]
setblock 8 -60 8 interregnum:warden_statue[facing=north,woken=false] replace
interregnum warden post 8 -60 8
say A_AFTER_UNWOKEN
execute positioned 0 -60 0 run interregnum record deicide
interregnum warden post 8 -60 8
say B_AFTER_WOKEN
data get entity @e[type=interregnum:warden,limit=1]
interregnum warden post 8 -60 8
say C_AFTER_SECOND_SWEEP" \
    LOG=/tmp/wp.log timeout 2000 ./tools/server_smoke.sh > /tmp/wp.txt 2>&1 \
    || { tail -25 /tmp/wp.txt; fail "the run did not complete"; }

grep -q E_CHUNK_TAKES_ENTITIES /tmp/wp.log || {
    echo "  A marker summoned into the chunk was invisible to @e, so any Warden a"
    echo "  statue posted would be invisible too. Lengthen the wait; do not delete"
    echo "  the probe."
    fail "the chunk was still loading when the statues were swept"; }

# The setup, before anything that depends on it. A sweep that found no statue at all
# would satisfy "nobody was posted" perfectly (docs/LESSONS.md #15).
want /tmp/wp.txt 'Changed the block' "the statue was never placed, so no sweep saw one"

# --- an unwoken statue is a garden ornament --------------------------------
# The deicide has not happened yet. `postAround` refuses on the chapter data, and
# even if it did not the statue is not woken. Both gates, one assertion: nobody.
unwoken=$(sed -n '1,/A_AFTER_UNWOKEN/p' /tmp/wp.txt | grep -c 'posted the Warden\|posted=1' || true)
[ "$unwoken" = "0" ] || { grep -n 'posted' /tmp/wp.txt | head -10 || true
    fail "a statue posted a Warden before the god was dead"; }

# --- a woken statue posts exactly one ---------------------------------------
want /tmp/wp.txt 'posted=1' "a woken statue posted nobody -- band 2 is still unreachable by playing"
posts=$(grep -c 'posted a Warden at' /tmp/wp.log || true)
[ "$posts" = "1" ] || { grep -n 'posted a Warden at\|nowhere to post' /tmp/wp.log | head || true
    fail "the statue posted $posts Warden(s) across two sweeps, want exactly 1"; }

# --- and does not post a second once answered -------------------------------
# The third sweep runs with a Warden already standing there. If `alreadyAnswered` is
# wrong this breeds them, one per tick, forever -- which is the failure that would
# actually hurt somebody's server.
after=$(sed -n '/B_AFTER_WOKEN/,$p' /tmp/wp.txt | grep -c 'posted=1' || true)
[ "$after" = "0" ] || { grep -n 'posted=' /tmp/wp.txt || true
    fail "a statue whose call was already answered posted another Warden"; }

# --- the statue is permanent; the Warden is not -----------------------------
# A posted Warden must be able to stand down, or every statue on a long-running
# server accretes an immortal mob. This is the one assertion that would silently
# stop being true if `posted` ever failed to survive NBT.
# Read off the whole-entity dump rather than a path: the key is namespaced, so a
# bare `data get entity <sel> interregnum:posted` path does not parse at all -- it
# fails as a command, which reads exactly like a missing flag.
want /tmp/wp.txt '"interregnum:posted": 1b' \
    "the posted flag did not survive onto the entity, so it will never stand down"
want /tmp/wp.txt 'PersistenceRequired: 0b' \
    "a posted Warden is persistence-required -- statues would accrete immortal Wardens"

echo
echo "OK: woken statues post a Warden, once, and it is free to stand down"
