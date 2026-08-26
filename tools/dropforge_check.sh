#!/bin/bash
# Crafting by crushing -- and the reason a drop-forge cannot also be a low-gravity field.
#
# WORLD.md, locked: "Weight (Anchorite): Lighten -- shared low-gravity zone, mobs float
# too . Drop-forge -- CRAFTING BY CRUSHING."
#
# The spell crushes nothing. It makes a few metres of ground somewhere that an impact
# MEANS something, and an impact is not something it provides -- you have to go and get
# the weight, get it above the thing, and let it go. So every assertion here is about a
# drop, not about a cast.
#
# WHAT IS BEING PROVED, and none of it was read off the implementation:
#
#   * a weight dropped in a forge crushes what it LANDS ON   (the spell)
#   * the same weight dropped anywhere else does not         (the control -- without it,
#                                                             "crushed" could be any of
#                                                             the four block tables this
#                                                             mod runs)
#   * the weight is still there afterwards                   (a forge does not eat its
#                                                             own hammer; you drop it
#                                                             again)
#   * ice packs as well as stone shatters                    (the table answers ONE
#                                                             question -- what does this
#                                                             do under force -- and the
#                                                             world has two answers)
#   * a block the player placed is crushed too               (the ledger gates the WORLD,
#                                                             not the caster: LESSONS #35)
#   * an untaught caster cannot open one                     (schools are learned in their
#                                                             gods' worlds or the journey
#                                                             buys nothing)
#   * a LIGHTEN is not a drop-forge                          (below)
#
# THE PAIR. Lighten and Drop-forge are both the Anchorite's, and both open zones -- the
# same shape that made Hush and Still collide when zones were keyed by school. This pair
# is worse, because these two are OPPOSITES: inside a Lighten nothing falls, so a
# drop-forge that was also a lighten would hover the very weight it was waiting for and
# crush nothing, forever, with no error anywhere.
#
# That makes the FORGE_CRUSHED line below a guard against the collision all by itself --
# under a school key it cannot pass. The Lighten column proves the other direction, and
# proves it honestly: it asserts the sand is HELD IN THE AIR as well as that the ground
# is intact, because ground nothing landed on is intact for a reason that is not a spell.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/dropforge.log; }

WHO=99999999-9999-4999-8999-999999999999

# Five columns, twenty blocks apart so no two zones can reach each other -- a forge is
# radius 3 and a lighten radius 5. Floors at y=100, so "what it landed on" is a block
# probe rather than an entity search.
#
#    0  forge over stone      the spell
#   20  no cast at all        the control
#   40  forge over ice        the other half of the table
#   60  forge over CLAIMED stone   a wall the player put up
#   80  LIGHTEN over stone    the collision that would have been
COMMANDS="forceload add -16 -16 96 16
wait 3
setblock 0 100 0 minecraft:stone replace
setblock 20 100 0 minecraft:stone replace
setblock 40 100 0 minecraft:ice replace
setblock 60 100 0 minecraft:stone replace
setblock 80 100 0 minecraft:stone replace
interregnum claim record 60 100 0 60 100 0
execute if block 0 100 0 minecraft:stone run say SETUP_FORGE
execute if block 20 100 0 minecraft:stone run say SETUP_FREE
execute if block 40 100 0 minecraft:ice run say SETUP_ICE
execute if block 60 100 0 minecraft:stone run say SETUP_CLAIMED
execute if block 80 100 0 minecraft:stone run say SETUP_LIGHTEN
say CAST_UNLEARNED
interregnum cast dropforge $WHO 0 101 0
interregnum learn $WHO weight
say CAST_TAUGHT
interregnum cast dropforge $WHO 0 101 0
interregnum cast dropforge $WHO 40 101 0
interregnum cast dropforge $WHO 60 101 0
interregnum cast lighten $WHO 80 101 0
say ZONES_OPEN
setblock 0 105 0 minecraft:sand replace
setblock 20 105 0 minecraft:sand replace
setblock 40 105 0 minecraft:sand replace
setblock 60 105 0 minecraft:sand replace
setblock 80 105 0 minecraft:sand replace
wait 5
say AFTER_DROP
execute if block 0 100 0 minecraft:cobblestone run say FORGE_CRUSHED
execute if block 0 101 0 minecraft:sand run say HAMMER_KEPT
execute if block 20 100 0 minecraft:stone run say FREE_INTACT
execute if block 20 101 0 minecraft:sand run say FREE_LANDED
execute if block 40 100 0 minecraft:packed_ice run say ICE_PACKED
execute if block 60 100 0 minecraft:cobblestone run say CLAIMED_CRUSHED
execute if block 80 100 0 minecraft:stone run say LIGHTEN_INTACT
execute if entity @e[type=minecraft:falling_block,x=75,y=95,z=-5,dx=10,dy=80,dz=10] run say LIGHTEN_HELD" \
    LOG=/tmp/dropforge.log timeout 900 ./tools/server_smoke.sh > /tmp/df.txt 2>&1 \
    || { tail -25 /tmp/df.txt; fail "the run did not complete"; }

