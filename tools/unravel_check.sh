#!/bin/bash
# Does the overworld actually spend itself -- and does it stop where it must?
#
# The unraveling is the one system that edits a world people live in, so this
# checks the STOPPING as hard as the starting. In order, the things that would
# each be a silent disaster:
#
#   * it runs in Chapter 0                  -- the mod's whole promise is that it does not
#   * it ignores the band                   -- band 2 damage in band 1
#   * it ignores the scope                  -- band 1 everywhere instead of at thin places
#   * it converts a player-placed block     -- the design failure that loses the server
#   * it places a block that cannot stand   -- the rule LOOKS implemented and never fires
#   * a datapack cannot retune it           -- a claim made in the loader's javadoc
#
# Every position is asserted on the block that is actually there afterwards, never
# on the command having been accepted (docs/VERIFICATION.md rule 2).
#
# Flat world on purpose: the surface is grass_block at y=-61 everywhere, so a
# position is a fact rather than a hope.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }

want() {   # want <file> <string> <explanation>
    grep -qF "$2" "$1" || { echo "--- looked for: $2"; grep -E 'unravel=|swept=' "$1" || true; fail "$3"; }
}

SURF=-61      # top solid layer of the default superflat
AIR=-60

# ONLY=1 or ONLY=2 runs a single half. Part 2 reuses the world part 1 leaves
# behind, so ONLY=2 is valid only straight after a part-1 run.
ONLY=${ONLY:-}

if [ "$ONLY" != "2" ]; then
echo "1/2 the gates, on one world, in order"
# The claimed 3x3 sits at the exact centre of the sweep, so if the sweep converts
# the region around it and those nine columns survive, the claim is the only thing
# that can have spared them.
COMMANDS="forceload add -64 -64 127 127
setblock 20 $AIR 20 minecraft:short_grass
interregnum unravel at 20 $AIR 20
execute positioned 0 $AIR 0 run interregnum record deicide
interregnum status
interregnum unravel at 20 $AIR 20
setblock 100 $AIR 100 minecraft:short_grass
interregnum unravel at 100 $AIR 100
interregnum unravel at 100 $SURF 100
setblock 22 $AIR 22 minecraft:short_grass
interregnum claim record 22 $AIR 22 22 $AIR 22
interregnum unravel at 22 $AIR 22
interregnum record warden_contact
interregnum status
interregnum unravel at 100 $SURF 100
interregnum unravel at 100 $AIR 100
interregnum claim record 60 $SURF 60 62 $SURF 62
execute positioned 60 $SURF 60 run interregnum unravel sweep 8 40000
execute if block 60 $SURF 60 minecraft:grass_block
execute if block 61 $SURF 60 minecraft:grass_block
execute if block 62 $SURF 60 minecraft:grass_block
execute if block 60 $SURF 61 minecraft:grass_block
execute if block 61 $SURF 61 minecraft:grass_block
execute if block 62 $SURF 61 minecraft:grass_block
execute if block 60 $SURF 62 minecraft:grass_block
execute if block 61 $SURF 62 minecraft:grass_block
execute if block 62 $SURF 62 minecraft:grass_block" \
    LOG=/tmp/unravel1.log ./tools/server_smoke.sh > /tmp/u1.txt 2>&1 \
    || { tail -25 /tmp/u1.txt; fail "run 1 did not complete"; }

# Chapter 0 changes nothing -- and `now=` proves the setup block is really there,
# so a DORMANT result cannot be an artefact of setblock having done nothing.
want /tmp/u1.txt 'unravel=DORMANT rule=none thin=false now=minecraft:short_grass' \
    "the unraveling ran (or the test block was never placed) while the god was still alive"

want /tmp/u1.txt 'chapter=VIGIL band=1' "the deicide did not move the world to band 1"

# The clock is connected.
#
# Everything else in this file reaches the unraveling through a command, so a
# build where the tick handler was never registered would pass every one of them.
# `ticks` is the only witness that the level tick actually arrives.
ticks=$(grep -oE 'ticks=[0-9]+' /tmp/u1.txt | head -1 | grep -oE '[0-9]+' || true)
[ -n "$ticks" ] && [ "$ticks" -gt 0 ] \
    || fail "the level tick never reached the unraveling (ticks=${ticks:-absent}) -- the handler is not wired up"

# The table is loaded, from the datapack, with everything in it.
want /tmp/unravel1.log 'Unraveling: 2 band(s), 9 conversion(s) in force.' \
    "the unraveling table did not load, or loaded the wrong number of rules"

# Band 1, in a thin place: it happens.
want /tmp/u1.txt 'unravel=CONVERTED rule=grass_thins thin=true now=minecraft:air' \
    "band 1 did not convert grass at a thin place -- the unraveling does nothing at all"

