#!/bin/bash
# Rot: age a thing forward past its end.
#
# WORLD.md, locked: "Age a thing forward PAST ITS END: compost, spoil, collapse. NEVER
# AIMED AT A PLAYER OR A MOB." And on why it needed deciding rather than sketching: "the
# obvious reading of 'age it past its end' is an instant-kill, and EVERY SPELL IS A
# WORLD-VERB rules that out -- so it ages the things that HAVE an end and leaves creatures
# alone. A school that broke the doctrine would take the doctrine with it."
#
# THE CONSTRAINT IS KEPT BY CONSTRUCTION. This is a block conversion running on the same
# table machinery as the ageing chain and the unraveling -- WORLD.md's locked reuse note,
# "one mechanism; a school and an apocalypse" -- and a table of blocks has no way to name a
# cow. So the clause written in bold is not enforced anywhere: there is nothing to enforce.
# The self-test guards the vocabulary; this file proves a creature standing on a rotting
# block is untouched, which is the same claim from the outside.
#
# IT IS THE AGEING TABLE CONTINUED. The Turning's chains stop at mossy cobblestone, mossy
# stone bricks and cobbled deepslate -- terminal states with no next step. Rot gives them
# one, and `stone -> cobblestone -> mossy cobblestone -> gravel` is one sentence finished
# rather than two ideas.
#
# WHAT IS ASSERTED:
#   * it cannot be cast untaught;
#   * a terminal state of the AGEING table rots, which is the join between the two;
#   * REWIND CANNOT UNDO IT. Nothing rotten is in the ageing table, so reading that table
#     backwards finds nothing -- past a thing's end there is no past left to keep, and the
#     god that remembers everything remembers what a thing WAS, not what is left of it;
#   * a creature standing on the block is untouched, and so is the block it stands on when
#     the table has no opinion about it;
#   * and the two tables are SEPARATE: a block the ageing table knows is not rotted by
#     this spell unless the rotting table names it too.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/rot.log; }

R=abababab-7777-4777-8777-777777777777

