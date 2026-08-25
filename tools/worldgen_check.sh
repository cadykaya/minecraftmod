#!/bin/bash
# Prove the shrine actually builds in a live world, and that it contains what it
# should. This is the only way worldgen gets verified without a client.
#
# Every assertion is an `execute if block` that prints a marker on success, so a
# missing marker is a failure -- the check cannot pass by finding nothing, which is
# the trap that caught this project three times (docs/LESSONS.md #5, #7, #10).
set -euo pipefail
cd "$(dirname "$0")/.."

LOG=${LOG:-/tmp/interregnum-worldgen.log}
export LOG

# A flat world (server_smoke.sh writes level-type=flat and deletes run/world) makes
# the ground level everywhere, so a placement failure means the FEATURE is wrong
# rather than the terrain being unlucky.
#
# Coordinates: flat-world surface is grass at y=-61. findSurface returns the topmost
# SOLID block, so paving replaces the grass AT -61 -- it does not sit on top of it.
# Steles are 1 or 2 tall, so -60 is the only course guaranteed to be there.
# The offering box stands ON the carved centre stone, so it is at -60 too.
COMMANDS='forceload add -16 -16 47 47
place feature interregnum:shrine 8 -60 8
execute if block 8 -61 8 interregnum:shrine_stone_carved run say A_CENTRE_CARVED
execute if block 6 -61 6 interregnum:shrine_stone run say B_CORNER_PAVING
execute if block 6 -60 6 interregnum:warning_stele run say C_STELE_STANDS
execute if block 8 -60 8 minecraft:chest run say D_OFFERING_BOX
data get entity @e[type=interregnum:shrine_keeper,limit=1] Pos
data get entity @e[type=interregnum:shrine_keeper,limit=1] home_pos
data get entity @e[type=interregnum:shrine_keeper,limit=1] home_radius
data get block 8 -60 8 LootTable
interregnum talk scene @e[limit=1,type=interregnum:shrine_keeper]
setblock 8 -60 8 minecraft:air replace
setblock 8 -60 8 minecraft:chest replace
data get block 8 -60 8 LootTable
interregnum talk scene @e[limit=1,type=interregnum:shrine_keeper]' \
    ./tools/server_smoke.sh > /tmp/wg_smoke_out.txt 2>&1 || {
        echo "FAIL: the server smoke run itself failed"; tail -20 /tmp/wg_smoke_out.txt; exit 1; }

grep -q 'Placed "interregnum:shrine"' /tmp/wg_smoke_out.txt || {
    echo "FAIL: the shrine feature did not place on flat ground"
    grep -A3 'place feature' /tmp/wg_smoke_out.txt; exit 1; }

missing=""
for marker in A_CENTRE_CARVED B_CORNER_PAVING C_STELE_STANDS D_OFFERING_BOX; do
    grep -q "$marker" "$LOG" || missing="$missing $marker"
done
if [ -n "$missing" ]; then
    echo "FAIL: the shrine placed but is missing:$missing"
    exit 1
fi
# Where the keeper is, and what holds them there.
#
# The tether is the assertion; the position is a consequence of it. An earlier
# version asserted "an entity is within four blocks" and "its yaw points at the box",
# both of which PASSED HERE EVERY TIME and failed on the CI runner -- because RCON
# commands are seconds apart, the server ticks throughout, and the keeper had simply
# walked away and looked elsewhere between being placed and being asked about. The
# assertions were fine; the mob was wrong, and it is now tethered. See LESSONS #21.
#
# Facing is not asserted here at all and cannot be: a mob's yaw is overwritten by
# whatever it looks at next. The arithmetic is tested in core/spatial/Facing instead.
#
# The parsing lives in tools/keeper_pos_check.py: a Python heredoc inside this script
# shares its terminator with the one feeding commands to the server, and nesting them
# closed the outer one early and corrupted the file with no useful error.
pos=$(grep -m1 -oE 'Shrine-Keeper has the following entity data: \[[^]]*d\]' /tmp/wg_smoke_out.txt || true)
home=$(grep -m1 -oE 'Shrine-Keeper has the following entity data: \[I; [^]]*\]' /tmp/wg_smoke_out.txt || true)
radius=$(grep -m1 -oE 'Shrine-Keeper has the following entity data: [0-9-]+' /tmp/wg_smoke_out.txt || true)
python3 tools/keeper_pos_check.py "$pos" "$home" "$radius" 8 8 -60 5 || {
    echo "--- keeper replies ---"; grep -E 'Shrine-Keeper' /tmp/wg_smoke_out.txt || true
    echo "--- placement log ---"; grep -E 'could not seat|Placed' "$LOG" /tmp/wg_smoke_out.txt || true
    echo "FAIL: the keeper is not tethered to the shrine"; exit 1; }

# --- which scene the keeper opens with -------------------------------------
#
# The ledger scene is about a shortfall the players caused; at an untouched shrine
# its first line is simply false. The keeper picks by reading the offering box's
# PENDING loot table, which Minecraft clears the instant anybody opens the container.
#
# The setup is asserted at both ends (LESSONS #15). It has to be: clearing the table
# by writing a plain chest over the loot chest is a NO-OP, because setblock on an
# identical block does nothing and the block entity -- loot table and all -- survives
# untouched (LESSONS #14). That is exactly how the first version of this check
# "proved" the keeper was still intact-scened after a looting that never happened.
# Hence air first, then chest.
grep -q 'has the following block data: "interregnum:chests/shrine"' /tmp/wg_smoke_out.txt || {
    echo "FAIL: the offering box has no pending loot table, so 'untouched' proves nothing"; exit 1; }
grep -q 'Found no elements matching LootTable' /tmp/wg_smoke_out.txt || {
    echo "FAIL: the box's loot table was not actually cleared -- the setblock was a no-op"; exit 1; }

intact=$(grep -c 'scene=interregnum:shrine_keeper_intact' /tmp/wg_smoke_out.txt || true)
ledger=$(grep -c 'scene=interregnum:shrine_keeper$' /tmp/wg_smoke_out.txt || true)
[ "$intact" = "1" ] && [ "$ledger" = "1" ] || {
    grep -E 'scene=' /tmp/wg_smoke_out.txt
    echo "FAIL: the keeper does not change scene when the box is opened (intact=$intact ledger=$ledger, want 1 and 1)"
    exit 1; }
echo "  keeper greets an untouched shrine, and reaches for the ledger once it is not"

echo "OK: shrine places on flat ground with carved centre, paving, a standing stele,"
echo "    an offering box on the centre stone, and a keeper attending it"

