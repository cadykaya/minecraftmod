#!/bin/bash
# The Verdant's portal: a door you plant, and somebody else can fell.
#
# WORLD.md, locked: "You PLANT it and wait. It opens when mature and closes when cut: the
# only portal in the mod with a lifespan. Opened with Bridgeroot / Wildgrowth -- or
# patience, which is worse there."
#
# THERE IS NO TIMER, and that is the design rather than an omission. The obvious build is
# "planted at tick T, opens at T+N", and it would quietly delete the locked clause about
# patience: with a duration, the school does nothing that waiting does not. So maturity is
# a FACT ABOUT THE WORLD -- did the thing you planted become a tree -- which vanilla's own
# growth answers, Wildgrowth makes true now, and patience makes true eventually.
#
# WHAT IS ASSERTED:
#   * a sapling somebody planted is a door that is NOT open. The waiting is the feature;
#   * WILDGROWTH OPENS IT. The school's own verb, cast on the thing you planted, and the
#     next probe says the door is open. This is the whole locked rule -- each god's portal
#     is opened by the school that god teaches -- in one pair of lines;
#   * something that stands still under it GOES THROUGH, into the Verdant's under-layer;
#   * THE CONTROL -- the identical thing, in the identical world, standing perfectly still
#     the identical length of time twenty blocks from any door -- does not. Without this
#     the whole check is satisfied by a mod that teleports anything that stops moving;
#   * CUTTING IT CLOSES IT. Break the trunk and the doorway is shut, with nothing having
#     told the ledger: the state is read off blocks every time, so a felled tree stops
#     being a door in the instant it stops being a tree;
#   * and the ledger FORGETS the position, so a door cannot be re-opened by regrowing
#     something at a place the world still thinks is a portal.
#
# Every entity probe carries a position and a `distance`. A bare `execute in <dimension>
# if entity @e[tag=x]` does not scope the selector to that dimension -- see
# docs/LESSONS.md #43, which cost descent_check.sh a false pass.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/grove.log; }

WHO=77777777-7777-4777-8777-777777777777

# A sapling needs dirt under it or it pops off, and the Verdant's world is stone at the
# heights this works at. So the ground is built: dirt at y=100, sapling at y=101.
#
# The waiter stands ON THE GROUND, two blocks out from the trunk, and not at head height
# beside it. A tree is grown here on purpose and its canopy is not deterministic: the
# first version put the waiter at y=102 within the crown's reach, where a leaf placed by
# one roll of the tree's shape shouldered it a block sideways -- and moving is the one
# thing that resets this door's count. The check passed and failed on alternate runs.
#
# `setblock` is NOT how the sapling is planted. The portal ledger listens for a PLAYER
# placing one -- WORLD.md's verb is `plant`, and planting is a thing a person does -- and
# there is no player on a headless server. `interregnum plant` is the operator seam for
# exactly this, the same affordance `record deicide` is.
COMMANDS="execute in interregnum:green_authority run forceload add -16 -16 47 47
execute in interregnum:green_authority_lower run forceload add -16 -16 47 47
forceload add -16 -16 15 15
wait 4

interregnum learn $WHO verdancy
execute in interregnum:green_authority run fill 2 100 2 6 100 6 minecraft:dirt replace
execute in interregnum:green_authority run fill 2 101 2 6 104 6 minecraft:air replace
execute in interregnum:green_authority run setblock 4 101 4 minecraft:oak_sapling replace
execute in interregnum:green_authority run interregnum plant 4 101 4
execute in interregnum:green_authority run interregnum grove 4 101 4

say RIPENING
execute in interregnum:green_authority run interregnum cast wildgrowth $WHO 4 101 4
execute in interregnum:green_authority run interregnum grove 4 101 4

