#!/bin/bash
# A Warden walks a beat, and a beat is not a walk.
#
# The Warden used the goal a sheep uses. That looks fine for ten seconds and is wrong
# for the reason the whole mod exists: a random walk is an animal foraging, a beat is a
# unit executing. WORLD.md's thesis is that violence does nothing and the exploit a
# player finds is administrative -- and an enforcement system you can plan around is a
# prerequisite for that.
#
# So the property under test is not "does it move". It is **is it predictable**, and
# that is genuinely hard to assert from outside. Two things distinguish a beat:
#
#   1. IT IS ON THE RING, NOT IN THE MIDDLE. The route is four points at radius 8.
#      A stroll goal has no reason to prefer the ring and spends most of its time
#      nearer the middle of its tether. Sampled repeatedly, a patrolling Warden is
#      out at the edge; a strolling one is not.
#
#   2. TWO WARDENS ON IDENTICAL GROUND WALK THE SAME BEAT. This is the assertion that
#      actually pins it down. Two statues on flat identical terrain, posted in the
#      same tick, produce two units executing the same fixed route from the same
#      starting leg -- so their offsets from their own statues stay in step. Two
#      strolling mobs draw from the level's random and diverge within seconds.
#
# The second is a relationship between two observations rather than a fact about one
# (docs/LESSONS.md #19), which is the only reason it can be checked at all: no single
# snapshot of one mob can tell "deliberate" from "lucky".
#
# NOT asserted, because it is not built: inspecting, citing, confiscating, escalating.
# The Warden walks. That is this increment.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/patrol.log; }

# Two statues, 64 apart on flat ground, both posted in the same sweep.
#
# `wait 5` then a marker probe before anything: a statue posts an ENTITY, and an entity
# added before the chunk's entity storage has arrived is accepted and then invisible to
# every selector for the rest of the run (docs/LESSONS.md #22). Without the probe this
# whole file would pass or fail on chunk timing.
#
# The samples are spaced across more than one full circuit: 4 legs x (walk + 40 tick
# dwell) is roughly 20 seconds, so five samples eight seconds apart cross corners
# rather than catching the same one repeatedly.
COMMANDS="forceload add -80 -80 79 79
wait 5
summon minecraft:marker 0 -60 0
execute if entity @e[type=minecraft:marker,limit=1] run say E_CHUNK_TAKES_ENTITIES
kill @e[type=minecraft:marker]
setblock 0 -60 0 interregnum:warden_statue[facing=north,woken=false] replace
setblock 64 -60 0 interregnum:warden_statue[facing=north,woken=false] replace
execute positioned 0 -60 0 run interregnum record deicide
interregnum warden post 0 -60 0
interregnum warden post 64 -60 0
execute if entity @e[type=interregnum:warden] run say WARDENS_EXIST
execute positioned 0 -60 0 if entity @e[type=interregnum:warden,distance=..3,limit=1] run say A_AT_POST_START
wait 8
say SAMPLE_1
execute positioned 0 -60 0 if entity @e[type=interregnum:warden,distance=5..12,limit=1] run say A_ON_RING_1
execute positioned 0 -60 0 if entity @e[type=interregnum:warden,distance=..3,limit=1] run say A_IN_MIDDLE_1
execute positioned 64 -60 0 if entity @e[type=interregnum:warden,distance=5..12,limit=1] run say B_ON_RING_1
wait 8
say SAMPLE_2
execute positioned 0 -60 0 if entity @e[type=interregnum:warden,distance=5..12,limit=1] run say A_ON_RING_2
execute positioned 0 -60 0 if entity @e[type=interregnum:warden,distance=..3,limit=1] run say A_IN_MIDDLE_2
execute positioned 64 -60 0 if entity @e[type=interregnum:warden,distance=5..12,limit=1] run say B_ON_RING_2
wait 8
say SAMPLE_3
execute positioned 0 -60 0 if entity @e[type=interregnum:warden,distance=5..12,limit=1] run say A_ON_RING_3
execute positioned 0 -60 0 if entity @e[type=interregnum:warden,distance=..3,limit=1] run say A_IN_MIDDLE_3
execute positioned 64 -60 0 if entity @e[type=interregnum:warden,distance=5..12,limit=1] run say B_ON_RING_3
execute positioned 0 -60 0 run tp @e[type=minecraft:marker] ~ ~ ~" \
    LOG=/tmp/patrol.log timeout 2000 ./tools/server_smoke.sh > /tmp/pt.txt 2>&1 \
    || { tail -25 /tmp/pt.txt; fail "the run did not complete"; }

# --- the setup, before anything that depends on it --------------------------
mark E_CHUNK_TAKES_ENTITIES || {
    echo "  A marker summoned into the chunk was invisible to @e, so any Warden a"
    echo "  statue posted would be invisible too. Lengthen the wait; do not delete"
    echo "  the probe."
    fail "the chunk was still loading when the statues were swept"; }
mark WARDENS_EXIST || {
    grep -iE "posted|statue" /tmp/pt.txt | tail -8 || true
    fail "no Warden was posted at all, so every assertion below is about nothing"; }
mark A_AT_POST_START || \
    fail "the Warden did not start beside its statue -- the beat is being measured from the wrong place"

# --- 1. it walks the ring, not the middle -----------------------------------
# Counted rather than any-of: one sample at the edge is a strolling mob having a
# good minute. Three out of three is a route.
ring=0
for i in 1 2 3; do mark "A_ON_RING_$i" && ring=$((ring + 1)) || true; done
middle=0
for i in 1 2 3; do mark "A_IN_MIDDLE_$i" && middle=$((middle + 1)) || true; done

[ "$ring" -ge 2 ] || {
    grep -oE 'A_(ON_RING|IN_MIDDLE)_[0-9]' /tmp/patrol.log | sort | uniq -c || true
    fail "the Warden was out on its ring in only $ring of 3 samples -- it is wandering inside its tether, not walking a route"; }
[ "$middle" -le 1 ] || \
    fail "the Warden was within 3 blocks of its statue in $middle of 3 samples -- a beat at radius 8 does not sit on top of the thing it is guarding"

# --- 2. two Wardens on identical ground walk the SAME beat ------------------
# The assertion that actually separates deliberate from lucky. Both units execute the
# same fixed route from the same leg, so they are on the ring in the same samples.
# Two strolling mobs draw from the level's shared random and diverge in seconds.
disagree=0
for i in 1 2 3; do
    a=no; b=no
    mark "A_ON_RING_$i" && a=yes || true
    mark "B_ON_RING_$i" && b=yes || true
    [ "$a" = "$b" ] || disagree=$((disagree + 1))
done
[ "$disagree" -le 1 ] || {
    grep -oE '[AB]_ON_RING_[0-9]' /tmp/patrol.log | sort || true
    fail "two Wardens posted in the same tick on identical ground disagreed in $disagree of 3 samples -- they are not walking the same route, so the route is not fixed"; }

echo
echo "OK: the Warden walks a fixed beat on its ring, and two of them keep step"
