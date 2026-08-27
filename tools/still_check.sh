#!/bin/bash
# Things already in motion, stopped where they are -- and the collision that would have
# happened if zones were still keyed by school.
#
# WORLD.md: "Still -- freeze primed TNT / falling block MID-STATE." The last word is the
# spell. Not prevent and not defuse: the thing is already happening and it stops, holding
# the state it was in. When the zone lapses it resumes, which is what keeps this a
# reprieve rather than a damage button with extra steps.
#
# THE ASSERTION THAT MATTERS is the pair of them. Hush and Still are BOTH the Quiet One's,
# and until this increment spell zones were keyed by SCHOOL -- so a Hush would have frozen
# falling blocks and a Still would have muted creepers, with nothing failing and both
# spells looking like they worked from inside either one. Zones are now keyed by spell,
# and this file is the only place that difference has a symptom:
#
#   * sand inside a STILL does not fall  (the spell)
#   * sand inside a HUSH falls normally  (the collision that would have been)
#   * sand outside both falls normally   (the control, without which the first line is
#                                         satisfied by gravity being broken everywhere)
#
# Also asserted: nothing is DELETED. A frozen falling block is still a falling block --
# a spell that removed the hazard would satisfy "it did not land" for the wrong reason,
# and would be a much bigger and much worse spell than the one WORLD.md locked.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/still.log; }

WHO=cccccccc-cccc-4ccc-8ccc-cccccccccccc

# Three sand columns: one in a Still, one in a Hush, one in neither. Floors at y=100 so
# "landed" is a block probe rather than an entity search.
COMMANDS="forceload add -16 -16 47 47
wait 3
setblock 0 100 0 minecraft:stone replace
setblock 20 100 0 minecraft:stone replace
setblock 40 100 0 minecraft:stone replace
interregnum learn $WHO silence
interregnum cast still $WHO 0 110 0
interregnum cast hush $WHO 20 110 0
say ZONES_OPEN
setblock 0 112 0 minecraft:sand replace
setblock 20 112 0 minecraft:sand replace
setblock 40 112 0 minecraft:sand replace
wait 3
say AFTER_FALL
execute if block 0 101 0 minecraft:sand run say STILL_LANDED
execute if block 20 101 0 minecraft:sand run say HUSH_LANDED
execute if block 40 101 0 minecraft:sand run say FREE_LANDED
execute if entity @e[type=minecraft:falling_block,x=-3,y=105,z=-3,dx=6,dy=10,dz=6] run say STILL_HELD" \
    LOG=/tmp/still.log timeout 900 ./tools/server_smoke.sh > /tmp/st.txt 2>&1 \
    || { tail -25 /tmp/st.txt; fail "the run did not complete"; }

# --- both zones opened ------------------------------------------------------
grep -q 'cast=still opened=true' /tmp/st.txt || {
    grep -oE 'cast=(still|hush) [a-z=0-9 ]+' /tmp/st.txt | head -4 || true
    fail "the Still zone did not open, so nothing below is about a stillness"; }
grep -q 'cast=hush opened=true' /tmp/st.txt || \
    fail "the Hush zone did not open, so the collision case has nothing in it"

# --- THE CONTROL: sand in neither zone still falls --------------------------
# Asserted first. Without it, "the sand in the Still did not land" is equally satisfied by
# gravity being broken everywhere -- and a mod that broke falling blocks globally would
# pass every other assertion in this file.
mark FREE_LANDED || {
    grep -iE "sand|falling|forceload" /tmp/st.txt | tail -8 || true
    fail "sand forty blocks from any zone did not land either. Falling blocks are not working anywhere in this world, so nothing below is evidence of a spell"; }

# --- the spell: mid-state, held ---------------------------------------------
if mark STILL_LANDED; then
    fail "sand inside a Still landed on the floor. WORLD.md locks the spell as freezing a falling block MID-STATE -- if it lands, nothing was held"
fi
mark STILL_HELD || {
    grep -oE '(STILL|HUSH|FREE)_[A-Z_]+' /tmp/still.log | sort -u || true
    fail "no falling block is being held in the Still zone. Sand that did not land and is not there has been DELETED, which satisfies 'it did not land' for entirely the wrong reason -- and a spell that removes the hazard is a far bigger and worse spell than the one that was locked"; }

# --- and a Hush is not a Still ----------------------------------------------
# The whole reason zones stopped being keyed by school. Both spells are the Quiet One's;
# if one list served both, this sand would hang in the air and the bug would be invisible
# from inside either spell.
mark HUSH_LANDED || \
    fail "sand inside a HUSH zone was frozen too. Hush and Still are both the Quiet One's, and keyed by school they become each other -- a silence would stop falling blocks and a stillness would mute creepers, with nothing failing anywhere and both spells appearing to work"

echo
echo "OK: a Still holds what is already falling without deleting it, and a Hush -- its own school-mate -- does not"
