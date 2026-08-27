#!/bin/bash
# The first portal, and the first place in the mod where a spell is a DOOR.
#
# WORLD.md, locked: "Each god's portal is opened by the school that god teaches" -- and
# for the Anchorite, "a shaft you do not build but LET GO INTO. It takes anything
# unanchored, which there means everything. Going down, into the place where down does
# not hold."
#
# Nothing here builds a frame or places a block. A Lighten zone cast in the Anchorite's
# own world means that within its footprint weight is the god's to decide, and what the
# god decides is that an unanchored thing keeps going. So the whole portal is: cast the
# spell you were taught in that god's world, and stop holding on.
#
# WHAT IS ASSERTED, and every one of them is a RELATIONSHIP rather than a fact about one
# probe, for the reason crossing_check.sh gives at length -- a pig that vanished satisfies
# "it is not here any more" for a dozen reasons that have nothing to do with a portal:
#
#   * a thing that lets go inside the shaft ARRIVES IN THE UNDER-LAYER. Not "is gone":
#     found, alive, in a named world it could not have walked to;
#
# EVERY PROBE BELOW CARRIES A POSITION AND A `distance`, and that is not tidiness. A bare
# `execute in <dimension> if entity @e[tag=x]` DOES NOT SCOPE THE SELECTOR TO THAT
# DIMENSION -- it matched a pig standing in the overworld. The first version of this file
# was written that way and every one of its six probes fired, in both worlds, including
# against a build where the portal had been deliberately broken. `distance=` forces the
# positional test that makes the level matter. See docs/LESSONS.md.
#   * THE CONTROL -- the identical thing, in the identical world, falling the identical
#     distance twenty blocks away with no zone over it, does NOT go anywhere. Without
#     this, every assertion below is equally satisfied by a mod that teleports everything
#     that falls;
#   * THE SECOND CONTROL -- something standing on the ground INSIDE the shaft does not go
#     either. The shaft takes the unanchored, and a door that took whatever stood near it
#     would be a door, not this one;
#   * IT COMES BACK. The same act on the far side returns you, because down does not hold
#     there and the shaft carries you the way that world's weight goes. A one-way portal
#     into an under-layer is a trap;
#   * and the shaft is REPORTED SHUT in the overworld with a zone open in it. The school
#     is cast everywhere; the door is this god's. If Lighten opened a shaft wherever it
#     was cast, every god's system would be reachable from every other one without a
#     ferry, and the crossing that teaches each world's law before you arrive would be
#     optional.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/descent.log; }

WHO=99999999-9999-4999-8999-999999999999

# The shaft is the zone's FOOTPRINT taken through the height of the world, so a thing
# dropped high above the cast point is inside it the whole way down. Lighten's radius is
# 5; the control column at x=40 is far outside it and inside the same forceloaded region.
#
# `interregnum:mass_authority` has a floor at y=0 and terrain around y=60-100, so a thing
# released at y=200 has well over the two seconds of falling the shaft asks for. The
# grounded one is placed on a block put down for it, INSIDE the footprint, so the only
# difference between it and the one that goes through is whether it is holding on.
#
# THE PROBES ARE DROPPED ITEMS AND THE FIRST VERSION USED `pig ... {NoAI:1b}`, which is
# worth the paragraph. `NoAI` does not merely switch off the wandering: `travel()` is
# gated on `isEffectiveAi()`, so a NoAI mob never moves, never has gravity applied, and
# NEVER REPORTS `onGround` -- its default is false and nothing ever sets it. So the
# "falling" pig hung motionless at y=200 and went through because it was permanently
# unsupported, and the "standing" pig never landed on the block under it and went through
# for the same reason. Both markers looked like the portal working. A dropped item falls,
# lands, reports the ground, and does not wander off the platform, which is every property
# this needs and none it does not.
COMMANDS="execute in interregnum:mass_authority run forceload add -16 -16 47 47
execute in interregnum:mass_authority_lower run forceload add -16 -16 47 47
forceload add -16 -16 15 15
wait 4

interregnum learn $WHO weight
execute in interregnum:mass_authority run interregnum cast lighten $WHO 4 150 4
execute in interregnum:mass_authority run interregnum shaft 4 20 4
execute in interregnum:mass_authority run interregnum shaft 40 20 4