# --- the setup, before anything that rests on it ----------------------------
mark SETUP_FORGE   || fail "no stone under the forge -- there is nothing for a weight to land on"
mark SETUP_FREE    || fail "no stone at the control column, so 'the same drop changes nothing there' is about empty air"
mark SETUP_ICE     || fail "no ice was placed, so the packing half of the table is untested"
mark SETUP_CLAIMED || fail "the claimed stone was never placed, so crushing it proves nothing"
mark SETUP_LIGHTEN || fail "no stone under the lighten, so the collision column has nothing in it"

# --- nothing is known by default --------------------------------------------
# Asserted before every success below, for the reason casting_check.sh gives: if an
# untaught caster can open a forge, every crush here is a default rather than a rule.
grep -q 'cast=dropforge opened=false' /tmp/df.txt || {
    grep -oE 'cast=dropforge [a-z=0-9 ]+' /tmp/df.txt | head -3 || true
    fail "somebody who had never been taught Weight opened a drop-forge anyway. WORLD.md locks schools as learned in their gods' worlds -- casting untaught removes the reason to cross"; }
grep -q 'refused=unlearned' /tmp/df.txt || \
    fail "the untaught cast was refused for some reason other than not having been taught -- a caster who has never learned should be told that, not told something about the ground"

# --- THE CONTROL: a weight dropped on nobody's spell changes nothing ---------
# First, because without it "the stone became cobblestone" is equally satisfied by the
# unraveling, the Turning's clock, or attrition -- three other tables that all convert
# blocks in this same world, on their own, while this check is running.
mark FREE_LANDED || {
    grep -iE 'sand|falling|forceload' /tmp/df.txt | tail -8 || true
    fail "the control sand never landed at all. Falling blocks are not working in this world, so nothing below is evidence of anything"; }
mark FREE_INTACT || \
    fail "stone twenty blocks from any spell turned to cobblestone when sand landed on it. Something in this mod crushes ground everywhere, and every success below is that, not a drop-forge"

# --- the spell: an impact means something -----------------------------------
# This line alone is the guard against the zone key collapsing back to School. Keyed by
# school, the forge would ALSO be a lighten, the sand would hang above it, and this could
# not pass.
mark FORGE_CRUSHED || {
    grep -oE '(FORGE|FREE|ICE|CLAIMED|LIGHTEN|HAMMER)_[A-Z]+' /tmp/dropforge.log | sort -u || true
    fail "a weight landed in a drop-forge and the stone under it was untouched. Either the spell does nothing, or the zone is keyed by school again -- in which case the forge is also a low-gravity field and is hovering the very weight it is waiting for"; }

# --- and it does not eat the hammer -----------------------------------------
mark HAMMER_KEPT || \
    fail "the sand that did the crushing is gone. A forge that consumes what you drop into it is a one-shot, and the spell's whole economy is that the cost of a crush is a CLIMB, not a block"

# --- the table has two answers, not one theme -------------------------------
# Rock shatters; loose and soft matter packs. If only the shattering half worked, the
# table would be a rubble generator with a nicer name.
mark ICE_PACKED || \
    fail "ice under a landing weight did not become packed ice. Crushing answers one question -- what does this do under force -- and half the world's answer to it is that it gets DENSER. Without that half the spell is a rubble generator, and 'crafting by crushing' crafts nothing"

# --- a caster may crush their own wall --------------------------------------
# The principle from LESSONS #35, asserted in the next feature after the one that got it
# wrong: the ledger is the promise that the WORLD will not eat your work. It was never a
# promise that you cannot aim a spell at your own cobblestone on purpose.
mark CLAIMED_CRUSHED || \
    fail "a drop-forge refused a block the player had placed. The claim ledger stops the unraveling, attrition and the Turning's clock from taking your work -- it does not stop you working on it. A spell you aim by hand, and then feed by hand, that silently declines on your own floor is the same bug Weather shipped with"

# --- and a lighten is not a forge -------------------------------------------
mark LIGHTEN_HELD || {
    grep -oE '(FORGE|FREE|ICE|CLAIMED|LIGHTEN|HAMMER)_[A-Z]+' /tmp/dropforge.log | sort -u || true
    fail "no falling block is in the air over the Lighten column, so the lighten did not take -- and 'the ground there was not crushed' is then about ground nothing ever landed on"; }
mark LIGHTEN_INTACT || \
    fail "stone under a LIGHTEN was crushed. Lighten and Drop-forge are both the Anchorite's, and if one zone serves both then every low-gravity field is also a forge -- which nothing anywhere would report, because from inside either spell it looks like both working"

echo
printf "OK: a weight dropped into a drop-forge crushes what it lands on and survives doing it,\n    stone shatters and ice packs, a wall its caster built is not spared, and the\n    Anchorite's other spell is not the same zone\n"
