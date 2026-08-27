#!/bin/bash
# Do conversations leave a mark, and is it the right person's mark?
#
# The rule this exists to protect is the one that makes ensemble dialogue mean
# anything: **each participant is judged on what THEY said, not on what the table
# decided.** Without it every player ends up with the initiator's record, and the
# only choice that ever mattered was whoever clicked first -- the ensemble system
# becomes decoration, and it is the feature the owner asked for by name.
#
# The other half is the scar. A deicide caps every surviving god permanently, and
# those caps have to survive a restart: if they came back as MAX the atrocity would
# launder itself overnight, and the difference between a scar and a debt is exactly
# that it does not.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
want() { grep -qF "$2" "$1" || { echo "--- looked for: $2"; grep -E 'regard=' "$1" || true; fail "$3"; }; }

A=11111111-1111-4111-8111-111111111111
B=22222222-2222-4222-8222-222222222222
K=33333333-3333-4333-8333-333333333333

echo "1/2 the table decides one thing; two people are on record for two"
# Both vote to admit it, then split: A accepts the keeper's "authorised holder"
# label, B refuses it. `accept_label` carries VILLAGES +5 / WARDENATE -4;
# `refuse_label` carries VILLAGES +3 / WARDENATE +2. The table resolves to
# accept_label -- so if effects followed the WINNER, both would read -4.
COMMANDS="forceload add -32 -32 31 31
interregnum talk start interregnum:shrine_keeper $A+$B
interregnum talk say $A admit
interregnum talk say $B admit
interregnum talk say $A accept_label
interregnum talk say $B refuse_label
interregnum regard $A
interregnum regard $B
interregnum regard nobody
execute positioned 0 -60 0 run interregnum record deicide $K
interregnum regard $K" \
    LOG=/tmp/regard1.log timeout 2000 ./tools/server_smoke.sh > /tmp/rc1.txt 2>&1 \
    || { tail -25 /tmp/rc1.txt; fail "run 1 did not complete"; }

want /tmp/rc1.txt 'talk=ADVANCED chose=accept_label' \
    "the table did not resolve to accept_label, so the split below proves nothing"

# THE assertion. Same node, same moment, opposite records.
want /tmp/rc1.txt 'killer=false WARDENATE=WARY(-4) VILLAGES=WARY(5)' \
    "the player who accepted the label does not carry what they said"
want /tmp/rc1.txt 'killer=false WARDENATE=WARY(2) VILLAGES=WARY(3)' \
    "the DISSENTER carries the winning option's consequences instead of their own -- ensemble dialogue is decoration if this is wrong"

# A non-killer has no ghost relationship at all. Printing a band would imply a
# nought where there is an absence.
want /tmp/rc1.txt 'THE_GHOST=none' \
    "a non-killer was given a relationship with the ghost"
want /tmp/rc1.txt 'regard=none reason=not a player id' \
    "a record was invented for something that is not a player"

# --- what the player is actually TOLD -------------------------------------------
#
# There is no karma bar, and there is also no silence: a band CROSSING is a
# relationship event and gets one line of text with no number in it. The distinction
# is the whole design, and both halves of it are asserted here because only one of
# them is obvious.
#
# The quiet half first. A and B both moved regard in that conversation -- -4, +5,
# +2, +3 -- and none of it crossed a band, so neither of them hears anything at all.
# If this ever starts firing, every conversation ends in a burst of notifications and
# the mod has grown the meter it exists to avoid. This is the assertion that keeps
# band notices from becoming a karma bar with a thesaurus.
for who in "$A" "$B"; do
    if grep -q "Regard crossing for $who" /tmp/regard1.log; then
        grep "Regard crossing for $who" /tmp/regard1.log
        fail "regard moved without crossing a band and the player was told about it anyway"
    fi
done

# The loud half. A deicide moves several institutions across bands at once, and the
# killer hears one line per institution -- which is how the mod says "you killed a
# god" without ever saying it: several people who have never spoken to you make up
# their minds in the same moment.
want /tmp/regard1.log "Regard crossing for $K: WARDENATE WARY -> RESENTED" \
    "the Wardenate crossed a band over a deicide and said nothing"
