#!/bin/bash
# Does the dead god reach the right person, once?
#
# The Haunt is a [LOCKED] headline beat -- the owner asked for the dead god to haunt
# the one who killed it -- and it is delivered by exactly one event: a player waking
# up. A headless server has no sleeping players, so every decision lives in
# `TheHaunt.offer` where a command can reach it, and the event handler is three lines.
# What is asserted here is the gate, which is the whole feature:
#
#   * nothing haunts anybody while the god is alive
#   * ONLY the killer -- this is the ghost's private conversation and an admin with
#     good intentions must not be able to hand it to somebody else
#   * once, because that is what "first dream-audience" means
#   * and a deferral must not silently spend it
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
want() { grep -qF "$2" "$1" || { echo "--- looked for: $2"; grep -E 'haunt=' "$1" || true; fail "$3"; }; }
count() { grep -cF "$2" "$1" || true; }

K=55555555-5555-4555-8555-555555555555
O=66666666-6666-4666-8666-666666666666

COMMANDS="forceload add -32 -32 31 31
interregnum haunt dream $K
execute positioned 0 -60 0 run interregnum record deicide $K
interregnum haunt dream $O
interregnum talk start interregnum:shrine_keeper $K
interregnum haunt dream $K
interregnum talk leave $K
interregnum haunt dream $K
interregnum talk show $K
interregnum talk say $K refuse
interregnum talk say $K no
interregnum haunt dream $K
interregnum haunt dream $K force
interregnum regard $K
interregnum regard $O
interregnum haunt dream notauuid" \
    LOG=/tmp/haunt.log timeout 2000 ./tools/server_smoke.sh > /tmp/hc.txt 2>&1 \
    || { tail -25 /tmp/hc.txt; fail "the run did not complete"; }

want /tmp/hc.txt 'haunt=NO_GHOST' \
    "something haunted a player while the god was still alive"
want /tmp/hc.txt 'haunt=NOT_THE_KILLER' \
    "the ghost's private conversation was offered to somebody who did not kill it"

# A player already at a table is not evicted for the dream -- and, the part that
# matters, the deferral must NOT record the milestone. The OPENED below comes after
# a BUSY, so if BUSY had spent it this would read ALREADY and the killer would have
# lost the scene to a coincidence of timing.
want /tmp/hc.txt 'haunt=BUSY' \
    "the dream trampled a conversation the player was already in"
[ "$(count /tmp/hc.txt 'haunt=OPENED')" = "2" ] \
    || { grep -n 'haunt=' /tmp/hc.txt;
         fail "expected exactly two openings (one after the deferral, one forced); a BUSY that consumed the milestone would leave only one"; }

want /tmp/hc.txt 'show| ...  Executor. Sit.' \
    "the dream opened but the ghost's scene is not what the player was shown"

want /tmp/hc.txt 'haunt=ALREADY' \
    "the first dream-audience can happen more than once"
want /tmp/hc.txt 'haunt=refused reason=not a player id' \
    "a non-player id was accepted"

# The dream's choices land on THE_GHOST, which is the one relationship a deicide
# leaves open -- and which nobody but the killer has at all. Refusing at `wake` (-5)
# and again at `estate` (-10) puts them at -15.
want /tmp/hc.txt 'killer=true' "the killer's record does not know they are the killer"
want /tmp/hc.txt 'THE_GHOST=WARY(-15)' \
    "the dream's answers did not move the one relationship the killer still has"
# The bystander has NO RECORD AT ALL, which is stronger than having an empty one.
# Being offered the dream and refused must not bring a file into existence -- an
# institution's opinion of somebody it has never dealt with is an absence, not a
# nought, and `peek` exists precisely so reading cannot create one.
grep -qF 'regard=none' /tmp/hc.txt \
    || { grep -E 'regard=' /tmp/hc.txt;
         fail "a record was created for a bystander who was merely offered the dream and refused"; }

echo
echo "OK: the dead god reaches its killer, once, and nobody else ever"
