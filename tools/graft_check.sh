#!/bin/bash
# Graft: a plant lives somewhere it could not.
#
# WORLD.md, locked: "Join two growing things, or a growing thing to a block, so one feeds
# the other -- and A PLANT LIVES SOMEWHERE IT COULD NOT."
#
# THE ONLY SPELL IN THE KIT THAT ACTS ON A RELATIONSHIP. Everything else acts on a place
# (Lighten, Hush), a volume (Wildgrowth), a thing (Moor, Quell) or a person (Held-breath).
# This one acts on the fact that two positions are joined, and it is the reason Graft was
# left until last: it needed a ledger of PAIRS, and nothing else in the mod wanted one.
#
# WHAT THE SPELL DOES AND DOES NOT DO. It does not make the ground suitable and does not
# make the plant hardier. It makes something ELSE responsible for keeping it alive, which
# is what a graft is -- so the world removes the scion whenever it happens to look, and the
# graft puts it back, for as long as the stock is there.
#
# WHAT IS ASSERTED:
#   * it cannot be cast untaught;
#   * a plant cannot be grafted TO ITSELF -- that is not a short graft, it is a join
#     nothing could ever cut;
#   * A SCION SURVIVES WHERE ITS OWN BLOCK CANNOT. Wheat on bare stone, put back;
#   * THE CONTROL -- identical wheat, on identical stone, with no graft -- does not come
#     back. Without it, "the wheat is there" is equally satisfied by wheat that was never
#     in danger;
#   * CUTTING THE STOCK ENDS IT. The join is dropped and the scion is left on its own,
#     which for a plant with no business being there means gone;
#   * and a graft LOSES to somebody's building. A spell that overwrote a block a person put
#     down would eat other people's work on a timer, which is the one shape this mod
#     refuses everywhere else.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/graft.log; }

G=cdcdcdcd-9999-4999-8999-999999999999

# The stock is wheat on farmland, which is where wheat belongs. The scion is bare stone,
# which is where it does not. Both stand on the flat world's ground at y=-61.
COMMANDS="forceload add -16 -16 31 31
wait 3
setblock 4 -61 4 minecraft:farmland replace
setblock 4 -60 4 minecraft:wheat[age=3] replace
setblock 6 -61 4 minecraft:stone replace
setblock 20 -61 20 minecraft:stone replace
wait 2

say UNTAUGHT
interregnum cast graft $G 4 -60 4 6 -60 4
interregnum learn $G verdancy
say ITSELF
interregnum cast graft $G 4 -60 4 4 -60 4
say TOO_FAR
interregnum cast graft $G 4 -60 4 40 -60 4

say JOINING
interregnum cast graft $G 4 -60 4 6 -60 4
say THE_CONTROL
setblock 20 -60 20 minecraft:wheat[age=3] replace

say CUTTING_BOTH
setblock 6 -60 4 minecraft:air replace
setblock 20 -60 20 minecraft:air replace
interregnum graft tend
execute if block 6 -60 4 minecraft:wheat[age=3] run say SCION_RESTORED
execute if block 20 -60 20 minecraft:wheat[age=3] run say CONTROL_RESTORED

say SOMEBODY_BUILDS_THERE
setblock 6 -60 4 minecraft:cobblestone replace
interregnum graft tend
execute if block 6 -60 4 minecraft:cobblestone run say BUILDING_KEPT

say REJOINING
setblock 6 -60 4 minecraft:air replace
interregnum cast graft $G 4 -60 4 6 -60 4
say CUTTING_THE_STOCK
setblock 4 -60 4 minecraft:air replace
setblock 6 -60 4 minecraft:air replace
interregnum graft tend
execute if block 6 -60 4 minecraft:wheat[age=3] run say SCION_HELD_WITHOUT_STOCK" \
    LOG=/tmp/graft.log timeout 900 ./tools/server_smoke.sh > /tmp/gf.txt 2>&1 \
    || { tail -25 /tmp/gf.txt; fail "the run did not complete"; }

casts=$(grep -oE 'cast=graft outcome=[A-Z_]+ frayed=[0-9]+ joins=[0-9]+' /tmp/gf.txt || true)
tends=$(grep -oE 'graft=tended restored=[0-9]+ joins=[0-9]+' /tmp/gf.txt || true)
[ -n "$casts" ] || { tail -20 /tmp/gf.txt; fail "the graft cast produced no answer at all"; }
[ -n "$tends" ] || { tail -20 /tmp/gf.txt; fail "graft tend produced no answer at all"; }

