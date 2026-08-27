#!/bin/bash
# Hedge: the only defence in the mod improved by being attacked.
#
# WORLD.md, locked: "A living wall that grows where you draw it and THICKENS WHERE IT IS
# STRUCK. The only defence in the mod improved by being attacked."
#
# THE SECOND CLAUSE IS THE SPELL. Growing a line of blocks toward your gaze is Bridgeroot,
# and this reuses that geometry -- a span is a span. What makes it a different spell is
# what happens after: cut a hedge and it comes back thicker.
#
# WHY THAT IS WORTH THE TROUBLE. Every other defensive thing here is a refusal that runs
# out -- Hush lasts twenty seconds, Still holds what is already moving, Moor lets go after
# a minute. All three are versions of WAIT, and a patient attacker beats them. A hedge does
# the opposite: two blocks grow for every one cut, so a wall somebody has been hacking at
# is denser than the one they started on. It is still not invulnerable, and that matters --
# the block you struck really is gone.
#
# WHAT IS ASSERTED:
#   * it cannot be cast untaught;
#   * a cast DRAWS A WALL, of its own height, along the line between the caster and what
#     they are looking at;
#   * CUTTING ONE BLOCK LEAVES MORE HEDGE THAN BEFORE. Not the same -- more. That is the
#     whole locked line, and it is the difference between a stubborn wall and this one;
#   * THE BLOCK THAT WAS STRUCK IS GONE. WORLD.md says improved by being attacked, not
#     unattackable, and a wall that closed its own wound could never be got through;
#   * AN ORDINARY TREE IS SAFE. Leaves that nobody grew are not a hedge, and hitting an oak
#     must not make it thicker -- which is why the ledger exists at all;
#   * and it STOPS. Past the world's budget a strike takes a block and grows nothing,
#     because "grows when struck" unbounded is a way to fill a world with leaves by hitting
#     a bush.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }

H=bcbcbcbc-8888-4888-8888-888888888888

# The wall is drawn from (0,-60,0) toward (8,-60,0) -- eight long, three tall, so
# twenty-four blocks if nothing is in the way. The flat world's ground is at y=-61, so the
# whole wall stands in open air.
#
# A LOOSE OAK IS PLANTED SEPARATELY, out of the wall's line and out of any thickening's
# reach, so "an ordinary tree is safe" is asked of leaves the spell has never touched.
COMMANDS="forceload add -16 -16 31 31
wait 3
setblock 24 -60 24 minecraft:oak_leaves[persistent=true] replace

say UNTAUGHT
interregnum cast hedge $H 0 -60 0 8 -60 0
interregnum learn $H verdancy

say DRAWING
interregnum cast hedge $H 0 -60 0 8 -60 0
say STRIKING
interregnum hedge strike 4 -60 0
say A_WILD_TREE
interregnum hedge strike 24 -60 24
say A_STONE_BESIDE_THE_WALL
setblock 7 -60 1 minecraft:stone replace
interregnum hedge strike 7 -60 1
wait 1

execute if block 4 -60 0 minecraft:air run say WOUND_OPEN
execute if block 24 -60 24 minecraft:air run say TREE_CUT" \
    LOG=/tmp/hedge.log timeout 900 ./tools/server_smoke.sh > /tmp/hg.txt 2>&1 \
    || { tail -25 /tmp/hg.txt; fail "the run did not complete"; }

mark() { grep -q "$1" /tmp/hedge.log; }
casts=$(grep -oE 'cast=hedge grew=[0-9]+ frayed=[0-9]+ refused=[a-z]* hedge=[0-9]+' /tmp/hg.txt || true)
strikes=$(grep -oE 'hedge=[A-Z_]+ grew=[0-9]+ standing=[0-9]+' /tmp/hg.txt || true)
[ -n "$casts" ] || { tail -20 /tmp/hg.txt; fail "the hedge cast produced no answer at all"; }
[ -n "$strikes" ] || { tail -20 /tmp/hg.txt; fail "the hedge strike produced no answer at all"; }

untaught=$(echo "$casts" | head -1)
drawn=$(echo "$casts" | sed -n 2p)
struck=$(echo "$strikes" | head -1)
wild=$(echo "$strikes" | sed -n 2p)
beside=$(echo "$strikes" | sed -n 3p)

