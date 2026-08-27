#!/bin/bash
# Does the interregnum survive a server restart?
#
# Saved data fails in the worst possible way: it works all session and is simply
# gone after a restart, so nobody finds out until a server has run for a week.
#
# FIVE runs, and the middle ones are the point. Minecraft's SavedDataStorage.set()
# marks freshly-CREATED data dirty, so anything mutated in the same session that
# created it saves whether or not the mod calls setDirty(). Data LOADED from disk
# is clean. So a check that only ever creates-then-mutates passes happily with
# setDirty() deleted -- this one did, until run 3 and 4 were added.
#
# Run 5 is the control: without it, "chapter=X" in run 2 proves nothing, because
# it could be a default, a leaked static, or a value the command invents.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }

run() {           # run <label> <keep-world 0|1> <commands> <outfile>
    local label=$1 keep=$2 cmds=$3 out=$4
    if [ "$keep" = "1" ]; then
        KEEP_WORLD=1 COMMANDS="$cmds" LOG="/tmp/persist_$label.log" \
            ./tools/server_smoke.sh > "$out" 2>&1 || { cat "$out"; fail "$label did not complete"; }
    else
        COMMANDS="$cmds" LOG="/tmp/persist_$label.log" \
            ./tools/server_smoke.sh > "$out" 2>&1 || { cat "$out"; fail "$label did not complete"; }
    fi
}

expect() {        # expect <outfile> <needle> <message>
    grep -q "$2" "$1" || { grep -A6 'commands' "$1" || true; fail "$3"; }
}

echo "1/5 fresh world: record the deicide"
run r1 0 'interregnum record deicide
interregnum status' /tmp/p1.txt
expect /tmp/p1.txt 'chapter=VIGIL' "recording DEICIDE did not reach VIGIL"

echo "2/5 restart: is it remembered?"
run r2 1 'interregnum status' /tmp/p2.txt
expect /tmp/p2.txt 'chapter=VIGIL' "chapter did NOT survive the restart"

echo "3/5 restart: mutate data that was LOADED, not created (the setDirty path)"
run r3 1 'interregnum record warden_contact
interregnum status' /tmp/p3.txt
expect /tmp/p3.txt 'chapter=ENFORCEMENT' "recording WARDEN_CONTACT did not reach ENFORCEMENT"

echo "4/5 restart: did the mutation of loaded data persist?"
run r4 1 'interregnum status' /tmp/p4.txt
expect /tmp/p4.txt 'chapter=ENFORCEMENT' \
    "a change made to LOADED saved data was lost on restart -- setDirty() missing?"

echo "5/5 control: a fresh world must be dormant again"
run r5 0 'interregnum status' /tmp/p5.txt
expect /tmp/p5.txt 'chapter=DORMANT' "a FRESH world was not dormant -- the other runs proved nothing"

echo
echo "OK: milestones persist across restarts, including changes to already-loaded"
echo "    saved data, and a fresh world starts dormant"
