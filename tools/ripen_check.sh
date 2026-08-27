#!/bin/bash
# Ripen: age a living thing forward, and stop at grown.
#
# WORLD.md, locked: "Age a living thing forward: crop, sapling, animal. THE KIND HALF OF
# THE SCHOOL."
#
# THE SCHOOL HAD ONLY EVER AGED STONE. Weather ages masonry and Rewind un-ages it, both on
# the Turning's table -- wearing, cracking, greening. That is a strange gap for a god whose
# law is KEEPING EVERY PAST, because living things have more past than walls do.
#
# WHERE IT STOPS is the line between this spell and its own twin. An adult animal is not
# ripened: forward from grown is toward the end, and that is Rot's country, which WORLD.md
# locks as NEVER AIMED AT A PLAYER OR A MOB. Ripen does not refuse an adult by naming it --
# it asks for something with growing left to do, and an adult is not that.
#
# WHAT IS ASSERTED:
#   * it cannot be cast untaught;
#   * A CALF BECOMES A COW. Nothing else in this mod ages a creature;
#   * AN ADULT IS NOT A SUBJECT. Same species, same distance, same cast -- and the answer
#     is NOTHING rather than a refusal that names adults, because the spell asks a question
#     an adult simply does not answer;
#   * A CROP IS CARRIED FORWARD, visibly -- from freshly planted to a stage a player can
#     see, which is why one cast is worth eight pushes and not one;
#   * THE CREATURE WINS over the ground it is standing on. A calf on a wheat field is both,
#     and nobody aims a spell at the earth under a cow and means the wheat;
#   * and STONE IS NOTHING. The spell asks for something living and a floor is not.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }

T=ffffffff-6666-4666-8666-666666666666

# Everything stands on the flat world's ground at y=-61, so the subjects are at y=-60.
# The animals are summoned with {Age:-24000} -- a newborn -- and NoAI so they stay put;
# NoAI stops `travel()` entirely (docs/LESSONS.md #44) and nothing here depends on them
# moving. Ageing is not movement.
#
# THE SUBJECTS ARE TWENTY BLOCKS APART, and the spacing is not generosity. An aimed spell's
# reach is an inflated AABB -- a BOX, not a sphere -- so a subject at (8.5, 8.5) is inside
# the reach of a cast at (4, 4) even though it is six and a half blocks away by any measure
# a person would use. The first version put them eight apart and the cast on the wheat
# found the calf, correctly and unhelpfully.
COMMANDS="forceload add -16 -16 79 79
wait 4
setblock 4 -61 4 minecraft:farmland replace
setblock 4 -60 4 minecraft:wheat[age=0] replace
setblock 4 -61 24 minecraft:farmland replace
setblock 4 -60 24 minecraft:wheat[age=0] replace

