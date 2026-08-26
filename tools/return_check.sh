#!/bin/bash
# The way home.
#
# WORLD.md: "Travel between systems is only by ferry." Until this increment that was a
# one-way sentence: four crossing laws, four destinations, and no law whose destination
# was the overworld -- so a player who sailed to a god's world could hop to another god's
# world forever and never get back. Not a design decision; a missing half.
#
# THE RETURN IS NOT A FIFTH LAW, and the reason is the interesting part. `Law` refuses a
# law with no rules and says why: "a crossing that refuses nothing is not a law" -- a
# checklist that can never refuse anything can never be seen to be broken. So a home law
# would need something to refuse, and the overworld has nobody left to refuse it. Every
# other checklist is a god's policy about its own world; inventing one for the world whose
# god this player killed would be inventing an authority the fiction has spent the whole
# game removing. And WORLD.md says what a checklist is FOR -- "teaches each world's rule
# before arrival" -- and the overworld's rule is the one the player already lives under.
#
# So the return leg is a mail service returning a vessel to the depot it left, and it
# needs only a record of where the ferry came from. Which a mail service would have.
#
# WHAT IS BEING PROVED:
#
#   * a hull that sailed out comes back to the EXACT coordinates it left from
#   * and the world it left is empty while it is away    (without this, "it is at home"
#                                                         is satisfied by a hull that was
#                                                         copied rather than moved)
#   * a keel that never sailed is refused, by name       ("no return leg on file" -- the
#                                                         answer a desk gives, and the
#                                                         only honest one)
#   * a ferry that has gone home has no second leg       (the record is spent, not kept)
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/ret.log; }

# The ferry is somewhere on the Quiet One's dock and this file does not know where: the
# pad sits on that world's own surface, which is terrain, not a constant. So the return is
# asked for at every height in a band, guarded on there actually being a keel there --
# exactly one of these can fire, and it locates the ferry the way a person would, by
# looking for it. Hard-coding the y would be hard-coding a fact about worldgen.
# The ferry is somewhere on the Quiet One's dock and this file does not know where: the pad
# sits on that world's own surface, which is terrain, not a constant. So each thing that
# has to happen at the dock is asked for at every height in a band, guarded on what is
# actually there -- exactly one height can fire, and it locates the dock the way a person
# would, by looking for it. Hard-coding the y would be hard-coding a fact about worldgen.
SEND_HOME=""      # send whatever ferry is on the landing back where it came from
PLANT=""          # put a NEW keel on the empty landing, as a player would
ASK_PLANTED=""    # and ask that one to go home
for y in $(seq 40 120); do
    up=$((y + 1))
    SEND_HOME="$SEND_HOME
execute in interregnum:unresponsive if block 0 $y 0 interregnum:ferry_keel run interregnum ferry home 0 $y 0"
    PLANT="$PLANT
execute in interregnum:unresponsive if block 0 $y 0 minecraft:polished_andesite run setblock 0 $up 0 interregnum:ferry_keel replace"
    ASK_PLANTED="$ASK_PLANTED
execute in interregnum:unresponsive if block 0 $y 0 minecraft:polished_andesite run interregnum ferry home 0 $up 0"
done

COMMANDS="forceload add -32 -32 31 31
execute in interregnum:unresponsive run forceload add -16 -16 47 47
wait 4
setblock 4 100 4 interregnum:ferry_keel replace
setblock 5 100 4 minecraft:oak_planks replace
interregnum claim record 4 100 4 5 100 4
setblock 20 100 4 interregnum:ferry_keel replace
setblock 21 100 4 minecraft:oak_planks replace
interregnum claim record 20 100 4 21 100 4
setblock 28 100 4 interregnum:ferry_keel replace
setblock 29 100 4 minecraft:oak_planks replace
interregnum claim record 28 100 4 29 100 4
execute if block 4 100 4 interregnum:ferry_keel run say SETUP_KEEL
execute if block 5 100 4 minecraft:oak_planks run say SETUP_HULL
say OUTBOUND
interregnum ferry sail 4 100 4 quiet_one
execute if block 4 100 4 minecraft:air run say ORIGIN_EMPTIED
execute if block 5 100 4 minecraft:air run say ORIGIN_HULL_GONE
say SECOND_FERRY
execute in interregnum:unresponsive run forceload add 16 16 47 47
interregnum ferry sail 20 100 4 quiet_one 30 120 30
say NEVER_SAILED
interregnum ferry home 28 100 4
say HOMEBOUND
$SEND_HOME
say SECOND_LEG
interregnum ferry home 4 100 4
say PLANT_NEW
$PLANT
say ASK_NEW
$ASK_PLANTED
say PROBES
execute if block 4 100 4 interregnum:ferry_keel run say KEEL_HOME
execute if block 5 100 4 minecraft:oak_planks run say HULL_HOME
execute if block 20 100 4 interregnum:ferry_keel run say WRONG_FERRY_CAME_BACK" \
    LOG=/tmp/ret.log timeout 900 ./tools/server_smoke.sh > /tmp/ret.txt 2>&1 \
    || { tail -25 /tmp/ret.txt; fail "the run did not complete"; }

