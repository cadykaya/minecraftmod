#!/bin/bash
# The Hearth-Turner's door: always present, open at one hour only.
#
# WORLD.md, locked: "Always present, open AT ONE HOUR ONLY -- and the sky is fixed, so you
# cannot wait for it. You have to MAKE the hour happen. Opened with Weather / Rewind --
# the school is the clock."
#
# THE HOUR IS NOT A TIME OF DAY. This god's world has a stopped sky, so there is no hour up
# there to wait for -- the locked line says exactly that. What this god's school moves is
# the AGE OF THINGS: the Turning's table runs stone_bricks -> cracked -> mossy, Weather
# walks it forward and Rewind walks it back, and the doorway keeps one stage.
#
# The open hour is the MIDDLE link, and that is the whole reason both verbs are named: it
# is the only stage reachable from both directions. Copper would have been the obvious
# material and is deliberately not used -- the ageing table's own comment rules it out,
# because vanilla oxidises copper everywhere and the two clocks would fight.
#
# WHAT IS ASSERTED:
#   * a frame of FRESH stone brick is shut. The door is not the shape, it is the shape at
#     an age;
#   * WEATHERING IT OPENS IT -- six casts of the school's forward verb and nothing else
#     changed;
#   * something that walks in CROSSES, into the Hearth-Turner's under-layer;
#   * A DOOR IS STAMPED WHERE IT ARRIVES. This portal is six blocks somebody built, and
#     nothing on the far side built anything, so a one-way version would strand whoever
#     carried no stone brick;
#   * THE CONTROL -- an identical thing, in the identical world, standing in an identical
#     doorway whose frame is one stage YOUNG -- does not cross. Without it the whole check
#     is satisfied by a portal that is any hole in any wall;
#   * THE SECOND CONTROL -- a frame at the hour with one post missing -- does not either;
#   * and REWIND OPENS IT TOO, from the other side of the hour. A frame aged past into
#     mossy is shut, and the school's backward verb brings it back. That is what makes the
#     clock have two hands rather than one.
#
# Every entity probe carries a position and a `distance`: a bare `execute in <dimension> if
# entity @e[tag=x]` does not scope the selector to that dimension. docs/LESSONS.md #43.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/hour.log; }

WHO=55555555-5555-4555-8555-555555555555
D=interregnum:temporal_authority