for god in VERDANT ANCHORITE HEARTH_TURNER QUIET_ONE; do
    want /tmp/regard1.log "Regard crossing for $K: $god WARY -> RESENTED" \
        "$god learned what the killer is and did not register an opinion"
done

# And the one that stays silent, which is the point rather than an omission. The
# deicide floors and caps every god who HEARS about it; the god who was killed is
# deliberately left where it was, because its opinion of its killer is the single
# relationship still open and the whole back half of the mod is spent on it
# (RegardState.recordDeicide, and the comment in Deicide). So at the moment of its
# death the ghost says nothing -- every other god in the world has an opinion and
# the only one with standing to judge you has not formed one yet.
#
# This assertion was originally written the other way round, expecting the ghost to
# bottom out with everyone else. It failed, and it was the assertion that was wrong.
if grep -q "Regard crossing for $K: THE_GHOST" /tmp/regard1.log; then
    grep "Regard crossing for $K: THE_GHOST" /tmp/regard1.log
    fail "the ghost passed judgement on its own killer at the moment of dying -- that relationship is supposed to be the one still open"
fi

# And every key that a live server emitted actually resolves. regard_lines_check.py
# proves the lang file is complete against a rule it writes out itself; this proves
# the RUNNING CODE builds the same keys, which two copies of a naming rule stop doing
# the moment one of them is edited.
python3 tools/regard_keys_check.py /tmp/regard1.log \
    || fail "a crossing was announced with a key that has no line behind it"

echo "2/2 the scar, and whether it survives the night"
# The gods write the killer off and cap them there. The Wardenate takes a flat hit.
# The VILLAGES do not move -- WORLD.md's four voices has them whispering *saint*,
# and mechanics that contradicted locked lore would flatten every village scene.
want /tmp/rc1.txt 'killer=true WARDENATE=RESENTED(-30)' \
    "the Wardenate did not file the killer"
want /tmp/rc1.txt 'VERDANT=RESENTED(-45)cap-10' \
    "a surviving god was not hit and capped by the deicide"
want /tmp/rc1.txt 'VILLAGES=WARY(0) VERDANT' \
    "the deicide dragged the villages down with the gods; the people are not the pantheon"

KEEP_WORLD=1 COMMANDS="forceload add -32 -32 31 31
interregnum regard $K
interregnum regard $A" \
    LOG=/tmp/regard2.log timeout 2000 ./tools/server_smoke.sh > /tmp/rc2.txt 2>&1 \
    || { tail -25 /tmp/rc2.txt; fail "run 2 did not complete"; }

want /tmp/rc2.txt 'killer=true' "the killer forgot they were the killer overnight"
# The ceiling is the whole point. A value that survives while its cap does not is
# worse than losing both: the record looks intact and the atrocity has quietly
# stopped costing anything.
want /tmp/rc2.txt 'VERDANT=RESENTED(-45)cap-10' \
    "the deicide's permanent ceilings did not survive a restart -- the scar became a debt"
want /tmp/rc2.txt 'WARDENATE=WARY(-4) VILLAGES=WARY(5)' \
    "an ordinary conversation's consequences did not survive a restart"

# A THIRD boot, because the bug this caught did not corrupt the record -- it DRIFTED
# it, by the size of the ceiling, every single time. One reload looks like rounding;
# it is the second that shows the record walking. A round trip is only proved by
# doing it twice.
KEEP_WORLD=1 COMMANDS="forceload add -32 -32 31 31
interregnum regard $K" \
    LOG=/tmp/regard3.log timeout 2000 ./tools/server_smoke.sh > /tmp/rc3.txt 2>&1 \
    || { tail -25 /tmp/rc3.txt; fail "run 3 did not complete"; }
want /tmp/rc3.txt 'VERDANT=RESENTED(-45)cap-10' \
    "the record drifted on a second reload -- it is being restored relative to itself"


echo
echo "OK: people carry what they said, and a deicide leaves a mark that keeps"