summon minecraft:cow 24.5 -60 4.5 {Age:-24000,NoAI:1b,Tags:[\"calf\"]}
summon minecraft:cow 44.5 -60 4.5 {NoAI:1b,Tags:[\"grown\"]}
summon minecraft:cow 4.5 -60 24.5 {Age:-24000,NoAI:1b,Tags:[\"standing\"]}
wait 2

say UNTAUGHT
interregnum cast ripen $T 4 -60 4
interregnum learn $T turning

say A_CROP
interregnum cast ripen $T 4 -60 4
say A_CALF
interregnum cast ripen $T 24 -60 4
say AN_ADULT
interregnum cast ripen $T 44 -60 4
say A_FLOOR
interregnum cast ripen $T 64 -61 4
say ON_THE_FIELD
interregnum cast ripen $T 4 -60 24
wait 1

execute if entity @e[tag=calf,nbt={Age:0}] run say CALF_GREW
execute if entity @e[tag=grown,nbt={Age:0}] run say ADULT_INTACT
execute if entity @e[tag=standing,nbt={Age:0}] run say STANDING_GREW
execute if block 4 -60 24 minecraft:wheat[age=0] run say FIELD_UNTOUCHED" \
    LOG=/tmp/ripen.log timeout 900 ./tools/server_smoke.sh > /tmp/rp.txt 2>&1 \
    || { tail -25 /tmp/rp.txt; fail "the run did not complete"; }

mark() { grep -q "$1" /tmp/ripen.log; }
casts=$(grep -oE 'cast=ripen subject=[A-Z]+ what=[a-z:_]* frayed=[0-9]+ refused=[a-z]*' /tmp/rp.txt || true)
[ -n "$casts" ] || { tail -20 /tmp/rp.txt; fail "the ripen cast produced no answer at all"; }

untaught=$(echo "$casts" | head -1)
crop=$(echo "$casts" | sed -n 2p)
calf=$(echo "$casts" | sed -n 3p)
adult=$(echo "$casts" | sed -n 4p)
floor=$(echo "$casts" | sed -n 5p)
field=$(echo "$casts" | sed -n 6p)

# --- untaught, like every other spell ----------------------------------------
echo "$untaught" | grep -q 'refused=unlearned' || {
    echo "  casting before being taught: $untaught"
    fail "somebody who had never been taught the Turning ripened something"; }

# --- a crop is carried forward, visibly --------------------------------------
echo "$crop" | grep -q 'subject=PLANT' || {
    echo "  casting on wheat at age 0: $crop"
    fail "a cast on freshly planted wheat did not report a plant. WORLD.md's list is 'crop, sapling, animal' and the crop is the first of the three"; }

# --- a calf becomes a cow ----------------------------------------------------
# The half nothing else in this mod does. Weather ages stone; this ages something alive.
echo "$calf" | grep -q 'subject=CREATURE' || {
    echo "  casting near a newborn calf: $calf"
    fail "a cast beside a calf did not find a creature"; }
mark CALF_GREW || {
    grep -oE '(CALF|ADULT|STANDING|FIELD)_[A-Z]+' /tmp/ripen.log | sort -u || true
    fail "the calf is not grown. The cast reported a creature and the creature's age did not move, so something was found and nothing was done to it"; }

# --- AN ADULT IS NOT A SUBJECT ----------------------------------------------
# The line between this spell and Rot. Same species, same distance -- and the answer must
# be NOTHING rather than a refusal, because the spell asks a question an adult does not
# answer rather than checking a list of things it will not touch.
echo "$adult" | grep -q 'subject=NOTHING' || {
    echo "  casting beside a grown cow: $adult"
    fail "a grown animal was ripened. Forward from grown is toward the end, which is Rot's country and locked as never aimed at a creature -- if the kind half does not stop at adulthood then the two spells are one and the constraint is gone"; }
mark ADULT_INTACT || \
    fail "the adult cow's age moved, or it is not where it was put -- either way the probe above is not about what it claims"

# --- the creature wins over the ground it stands on --------------------------
# A calf on a wheat field is both a young animal and a position holding a crop. Nobody
# aims a spell at the earth under a cow and means the wheat.
echo "$field" | grep -q 'subject=CREATURE' || {
    echo "  casting at a calf standing on wheat: $field"
    fail "a cast at a calf standing on a crop ripened the crop instead of the calf"; }
mark STANDING_GREW || fail "the calf on the field did not grow"
mark FIELD_UNTOUCHED || {
    fail "the wheat under the calf was ripened as well. One cast, one subject -- a spell that does both is a small volume, and this school already has one of those in the other god's kit"; }

# --- and stone is nothing ----------------------------------------------------
echo "$floor" | grep -q 'subject=NOTHING' || {
    echo "  casting at the floor: $floor"
    fail "the ground was ripened. The spell asks for something living and a floor is not -- if stone answers, then 'age a LIVING thing forward' describes nothing"; }

echo
printf "OK: a crop is carried forward and a calf becomes a cow, a grown animal and a floor\n    are nothing at all, and a calf standing on wheat is the subject rather than the\n    wheat under it\n"
