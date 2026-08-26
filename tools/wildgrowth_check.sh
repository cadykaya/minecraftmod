#!/bin/bash
# The Verdant's second spell: everything in a small volume lurches forward at once.
#
# WORLD.md, locked: "Verdancy (Verdant): Bridgeroot ... Hedge . Graft . Wildgrowth -- and
# in the Verdant's own world, accelerating growth is a HAZARD."
#
# WHAT IS BEING PROVED:
#
#   * a cast advances what is growing inside it   (the spell)
#   * an identical plant outside it does not      (the control, and the whole reason
#                                                  `random_tick_speed` is 0 below)
#   * a plant somebody PLACED is spared           (the ledger gates what you did not aim
#                                                  at -- see Wildgrowth's javadoc; this is
#                                                  the half that separates an area spell
#                                                  from an aimed one)
#   * an untaught caster cannot cast at all       (schools are learned in their worlds)
#
# `gamerule random_tick_speed 0` is load-bearing, not tidiness, and it is what makes the
# control CATEGORICAL rather than a comparison. Vanilla grows sugar cane on its own random
# ticks; with the rule at zero, the only thing in this world that can advance a cane is
# `Verdant.quicken`, and `Verdant` does its own explicit ticks and never consults the
# gamerule. So "the control did not grow" is a fact about the spell rather than a margin
# somebody chose (docs/LESSONS.md #27). The rule is read back from the server because 26.x
# renamed the gamerule set to snake_case and a rejected one is silent (LESSONS #15).
#
# SUGAR CANE, and not wheat, on purpose. Cane counts its own ticks: sixteen advance it one
# segment, deterministically, with no light check and no probability anywhere. Wheat would
# have made every assertion here a threshold on a random variable, which is the mistake
# docs/LESSONS.md #31 records three times in one night.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/wildgrowth.log; }

WHO=11111111-2222-4333-8444-555555555555

# Three canes, twenty blocks apart so one cast cannot reach two of them. Each stands on
# DIRT with a water source walled into the bed beside it.
#
# Every part of that is a repair. The first version stood the cane on a single sand block
# in mid-air with a loose water source next to it: SAND FALLS, so the whole column
# collapsed the tick after it was placed and all three canes broke. The setup probe passed
# anyway, because it ran before gravity did -- so the file reported "the control cane is
# gone" about its own scenery.
#
# Hence: dirt rather than sand, a stone floor under everything so nothing is standing on
# air, and the water sunk into a bed of dirt so the source cannot flow away from the block
# it has to be adjacent to. A cane that broke for want of water reads exactly like a cane
# that failed to grow.
#
#    0  inside the cast          the spell
#   20  no cast at all           the control
#   40  inside the cast, CLAIMED the ledger
COMMANDS="gamerule random_tick_speed 0
gamerule random_tick_speed
forceload add -16 -16 63 16
wait 3
fill -1 99 -1 2 99 1 minecraft:stone replace
fill -1 100 -1 2 100 1 minecraft:dirt replace
setblock 1 100 0 minecraft:water replace
setblock 0 101 0 minecraft:sugar_cane replace
fill 19 99 -1 22 99 1 minecraft:stone replace
fill 19 100 -1 22 100 1 minecraft:dirt replace
setblock 21 100 0 minecraft:water replace
setblock 20 101 0 minecraft:sugar_cane replace
fill 39 99 -1 42 99 1 minecraft:stone replace
fill 39 100 -1 42 100 1 minecraft:dirt replace
setblock 41 100 0 minecraft:water replace
setblock 40 101 0 minecraft:sugar_cane replace
interregnum claim record 40 101 0 40 101 0
execute if block 0 101 0 minecraft:sugar_cane run say SETUP_CAST
execute if block 20 101 0 minecraft:sugar_cane run say SETUP_FREE
execute if block 40 101 0 minecraft:sugar_cane run say SETUP_CLAIMED
say CAST_UNLEARNED
interregnum cast wildgrowth $WHO 0 101 0
interregnum learn $WHO verdancy
say CAST_TAUGHT
interregnum cast wildgrowth $WHO 0 101 0
interregnum cast wildgrowth $WHO 40 101 0
wait 2
say AFTER_CAST
execute if block 0 102 0 minecraft:sugar_cane run say CAST_GREW
execute if block 20 102 0 minecraft:sugar_cane run say FREE_GREW
execute if block 20 101 0 minecraft:sugar_cane run say FREE_ALIVE
execute if block 40 102 0 minecraft:sugar_cane run say CLAIMED_GREW
execute if block 40 101 0 minecraft:sugar_cane run say CLAIMED_ALIVE" \
    LOG=/tmp/wildgrowth.log timeout 900 ./tools/server_smoke.sh > /tmp/wg.txt 2>&1 \
    || { tail -25 /tmp/wg.txt; fail "the run did not complete"; }

