#!/bin/bash
# Moor: fix a thing where it is, against any push.
#
# WORLD.md, locked: "The exact opposite of Lighten: fix a thing where it is, against any
# push. NOT WATER, NOT PISTONS, NOT THE ANCHORITE'S OWN LAW."
#
# THREE NAMED FORCES, ONE RULE. The list looks like three mechanisms and is one: water
# pushes entities, a piston pushes what stands in front of it, and the Anchorite's law
# lifts a falling block -- which is an entity too. So a moored thing is an entity fixed in
# place, and "its position does not change" refuses all three without knowing what any of
# them is.
#
# THE THIRD CLAUSE IS THE ONE WORTH THE SPELL. In the Anchorite's world unanchored things
# rise AND THEY DO NOT STOP -- the ferry's boarding notice has promised it since before
# that world existed, and a falling block that gets away climbs past the build height and
# is gone. Moor is the school's answer to the law of the god that teaches it.
#
# WHAT IS ASSERTED:
#   * it cannot be cast untaught;
#   * A MOORED FALLING BLOCK DOES NOT RISE in the Anchorite's world;
#   * THE CONTROL -- an identical falling block, in the identical world, twenty blocks away
#     and not moored -- does rise, and keeps rising. Without it, "the moored one stayed"
#     is equally satisfied by a world where the god's law had stopped working;
#   * A MOORED THING IS NOT PUSHED BY WATER, and an unmoored one in the same current is;
#   * and the mooring is reported held.
#
# Everything here is measured with `execute if entity` at a POSITION, because what the
# spell claims is about position and nothing else.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/moor.log; }

W=eeeeeeee-5555-4555-8555-555555555555
D=interregnum:mass_authority

# --- the Anchorite's own law ------------------------------------------------
# THE FALLING BLOCKS ARE SUMMONED, NOT DROPPED, and the difference cost this check a run.
# `setblock sand` over air does become a FallingBlockEntity -- on a scheduled block tick,
# not in the same tick -- so the cast had to wait for it. One second later the Anchorite's
# law had already carried it eight blocks up, well outside the spell's reach of five, and
# the cast truthfully reported that there was nothing there. `summon` adds the entity
# synchronously, so the very next command in the same tick finds it where it was put.
#
# In this world a loose falling block rises instead of falling. The moored one is cast on
# at once; the control twenty blocks away is left alone. Both are then looked for at their
# ORIGINAL position a few seconds later, and the control is looked for again far above it.
#
# --- water -------------------------------------------------------------------
# A source block beside a dropped item pushes it along the flow. Run in the overworld,
# because the Anchorite's world would be lifting the item as well and the two forces would
# be indistinguishable from one probe.
COMMANDS="execute in $D run forceload add -16 -16 47 47
forceload add -16 -16 31 31
wait 4

say UNTAUGHT
execute in $D run interregnum cast moor $W 4 150 4
interregnum learn $W weight