say RELEASED
execute in interregnum:mass_authority run summon minecraft:item 4.5 200 4.5 {Tags:[\"faller\"],Item:{id:\"minecraft:stone\",count:1}}
execute in interregnum:mass_authority run summon minecraft:item 40.5 200 4.5 {Tags:[\"control\"],Item:{id:\"minecraft:stone\",count:1}}
execute in interregnum:mass_authority run setblock 4 190 6 minecraft:stone replace
execute in interregnum:mass_authority run summon minecraft:item 4.5 191.1 6.5 {Tags:[\"standing\"],Item:{id:\"minecraft:stone\",count:1}}
wait 6

say COUNTED
execute in interregnum:mass_authority_lower positioned 4 128 4 if entity @e[tag=faller,distance=..400] run say FALLER_ARRIVED
execute in interregnum:mass_authority positioned 4 128 4 if entity @e[tag=faller,distance=..400] run say FALLER_STAYED
execute in interregnum:mass_authority positioned 4 128 4 if entity @e[tag=control,distance=..400] run say CONTROL_STAYED
execute in interregnum:mass_authority_lower positioned 4 128 4 if entity @e[tag=control,distance=..400] run say CONTROL_WENT
execute in interregnum:mass_authority positioned 4 128 4 if entity @e[tag=standing,distance=..400] run say STANDING_STAYED
execute in interregnum:mass_authority_lower positioned 4 128 4 if entity @e[tag=standing,distance=..400] run say STANDING_WENT

say RETURNING
execute in interregnum:mass_authority_lower run interregnum cast lighten $WHO 4 150 4
execute in interregnum:mass_authority_lower run interregnum shaft 4 20 4
wait 6
execute in interregnum:mass_authority positioned 4 128 4 if entity @e[tag=faller,distance=..400] run say FALLER_CAME_BACK

say ELSEWHERE
interregnum cast lighten $WHO 4 90 4
interregnum shaft 4 90 4" \
    LOG=/tmp/descent.log timeout 900 ./tools/server_smoke.sh > /tmp/dc.txt 2>&1 \
    || { tail -25 /tmp/dc.txt; fail "the run did not complete"; }

# --- the shaft is a fact about a place --------------------------------------
# Read before anything falls, so that a failure below can be told apart from a shaft
# that was never open. `head -1` and `sed -n 2p`: the two probes are the same command at
# two positions, so they are told apart by ORDER rather than by anything in the text.
shafts=$(grep -oE 'shaft=[A-Z]+ layer=[A-Z]+ letting_go=[0-9]+' /tmp/dc.txt || true)
[ -n "$shafts" ] || { tail -20 /tmp/dc.txt; fail "the shaft command produced no answer at all"; }
inside=$(echo "$shafts" | head -1)
outside=$(echo "$shafts" | sed -n 2p)

echo "$inside" | grep -q 'shaft=OPEN layer=SURFACE' || \
    fail "a Lighten zone cast in the Anchorite's world did not open a shaft under it ('$inside'). The portal IS the zone's footprint taken through the height of the world; if it is not open here nothing below is about a portal"
echo "$outside" | grep -q 'shaft=SHUT' || \
    fail "the shaft is reported open thirty-six blocks outside the field that opened it ('$outside'). The footprint has to have exactly the zone's edge, or it is a door you can fall through from outside the spell"

# --- it takes the thing that let go ------------------------------------------
mark FALLER_ARRIVED || {
    grep -oE '(FALLER|CONTROL|STANDING)_[A-Z_]+' /tmp/descent.log | sort -u || true
    fail "a pig that fell for six seconds inside the shaft is not in the under-layer. Either the count never completed, or the crossing did not happen -- and the next assertion says which"; }
if mark FALLER_STAYED; then
    fail "the pig is in the under-layer AND still in the Anchorite's world. A cross-dimension teleport removes the old entity and builds a new one; two of them means something copied it instead of moving it"
fi

# --- THE CONTROL: the same fall, twenty blocks away, goes nowhere ------------
# Without this the whole check is equally satisfied by a mod that teleports anything
# that falls in this world, which would need no spell and no zone at all.
mark CONTROL_STAYED || {
    fail "the control pig -- same world, same tick, same 200-block fall, thirty-six blocks outside the field -- is not where it was dropped. If it fell out of the world the two probes are not comparable; check the terrain height at x=40"; }
if mark CONTROL_WENT; then
    fail "a pig falling OUTSIDE any zone went through to the under-layer. The shaft is not the spell's footprint, it is the whole world -- so the portal is not opened by the school and WORLD.md's one rule with four faces has become a property of the dimension"
fi

# --- THE SECOND CONTROL: standing inside it is not letting go ----------------
mark STANDING_STAYED || \
    fail "the pig standing on a block inside the shaft is not there any more. It was never falling, so whatever moved it did not measure whether it was holding on"
if mark STANDING_WENT; then
    fail "a pig STANDING STILL inside the shaft went through. WORLD.md's portal is one you let go into -- if standing near it is enough, the door is a trigger volume and the spell is decoration on it"
fi

# --- and it comes back -------------------------------------------------------
# The same act on the far side. Down does not hold there, so the shaft supplies the rise
# the surface gets from gravity for free -- which means this half also proves the buoying
# works, and it is the only assertion here that could not be satisfied by a teleport.
mark FALLER_CAME_BACK || {
    grep -oE 'shaft=[A-Z]+ layer=[A-Z]+ letting_go=[0-9]+' /tmp/dc.txt | tail -2 || true
    fail "the pig could not get out of the under-layer. A one-way portal into a place with no ferry law naming it is a trap, and the return trip is the half that proves the shaft carries things the way that world's weight goes rather than merely deleting them downward"; }

# --- and the door is this god's ---------------------------------------------
home=$(echo "$shafts" | tail -1)
echo "$home" | grep -q 'shaft=SHUT layer=NONE' || {
    echo "  overworld probe: $home"
    fail "a Lighten zone opened a shaft in the OVERWORLD. The school is cast everywhere and the door is the Anchorite's; a portal that opened wherever its school was cast would make every god's system reachable from every other without a ferry, and the crossing that teaches each world's law before you arrive would be optional"; }

echo
printf "OK: a thing that lets go inside a lightened shaft in the Anchorite's world arrives\n    below, the same fall outside it does not, standing in it is not letting go, and the\n    same act brings you back up\n"