COMMANDS="forceload add -16 -16 47 47
wait 3
setblock 4 -60 4 minecraft:mossy_cobblestone replace
setblock 8 -60 8 minecraft:stone replace
setblock 12 -60 12 minecraft:oak_leaves[persistent=true] replace
setblock 20 -61 20 minecraft:mossy_cobblestone replace
summon minecraft:cow 20.5 -60 20.5 {NoAI:1b,Tags:[\"bystander\"]}
wait 2

say UNTAUGHT
interregnum cast rot $R 4 -60 4
interregnum learn $R turning

say A_TERMINAL_WALL
interregnum cast rot $R 4 -60 4
say REWINDING_IT
interregnum cast rewind $R 4 -60 4
say PLAIN_STONE
interregnum cast rot $R 8 -60 8
say AGEING_STILL_WORKS
interregnum cast weather $R 8 -60 8
say LEAVES
interregnum cast rot $R 12 -60 12
say UNDER_A_COW
interregnum cast rot $R 20 -61 20
wait 1

execute if block 4 -60 4 minecraft:gravel run say WALL_COLLAPSED
execute if block 12 -60 12 minecraft:air run say LEAVES_GONE
execute if entity @e[tag=bystander] run say COW_ALIVE
execute if block 20 -61 20 minecraft:gravel run say UNDER_COW_ROTTED" \
    LOG=/tmp/rot.log timeout 900 ./tools/server_smoke.sh > /tmp/rt2.txt 2>&1 \
    || { tail -25 /tmp/rt2.txt; fail "the run did not complete"; }

rots=$(grep -oE 'cast=rot subject=[A-Z]+ what=[a-z:_]* frayed=[0-9]+ refused=[a-z]*' /tmp/rt2.txt || true)
[ -n "$rots" ] || { tail -20 /tmp/rt2.txt; fail "the rot cast produced no answer at all"; }
untaught=$(echo "$rots" | head -1)
wall=$(echo "$rots" | sed -n 2p)
stone=$(echo "$rots" | sed -n 3p)
leaves=$(echo "$rots" | sed -n 4p)
undercow=$(echo "$rots" | sed -n 5p)

# --- untaught, like every other spell ----------------------------------------
echo "$untaught" | grep -q 'refused=unlearned' || {
    echo "  casting before being taught: $untaught"
    fail "somebody who had never been taught the Turning rotted something"; }

# --- a terminal state of the ageing table rots -------------------------------
# The join between the two tables, and the reason this is one sentence finished rather
# than a second idea: mossy cobblestone is where the ageing chain STOPS.
echo "$wall" | grep -q 'subject=THING' || {
    echo "  casting on mossy cobblestone: $wall"
    fail "the last step of the Turning's own chain has nowhere past its end to go. Rot exists to give the table's terminal states a next step; if it does not know them, the two tables do not join"; }
mark WALL_COLLAPSED || \
    fail "the cast reported a thing and the block did not change -- something was found and nothing was done to it"

# --- REWIND CANNOT UNDO IT ---------------------------------------------------
# The character of the spell in one probe. Rewind reads the AGEING table backwards, and
# nothing rotten is in it.
# `no-single-past` is Rewind's own word, and StepTable is explicit that it means both "no
# rule ends here" and "more than one does" -- the two nulls are deliberately
# indistinguishable, because the answer to both is that the table cannot tell you. For
# gravel it is the first: nothing in the AGEING table ends there, because rotting is not in
# that table at all.
rewound=$(grep -oE 'cast=rewind became=[a-z:_-]* frayed=[0-9]+' /tmp/rt2.txt | head -1 || true)
echo "$rewound" | grep -q 'became=no-single-past' || {
    echo "  rewinding a collapsed wall: $rewound"
    fail "Rewind undid a rot. The two tables are separate on purpose: anything in the ageing table can be undone, which is what 'keeping every past' means when it is a block -- and past a thing's end there is no past left to keep. A reversible rot is an ageing step with a louder name"; }

# --- the tables are separate, in both directions -----------------------------
echo "$stone" | grep -q 'subject=NOTHING' || {
    echo "  rotting plain stone: $stone"
    fail "plain stone rotted. It is the FIRST step of the ageing chain, not a terminal state -- if Rot knows it, then Rot is the ageing table with extra steps and 'past its end' means nothing"; }
grep -qE 'cast=weather became=minecraft:cobblestone' /tmp/rt2.txt || {
    grep -oE 'cast=weather became=[a-z:_]*' /tmp/rt2.txt | head -2 || true
    fail "Weather no longer ages plain stone to cobblestone. Adding a second table has broken the first, which is the failure a shared mechanism makes possible"; }

# --- leaves spoil ------------------------------------------------------------
echo "$leaves" | grep -q 'subject=THING' || {
    echo "  rotting leaves: $leaves"
    fail "leaves did not spoil. WORLD.md's three words are compost, spoil, collapse, and this is the middle one"; }
mark LEAVES_GONE || fail "the leaves were reported rotted and are still there"

# --- A CREATURE STANDING ON IT IS UNTOUCHED ----------------------------------
# The locked clause, from the outside. The block under the cow rots and the cow does not,
# because the spell has no vocabulary for a creature -- see the self-test, which guards
# that the enum cannot even NAME one.
echo "$undercow" | grep -q 'subject=THING' || {
    echo "  rotting the block under a cow: $undercow"
    fail "the block under the cow did not rot, so the probe below proves nothing about creatures"; }
mark UNDER_COW_ROTTED || fail "the block under the cow was reported rotted and did not change"
mark COW_ALIVE || {
    fail "THE COW IS GONE. WORLD.md locks this spell as never aimed at a player or a mob, in bold, because the obvious reading of 'age it past its end' is an instant-kill and every spell being a world-verb rules it out. A school that broke the doctrine would take the doctrine with it"; }

echo
printf "OK: the last step of the ageing chain has somewhere past its end to go, Rewind\n    cannot follow it there, plain stone is untouched and still ages normally, and a cow\n    standing on a block that rotted is still a cow\n"