# --- the world this was measured in -----------------------------------------
grep -q 'random_tick_speed is currently set to: 0' /tmp/wg.txt || {
    grep -iE 'random_tick_speed|gamerule' /tmp/wg.txt | head -4 || true
    fail "vanilla's random ticking is not off in this world -- the gamerule was rejected or ignored, so vanilla grows cane too and 'the control did not grow' would be luck rather than a fact about the spell"; }

# --- the setup, before anything that rests on it ----------------------------
mark SETUP_CAST    || fail "no cane was planted where the spell is cast -- there is nothing for it to advance"
mark SETUP_FREE    || fail "no cane was planted at the control column, so 'it did not grow there' is about bare sand"
mark SETUP_CLAIMED || fail "the claimed cane was never planted, so sparing it proves nothing"

# --- nothing is known by default --------------------------------------------
grep -q 'cast=wildgrowth grew=0 frayed=0 refused=unlearned' /tmp/wg.txt || {
    grep -oE 'cast=wildgrowth [a-z=0-9 ]+' /tmp/wg.txt | head -3 || true
    fail "somebody who had never been taught Verdancy cast Wildgrowth anyway, or was refused for some other reason. WORLD.md locks schools as learned in their gods' worlds -- casting untaught removes the reason to cross, and every success below would be a default rather than a rule"; }

# --- THE CONTROL: nothing grows on its own in this world --------------------
# Asserted before the spell, because it is what gives the spell's success a meaning.
mark FREE_ALIVE || \
    fail "the control cane is gone entirely -- it broke rather than stood still, so this column is measuring cane survival and not growth"
if mark FREE_GREW; then
    fail "cane twenty blocks from any cast grew anyway. Something in this world is advancing plants with random_tick_speed read back as zero, so the cast column below proves nothing"
fi

# --- the spell --------------------------------------------------------------
mark CAST_GREW || {
    grep -oE 'cast=wildgrowth [a-z=0-9 ]+' /tmp/wg.txt | head -4 || true
    grep -oE '(CAST|FREE|CLAIMED)_[A-Z]+' /tmp/wildgrowth.log | sort -u || true
    fail "a cast of Wildgrowth did not advance the cane standing in it. Sugar cane takes exactly sixteen random ticks per segment and one cast delivers more than that to every position it covers, so either the surge is not reaching the block or it is not ticking it"; }

# --- and it spares what nobody aimed at -------------------------------------
# The half that makes this an AREA spell rather than an aimed one. LESSONS #35 says the
# ledger gates the world and not the caster, which is right for a spell that names one
# block; a cube is full of things nobody named, including other people's gardens.
mark CLAIMED_ALIVE || \
    fail "the claimed cane is gone -- it broke rather than stood still, so this column says nothing about the ledger"
if mark CLAIMED_GREW; then
    fail "Wildgrowth advanced a plant somebody had placed. A surge that cannot be aimed must not reach into what people built: the ledger gates what you did not aim at, and every block in a cube except the one at its centre is something nobody aimed at"
fi

echo
printf "OK: a cast advances what is growing where it lands, an identical plant outside it does\n    not, and a plant somebody placed is left alone\n"