# --- the setup, before anything that rests on it ----------------------------
mark SETUP_KEEL || fail "no keel was placed -- there is no ferry for any of this to be about"
mark SETUP_HULL || fail "no hull was attached to the keel, so the crossing below moves one block and proves nothing about a vessel"

# --- it left ----------------------------------------------------------------
# Asserted before the return. Without it, "the ferry is at home" is equally satisfied by
# a ferry that never went anywhere, which is the most likely way for this to be wrong.
grep -q 'ferry=sailed law=quiet_one' /tmp/ret.txt || {
    grep -oE 'ferry=[a-z]+[a-z= :_0-9]*' /tmp/ret.txt | head -5 || true
    fail "the outbound crossing did not happen, so nothing below is about a return"; }
mark ORIGIN_EMPTIED || \
    fail "the keel is still at its origin after sailing. The hull was COPIED rather than moved, and 'it came home' below would be satisfied by a ferry that never left"
mark ORIGIN_HULL_GONE || \
    fail "the hull is still at its origin after sailing, so the crossing left a duplicate behind"

# --- a keel that never sailed has no way home -------------------------------
grep -q 'ferry=refused reason=no return leg on file' /tmp/ret.txt || {
    grep -oE 'ferry=[a-z]+[a-z= :_0-9]*' /tmp/ret.txt | head -8 || true
    fail "a keel that has never sailed was given a way home anyway. Either the record is being invented, or every keel shares one"; }

# --- it came back, to ITS OWN coordinates -----------------------------------
# TWO ferries sailed to the Quiet One's world, from two different origins, before either
# came back. That is what makes this an assertion about the RETURN ADDRESS rather than
# about the existence of one: a record keyed by anything coarser than the individual keel
# -- by the world, say -- lets the second departure overwrite the first, and the ferry
# that comes home lands at somebody else's coordinates. The first version of this file
# sailed one ferry and could not tell the difference.
grep -q 'ferry=returned to=minecraft:overworld' /tmp/ret.txt || {
    grep -oE 'ferry=[a-z]+[a-z= :_0-9]*' /tmp/ret.txt | tail -6 || true
    fail "the ferry did not return. WORLD.md says travel between systems is only by ferry, so a ferry that cannot come back makes every god's world a place a player goes once and stays"; }
mark KEEL_HOME || {
    grep -oE '[A-Z_]{4,}' /tmp/ret.log | sort -u || true
    fail "the keel is not back at the coordinates it left from. Coming back to SOMEWHERE is not coming back, and a mail service that returns a vessel to the wrong depot has lost it"; }
mark HULL_HOME || \
    fail "the keel came home without its hull -- the return moved the keel and left the vessel behind"
if mark WRONG_FERRY_CAME_BACK; then
    fail "the returning ferry landed at the OTHER ferry's origin. Two vessels sailed to one world from two places, and the return address is being kept per world rather than per keel -- so the second departure overwrote the first, and this hull has been delivered to somebody else's dock"
fi

# --- and the leg is spent ----------------------------------------------------
# Asked of a keel a player has just PUT DOWN on the empty landing, which is the hazard
# the deletion exists for: a stale record attached to a position would hand the next
# thing standing there a return address to a stranger's coordinates, and teleport their
# build across worlds. Asking the same ferry twice cannot see this -- a departure
# overwrites its own key -- which is why the first version of this assertion passed
# against a `Voyages` that never deleted anything.
grep -q 'ferry=refused reason=no return leg on file' <(sed -n '/ASK_NEW/,/PROBES/p' /tmp/ret.txt) || {
    sed -n '/ASK_NEW/,/PROBES/p' /tmp/ret.txt | grep -E 'ferry=' | head -4 || true
    fail "a keel placed by hand on a landing a ferry had already left from was given that ferry's return address. The record is meant to be spent on use; a stale one attached to a position teleports whatever is standing there to a stranger's dock"; }

echo
printf "OK: a ferry comes back to its own coordinates and not another ferry's, leaves nothing\n    behind when it goes, and a keel with no leg on file is told so\n"