say MOORING
execute in $D run summon minecraft:falling_block 4.5 150 4.5 {BlockState:{Name:\"minecraft:sand\"},Tags:[\"anchor\"],Time:1}
execute in $D run summon minecraft:falling_block 24.5 150 4.5 {BlockState:{Name:\"minecraft:sand\"},Tags:[\"loose\"],Time:1}
execute in $D run interregnum cast moor $W 4 150 4
wait 4

execute in $D positioned 4 150 4 if entity @e[tag=anchor,distance=..3] run say MOORED_STAYED
execute in $D positioned 24 150 4 if entity @e[tag=loose,distance=..3] run say CONTROL_STAYED
execute in $D positioned 24 200 4 if entity @e[tag=loose,distance=..90] run say CONTROL_ROSE

say WATER
fill 0 -61 0 12 -61 12 minecraft:stone replace
fill 0 -60 0 12 -60 12 minecraft:air replace
summon minecraft:item 2.5 -60 6.5 {Tags:[\"anchored\"],Item:{id:\"minecraft:stone\",count:1}}
summon minecraft:item 2.5 -60 10.5 {Tags:[\"adrift\"],Item:{id:\"minecraft:stone\",count:1}}
wait 1
interregnum cast moor $W 2 -60 6
setblock 0 -60 6 minecraft:water replace
setblock 0 -60 10 minecraft:water replace
wait 6

execute positioned 2 -60 6 if entity @e[tag=anchored,distance=..1.5] run say ANCHORED_HELD
execute positioned 2 -60 10 if entity @e[tag=adrift,distance=..1.5] run say ADRIFT_HELD" \
    LOG=/tmp/moor.log timeout 900 ./tools/server_smoke.sh > /tmp/mr.txt 2>&1 \
    || { tail -25 /tmp/mr.txt; fail "the run did not complete"; }

casts=$(grep -oE 'cast=moor took=[a-z]+ subject=[a-z:_]* frayed=[0-9]+ refused=[a-z ]*moored=[0-9]+' /tmp/mr.txt || true)
[ -n "$casts" ] || {
    grep -oE 'cast=moor[^"]*' /tmp/mr.txt | head -3 || true
    tail -20 /tmp/mr.txt
    fail "the moor cast produced no answer at all"; }
untaught=$(echo "$casts" | head -1)
onsand=$(echo "$casts" | sed -n 2p)

# --- untaught, like every other spell ----------------------------------------
echo "$untaught" | grep -q 'refused=unlearned' || {
    echo "  casting before being taught: $untaught"
    fail "somebody who had never been taught Weight moored a thing"; }

# --- the cast found the falling block ----------------------------------------
echo "$onsand" | grep -q 'took=true' || {
    echo "  casting on the falling sand: $onsand"
    fail "the cast found nothing to moor at the sand's position. Everything below is about a spell that was never cast"; }
echo "$onsand" | grep -qE 'moored=[1-9]' || \
    fail "the cast reported success and the world holds no mooring ('$onsand')"

# --- the moored block did not rise -------------------------------------------
# WHAT THIS PROVES, AND WHAT IT CANNOT. Two things keep a moored block down: MoorEvents
# pins its position every tick, and Anchorite.lift refuses to lift a moored thing at all.
# The pin alone is enough -- removing the exception in the law was watched, and this check
# stayed green -- so what is asserted below is the OUTCOME, not which mechanism produced
# it. The exception survives as insurance against the order two `EntityTickEvent.Pre`
# subscribers happen to run in, which is not a contract; see the note in Anchorite.lift and
# docs/LESSONS.md. Saying which half a green run covers is cheaper than a later reader
# assuming it covered both.
mark MOORED_STAYED || {
    grep -oE '(MOORED|CONTROL|ANCHORED|ADRIFT)_[A-Z]+' /tmp/moor.log | sort -u || true
    fail "a moored falling block is not where it was moored. WORLD.md's third named force is the Anchorite's own law, and this is the world where that law runs -- if the mooring loses to it, the clause is false in the only place it is about"; }

# --- THE CONTROL: the same block, unmoored, is gone --------------------------
# Without this the whole check is satisfied by a world where the god's law stopped working,
# which would look identical from the moored block's side.
if mark CONTROL_STAYED; then
    fail "the UNMOORED sand is also still where it was dropped. The Anchorite's law is not lifting anything in this world, so the moored block staying put proves nothing at all"
fi
mark CONTROL_ROSE || {
    grep -oE 'CONTROL_[A-Z]+' /tmp/moor.log | sort -u || true
    fail "the unmoored sand neither stayed nor was found rising. If it was deleted rather than lifted, the comparison is void -- check that the column above it is clear"; }

# --- water ---------------------------------------------------------------------
mark ANCHORED_HELD || {
    fail "a moored item was carried off by a current. 'Not water' is the first of the three forces WORLD.md names"; }
if mark ADRIFT_HELD; then
    fail "the UNMOORED item did not move either, so the water is not pushing anything and the moored one proves nothing. Check that the source block reached it"
fi

echo
printf "OK: a moored falling block stays where it was in the world whose law would take it,\n    an identical one twenty blocks away is gone, and a current moves the item that was\n    not moored and not the one that was\n"
