#!/bin/bash
# The second spell, and the proof that a school is a system rather than one special case.
#
# WORLD.md, locked: "Weight (Anchorite): LIGHTEN -- shared low-gravity zone, MOBS FLOAT
# TOO." Those last three words are the spell. It is not a buff you put on yourself; it is
# a piece of the world briefly obeying the Anchorite's law, and everything inside it is
# subject. That is what satisfies the doctrine that a spell's "combat use falls out of its
# world use, never the reverse" -- you cannot aim Lighten at anybody, only change the
# rules where they are standing.
#
# THE ASSERTION THAT MATTERS is that this is the SAME LAW, not a similar one. Sand rises
# in the Anchorite's world; sand rises in a band-3 patch of overworld that has forgotten
# whose it is; and now sand rises inside a zone somebody opened. Three callers, one
# `Anchorite.lift`. If Lighten had its own floating code it could drift from the god's,
# and the school would stop being a thing you learned from that god.
#
# So the check is deliberately shaped like `anchorite_check.sh` -- same sand, same
# floor, same probes -- because the claim is that the outcome is identical.
#
# Also asserted: the zone has an EDGE, and the spell cannot be cast untaught. The edge is
# how a player learns it is a rule rather than the world breaking, which is the same
# reason band 3's leaks have one.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/lighten.log; }

WHO=88888888-8888-4888-8888-888888888888

# Sand at y=120 with air beneath becomes a FallingBlockEntity at once. The floor at y=100
# is what it lands on when nothing is holding it up. Inside the zone it must not land;
# outside -- same world, same tick, 12 blocks away, well past the radius of 5 -- it must.
COMMANDS="forceload add -16 -16 31 31
wait 3
setblock 4 100 4 minecraft:stone replace
setblock 24 100 4 minecraft:stone replace
say UNLEARNED
interregnum cast lighten $WHO 4 120 4
interregnum learn $WHO weight
interregnum cast lighten $WHO 4 120 4
say ZONE_OPEN
setblock 4 120 4 minecraft:sand replace
setblock 24 120 4 minecraft:sand replace
wait 2
execute if block 4 101 4 minecraft:sand run say INSIDE_LANDED
execute if block 24 101 4 minecraft:sand run say OUTSIDE_LANDED
execute if block 4 120 4 minecraft:sand run say INSIDE_NEVER_MOVED
execute if entity @e[type=minecraft:falling_block,x=-1,y=125,z=-1,dx=11,dy=200,dz=11] run say INSIDE_ROSE" \
    LOG=/tmp/lighten.log timeout 900 ./tools/server_smoke.sh > /tmp/lt.txt 2>&1 \
    || { tail -25 /tmp/lt.txt; fail "the run did not complete"; }

# --- nothing is known by default --------------------------------------------
first=$(sed -n '/UNLEARNED/,/ZONE_OPEN/p' /tmp/lt.txt | grep -oE 'cast=lighten opened=[a-z]+ frayed=[0-9]+ refused=[a-z]*' | head -1)
# The two fields are asserted SEPARATELY rather than as one adjacent string. The first
# version looked for `opened=false refused=unlearned`, which the output never contains --
# `frayed=0` sits between them -- so the check failed against behaviour that was correct.
# Guessing at the shape of a line instead of reading one, docs/LESSONS.md #26, in the
# cheapest possible form: it failed loudly rather than passing, because it was a positive
# assertion about text that was right there in the output.
echo "$first" | grep -q 'opened=false' && echo "$first" | grep -q 'refused=unlearned' || {
    grep -oE 'cast=lighten [a-z=0-9 ]+' /tmp/lt.txt | head -3 || true
    fail "somebody who had never been taught Weight opened a Lighten zone ('$first'). Schools are learned in their worlds; if the second spell skips that the rule was only ever enforced in the first one"; }

# --- the zone opened once taught --------------------------------------------
opened=$(sed -n '/UNLEARNED/,/ZONE_OPEN/p' /tmp/lt.txt | grep -oE 'cast=lighten opened=[a-z]+ frayed=[0-9]+ refused=[a-z]* zones=[0-9]+' | tail -1)
echo "$opened" | grep -q 'opened=true' || {
    grep -oE 'cast=lighten [a-z=0-9 ]+' /tmp/lt.txt | head -3 || true
    fail "casting Lighten after being taught Weight did not open a zone ('$opened')"; }
echo "$opened" | grep -qE 'zones=[1-9]' || \
    fail "the zone was reported open but the world holds none ('$opened') -- it was created and immediately lost, so nothing below is about a zone"

# --- THE CONTROL: outside the zone, sand falls ------------------------------
# Without this, "the sand inside did not land" is also satisfied by sand that never
# spawned, a chunk that never loaded, and a setblock that silently failed. Same world,
# same tick, twenty blocks away.
mark OUTSIDE_LANDED || {
    grep -iE "sand|falling|forceload" /tmp/lt.txt | tail -8 || true
    fail "sand dropped OUTSIDE the zone did not land on the floor beneath it -- gravity is not working in this world at all, so the sand inside the zone proves nothing"; }

# --- and inside it, the Anchorite's law is in force --------------------------
if mark INSIDE_LANDED; then
    fail "sand inside a Lighten zone landed on the floor. WORLD.md locks the spell as a shared low-gravity zone where mobs float too -- if things fall in it, the spell is a message rather than a change to the world"
fi
if mark INSIDE_NEVER_MOVED; then
    fail "the sand inside the zone is still sitting where it was placed -- it never became a falling block, so gravity there is OFF rather than REVERSED. That is a different bug with the same symptom, and it is the one anchorite_check.sh was written to tell apart"
fi
mark INSIDE_ROSE || {
    grep -iE "falling_block" /tmp/lt.txt | tail -5 || true
    fail "no falling block was found above the zone. Sand that did not land and is not rising has been deleted or never spawned, which satisfies 'did not land' for entirely the wrong reason"; }

echo
echo "OK: a taught caster opens a zone where the Anchorite's law holds, unanchored things in it rise, and twenty blocks away they still fall"