say STANDING
execute in interregnum:green_authority run fill 20 100 20 24 100 24 minecraft:dirt replace
execute in interregnum:green_authority run summon minecraft:item 6.5 101.1 6.5 {Tags:[\"waiter\"],Item:{id:\"minecraft:stone\",count:1}}
execute in interregnum:green_authority run interregnum grove 6 101 6
execute in interregnum:green_authority run summon minecraft:item 22.5 102 22.5 {Tags:[\"lonely\"],Item:{id:\"minecraft:stone\",count:1}}
wait 6

execute in interregnum:green_authority_lower positioned 4 128 4 if entity @e[tag=waiter,distance=..400] run say WAITER_ARRIVED
execute in interregnum:green_authority positioned 4 128 4 if entity @e[tag=waiter,distance=..400] run say WAITER_STAYED
execute in interregnum:green_authority positioned 22 128 22 if entity @e[tag=lonely,distance=..400] run say LONELY_STAYED
execute in interregnum:green_authority_lower positioned 22 128 22 if entity @e[tag=lonely,distance=..400] run say LONELY_WENT

say FELLING
execute in interregnum:green_authority run setblock 4 101 4 minecraft:air replace
execute in interregnum:green_authority run interregnum grove 4 101 4
execute in interregnum:green_authority run interregnum grove 4 101 4" \
    LOG=/tmp/grove.log timeout 900 ./tools/server_smoke.sh > /tmp/gv.txt 2>&1 \
    || { tail -25 /tmp/gv.txt; fail "the run did not complete"; }

groves=$(grep -oE 'grove=[A-Z]+ doorway=[A-Z]+ planted=[0-9]+ waiting=[0-9]+' /tmp/gv.txt || true)
[ -n "$groves" ] || { tail -20 /tmp/gv.txt; fail "the grove command produced no answer at all"; }
seeded=$(echo "$groves" | head -1)
grown=$(echo "$groves" | sed -n 2p)
waiterspot=$(echo "$groves" | sed -n 3p)
cut=$(echo "$groves" | sed -n 4p)
after=$(echo "$groves" | sed -n 5p)

# --- planted, and NOT a door yet ---------------------------------------------
echo "$seeded" | grep -q 'grove=SEEDED' || {
    echo "  after planting: $seeded"
    fail "a freshly planted sapling did not register as a seeded door. Either the ledger did not take the planting, or the position is not holding a sapling -- and if nothing was planted, everything below is about an empty field"; }
echo "$seeded" | grep -q 'doorway=SHUT' || {
    echo "  after planting: $seeded"
    fail "a sapling is already an open doorway. The waiting IS the feature: WORLD.md calls the lifespan this portal's whole distinction, and a door that opens the moment it is planted has one that starts at its end"; }
echo "$seeded" | grep -qE 'planted=[1-9]' || \
    fail "the ledger reports nothing planted while answering about a planted position ('$seeded')"

# --- and the school opens it -------------------------------------------------
# The locked rule in one pair of lines: nothing changed between the two probes except one
# cast of the Verdant's own second spell.
echo "$grown" | grep -q 'grove=OPEN' || {
    echo "  before: $seeded"
    echo "  after Wildgrowth: $grown"
    fail "one cast of Wildgrowth on a planted sapling did not ripen it into a door. That is the locked rule -- each god's portal is opened by the school that god teaches -- and if the school does not open it, the door is opened by time and the school is decoration on it"; }
echo "$grown" | grep -q 'doorway=OPEN' || \
    fail "the trunk is there and the doorway beside it is shut ('$grown'). The reach is what a player stands in; a door with no doorway cannot be used"

# --- the waiter is standing in the doorway ------------------------------------
# Probed at ITS OWN position rather than the trunk's, and it is not a formality: the
# doorway is a Chebyshev reach in three axes, and a probe at the trunk says nothing about
# two blocks away. Without it, "the thing did not cross" is equally explained by a thing
# that was never in the doorway at all -- which is exactly what happened the first time,
# when the waiter was put at head height and the growing canopy shouldered it around.
echo "$waiterspot" | grep -q 'doorway=OPEN' || {
    echo "  at the waiter's feet: $waiterspot"
    fail "the place the waiter was put is not in the doorway, so it was never going to cross and nothing below this line is about the portal"; }

# --- standing still under it goes through ------------------------------------
mark WAITER_ARRIVED || {
    grep -oE '(WAITER|LONELY)_[A-Z_]+' /tmp/grove.log | sort -u || true
    fail "a thing that stood still under an open door for six seconds is not in the Verdant's under-layer"; }
if mark WAITER_STAYED; then
    fail "it is in the under-layer AND still on the surface. A cross-dimension teleport removes the old entity and builds a new one; two means something copied it"
fi

# --- THE CONTROL: standing still is not enough on its own --------------------
mark LONELY_STAYED || \
    fail "the control -- same world, same tick, standing equally still eighteen blocks from any door -- is not where it was put. If it moved or fell the two are not comparable"
if mark LONELY_WENT; then
    fail "a thing standing still NOWHERE NEAR a door went through. The doorway is not the tree's reach, it is the whole world -- so the portal is a property of the dimension and the planting was decoration"
fi

# --- cutting it closes it ----------------------------------------------------
# Nothing tells the ledger. The state is read off the blocks every time, which is what
# makes "closes when cut" a fact rather than a promise kept by a listener.
echo "$cut" | grep -q 'grove=GONE' || {
    echo "  after felling: $cut"
    fail "the trunk was removed and the position still reports a door. WORLD.md: 'closes when cut' -- and a door that survives the tree is a door that was never the tree"; }
echo "$cut" | grep -q 'doorway=SHUT' || \
    fail "the tree is felled and the doorway is still open ('$cut')"

# --- and the ledger lets it go ----------------------------------------------
# Without this the position stays in the ledger for ever, so every felled door is a
# permanent entry and re-growing anything there resurrects a portal nobody planted.
echo "$after" | grep -q 'grove=UNPLANTED' || {
    echo "  second look: $after"
    fail "the felled position is still in the ledger. A remembered planting with no tree in it is a portal waiting to come back on its own, and the ledger would only ever grow"; }
echo "$after" | grep -q 'planted=0' || \
    fail "the ledger still counts a planting after the tree is gone ('$after')"

echo
printf "OK: a planted sapling is not yet a door, one cast of the school ripens it, standing\n    still under it crosses, standing still away from it does not, and felling the tree\n    closes it and clears the ledger\n"