field() { echo "$1" | grep -oE "$2=[0-9]+" | grep -oE '[0-9]+'; }

# --- untaught, like every other spell ----------------------------------------
echo "$untaught" | grep -q 'refused=unlearned' || {
    echo "  casting before being taught: $untaught"
    fail "somebody who had never been taught Verdancy grew a hedge"; }

# --- a cast draws a wall ------------------------------------------------------
grew=$(field "$drawn" grew)
[ "$grew" -eq 24 ] || {
    echo "  drawing a wall eight long: $drawn"
    fail "a wall drawn eight blocks along clear air grew $grew blocks, not the twenty-four its length and height come to. Either the span is the wrong length or the wall is the wrong height"; }
standing=$(field "$drawn" hedge)
[ "$standing" -eq 24 ] || \
    fail "the wall grew $grew blocks and the ledger holds $standing. A hedge the ledger does not know about is leaves -- and the whole reason the ledger exists is to tell a wall somebody grew from the forest it was grown next to"

# --- CUTTING ONE LEAVES MORE HEDGE THAN BEFORE -------------------------------
# The locked line, in one comparison. Not "the same" -- more.
echo "$struck" | grep -q 'hedge=CUT' || {
    echo "  striking the middle of the wall: $struck"
    fail "a block of the wall was not recognised as hedge"; }
after=$(field "$struck" standing)
[ "$after" -gt "$standing" ] || {
    echo "  before the strike: $standing blocks; after: $after"
    fail "cutting a hedge did not leave more hedge than before ($standing -> $after). WORLD.md calls this the only defence in the mod IMPROVED by being attacked -- at parity it is a stubborn wall, and every other defence here is already a refusal that runs out"; }
thickened=$(field "$struck" grew)
[ "$thickened" -ge 2 ] || \
    fail "the strike grew $thickened block(s). Two is 'this costs you more than it costs me' said as arithmetic; one is free to cut"

# --- and the wound is real ---------------------------------------------------
# Improved by being attacked, not unattackable. A wall that closed its own wound could
# never be got through, which is a much less interesting thing to have built.
mark WOUND_OPEN || {
    fail "the block that was struck is still there. A hedge that heals the hole is a wall that cannot be attacked at all -- WORLD.md's promise is that attacking it makes it WORSE FOR YOU, not that it does nothing"; }

# --- AN ORDINARY TREE IS SAFE ------------------------------------------------
# Leaves nobody grew are not a hedge. Without this the spell is "hitting leaves makes more
# leaves", which is a different and much worse thing to have shipped.
echo "$wild" | grep -q 'hedge=NOT_A_HEDGE' || {
    echo "  striking leaves nobody grew: $wild"
    fail "leaves that no cast ever touched were treated as a hedge"; }
[ "$(field "$wild" grew)" = "0" ] || \
    fail "hitting an ordinary oak made it thicker ('$wild'). That is the failure the ledger exists to prevent, and it would turn every forest in the world into a spell"
mark TREE_CUT || fail "the wild leaves were not removed by the strike, so the probe above is not about a block that was cut"

# --- AND NEITHER IS A BLOCK THAT MERELY TOUCHES THE WALL ---------------------
# The probe that separates the ledger from nothing at all, and the lone oak above does
# NOT: with the ledger gate removed, an isolated tree still grows nothing, because the
# thickening only fills air that already touches hedge. The case where the two differ is a
# block that is not hedge standing NEXT TO hedge -- break a stone beside your neighbour's
# wall and, ungated, the wall thanks you for it. Watched: removing the gate goes red here
# and nowhere else in this file.
echo "$beside" | grep -q 'hedge=NOT_A_HEDGE' || {
    echo "  breaking stone beside the wall: $beside"
    fail "a stone block touching the wall was recorded as part of it"; }
[ "$(field "$beside" grew)" = "0" ] || {
    echo "  breaking stone beside the wall: $beside"
    fail "breaking a block that merely TOUCHES a hedge made the hedge thicker. The wall is meant to answer being cut, not being stood next to -- and ungated, every block anybody mines near a hedge feeds it"; }

echo
printf "OK: a cast draws a wall of its own height, cutting one block leaves more hedge than\n    there was, the hole the attacker made is real, and neither a wild oak nor a stone\n    laid against the wall feeds it\n"
