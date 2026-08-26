#!/bin/bash
# One table, two directions -- and the spell that is allowed to touch what you built.
#
# WORLD.md names REWIND alongside Weather in the Turning's kit: "repair by un-aging". One
# reads the ageing table forwards, the other backwards, and that pairing is the school.
#
# TWO DECISIONS ARE ASSERTED HERE, and both are the kind that would look fine while being
# wrong:
#
#   * REWIND MAY TOUCH A PLAYER'S BLOCK. Every other system in this mod consults the claim
#     ledger and refuses anything somebody placed -- the unraveling, attrition, the
#     Turning's clock, Weather. This one does not, because the ledger exists to stop the
#     WORLD eating your work, not to stop you working on it. A Rewind that refused your
#     own wall would be useless at exactly what it is for: you do not un-crack a cave.
#
#   * SOME BLOCKS HAVE NO SINGLE PAST, and it refuses rather than guessing. Plain
#     deepslate wears into cobbled deepslate and deepslate tiles crumble into it, so
#     asking what a piece of cobbled deepslate used to be has two answers and no way to
#     choose. The god whose entire law is keeping every version is precisely the one that
#     will not invent one.
#
#     THE FIRST VERSION OF THIS ASSERTION PASSED FOR THE WRONG REASON, which is worth
#     recording because it is the defect this repository keeps finding. It aimed at a dead
#     bush, on the grounds that a dandelion and a poppy both become one -- true, and true
#     in the UNRAVELING's table, which Rewind does not read. Rewind reads the Turning's,
#     where nothing becomes a dead bush at all, so the refusal was "nothing ages into
#     this" wearing the label of "two things do". Green, and testing nothing: the
#     ambiguity logic had no live coverage whatsoever. The converging pair now exists in
#     the table Rewind actually reads.
#
# The round trip is the third assertion and the cheapest: stone aged twice and rewound
# twice is stone again. If the two directions disagree anywhere, that is where it shows.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/rewind.log; }
outcome() { sed -n "/$1/,\$p" /tmp/rw.txt | grep -oE 'cast=rewind became=[a-z:_-]+ frayed=[0-9]+' | head -1; }

WHO=bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb

COMMANDS="execute in interregnum:temporal_authority run forceload add -16 -16 15 15
forceload add -16 -16 15 15
wait 3
interregnum learn $WHO turning
execute in interregnum:temporal_authority run setblock 0 100 0 minecraft:stone replace
execute in interregnum:temporal_authority run setblock 4 100 0 minecraft:stone replace
execute in interregnum:temporal_authority run interregnum claim record 4 100 0 4 100 0
execute in interregnum:temporal_authority run setblock 8 100 0 minecraft:cobbled_deepslate replace
execute in interregnum:temporal_authority if block 0 100 0 minecraft:stone run say SETUP_PLAIN
execute in interregnum:temporal_authority if block 4 100 0 minecraft:stone run say SETUP_CLAIMED
say AGE_TWICE
execute in interregnum:temporal_authority run interregnum turning age 0 100 0
execute in interregnum:temporal_authority run interregnum turning age 0 100 0
execute in interregnum:temporal_authority if block 0 100 0 minecraft:mossy_cobblestone run say AGED_TO_MOSS
say REWIND_ONCE
execute in interregnum:temporal_authority run interregnum cast rewind $WHO 0 100 0
execute in interregnum:temporal_authority if block 0 100 0 minecraft:cobblestone run say BACK_TO_COBBLE
say REWIND_TWICE
execute in interregnum:temporal_authority run interregnum cast rewind $WHO 0 100 0
execute in interregnum:temporal_authority if block 0 100 0 minecraft:stone run say BACK_TO_STONE
say CLAIMED_CASE
execute in interregnum:temporal_authority run interregnum turning age 4 100 0
execute in interregnum:temporal_authority if block 4 100 0 minecraft:stone run say CLAIMED_UNAGED
execute in interregnum:temporal_authority run setblock 4 100 0 minecraft:cobblestone replace
execute in interregnum:temporal_authority run interregnum cast rewind $WHO 4 100 0
execute in interregnum:temporal_authority if block 4 100 0 minecraft:stone run say CLAIMED_REPAIRED
say AMBIGUOUS_CASE
execute in interregnum:temporal_authority run interregnum cast rewind $WHO 8 100 0
execute in interregnum:temporal_authority if block 8 100 0 minecraft:cobbled_deepslate run say AMBIGUOUS_UNTOUCHED
execute in interregnum:temporal_authority if block 8 100 0 minecraft:deepslate run say AMBIGUOUS_GUESSED_DEEPSLATE
execute in interregnum:temporal_authority if block 8 100 0 minecraft:deepslate_tiles run say AMBIGUOUS_GUESSED_TILES" \
    LOG=/tmp/rewind.log timeout 900 ./tools/server_smoke.sh > /tmp/rw.txt 2>&1 \
    || { tail -25 /tmp/rw.txt; fail "the run did not complete"; }

