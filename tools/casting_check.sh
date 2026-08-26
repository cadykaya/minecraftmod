#!/bin/bash
# The first spell, and the reason the Wardens are right about it.
#
# WORLD.md, locked: "Weather -- age blocks: instant mossy/cracked/oxidized -- magic as a
# builder's palette", under the doctrine "EVERY SPELL IS A WORLD-VERB. No damage buttons
# with particle effects."
#
# And the economics, also locked: "The overworld ban is CORRECT. With the god dead, all
# overworld casting draws on the corpse -- the residue still holding the world together.
# Heavy casting visibly frays its surroundings. The Wardens' law is right, and the player
# can DISCOVER it is right. Off-world, living gods replenish what casting spends."
#
# THE ASSERTION THAT MATTERS is that same cast costing different amounts in two worlds.
# Not that the spell works -- a spell that works everywhere for free is a spell that makes
# the ban arbitrary, and "the enforcement agency is not wrong" is the one reading WORLD.md
# rules out. So this casts the SAME spell on the SAME block type in the overworld and in
# the Hearth-Turner's world, and the difference between what the two cost is the feature.
#
# ALSO ASSERTED, because it is the oldest promise in the mod and the spell inherits it
# rather than reimplementing it: you cannot Weather a block somebody placed. That falls
# out of calling `Hearth.step`, which is the same method the Hearth-Turner's own world
# runs on its clock -- WORLD.md's locked reuse, "one mechanism; a school and an
# apocalypse". The check proves the promise survived the reuse.
#
# NOT asserted: how a player LEARNS the spell. There is no learning path yet -- the
# command is the seam, the way `unravel at` and `turning age` are -- and WORLD.md's
# "schools, one per god, learned in their worlds" is the next increment. Stated rather
# than quietly omitted.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }

# The fraying is the unraveling spending the corpse, so it only has anything to spend
# once the god is dead. A deicide first is not scene-setting, it is the precondition the
# locked text names: "with the god dead, all overworld casting draws on the corpse".
# The fraying needs something to fray. Band 1's table converts ground plants, so the cast
# sits in a patch of them on a floor that supports them -- a cast into empty air would
# cost nothing for a reason that has nothing to do with the law under test, and the check
# would read as "casting is free" when it was really "there was nothing there".
COMMANDS="forceload add -16 -16 31 31
execute in interregnum:temporal_authority run forceload add -16 -16 15 15
wait 3
execute positioned 0 -60 0 run interregnum record deicide
fill -2 99 -2 10 99 10 minecraft:grass_block replace
fill -2 100 -2 10 100 10 minecraft:short_grass replace
execute in interregnum:temporal_authority run fill -2 99 -2 10 99 10 minecraft:grass_block replace
execute in interregnum:temporal_authority run fill -2 100 -2 10 100 10 minecraft:short_grass replace
setblock 4 100 4 minecraft:stone replace
execute in interregnum:temporal_authority run setblock 4 100 4 minecraft:stone replace
setblock 8 100 4 minecraft:stone replace
interregnum claim record 8 100 4 8 100 4
execute if block 4 100 4 minecraft:stone run say SETUP_HOME
execute in interregnum:temporal_authority if block 4 100 4 minecraft:stone run say SETUP_THERE
execute if block 8 100 4 minecraft:stone run say SETUP_CLAIMED
say CAST_HOME
interregnum cast weather 4 100 4
say CAST_THERE
execute in interregnum:temporal_authority run interregnum cast weather 4 100 4
say CAST_CLAIMED
interregnum cast weather 8 100 4
say CAST_NOTHING
interregnum cast weather 4 101 4
execute if block 4 100 4 minecraft:cobblestone run say HOME_AGED
execute in interregnum:temporal_authority if block 4 100 4 minecraft:cobblestone run say THERE_AGED
execute if block 8 100 4 minecraft:stone run say CLAIMED_SPARED" \
    LOG=/tmp/casting.log timeout 900 ./tools/server_smoke.sh > /tmp/cast.txt 2>&1 \
    || { tail -25 /tmp/cast.txt; fail "the run did not complete"; }

mark() { grep -q "$1" /tmp/casting.log; }
frayed() { sed -n "/$1/,\$p" /tmp/cast.txt | grep -oE 'cast=weather became=[a-z:_]+ frayed=[0-9]+' | head -1; }

# --- the setup, before anything that rests on it ----------------------------
mark SETUP_HOME    || fail "no stone was placed in the overworld -- the control does not exist"
mark SETUP_THERE   || fail "no stone was placed in interregnum:temporal_authority -- the comparison has one side"
mark SETUP_CLAIMED || fail "the claimed stone was never placed, so sparing it proves nothing"

# --- the spell is a world-verb: it changes a block --------------------------
mark HOME_AGED || {
    grep -oE 'cast=weather [a-z=:_ 0-9]+' /tmp/cast.txt | head -4 || true
    fail "casting Weather on stone in the overworld did not turn it to cobblestone -- the spell does nothing, so nothing below is about its cost"; }
mark THERE_AGED || \
    fail "casting Weather in the Hearth-Turner's own world did not age the stone -- the spell fails in the one place its school is taught"

# --- and it inherits the oldest promise in the mod --------------------------
# Not reimplemented here: Weather calls the same `Hearth.step` the Turning's clock calls,
# so the claim ledger protects a build from the spell for the same reason it protects one
# from the apocalypse. This asserts the promise survived the reuse.
mark CLAIMED_SPARED || \
    fail "Weather aged a block a player had placed. The mod's oldest guarantee is that the world may warp but a player's work may not, and a spell that breaks it is worse than an apocalypse that does -- the apocalypse at least is not aimed"

# --- THE ASSERTION: the same cast costs differently in two worlds -----------
home=$(frayed CAST_HOME)
there=$(frayed CAST_THERE)
home_n=$(echo "$home" | grep -oE 'frayed=[0-9]+' | cut -d= -f2)
there_n=$(echo "$there" | grep -oE 'frayed=[0-9]+' | cut -d= -f2)
[ -n "$home_n" ] && [ -n "$there_n" ] || {
    grep -oE 'cast=weather [a-z=:_ 0-9]+' /tmp/cast.txt | head -4 || true
    fail "could not read what either cast cost -- the comparison has nothing to compare"; }

echo "  cast in the overworld frayed $home_n place(s); in the Hearth-Turner's world, $there_n"

[ "$there_n" -eq 0 ] || \
    fail "casting in a living god's world frayed $there_n place(s). WORLD.md: off-world, living gods replenish what casting spends -- legal, sustainable. A cost that follows you everywhere removes the reason to travel and makes the ban a tax rather than a fact about where you are"
[ "$home_n" -gt 0 ] || \
    fail "casting in the overworld cost nothing. The ban is supposed to be CORRECT -- the residue holding the world together is what pays for it, and a player is meant to be able to DISCOVER that by watching the ground. Free overworld casting makes the Wardens wrong, which is the one reading WORLD.md rules out"

# --- and a miss is free ------------------------------------------------------
# Aimed at air, which has no rule in the table. Nothing should be spent: the residue is
# drawn on by the world changing, not by the attempt. A spell that frayed the world for a
# miss would punish experimenting with it.
miss=$(frayed CAST_NOTHING)
echo "$miss" | grep -q 'became=nothing frayed=0' || \
    fail "a cast that changed nothing still cost something ('$miss') -- experimenting with a spell would fray the world, and a player learning what Weather does would be punished for finding out"

echo
echo "OK: the first spell ages a block, spares what a player built, and costs the overworld something it does not cost a living god's world"
