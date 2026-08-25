#!/bin/bash
# When the ground gives way, does a player's work survive it?
#
# This is WORLD.md's standing guarantee -- the world may warp, but the player's
# work may not -- and the crater is the first thing in the mod that could break it.
# Minecraft does not record who placed a block, so the guarantee is enforced by a
# narrow tag whitelist that errs toward sparing. This proves both halves: natural
# ground goes, placed blocks stay.
#
# The failure this guards against is unrecoverable. A slightly lumpy crater is a
# cosmetic complaint; a deleted house is somebody quitting the server.
set -euo pipefail
cd "$(dirname "$0")/.."

COMMANDS='forceload add -32 -32 31 31
setblock 0 -61 0 minecraft:diamond_block replace
setblock 1 -61 1 minecraft:chest replace
setblock 2 -61 2 minecraft:oak_planks replace
setblock 3 -61 3 minecraft:glass replace
execute if block 0 -62 0 minecraft:dirt run say PRE_GROUND_PRESENT
interregnum record deicide
execute if block 0 -61 0 minecraft:diamond_block run say SPARED_DIAMOND
execute if block 1 -61 1 minecraft:chest run say SPARED_CHEST
execute if block 2 -61 2 minecraft:oak_planks run say SPARED_PLANKS
execute if block 3 -61 3 minecraft:glass run say SPARED_GLASS
execute if block 0 -62 0 air run say GROUND_GONE_CENTRE
execute if block 4 -62 4 air run say GROUND_GONE_WIDE
execute if block 0 -64 0 minecraft:bedrock run say BEDROCK_INTACT' \
    LOG=/tmp/crater.log ./tools/server_smoke.sh > /tmp/crater_out.txt 2>&1 \
    || { tail -20 /tmp/crater_out.txt; echo "FAIL: the run did not complete"; exit 1; }

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/crater.log; }

mark PRE_GROUND_PRESENT || fail "there was no natural ground to remove -- the test proves nothing"

missing=""
for m in SPARED_DIAMOND SPARED_CHEST SPARED_PLANKS SPARED_GLASS; do
    mark "$m" || missing="$missing ${m#SPARED_}"
done
[ -z "$missing" ] || fail "the crater DESTROYED player-placed blocks:$missing"

mark GROUND_GONE_CENTRE || fail "the ground did not subside at the centre"
mark GROUND_GONE_WIDE   || fail "the crater is too small -- nothing moved 4 blocks out"
mark BEDROCK_INTACT     || fail "bedrock was removed; the crater must never breach the floor"

echo
grep -oE 'The ground gave way.*' /tmp/crater.log | head -1 | sed 's/^/    /'
echo
echo "OK: natural ground subsides, bedrock holds, and everything a player placed"
echo "    is left standing over the pit"