# --- the setup, before anything that rests on it ----------------------------
mark SETUP_PLAIN   || fail "no stone was placed to age and rewind -- the round trip has nothing to run on"
mark SETUP_CLAIMED || fail "the claimed stone was never placed, so the claim case proves nothing"
mark AGED_TO_MOSS  || {
    grep -oE 'turning=[a-z:_]+' /tmp/rw.txt | head -4 || true
    fail "two ageing passes did not reach mossy cobblestone, so there is nothing aged to rewind"; }

# --- the round trip: forwards twice, backwards twice ------------------------
# If the two directions disagree anywhere in the table, this is where it shows.
mark BACK_TO_COBBLE || {
    outcome REWIND_ONCE || true
    fail "rewinding mossy cobblestone did not restore cobblestone -- the table read backwards does not agree with the table read forwards, so the school's two spells are not the same table"; }
mark BACK_TO_STONE || {
    outcome REWIND_TWICE || true
    fail "a second rewind did not restore plain stone. Stone aged twice and rewound twice must be stone: an un-aging that stops halfway is a spell that repairs some of your wall"; }

# --- THE DECISION: repair is not destruction --------------------------------
# The claim ledger stops the world eating your work. It is not there to stop you working
# on it, and a Rewind that refused your own wall would be useless at the one thing WORLD.md
# says it is for. The control is CLAIMED_UNAGED directly above: the Turning's own clock
# still refuses that same block, so this is a distinction between the two, not a hole.
mark CLAIMED_UNAGED || \
    fail "the Turning's ordinary ageing touched a player-placed block -- that is the guarantee this whole mod rests on, and if it is gone then Rewind being allowed to touch one proves nothing about intent"
mark CLAIMED_REPAIRED || {
    outcome CLAIMED_CASE || true
    fail "Rewind refused to repair a block the player had placed. The claim ledger exists to stop the WORLD eating somebody's work, not to stop them mending it -- refusing here makes 'repair by un-aging' a spell for tidying up caves"; }

# --- and it will not invent a past ------------------------------------------
# Two rules in the Turning's OWN table end at cobbled deepslate, so it has no single past.
# The two diagnostic markers above name which way a guess went, because "it guessed" and
# "it guessed toward tiles" are different bugs and the second is much easier to fix.
mark AMBIGUOUS_UNTOUCHED || {
    grep -oE 'AMBIGUOUS_[A-Z_]+' /tmp/rewind.log | sort -u || true
    fail "Rewind changed a block with two pasts. Plain deepslate wears into cobbled deepslate and tiles crumble into it, so restoring either one means the spell guessed -- and the god that keeps every version of everything is the last thing that should be inventing one"; }
echo "$(outcome AMBIGUOUS_CASE)" | grep -q 'became=no-single-past' || {
    outcome AMBIGUOUS_CASE || true
    fail "the ambiguous case was refused for some other reason than having no single past -- the refusal is right by accident, and the next change to the table will move it"; }

echo
echo "OK: the ageing table runs both ways, Rewind mends what its owner built, and refuses to invent a past that two rules share"