# A doorway is a hole in a wall: posts east and west, a sill, a lintel, and a two-block
# gap. Three of them are built, twenty blocks apart, out of three different stages.
#
#   x=4   the real one -- fresh brick, weathered up to the hour
#   x=14  the control  -- left fresh, never cast on
#   x=24  the broken one -- at the hour, one post short
#   x=34  the rewind one -- mossy, brought back down to the hour
#
# ALL FOUR SIT INSIDE THE FORCELOADED REGION, and the spacing is not free choice.
# `forceload add` takes BLOCK coordinates, not chunk coordinates: `-16 -16 47 47` is
# blocks -16..47. The first version of this file put the rewind frame at x=64, outside it,
# and the failure was the quietest kind -- `fill` and `setblock` load a chunk on demand, so
# every BLOCK query answered correctly and `interregnum doorway` reported the door open,
# while the item standing in it NEVER TICKED, because a loaded chunk is not a ticking one.
# The door worked. Nothing was ever going to walk through it.
#
# `fill` builds the solid block and then the gap is carved, which is both fewer commands
# and closer to what a player does with a wall.
build() { echo "execute in $D run fill $1 100 4 $1 103 4 $2 replace
execute in $D run fill $(($1-1)) 100 4 $(($1-1)) 103 4 $2 replace
execute in $D run fill $(($1+1)) 100 4 $(($1+1)) 103 4 $2 replace
execute in $D run fill $1 101 4 $1 102 4 minecraft:air replace"; }

# Weather/Rewind one frame: two posts of two, a sill and a lintel. Six casts, which is the
# frame, and is why six blocks all at one stage reads as a thing somebody did.
age() { for c in "$(($1-1)) 101 4" "$(($1-1)) 102 4" "$(($1+1)) 101 4" "$(($1+1)) 102 4" "$1 100 4" "$1 103 4"; do
    echo "execute in $D run interregnum cast $2 $WHO $c"; done; }

COMMANDS="execute in $D run forceload add -16 -16 47 47
execute in interregnum:temporal_authority_lower run forceload add -16 -16 47 47
forceload add -16 -16 15 15
wait 4

interregnum learn $WHO turning
$(build 4 minecraft:stone_bricks)
$(build 14 minecraft:stone_bricks)
$(build 24 minecraft:cracked_stone_bricks)
$(build 34 minecraft:mossy_stone_bricks)
execute in $D run setblock 23 101 4 minecraft:air replace

execute in $D run interregnum doorway 4 101 4
say MAKING_THE_HOUR
$(age 4 weather)
$(age 34 rewind)
execute in $D run interregnum doorway 4 101 4
execute in $D run interregnum doorway 14 101 4
execute in $D run interregnum doorway 24 101 4
execute in $D run interregnum doorway 34 101 4

say WALKING_IN
execute in $D run summon minecraft:item 4.5 101.5 4.5 {Tags:[\"walker\"],Item:{id:\"minecraft:stone\",count:1}}
execute in $D run summon minecraft:item 14.5 101.5 4.5 {Tags:[\"young\"],Item:{id:\"minecraft:stone\",count:1}}
execute in $D run summon minecraft:item 24.5 101.5 4.5 {Tags:[\"broken\"],Item:{id:\"minecraft:stone\",count:1}}
execute in $D run summon minecraft:item 34.5 101.5 4.5 {Tags:[\"returned\"],Item:{id:\"minecraft:stone\",count:1}}
wait 4

execute in interregnum:temporal_authority_lower positioned 4 128 4 if entity @e[tag=walker,distance=..400] run say WALKER_ARRIVED
execute in $D positioned 4 128 4 if entity @e[tag=walker,distance=..400] run say WALKER_STAYED
execute in $D positioned 14 128 4 if entity @e[tag=young,distance=..400] run say YOUNG_STAYED
execute in interregnum:temporal_authority_lower positioned 14 128 4 if entity @e[tag=young,distance=..400] run say YOUNG_WENT
execute in $D positioned 24 128 4 if entity @e[tag=broken,distance=..400] run say BROKEN_STAYED
execute in interregnum:temporal_authority_lower positioned 24 128 4 if entity @e[tag=broken,distance=..400] run say BROKEN_WENT
execute in interregnum:temporal_authority_lower positioned 34 128 4 if entity @e[tag=returned,distance=..400] run say RETURNED_ARRIVED
execute in $D positioned 34 128 4 if entity @e[tag=returned,distance=..400] run say RETURNED_STAYED

say COUNTING_STAMP
execute in interregnum:temporal_authority_lower run fill 2 0 2 6 255 6 minecraft:air replace minecraft:cracked_stone_bricks
say COUNTED" \
    LOG=/tmp/hour.log timeout 900 ./tools/server_smoke.sh > /tmp/hr.txt 2>&1 \
    || { tail -25 /tmp/hr.txt; fail "the run did not complete"; }

doors=$(grep -oE 'doorway=[A-Z]+ leads=[a-z_]+ standing=[0-9]+' /tmp/hr.txt || true)
[ -n "$doors" ] || { tail -20 /tmp/hr.txt; fail "the doorway command produced no answer at all"; }
fresh=$(echo "$doors" | head -1)
made=$(echo "$doors" | sed -n 2p)
control=$(echo "$doors" | sed -n 3p)
broken=$(echo "$doors" | sed -n 4p)
rewound=$(echo "$doors" | sed -n 5p)

# --- a frame of fresh brick is not a door ------------------------------------
echo "$fresh" | grep -q 'doorway=SHUT' || {
    echo "  before any casting: $fresh"
    fail "a doorway built of fresh stone brick is already open. The door is not the SHAPE, it is the shape AT AN AGE -- and if any wall with a hole in it is a portal then the hour is decoration and WORLD.md's 'open at one hour only' is false"; }
echo "$fresh" | grep -q 'leads=temporal_authority_lower' || \
    fail "the Hearth-Turner's surface does not know where its doors lead ('$fresh')"

# --- and the school opens it -------------------------------------------------
echo "$made" | grep -q 'doorway=OPEN' || {
    echo "  before: $fresh"
    echo "  after six casts of Weather: $made"
    fail "six casts of Weather on the frame did not open it. That is the locked rule -- each god's portal is opened by the school that god teaches -- and nothing else changed between these two lines"; }

# --- THE CONTROL: one stage young, twenty blocks away, still shut ------------
echo "$control" | grep -q 'doorway=SHUT' || {
    echo "  the un-aged frame: $control"
    fail "an identical doorway that was never cast on is open. Either the hour is not being checked, or casting on one frame opened all of them"; }

# --- THE SECOND CONTROL: at the hour, and one post short --------------------
echo "$broken" | grep -q 'doorway=SHUT' || {
    echo "  the incomplete frame: $broken"
    fail "a frame with a post missing is open. Six blocks all at one stage is what makes this a thing somebody built rather than a coincidence of terrain, and a frame that does not have to be whole is a much smaller coincidence"; }

# --- and REWIND opens it from the other side of the hour --------------------
# The half that makes the clock have two hands. A frame aged past the hour into mossy is
# shut, and the school's backward verb is the only thing that brings it back.
echo "$rewound" | grep -q 'doorway=OPEN' || {
    echo "  the mossy frame after six casts of Rewind: $rewound"
    fail "six casts of Rewind on a frame aged PAST the hour did not bring it back to the hour. The open stage is the middle of the chain precisely so that both of the school's verbs reach it; if only Weather works, half the locked line is decoration and a frame that has aged out is lost for ever"; }

# --- walking in crosses -------------------------------------------------------
mark WALKER_ARRIVED || {
    grep -oE '(WALKER|YOUNG|BROKEN|RETURNED)_[A-Z_]+' /tmp/hour.log | sort -u || true
    fail "a thing that entered an open doorway is not in the Hearth-Turner's under-layer"; }
if mark WALKER_STAYED; then
    fail "it is in the under-layer AND still on the surface -- something copied it rather than moving it"
fi
if mark RETURNED_STAYED; then
    fail "the thing put in the REWOUND doorway is still standing in it. The door reports open and nothing moved -- and before blaming the portal, check that x=34 is inside the forceloaded region, because a loaded-but-not-ticking chunk answers every block query and runs no entity ticks at all"
fi
mark RETURNED_ARRIVED || \
    fail "the thing that walked into the REWOUND doorway did not cross. The door reports open and does not work, so 'open' and 'passable' are two different conditions and only one of them is being checked"

# --- and the shut ones take nobody -------------------------------------------
for pair in "YOUNG young frame that was never aged to the hour" \
            "BROKEN frame at the hour with a post missing"; do
    set -- $pair
    tag=$1
    mark "${tag}_STAYED" || fail "the control in the $tag doorway is not where it was put -- if it fell or was destroyed the comparison is void"
    if mark "${tag}_WENT"; then
        fail "a thing standing in a SHUT doorway crossed anyway. Whatever is moving things is not reading the frame, and every 'open' assertion above is about a report rather than about a door"
    fi
done

# --- a door is stamped where it arrives --------------------------------------
# This portal is six blocks somebody built, and nothing on the far side built anything. A
# one-way version strands whoever crossed without stone brick in their pockets.
stamped=$(sed -n '/COUNTING_STAMP/,/COUNTED/p' /tmp/hr.txt \
    | grep -oE 'filled [0-9]+ block' | grep -oE '[0-9]+' | head -1 || true)
stamped=${stamped:-0}
echo "  the far side holds $stamped block(s) of the hour around the arrival column"
[ "$stamped" -ge 6 ] || \
    fail "no doorway was built where the crossing arrived ($stamped blocks of cracked stone brick found). The way back is six blocks of a material the far side has no reason to contain, so a portal that does not stamp one is a one-way trip into a world no ferry law names"

echo
printf "OK: a fresh frame is shut, six casts of Weather open it and six of Rewind bring a\n    mossy one back to the same hour, walking in crosses, a frame one stage young or one\n    post short takes nobody, and the far side gets a door of its own\n"
