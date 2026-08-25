#!/bin/bash
# Does the ferry lift a boat off the ground without taking the ground with it?
#
# The hard problem of the whole system is the capture. A flood-fill from the keel
# through connected solid blocks eats the seabed, then the mountain, then the world.
# The answer is that the ferry takes only what a PLAYER PLACED -- Claims has recorded
# that since the unraveling needed to know what not to eat -- so the walk stops dead
# at natural terrain.
#
# That is the assertion this file exists for, and it is the one that would be a
# catastrophe to get wrong in either direction: too greedy and the ferry deletes
# somebody's island, too shy and it leaves half the hull at the dock.
#
# Also asserted, because the checklist is the mod's cheapest tutorial and a checklist
# that does not fire teaches nothing:
#   * a clean hull crosses
#   * a hull carrying a refused block is HELD, and the notice names the block AND the
#     count AND the reason
#   * every violation is listed, not just the first
#   * a bare keel on the ground is refused as NOTHING_BUILT rather than sailing empty
#   * a coordinate with no keel on it is refused as NOT_A_KEEL, because otherwise the
#     ferry is a command that teleports any structure anybody ever built
#   * a hull NUDGED two blocks along -- destination overlapping origin -- arrives whole.
#     This is the most ordinary thing anybody will do with a ferry and the one that
#     a block-by-block move silently eats: the move erases its own destination.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
# `|| true` on every dump: under set -e -o pipefail a dump that matches nothing kills
# the script before the message it exists to explain (docs/LESSONS.md #23).
want() { grep -qF "$2" "$1" || { echo "--- looked for: $2"; grep -E 'ferry=' "$1" || true; fail "$3"; }; }

# A flat world: the ground at y=-61 is NATURAL and nothing claims it, which is exactly
# the surface the hull has to refuse to pick up.
#
# The hull is built with setblock and then CLAIMED by command, because setblock is not
# a player placing a block and Claims only records real placements. `interregnum claim
# record` is the same call the place handler makes.
COMMANDS="forceload add -32 -32 31 31
wait 3
setblock 4 -60 4 interregnum:ferry_keel replace
setblock 5 -60 4 minecraft:oak_planks replace
setblock 6 -60 4 minecraft:oak_planks replace
setblock 7 -60 4 minecraft:oak_planks replace
setblock 5 -59 4 minecraft:chest replace
interregnum claim record 4 -60 4 7 -59 4
interregnum ferry manifest 4 -60 4
interregnum ferry check 4 -60 4 quiet_one
setblock 6 -59 4 minecraft:note_block replace
setblock 7 -59 4 minecraft:jukebox replace
interregnum claim record 6 -59 4 7 -59 4
interregnum ferry check 4 -60 4 quiet_one
interregnum ferry check 4 -60 4 anchorite
setblock 6 -59 4 minecraft:air replace
setblock 7 -59 4 minecraft:air replace
interregnum ferry sail 4 -60 4 quiet_one 20 -60 20
execute if block 20 -60 20 interregnum:ferry_keel run say KEEL_ARRIVED
execute if block 21 -60 20 minecraft:oak_planks run say HULL_ARRIVED
execute if block 23 -60 20 minecraft:oak_planks run say STERN_ARRIVED
execute if block 21 -59 20 minecraft:chest run say FURNITURE_ARRIVED
execute if block 4 -60 4 minecraft:air run say ORIGIN_CLEARED
execute if block 20 -61 20 minecraft:grass_block run say GROUND_INTACT
execute if block 4 -61 4 minecraft:grass_block run say SEABED_LEFT_BEHIND
interregnum ferry sail 20 -60 20 quiet_one 22 -60 20
interregnum ferry manifest 22 -60 20
execute if block 22 -60 20 interregnum:ferry_keel run say NUDGE_KEEL_INTACT
execute if block 25 -60 20 minecraft:oak_planks run say NUDGE_STERN_INTACT
execute if block 23 -59 20 minecraft:chest run say NUDGE_FURNITURE_INTACT
execute if block 20 -60 20 minecraft:air run say NUDGE_ORIGIN_CLEARED
interregnum ferry manifest 12 -60 12
setblock 12 -60 12 interregnum:ferry_keel replace
interregnum claim record 12 -60 12 12 -60 12
interregnum ferry manifest 12 -60 12" \
    LOG=/tmp/ferry.log timeout 2000 ./tools/server_smoke.sh > /tmp/fc.txt 2>&1 \
    || { tail -25 /tmp/fc.txt; fail "the run did not complete"; }

