#!/bin/bash
# The Anchorite's third, and the only spell in the kit with a middle.
#
# WORLD.md, locked: "Weight (Anchorite): ... LOFT -- make a small structure weightless and
# carry it." Carrying is picking a thing up, walking, and putting it down, so the spell is
# two casts and the distance is whatever the caster's own legs covered in between.
#
# WHAT IS ASSERTED:
#   * nothing is known by default -- an untaught caster is refused, which is also the only
#     way to be sure the successful casts below are evidence of a rule rather than a default;
#   * the walk takes what a player BUILT and stops dead at what they did not. A stone
#     block touching the shed is still there afterwards, which is the whole reason this
#     spell can be cast on a building standing on a hillside;
#   * SMALL is a real cap: a hundred-and-twenty-five-block cube is refused WHOLE and is
#     still standing afterwards. A structure that came up in halves would read as the
#     spell eating part of a building;
#   * one at a time, and a set-down refuses rather than overwrites;
#   * A LOAD SURVIVES A RESTART. This is the reason the store is a SavedData and every
#     other spell's state is not: while a shed is held, those blocks are not in the world,
#     so losing the map does not lose a spell effect, it loses the shed.
#
#     THIS TAKES THREE SERVER RUNS AND THE REASON IS WORTH READING. A two-run version was
#     written first, lifting in run one and setting down in run two, and it passed with
#     `setDirty()` deleted from the store -- so it was not testing what it said. Minecraft
#     writes only dirty saved data, but `computeIfAbsent` marks a NEWLY CREATED instance
#     dirty via `set()`, so on a world that has never had this file the whole object is
#     written at shutdown whatever the code does afterwards. The store has to already
#     exist on disk before a modification to it is a real test. So: run one creates it,
#     run two lifts into it, run three finds the shed still in hand.
#   * and a load may only be set down in the world it was lifted from. WORLD.md locks
#     "travel between systems is only by ferry" -- a workshop walked through a portal
#     would be a second way to do the one thing the ferry exists for.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" "$2"; }
want() { grep -qF "$2" "$1" || { echo "--- looked for: $2"; grep -oE 'cast=loft [a-zA-Z=0-9 ]+' "$1" | head -12 || true; fail "$3"; }; }

WHO=cccccccc-cccc-4ccc-8ccc-cccccccccccc

# ---------------------------------------------------------------------------
# Run one: build, and refuse the ways it should be refused. NO LIFT HAPPENS HERE --
# see the note above: this run exists to bring the saved-data file into being, so that
# run two's lift is a modification of a store that was loaded from disk rather than
# created in memory a moment earlier.
# ---------------------------------------------------------------------------
# The shed is 3x2x3 = 18 oak planks. `setblock`/`fill` is not a player placing a block and
# Claims only records real placements, so the claim is recorded by command -- the same
# arrangement ferry_check.sh uses, and for the same reason.
COMMANDS="forceload add -16 -16 79 79
execute in interregnum:mass_authority run forceload add -16 -16 15 15
wait 3
fill 0 -60 0 2 -59 2 minecraft:oak_planks replace
interregnum claim record 0 -60 0 2 -59 2
setblock 3 -60 0 minecraft:stone replace
fill 0 -60 32 1 -60 33 minecraft:oak_planks replace
interregnum claim record 0 -60 32 1 -60 33
fill 48 -60 48 52 -56 52 minecraft:oak_planks replace
interregnum claim record 48 -60 48 52 -56 52
interregnum cast loft lift $WHO 0 -60 0
interregnum learn $WHO weight
interregnum cast loft lift $WHO 48 -60 48
execute if block 50 -58 50 minecraft:oak_planks run say TOO_LARGE_INTACT
interregnum cast loft lift $WHO 64 -60 64" \
    LOG=/tmp/loft1.log timeout 900 ./tools/server_smoke.sh > /tmp/lc1.txt 2>&1 \
    || { tail -25 /tmp/lc1.txt; fail "the first run did not complete"; }

# --- nothing is known by default -------------------------------------------
want /tmp/lc1.txt 'cast=loft lift ok=false blocks=0 frayed=0 refused=UNLEARNED' \
    "an untaught caster lifted a building. WORLD.md locks schools as learned in their worlds, and if casting works by default the successful casts below are evidence of nothing"

# --- SMALL is a cap, and it refuses whole ----------------------------------
want /tmp/lc1.txt 'cast=loft lift ok=false blocks=0 frayed=0 refused=TOO_LARGE' \
    "a hundred-and-twenty-five-block cube was carried. 'Small' is the locked word and the cap is what makes it mean anything"
mark TOO_LARGE_INTACT /tmp/loft1.log || \
    fail "the over-large cube lost blocks to a cast that was refused. A structure that comes up in halves reads as the spell eating part of a building, which is the one thing it most needs never to look like"

want /tmp/lc1.txt 'cast=loft lift ok=false blocks=0 frayed=0 refused=NOTHING_BUILT' \
    "a cast on bare ground lifted something. The spell is for things people built"