# Band 1, outside a thin place: it does not.
want /tmp/u1.txt 'unravel=OUT_OF_SCOPE rule=grass_thins thin=false now=minecraft:short_grass' \
    "band 1 reached beyond the thin places; scope is not being honoured"

# A band-2 rule at band 1 must not fire.
want /tmp/u1.txt 'unravel=BAND_TOO_LOW rule=none thin=false now=minecraft:grass_block' \
    "a band-2 conversion fired at band 1"

# THE guarantee.
want /tmp/u1.txt 'unravel=CLAIMED rule=grass_thins thin=true now=minecraft:short_grass' \
    "the unraveling converted a player-placed block"

want /tmp/u1.txt 'chapter=ENFORCEMENT band=2' "warden contact did not move the world to band 2"

# Band 2 is world-wide, so the same position that was BAND_TOO_LOW now goes.
want /tmp/u1.txt 'unravel=CONVERTED rule=grass_dries thin=false now=minecraft:coarse_dirt' \
    "band 2 did not convert a grass block outside the thin places"

# ...but band 1 keeps ITS scope at band 2. Scope is per band, not per chapter.
grep -cF 'unravel=OUT_OF_SCOPE rule=grass_thins thin=false now=minecraft:short_grass' /tmp/u1.txt \
    | grep -qx 2 || fail "band 1's thin-places scope stopped applying once the world reached band 2"

converted=$(grep -oE 'swept=40000 converted=[0-9]+' /tmp/u1.txt | grep -oE '[0-9]+$' || true)
[ -n "$converted" ] || { grep -E 'swept=' /tmp/u1.txt || true; fail "the sweep produced no result line"; }
# 289 columns in radius 8, nine of them claimed. Anything much under 200 means the
# sweep barely reached the region, and then the nine survivors below prove nothing.
[ "$converted" -ge 200 ] \
    || fail "the sweep converted only $converted of ~280 available columns -- too few to conclude anything from the survivors"

passed=$(grep -cF 'Test passed' /tmp/u1.txt || true)
failed=$(grep -cF 'Test failed' /tmp/u1.txt || true)
[ "$passed" = "9" ] && [ "$failed" = "0" ] \
    || fail "a sweep that converted $converted columns also ate $failed of the 9 claimed ones"

fi

if [ "$ONLY" = "1" ]; then
    echo
    echo "OK: part 1 only"
    exit 0
fi

echo "2/2 a datapack can retune it, and a rule that cannot stand up never fires"
# The pack REPLACES the shipped table with a single rule that turns grass into a
# cactus. A cactus cannot stand on dirt, so the honest answer is UNSUPPORTED --
# and getting that answer at all proves the override took, because the shipped
# table would have answered CONVERTED/grass_dries at the same position.
PACK=run/world/datapacks/unravel_test
rm -rf "$PACK"
mkdir -p "$PACK/data/interregnum/unraveling"
cat > "$PACK/pack.mcmeta" <<'META'
{
  "pack": {
    "description": "unravel_check fixture -- replaces the unraveling table",
    "min_format": 88,
    "max_format": 107
  }
}
META
cat > "$PACK/data/interregnum/unraveling/bands.json" <<'BANDS'
{
  "_comment": [
    "Fixture for tools/unravel_check.sh. NOT shipped. One rule, deliberately",
    "impossible: a cactus cannot stand on dirt, so this must never convert."
  ],
  "bands": [
    {
      "band": 1,
      "chapter": "VIGIL",
      "scope": "overworld",
      "conversions": [
        { "id": "test_cactus", "from": "minecraft:grass_block", "to": "minecraft:cactus", "chance": 1.0 }
      ]
    }
  ]
}
BANDS

KEEP_WORLD=1 COMMANDS="forceload add -64 -64 127 127
interregnum status
interregnum unravel at 80 $SURF 80
setblock 20 $AIR 20 minecraft:short_grass
interregnum unravel at 20 $AIR 20" \
    LOG=/tmp/unravel2.log ./tools/server_smoke.sh > /tmp/u2.txt 2>&1 \
    || { rm -rf "$PACK"; tail -25 /tmp/u2.txt; fail "run 2 did not complete"; }
rm -rf "$PACK"

want /tmp/u2.txt 'chapter=ENFORCEMENT band=2' "the world forgot its chapter across the restart"
want /tmp/u2.txt 'unravel=UNSUPPORTED rule=test_cactus' \
    "either the datapack did not override the table, or a block that cannot stand there was placed anyway"
want /tmp/u2.txt 'unravel=NO_RULE' \
    "the shipped rules survived a datapack that replaces them -- the table is merging, not overriding"

echo
echo "OK: the unraveling honours dormancy, band, scope, claims, support, and datapacks"
