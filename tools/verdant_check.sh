#!/bin/bash
# In the Verdant's world, everything grows -- and that is the hazard.
#
# WORLD.md, on the Verdancy school: "and in the Verdant's own world, accelerating growth
# is a HAZARD." That word is the brief. The mechanism for a convenience and for a hazard
# is identical; what makes it a hazard is that it applies to everything, everywhere, and
# not to the things you wanted.
#
# THE TRAP: "it grew" proves nothing. Things grow in the overworld too, and vanilla's
# own random ticking would grow things in that dimension with this mod's code deleted
# entirely. So the measurement is a COMPARISON of the two worlds over the same
# wall-clock window (docs/LESSONS.md #19): identical rows of dirt beside identical rows
# of grass, planted in the same instant, and the Verdant's must convert strictly more.
#
# A SECOND TRAP, learned the hard way: a single target block is unobservable. Random
# ticking picks positions uniformly out of a 16x16x16 section, so one specific block is
# hit about once per eight seconds even at eight times the vanilla rate -- and a first
# draft of this file failed for exactly that reason while the law underneath it was
# working perfectly. The probe that settled it showed the handler happily ticking grass
# at the terrain surface while the two test blocks sat at y=100 being missed. Hence a
# ROW of sixteen targets, counted, rather than one block asked a yes/no question.
#
# NOT covered here, and it is a real gap rather than an absence of feature: the claim
# skip. `Verdant.grow` never applies its extra ticks to a block somebody placed, but
# vanilla's own ticking still reaches such blocks at the ordinary rate, so the
# difference between "protected" and "unprotected" is statistical rather than
# categorical over any window short enough for CI. The promise is stated narrowly in
# `Verdant`'s javadoc and recorded in HANDOFF; it is not asserted here, and pretending
# otherwise would be worse than the gap.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/verdant.log; }

# Wheat at age 0 in both worlds, on farmland, lit. `wait 3` after the forceload so the
# chunks are actually loaded before anything is planted -- growth only runs on loaded
# chunks, so planting into an unloaded one would test nothing and look like a failure of
# the law (docs/LESSONS.md #22 is the entity version of the same mistake).
#
# Dirt for the claims half: one claimed, one not, side by side under sky. Grass spreads
# onto dirt by random tick, which is exactly the mechanism under test.
# ONE mechanism for both halves: grass spreading onto dirt, which is a random tick and
# nothing else. Wheat was the first draft and was wrong twice -- farmland dehydrates to
# dirt without water and takes the crop with it, which would read as "the wheat vanished"
# rather than "the law did not run", and the god-worlds have min_y=0 so the overworld's
# y=-60 does not exist there at all.
#
# y=100 in BOTH worlds: open sky in the superflat overworld and above the terrain in the
# Verdant's, so both samples have the light grass needs and neither is buried.
#
# `wait 3` after the forceload: growth only runs on loaded chunks, so planting into an
# unloaded one would test nothing while looking exactly like the law failing.
# Sixteen dirt targets in a row at z=9, flanked by a grass source row at z=8, in BOTH
# worlds at y=100 -- open sky in the superflat overworld and above the terrain in the
# Verdant's, so both have the light grass needs and neither is buried.
#
# `wait 3` after the forceload: growth runs only on loaded chunks, so building into an
# unloaded one would test nothing while looking exactly like the law failing.
ROWS=""
for i in $(seq 0 31); do
    ROWS="$ROWS
setblock $i 100 8 minecraft:grass_block replace
setblock $i 100 9 minecraft:dirt replace
execute in interregnum:green_authority run setblock $i 100 8 minecraft:grass_block replace
execute in interregnum:green_authority run setblock $i 100 9 minecraft:dirt replace"
done
AFTER=""
for i in $(seq 0 31); do
    AFTER="$AFTER
execute if block $i 100 9 minecraft:grass_block run say HOME_CONVERTED
execute in interregnum:green_authority if block $i 100 9 minecraft:grass_block run say THERE_CONVERTED"
done

COMMANDS="execute in interregnum:green_authority run forceload add -16 -16 47 47
forceload add -16 -16 47 47
wait 3
$ROWS
execute if block 0 100 9 minecraft:dirt run say HOME_DIRT_PLACED
execute in interregnum:green_authority if block 0 100 9 minecraft:dirt run say THERE_DIRT_PLACED
execute in interregnum:green_authority if block 0 100 8 minecraft:grass_block run say THERE_SOURCE_PLACED
wait 25
$AFTER" \
    LOG=/tmp/verdant.log timeout 2000 ./tools/server_smoke.sh > /tmp/vd.txt 2>&1 \
    || { tail -25 /tmp/vd.txt; fail "the run did not complete"; }

# --- the setup, before anything that depends on it --------------------------
mark HOME_DIRT_PLACED || fail "no dirt was placed in the overworld -- the control does not exist"
mark THERE_DIRT_PLACED || {
    grep -iE "green_authority|dimension|That position" /tmp/vd.txt | tail -8 || true
    fail "no dirt was placed in interregnum:green_authority -- the dimension may not have loaded, or y=100 is outside it"; }
mark THERE_SOURCE_PLACED || \
    fail "there is no grass block beside the dirt to spread FROM, so 'it did not spread' would be true for a reason that has nothing to do with the law"


# --- the law: it converted MORE here than at home, over the same window ------
there=$(grep -c "THERE_CONVERTED" /tmp/verdant.log || true)
home=$(grep -c "HOME_CONVERTED" /tmp/verdant.log || true)
echo "converted in 25s:  interregnum:green_authority=$there/32   overworld=$home/32"

[ "$there" -gt 0 ] || {
    grep -oE "(HOME|THERE)_[A-Z_]+" /tmp/verdant.log | sort | uniq -c || true
    fail "nothing converted in interregnum:green_authority in 25 seconds -- growth is not accelerated, so the world has no law"; }

# Strictly more, with a margin, over THIRTY-TWO targets rather than sixteen.
#
# Not "the overworld converted nothing": at eight times the rate over 25 seconds the
# overworld genuinely converts a few, and a check demanding zero at home would be flaky
# by construction. What must hold is the RATIO, because that is what the law changes.
#
# The margin was set from measurement rather than taste. Four runs at sixteen targets
# gave the Verdant 8-12 and the overworld 1-4 -- a real gap, but close enough at the
# extremes to be uncomfortable, so the sample was doubled to halve the relative
# variance. A slower CI runner ticks less in the same wall-clock 25 seconds and lowers
# BOTH counts together, which is why the assertion is a comparison and not a threshold.
[ "$there" -gt $((home + 2)) ] || \
    fail "interregnum:green_authority converted $there of 32 and the overworld converted $home of 32 -- that is not an accelerated world, and this file would be green with the Verdant law deleted"

# --- and the law stayed in its own world ------------------------------------
# The ratio test above is not enough on its own, and a mutation proved it: removing the
# dimension check so the law fires in EVERY level gave 25 there and 21 at home, which
# still satisfies "more here than there" and is a catastrophe -- one god's law applied
# to the whole game. The overworld count is the diagnostic, so it is asserted directly.
# Four measured runs put it at 0-4 of 32; the ceiling is double the worst of those.
[ "$home" -le 8 ] || \
    fail "the OVERWORLD converted $home of 32, which is not vanilla's rate -- the Verdant's law has leaked out of its own dimension and is accelerating growth everywhere"

echo
echo "OK: the Verdant's world grows measurably faster than the overworld ($there vs $home of 32)"