grep -q 'crossing law(s) loaded' /tmp/ferry.log || {
    grep -iE "crossing law|ferry" /tmp/ferry.log | tail -5 || true
    fail "the crossing laws did not load, so every check below proves nothing"; }

# --- the manifest is a census of what was BUILT, not of the world ------------
# Four placed blocks. If the walk leaked into the ground this number is enormous;
# if it stopped too early it is smaller. Either way it is not four.
want /tmp/fc.txt 'ferry=manifest total=5' \
    "the hull census is not the five blocks that were built -- the capture is leaking into the terrain or stopping short"
want /tmp/fc.txt 'minecraft:oak_planksx3' "the hull lost its planks, or counted them wrong"
want /tmp/fc.txt 'interregnum:ferry_keelx1' "the keel is not on its own manifest, so it would sail without itself"
want /tmp/fc.txt 'minecraft:chestx1' "the furniture is not on the bill of lading"

# --- a clean hull crosses ---------------------------------------------------
want /tmp/fc.txt 'ferry=clear law=quiet_one total=5' \
    "a silent hull was not cleared for the Quiet One's crossing"

# --- a noisy one is held, and the notice TEACHES ----------------------------
want /tmp/fc.txt 'ferry=held law=quiet_one violations=2' \
    "a hull carrying a note block and a jukebox was not held, or only one of them was reported"
want /tmp/fc.txt 'ferry-notice no_sound minecraft:note_block x1 [interregnum.ferry.quiet_one.no_sound]' \
    "the notice does not name the block, the count and the reason -- the checklist is the tutorial"

# The SAME hull is legal somewhere else. Without this the check could pass against an
# implementation that simply refuses everything (docs/LESSONS.md #15).
want /tmp/fc.txt 'ferry=clear law=anchorite' \
    "a hull the Quiet One refused was also refused by the Anchorite -- the laws are not distinct"

# --- the crossing itself ----------------------------------------------------
want /tmp/fc.txt 'ferry=sailed law=quiet_one total=5' "the cleared hull did not sail"
for marker in KEEL_ARRIVED HULL_ARRIVED STERN_ARRIVED FURNITURE_ARRIVED ORIGIN_CLEARED; do
    grep -q "$marker" /tmp/ferry.log || {
        grep -E 'ferry=|Ferry' /tmp/fc.txt /tmp/ferry.log | tail -10 || true
        fail "the ferry did not arrive intact: missing $marker"; }
done

# --- AND IT LEFT THE PLANET WHERE IT WAS ------------------------------------
# The single most important assertion in this file. A capture that took the ground
# would have deleted the grass under the dock.
grep -q SEABED_LEFT_BEHIND /tmp/ferry.log || {
    fail "the ground under the dock is gone -- the ferry took the world with it"; }

# --- a two-block nudge does not eat the hull --------------------------------
# Origin and destination overlap. A move that clears and writes block-by-block
# erases blocks it has already placed, and the hull arrives short -- which reads
# to a player as the ferry having eaten part of their boat, because it did.
# COUNTED, not grepped. The nudged hull's manifest is character-for-character the
# dock's manifest -- that is the whole point of the assertion -- so a plain `want`
# for it matches the line printed before the crossing and can never tell the two
# apart. It sat here green through a deliberately broken one-pass move before this
# was noticed (docs/LESSONS.md #24).
seen=$(grep -cF 'ferry=manifest total=5 interregnum:ferry_keelx1 minecraft:chestx1 minecraft:oak_planksx3' /tmp/fc.txt || true)
[ "$seen" = 2 ] || {
    grep -E 'ferry=' /tmp/fc.txt || true
    fail "the hull did not survive a two-block nudge (saw $seen/2 identical manifests) -- the move is eating its own destination"; }
for marker in NUDGE_KEEL_INTACT NUDGE_STERN_INTACT NUDGE_FURNITURE_INTACT NUDGE_ORIGIN_CLEARED; do
    grep -q "$marker" /tmp/ferry.log || \
        fail "the nudged ferry did not arrive whole: missing $marker"
done

# --- a coordinate with no keel on it is not a ferry -------------------------
# Without this the sail command is "teleport any structure", and the keel means
# nothing. It is checked BEFORE the bare-keel case because the two refusals are
# easy to confuse and only one of them proves the block matters.
want /tmp/fc.txt 'ferry=refused reason=NOT_A_KEEL' \
    "bare ground answered to the ferry -- the keel block is not actually required"

# --- a bare keel is refused, not sailed empty -------------------------------
want /tmp/fc.txt 'ferry=refused reason=NOTHING_BUILT' \
    "a keel standing on the ground with nothing built on it did not refuse"

echo
echo "OK: the ferry lifts what was built, leaves the ground, and says why when it will not"
