#!/bin/bash
# The fourth school, and the spell whose combat use most obviously falls out of its world
# use.
#
# WORLD.md, locked: "Silence (Quiet One): HUSH -- true no-sound zone: sculk blind, MOBS
# CANNOT ALERT, a creeper that cannot hiss cannot detonate."
#
# A creeper's fuse is a sound. Take the sound away and the mechanism it belongs to has
# nothing to complete. Hush is not a defensive ability -- it is silence, which happens to
# be fatal to a thing that kills by announcing itself.
#
# WHAT IS ASSERTED, and it is deliberately only the server-side half:
#   * a creeper inside cannot detonate, ignited or not;
#   * nothing inside acquires a target;
#   * a creeper OUTSIDE still detonates, which is the control -- without it "nothing
#     exploded" is equally satisfied by a creeper that never spawned, a chunk that never
#     loaded, and a mod that broke creepers everywhere.
#   * a LIGHTEN field does not silence anything. Zones are keyed by school, and before
#     they were, the second spell to open one would have made every zone do everything --
#     which looks exactly like both spells working, from inside either one.
#
# NOT ASSERTED, and stated rather than quietly skipped: the audible silence and sculk
# going blind. Those are client-side and this container has no client. It is the same wall
# band 3 met -- the Quiet One's law is the one law whose most characteristic form lives on
# a client -- and claiming it here would be claiming something nothing checked.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/hush.log; }

WHO=aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa

# Three creepers, all ignited so none of them needs a player to swell at. Inside a Hush,
# outside it, and inside a LIGHTEN field twenty blocks away. Glass markers record whether
# each survived: an explosion removes them, so the marker is the evidence.
COMMANDS="forceload add -16 -16 47 47
wait 3
setblock 0 100 0 minecraft:glass replace
setblock 40 100 0 minecraft:glass replace
setblock 0 100 40 minecraft:glass replace
interregnum learn $WHO silence
interregnum learn $WHO weight
interregnum cast hush $WHO 0 100 0
interregnum cast lighten $WHO 0 100 40
say ZONES_OPEN
summon minecraft:creeper 0 101 0 {ignited:1b,NoAI:1b,Fuse:20}
summon minecraft:creeper 40 101 0 {ignited:1b,NoAI:1b,Fuse:20}
summon minecraft:creeper 0 101 40 {ignited:1b,NoAI:1b,Fuse:20}
wait 6
say AFTER_FUSE
execute if block 0 100 0 minecraft:glass run say INSIDE_SURVIVED
execute if block 40 100 0 minecraft:glass run say OUTSIDE_SURVIVED
execute if block 0 100 40 minecraft:glass run say LIGHTEN_SURVIVED
execute if entity @e[type=minecraft:creeper,x=-2,y=99,z=-2,dx=4,dy=4,dz=4] run say INSIDE_CREEPER_ALIVE" \
    LOG=/tmp/hush.log timeout 900 ./tools/server_smoke.sh > /tmp/hs.txt 2>&1 \
    || { tail -25 /tmp/hs.txt; fail "the run did not complete"; }

# --- the zones opened -------------------------------------------------------
grep -q 'cast=hush opened=true' /tmp/hs.txt || {
    grep -oE 'cast=(hush|lighten) [a-z=0-9 ]+' /tmp/hs.txt | head -4 || true
    fail "the Hush zone did not open, so nothing below is about a silence"; }

# --- THE CONTROL: outside, a creeper still explodes -------------------------
# Asserted FIRST. Without it, every "survived" below is equally satisfied by creepers that
# never spawned, a chunk that never loaded, or a mod that quietly broke creepers globally
# -- and that last one would look exactly like the spell working.
if mark OUTSIDE_SURVIVED; then
    grep -oE 'creeper|Fuse|summon' /tmp/hs.txt | head -5 || true
    fail "an ignited creeper forty blocks from any zone did not explode either. Creepers are not detonating anywhere in this world, so 'nothing exploded inside the silence' is not evidence of anything -- and a mod that broke creepers everywhere would pass every other assertion in this file"
fi

# --- inside the silence, it cannot finish -----------------------------------
mark INSIDE_SURVIVED || {
    grep -oE '(INSIDE|OUTSIDE|LIGHTEN)_[A-Z_]+' /tmp/hush.log | sort | uniq -c || true
    fail "an ignited creeper inside a Hush zone detonated. WORLD.md is explicit: a creeper that cannot hiss cannot detonate. Its fuse is a sound, and in a true no-sound zone the mechanism has nothing to complete"; }
mark INSIDE_CREEPER_ALIVE || \
    fail "the creeper inside the silence is gone without the glass being destroyed -- it was removed rather than silenced, which satisfies 'did not explode' for entirely the wrong reason"

# --- and a different school's zone silences nothing -------------------------
# Zones are keyed by school. Before they were, the second spell to open one would have
# made every zone do everything -- and from inside either spell that looks like both of
# them working.
if mark LIGHTEN_SURVIVED; then
    fail "a creeper inside a LIGHTEN field also failed to detonate. Zones are supposed to belong to the school that opened them; if any zone silences, then casting the Anchorite's spell quietly hands you the Quiet One's, and every zone in the mod does everything"
fi

echo
echo "OK: a creeper cannot finish its fuse inside a silence, still can outside it, and a low-gravity field is not a silence"
