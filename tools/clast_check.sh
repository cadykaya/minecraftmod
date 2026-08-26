#!/bin/bash
# Pieces of the god, and there are only ever so many.
#
# WORLD.md, locked: "The god's power enters its killer. An ordinary Minecraft body cannot
# hold it. The overflow detonates outward, scattering SPLINTERS at shrines and the
# crater." And: "the shattered god-pieces are CLASTS (item). Anyone may attune one;
# CLASTS ARE FINITE -- the class is a server negotiation."
#
# The item had existed since the first registry pass and nothing in the world produced
# one. PlayerTags says so in its own javadoc: "the Theoclast class does not exist yet --
# no clast can be attuned, so no player can truthfully hold it."
#
# WHAT IS BEING PROVED, in the order it has to happen:
#
#   * before the death there are none                 (a splinter is what the shattering
#                                                      produces; one lying about
#                                                      beforehand is scenery)
#   * the crater takes its share at the moment of death
#   * a shrine gives up one when it is FOUND           (not at the instant of the death --
#                                                      the deicide can only reach loaded
#                                                      chunks, the same constraint the
#                                                      statues have)
#   * TWO shrines give up one each                     (one shrine cannot show a pool is
#                                                      shared -- docs/LESSONS.md #38)
#   * a shrine found twice gives up one, not two       (the pool is finite, so a player
#                                                      walking back and forth must not be
#                                                      able to drain a world's whole
#                                                      allowance at a single shrine)
#   * and what lands does not despawn                  (there are seven in a world, ever:
#                                                      a finite thing that can be lost to
#                                                      a five-minute timer is not finite,
#                                                      it is random)
#
# THE LAST ONE IS ASSERTED ON `Age`, NOT `Lifespan`, and the difference cost a wrong
# assertion before it cost a bug: `ItemEntity.setUnlimitedLifetime` sets AGE to -32768 and
# leaves `Lifespan` at its default 6000, because the tick loop stops counting when age is
# that sentinel rather than raising the cap. A check reading `Lifespan` sees 6000 and
# reports a despawning item that is not despawning.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/clast.log; }

# `place feature` builds real shrines -- the same call the generator makes -- so these are
# the shrines the game builds and not piles of blocks shaped like one. The crater is put
# somewhere neither of them is, so the three sources of clasts stay countable apart.
echo "1/3 two shrines, then the god dies somewhere else"
COMMANDS='forceload add -64 -64 63 63
forceload add 192 192 207 207
wait 3
place feature interregnum:shrine 8 -60 8
place feature interregnum:shrine 40 -60 40
execute if block 8 -61 8 interregnum:shrine_stone_carved run say SETUP_SHRINE_ONE
execute if block 40 -61 40 interregnum:shrine_stone_carved run say SETUP_SHRINE_TWO
say BEFORE
interregnum clasts
execute if entity @e[type=minecraft:item] run say ITEM_BEFORE_DEATH
say DEATH
execute positioned 200 -60 200 run interregnum record deicide
wait 2
interregnum clasts
execute if entity @e[type=minecraft:item] run say ITEM_AFTER_DEATH
data get entity @e[type=minecraft:item,limit=1] Age
say DONE
forceload remove all' \
    LOG=/tmp/clast.log timeout 900 ./tools/server_smoke.sh > /tmp/cl.txt 2>&1 \
    || { tail -25 /tmp/cl.txt; fail "the first run did not complete"; }

# A RESTART rather than a forceload cycle, and it is not fussiness. Removing a ticket does
# not unload a chunk promptly -- the server keeps it for a while -- so a
# remove/wait/add sequence fires a Load event only sometimes, and the first version of this
# file passed once and then failed on a clean tree for that reason alone. A shrine that is
# FOUND is the thing under test, and a restart is the only way to guarantee the finding.
# KEEP_WORLD, or `server_smoke.sh` deletes the world between runs -- which is its default
# and the right one for worldgen checks, and exactly wrong here: a fresh world has no
# shrines, no dead god and an empty pool, so every count below would be measuring nothing.
echo "2/3 restart, and walk to the shrines"
COMMANDS='forceload add 0 0 47 47
wait 4
interregnum clasts
say DONE' \
    KEEP_WORLD=1 LOG=/tmp/clast2.log timeout 900 ./tools/server_smoke.sh > /tmp/cl2.txt 2>&1 \
    || { tail -25 /tmp/cl2.txt; fail "the second run did not complete"; }

