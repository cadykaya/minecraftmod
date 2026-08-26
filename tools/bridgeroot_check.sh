#!/bin/bash
# The third spell, and the one that makes magic a tool rather than an effect.
#
# WORLD.md, locked: "Verdancy (Verdant): BRIDGEROOT -- grow a living span toward your
# gaze, REAL PERSISTENT BLOCKS."
#
# Those last three words are the design brief. Most games' bridge spells are temporary
# platforms that evaporate, which is a movement ability wearing a spell's clothes. This
# one leaves actual world behind: you can build out of it, and somebody can walk across it
# a year later.
#
# THE ASSERTION THAT MATTERS is therefore not that blocks appear -- it is that they are
# YOURS. Every block a span leaves is recorded through the claim ledger exactly as if you
# had placed it by hand, which is what makes the apocalypse refuse to eat it: the
# unraveling, the Turning and band 4's attrition all consult that same ledger. A bridge
# the world dissolves next chapter is a temporary platform with extra steps, and a player
# who lost one that way would never trust the spell again.
#
# Also asserted:
#   * the span is CONTINUOUS. A bridge with a gap is worse than no bridge, because you
#     find out in the middle of it. Every block along the line, not a count.
#   * it never replaces anything. A span stops at the first block it meets rather than
#     boring through -- the alternative eats terrain, and worse, eats what somebody built.
#   * nothing is castable untaught, the rule every spell shares.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/bridge.log; }

WHO=99999999-9999-4999-8999-999999999999

# A span east from (0,100,0) toward (20,100,0): the cap is 12, so blocks 1..12 grow and
# 13 onward do not. A separate span runs at z=8 into a wall placed four blocks along, to
# prove it stops rather than bores.
PROBE=""
for i in $(seq 1 12); do
    PROBE="$PROBE
execute if block $i 100 0 minecraft:mangrove_roots run say SPAN_$i"
done

COMMANDS="forceload add -16 -16 31 31
wait 3
execute positioned 0 -60 0 run interregnum record deicide
say UNLEARNED
interregnum cast bridgeroot $WHO 0 100 0 20 100 0
execute if block 1 100 0 minecraft:mangrove_roots run say UNTAUGHT_GREW
interregnum learn $WHO verdancy
say TAUGHT
interregnum cast bridgeroot $WHO 0 100 0 20 100 0
$PROBE
execute if block 13 100 0 minecraft:mangrove_roots run say SPAN_OVERRAN
setblock 4 100 8 minecraft:obsidian replace
interregnum cast bridgeroot $WHO 0 100 8 20 100 8
execute if block 3 100 8 minecraft:mangrove_roots run say WALL_REACHED
execute if block 4 100 8 minecraft:obsidian run say WALL_INTACT
execute if block 5 100 8 minecraft:mangrove_roots run say WALL_BORED
interregnum claim at 1 100 0
interregnum claim at 12 100 0" \
    LOG=/tmp/bridge.log timeout 900 ./tools/server_smoke.sh > /tmp/br.txt 2>&1 \
    || { tail -25 /tmp/br.txt; fail "the run did not complete"; }

# --- nothing is castable untaught -------------------------------------------
if mark UNTAUGHT_GREW; then
    fail "somebody who had never been taught Verdancy grew a span anyway -- schools are learned in their worlds, and a spell that skips that makes the journey buy nothing"
fi
grep -q 'cast=bridgeroot grew=0 frayed=0 refused=unlearned' /tmp/br.txt || {
    grep -oE 'cast=bridgeroot [a-z=0-9 ]+' /tmp/br.txt | head -3 || true
    fail "an untaught cast did not report itself refused for want of teaching"; }

# --- the span is CONTINUOUS -------------------------------------------------
# Every block along the line, asked for individually. A count would be satisfied by twelve
# blocks with a hole in the middle, which is the one failure that matters here: you find
# out about a gap while standing over it.
missing=""
for i in $(seq 1 12); do
    mark "SPAN_$i\$" || missing="$missing $i"
done
[ -z "$missing" ] || {
    grep -oE 'cast=bridgeroot [a-z=0-9 ]+' /tmp/br.txt | head -3 || true
    fail "the span has holes in it at block(s):$missing -- a bridge you can fall through is worse than no bridge, because you only find out in the middle of it"; }

# --- and it stops at its cap ------------------------------------------------
if mark SPAN_OVERRAN; then
    fail "the span reached past its twelve-block cap. Crossing anything large is supposed to be several casts and therefore several costs; a span of any length makes the fraying a rounding error and the overworld ban unenforceable"
fi

# --- it never replaces anything ---------------------------------------------
mark WALL_REACHED || \
    fail "the second span did not even reach the wall, so nothing below is about what it did when it got there"
mark WALL_INTACT || \
    fail "a span grew THROUGH an obsidian block. It is supposed to stop at the first thing it meets -- a spell that bores through terrain also bores through whatever somebody built in the way"
if mark WALL_BORED; then
    fail "the span continued on the far side of the wall -- it tunnelled rather than stopping, which is the same failure wearing a politer shape"
fi

# --- THE ASSERTION: what you grew is yours ----------------------------------
# The claim ledger is what makes "real persistent blocks" true. Every system that eats the
# world consults it, so a span recorded there is a span the apocalypse will refuse.
claimed=$(grep -c 'claimed=true' /tmp/br.txt || true)
[ "$claimed" = "2" ] || {
    grep -oE 'claimed=[a-z]+ chunkPlacements=[0-9]+' /tmp/br.txt | head -4 || true
    fail "only $claimed of the 2 probed span blocks are recorded as somebody's work. WORLD.md promises real persistent blocks, and the only thing making them persistent is the claim ledger -- unrecorded, the unraveling and attrition will eat the bridge and the player will never trust the spell again"; }

echo
echo "OK: a taught caster grows a continuous span that stops where it should, and every block of it is somebody's work"