untaught=$(echo "$casts" | head -1)
itself=$(echo "$casts" | sed -n 2p)
toofar=$(echo "$casts" | sed -n 3p)
joined=$(echo "$casts" | sed -n 4p)
rejoined=$(echo "$casts" | sed -n 5p)
afterscion=$(echo "$tends" | head -1)
afterbuild=$(echo "$tends" | sed -n 2p)
afterstock=$(echo "$tends" | sed -n 3p)

field() { echo "$1" | grep -oE "$2=[0-9]+" | grep -oE '[0-9]+'; }

# --- the refusals ------------------------------------------------------------
echo "$untaught" | grep -q 'outcome=UNLEARNED' || {
    echo "  casting before being taught: $untaught"
    fail "somebody who had never been taught Verdancy grafted something"; }
echo "$itself" | grep -q 'outcome=OUT_OF_REACH' || {
    echo "  grafting a plant to its own position: $itself"
    fail "a plant was grafted to itself. That is not a short graft -- cutting the stock IS cutting the scion, so nothing could ever end it"; }
echo "$toofar" | grep -q 'outcome=OUT_OF_REACH' || {
    echo "  grafting across thirty-six blocks: $toofar"
    fail "a graft reached further than its own span. A link you cannot see both ends of is a link you will forget you made"; }

# --- the join takes ----------------------------------------------------------
echo "$joined" | grep -q 'outcome=TAKEN' || {
    echo "  joining wheat on farmland to bare stone: $joined"
    fail "the graft did not take"; }
[ "$(field "$joined" joins)" = "1" ] || fail "the graft was reported taken and the ledger holds none ('$joined')"

# --- A SCION SURVIVES WHERE ITS OWN BLOCK CANNOT ----------------------------
# The locked line. The wheat is removed and put back, on stone, where wheat cannot live.
mark SCION_RESTORED || {
    grep -oE '(SCION|CONTROL|BUILDING)_[A-Z_]+' /tmp/graft.log | sort -u || true
    fail "the scion was not put back after being cut. That is the entire spell: the world removes a plant with no business being there, and the graft restores it for as long as the stock lives"; }
[ "$(field "$afterscion" restored)" -ge 1 ] || \
    fail "the tending reported restoring nothing while the scion came back ('$afterscion') -- two things that must agree are being computed separately"

# --- THE CONTROL: identical wheat, identical stone, cut the same way, no graft ----
# Without this, "the scion came back" is equally satisfied by a world that puts any removed
# crop back, or by a probe reading a block that was never removed.
#
# THE FIRST VERSION OF THIS CONTROL ASKED THE WRONG QUESTION. It placed wheat on stone and
# asserted it had DIED on its own -- and it had not, because `/setblock` places with flag 2
# and calls `updateNeighboursOnBlockSet`, which pokes the NEIGHBOURS and never asks the new
# block whether it can survive. A plant with no business being somewhere sits there quite
# happily until something makes it look. That is also exactly why GraftSpell places its
# scion the same way; see the note on `place`.
if mark CONTROL_RESTORED; then
    fail "wheat with NO graft came back after being cut. Then the scion coming back proves nothing about the spell -- something is restoring every crop in this world"
fi

# --- a graft loses to somebody's building -----------------------------------
mark BUILDING_KEPT || {
    fail "the graft overwrote a block somebody put down. A spell that does that is a spell that eats other people's work on a timer, which is the one shape this mod refuses everywhere else"; }
[ "$(field "$afterbuild" joins)" = "0" ] || \
    fail "the graft kept its join after losing its position to a building ('$afterbuild') -- a join with nowhere to go is a join that will overwrite whatever is there the moment it becomes air"

# --- CUTTING THE STOCK ENDS IT ----------------------------------------------
echo "$rejoined" | grep -q 'outcome=TAKEN' || {
    echo "  re-joining after the building was cleared: $rejoined"
    fail "the graft could not be re-made, so the stock probe below is not about a live join"; }
if mark SCION_HELD_WITHOUT_STOCK; then
    fail "the scion was restored after the STOCK was cut. A graft is exactly as durable as the thing feeding it -- if the scion outlives its stock then nothing is feeding anything and the spell is a way to make a plant permanent"
fi
[ "$(field "$afterstock" joins)" = "0" ] || \
    fail "the join survived its own stock being cut ('$afterstock')"

echo
printf "OK: wheat lives on bare stone while something is feeding it, identical wheat with no\n    graft does not, the join loses to anything somebody builds there, and cutting the\n    stock ends it\n"