echo "3/3 restart again, and walk back"
COMMANDS='forceload add 0 0 47 47
wait 4
interregnum clasts
say DONE' \
    KEEP_WORLD=1 LOG=/tmp/clast3.log timeout 900 ./tools/server_smoke.sh > /tmp/cl3.txt 2>&1 \
    || { tail -25 /tmp/cl3.txt; fail "the third run did not complete"; }

# `|| true` on every extraction: a sed or grep that matches nothing would otherwise take
# this script down before the message it exists to print (docs/LESSONS.md #23).
pool() { sed -n "/$1/,/$2/p" /tmp/cl.txt | grep -oE 'clasts=[0-9]+' | head -1 | cut -d= -f2 || true; }
poolIn() { grep -oE 'clasts=[0-9]+' "$1" | head -1 | cut -d= -f2 || true; }

# --- the setup, before anything that rests on it ----------------------------
mark SETUP_SHRINE_ONE || fail "the first shrine did not generate -- there is nothing for a clast to be found at"
mark SETUP_SHRINE_TWO || fail "the second shrine did not generate, and one shrine cannot show that a pool is SHARED between them (docs/LESSONS.md #38)"

# --- before the death, nothing has shattered --------------------------------
before=$(pool BEFORE DEATH)
[ "${before:-x}" = "0" ] || \
    fail "the world had already given up ${before:-no} clast(s) before its god died. A splinter is what the shattering produces; one that exists beforehand is scenery, and every count below would be measuring it"
if mark ITEM_BEFORE_DEATH; then
    fail "there is an item lying in the world before the god has died. Either a clast has been scattered early, or this check is counting somebody else's dropped item and the numbers below mean nothing"
fi

# --- the crater takes its share ---------------------------------------------
after=$(pool DEATH DONE)
[ "${after:-x}" = "3" ] || \
    fail "the crater took ${after:-no} clast(s), not 3. WORLD.md locks the overflow as detonating outward from where it happened, and the crater's share is the largest single one"
mark ITEM_AFTER_DEATH || \
    fail "the pool says clasts were issued and there is no item anywhere in the world. The count was spent and nothing was produced, which is the worst of both: a finite allowance quietly consumed for nothing"

# --- and what landed is not on a timer --------------------------------------
grep -q 'entity data: -32768' /tmp/cl.txt || {
    grep -A 1 'Age' /tmp/cl.txt | head -4 || true
    fail "a scattered clast is on the ordinary five-minute despawn timer. There are seven in a world, ever, and a finite thing that can be lost to a timer is not finite -- it is random. (The field is Age, not Lifespan: setUnlimitedLifetime writes the -32768 sentinel into age and leaves Lifespan alone.)"; }

# --- the shrines give up one each, when found -------------------------------
found=$(poolIn /tmp/cl2.txt)
[ "${found:-x}" = "5" ] || \
    fail "after both shrines were found the world had given up ${found:-no} clast(s), not 5 (three in the crater, one at each shrine). Either a shrine gave nothing, or it gave more than its share"

# --- and finding one twice does not pay twice -------------------------------
# The assertion the finiteness rests on. Without the per-chunk mark, a player walking away
# from a shrine and back could take the world's whole allowance from one place.
again=$(poolIn /tmp/cl3.txt)
[ "${again:-x}" = "5" ] || \
    fail "a second visit to the same two shrines took the count from 5 to ${again:-?}. The pool is finite and the mark that spends a shrine is what makes it so; without it one shrine yields a clast every time its chunk loads, and WORLD.md's server negotiation never happens"

echo
printf "OK: nothing before the death, three in the crater, one at each shrine when it is\n    found, none the second time, and what lands is not on a timer\n"
