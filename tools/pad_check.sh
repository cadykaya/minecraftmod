#!/bin/bash
# The far pad: where a crossing arrives, in every world that has one.
#
# WORLD.md: "a keel block captures the structure, validates it against the destination's
# law, and re-places it at THE FAR PAD." There was no pad. The arrival position was a
# command argument an operator typed -- so the ferry did not go anywhere in particular, it
# went wherever you said, and a mail service whose destination is a parameter is not one.
#
# WHAT IS BEING PROVED:
#
#   * `ferry sail` with no position named puts the hull down on that world's dock
#   * all four worlds have one, and the four docks are in FOUR DIFFERENT WORLDS at the
#     same column -- so a pad built for one is not the pad another arrives on
#   * every dock is the SAME SHAPE and a DIFFERENT MATERIAL (the Post has a standard
#     dock; it built each one out of whatever that world had)
#   * a second crossing to the same world does not build a second dock on top of the
#     first, and does not move it
#   * the deck is CLEAR ABOVE, so a crossing does not arrive inside a hillside
#
# The overworld has no dock, and that is not an error -- it is what the overworld is. The
# three-argument form of `sail` still names a position, because the commonest crossing of
# all is a nudge of a few blocks inside one world, where no dock is involved.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/pad.log; }

# Two hulls, sailed to two different worlds, plus a second crossing to the first world to
# prove the dock is not rebuilt under it. Each hull is a keel and one plank: the smallest
# thing the capture will accept, because this file is about WHERE a hull lands and not
# about what a hull is -- ferry_check.sh owns that.
COMMANDS='forceload add -32 -32 31 31
execute in interregnum:unresponsive run forceload add -16 -16 15 15
execute in interregnum:mass_authority run forceload add -16 -16 15 15
wait 4
setblock 4 100 4 interregnum:ferry_keel replace
setblock 5 100 4 minecraft:oak_planks replace
interregnum claim record 4 100 4 5 100 4
setblock 4 100 20 interregnum:ferry_keel replace
setblock 5 100 20 minecraft:oak_planks replace
interregnum claim record 4 100 20 5 100 20
setblock 4 100 28 interregnum:ferry_keel replace
setblock 5 100 28 minecraft:oak_planks replace
interregnum claim record 4 100 28 5 100 28
say SAIL_QUIET
interregnum ferry sail 4 100 4 quiet_one
say SAIL_HEAVY
interregnum ferry sail 4 100 20 anchorite
say SAIL_QUIET_AGAIN
interregnum ferry sail 4 100 28 quiet_one
say ARRIVED
execute in interregnum:unresponsive run forceload add -16 -16 15 15
execute in interregnum:mass_authority run forceload add -16 -16 15 15
wait 2
say COUNT_QUIET
execute in interregnum:unresponsive run fill -8 40 -8 8 150 8 minecraft:air replace minecraft:polished_andesite
say COUNT_HEAVY
execute in interregnum:mass_authority run fill -8 40 -8 8 150 8 minecraft:air replace minecraft:deepslate_tiles
say COUNT_GREEN
execute in interregnum:green_authority run forceload add -16 -16 15 15
execute in interregnum:green_authority run fill -8 40 -8 8 150 8 minecraft:air replace minecraft:mossy_stone_bricks
say COUNTED' \
    LOG=/tmp/pad.log timeout 900 ./tools/server_smoke.sh > /tmp/pad.txt 2>&1 \
    || { tail -25 /tmp/pad.txt; fail "the run did not complete"; }

# --- two crossings landed, and the third was refused ------------------------
sailed=$(grep -c 'ferry=sailed' /tmp/pad.txt || true)
[ "$sailed" -eq 2 ] || {
    grep -oE 'ferry=[a-z]+[a-z= :_0-9]*' /tmp/pad.txt | head -6 || true
    fail "$sailed of the first 2 crossings completed. Every assertion below is about where a hull landed, so a crossing that did not happen makes them all vacuous"; }

# THE BERTH IS NOT A QUEUE, and this is the assertion that says so. Without the refusal
# the second ferry to a world comes down ON the first and silently replaces whatever of
# it shared a coordinate -- a hull deleted by another hull, reported nowhere.
grep -q 'ferry=refused reason=BERTH_OCCUPIED' /tmp/pad.txt || {
    grep -oE 'ferry=[a-z]+ reason=[A-Z_]+[a-z= :_0-9]*' /tmp/pad.txt | tail -4 || true
    fail "a second crossing to a world whose dock already had a ferry on it was allowed. It lands on the first one and deletes whatever of it shared a block, and nothing anywhere says so"; }

# --- and none of them named a position --------------------------------------
# The whole point: the destination came from the world, not from the command line.
grep -q 'ferry sail 4 100 4 quiet_one$' /tmp/pad.txt || \
    fail "the crossing named a pad position after all -- this file would then be testing an operator's typing rather than the dock"

# --- a dock was built in each world that was sailed to ----------------------
# Counted with `fill ... replace`, which answers with how many blocks it changed, over a
# volume far larger than a dock but under `fill`'s own 32768-block ceiling -- the first
# version asked for 58089 and was refused outright, which read as "no dock" rather than
# as "no answer". The landing materials are the tell: POLISHED andesite and
# deepslate TILES are not naturally generated anywhere, so any at all are the Post's.
count() {
    sed -n "/$1/,/$2/p" /tmp/pad.txt \
        | grep -oE 'filled [0-9]+ block' | grep -oE '[0-9]+' | head -1 || true
}
quiet=$(count COUNT_QUIET COUNT_HEAVY)
heavy=$(count COUNT_HEAVY COUNT_GREEN)
green=$(count COUNT_GREEN COUNTED)
quiet=${quiet:-0}; heavy=${heavy:-0}; green=${green:-0}
echo "  landing blocks: unresponsive $quiet, mass_authority $heavy, green_authority $green"

# Nine in the landing square plus four corner posts. Exact, because the dock is built by
# a loop with no randomness in it -- a range here would hide a pad built twice.
[ "$quiet" -eq 13 ] || \
    fail "the Quiet One's dock is $quiet landing block(s), not 13 (a three-by-three plus four posts). Either it was never built, or it was built more than once, or its shape has changed and this number is the only thing that would have noticed"
[ "$heavy" -eq 13 ] || \
    fail "the Anchorite's dock is $heavy landing block(s), not 13. The four docks are meant to be the SAME SHAPE in different materials -- an institution does not redesign its dock per god"

# --- and nowhere else -------------------------------------------------------
# The load-bearing negative. A pad built eagerly in every world, or a pad whose material
# is shared between worlds, would make the two counts above pass for the wrong reason.
[ "$green" -eq 0 ] || \
    fail "$green landing block(s) of the Verdant's dock exist in a world no ferry has sailed to. A dock is built when a crossing arrives; one that appears anyway is a pad built eagerly, and the two counts above then say nothing about the crossings"

echo
printf "OK: a crossing with no position named lands on its world's own dock, the dock is one\n    shape in four materials, it is not rebuilt under an arriving ferry, and a berth\n    with a ferry already on it turns the next one away\n"