# ---------------------------------------------------------------------------
# Run two: the same world, a new server, and NOW the lift. The store on disk is loaded
# rather than created, so writing into it is a real write.
# ---------------------------------------------------------------------------
COMMANDS="forceload add -16 -16 79 79
execute in interregnum:mass_authority run forceload add -16 -16 15 15
wait 3
interregnum cast loft lift $WHO 0 -60 0
execute if block 0 -60 0 minecraft:air run say ORIGIN_EMPTIED
execute if block 2 -59 2 minecraft:air run say ORIGIN_CORNER_EMPTIED
execute if block 3 -60 0 minecraft:stone run say NEIGHBOUR_LEFT
interregnum claim at 0 -60 0
interregnum cast loft lift $WHO 0 -60 32
interregnum cast loft place $WHO 0 -60 32
execute in interregnum:mass_authority run interregnum cast loft place $WHO 0 100 0" \
    KEEP_WORLD=1 LOG=/tmp/loft2.log timeout 900 ./tools/server_smoke.sh > /tmp/lc2.txt 2>&1 \
    || { tail -25 /tmp/lc2.txt; fail "the second run did not complete"; }

# The school was learned in run one, so this is also the grimoire surviving a restart --
# if it had not, this lift would read UNLEARNED rather than working.
want /tmp/lc2.txt 'cast=loft lift ok=true blocks=18' \
    "the shed was not lifted whole -- eighteen blocks were claimed and the cast did not take that many"
want /tmp/lc2.txt 'carrying=1' \
    "the caster is not carrying anything after a successful lift"
mark ORIGIN_EMPTIED /tmp/loft2.log || \
    fail "the shed was lifted and is also still standing where it was, which is a duplication rather than a carry"
mark ORIGIN_CORNER_EMPTIED /tmp/loft2.log || \
    fail "the far corner of the shed was left behind. A partial lift is worse than none: it takes a building apart"

# --- THE CONTROL: the walk stops at what nobody built -----------------------
# Without this, "it lifted the shed" is equally satisfied by a spell that lifts a cube of
# world -- and on flat ground with the shed sitting on it, that would look identical.
mark NEIGHBOUR_LEFT /tmp/loft2.log || \
    fail "a stone block touching the shed came up with it. The walk is supposed to stop dead at what a player did not place, which is the whole reason this can be cast on a building standing on a hillside"
grep -qF 'claimed=false chunkPlacements=0' /tmp/lc2.txt || {
    grep -oE 'claimed=[a-z]+ chunkPlacements=[0-9]+' /tmp/lc2.txt || true
    fail "the lifted shed's blocks are still on the claim ledger. The ledger is what keeps the unraveling off a player's work, and an entry for a block that is no longer there would spare air"; }

# --- one at a time, and a set-down does not eat what is there ----------------
want /tmp/lc2.txt 'cast=loft lift ok=false blocks=0 frayed=0 refused=HANDS_FULL' \
    "a second structure was picked up while the first was still being carried"
want /tmp/lc2.txt 'cast=loft place ok=false blocks=0 frayed=0 refused=BLOCKED' \
    "a shed was set down through a building that was already standing there, which deletes it without a word"

# --- and it stays in the world it was lifted from ---------------------------
want /tmp/lc2.txt 'cast=loft place ok=false blocks=0 frayed=0 refused=WRONG_WORLD' \
    "a structure was carried into another god's world. WORLD.md locks travel between systems as ferry-only, and a spell that walks a workshop through a portal is a second way to do the one thing the ferry exists for"

# ---------------------------------------------------------------------------
# Run three: the same world again. The shed must still be in hand.
# ---------------------------------------------------------------------------
COMMANDS="forceload add -16 -16 79 79
wait 3
interregnum cast loft lift $WHO 0 -60 32
interregnum cast loft place $WHO 20 -60 20
execute if block 20 -60 20 minecraft:oak_planks run say LANDED
execute if block 22 -59 22 minecraft:oak_planks run say LANDED_CORNER
interregnum claim at 20 -60 20
interregnum cast loft place $WHO 40 -60 40" \
    KEEP_WORLD=1 LOG=/tmp/loft3.log timeout 900 ./tools/server_smoke.sh > /tmp/lc3.txt 2>&1 \
    || { tail -25 /tmp/lc3.txt; fail "the third run did not complete"; }

# The refused lift is the persistence assertion. A load kept in memory would be gone with
# the server that held it, the hands would be empty, and this would read ok=true.
want /tmp/lc3.txt 'cast=loft lift ok=false blocks=0 frayed=0 refused=HANDS_FULL' \
    "the shed was not still in hand after a restart. It is saved with the world precisely because those blocks are not in the world while they are held -- losing the store does not lose a spell effect, it loses the building"

want /tmp/lc3.txt 'cast=loft place ok=true blocks=18' \
    "the carried shed was not set down whole after the restart"
want /tmp/lc3.txt 'carrying=0' \
    "the caster is still carrying something after setting it down"
mark LANDED /tmp/loft3.log || \
    fail "nothing arrived where the shed was set down"
mark LANDED_CORNER /tmp/loft3.log || \
    fail "the shed arrived without its far corner, so the offsets did not survive the save"
grep -qF 'claimed=true chunkPlacements=18' /tmp/lc3.txt || {
    grep -oE 'claimed=[a-z]+ chunkPlacements=[0-9]+' /tmp/lc3.txt || true
    fail "the set-down shed is not on the claim ledger. It is still somebody's work, and without the claim the unraveling is free to eat it where it now stands"; }

want /tmp/lc3.txt 'cast=loft place ok=false blocks=0 frayed=0 refused=CARRYING_NOTHING' \
    "a second set-down produced something out of empty hands"

echo
echo "OK: a small building is picked up, survives a restart in hand, and is set down whole -- and the ground it stood on stays where it is"
