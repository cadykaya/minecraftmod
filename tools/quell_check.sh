#!/bin/bash
# The third Silence spell, and the first that is done to a creature rather than to a place.
#
# WORLD.md, locked: "Silence (Quiet One): ... QUELL -- strip one ability (a blaze that
# cannot ignite)."
#
# The example decides the reading. Quell takes away the THROWING ARM: whatever a quelled
# mob tries to launch never leaves it. A blaze that cannot ignite is that rule applied to
# a blaze, and a skeleton that cannot loose an arrow is the same rule, not a second case.
# See core/.../magic/Quell.java for why it is not a spell with a mode per mob.
#
# WHAT IS ASSERTED:
#   * nothing is known by default -- an untaught caster is refused, which is also the only
#     way to be sure the successful cast below is evidence of a rule rather than a default;
#   * a projectile owned by a quelled mob never enters the world;
#   * A SECOND BLAZE THREE BLOCKS AWAY STILL SHOOTS. This is the control and it is also
#     the "one" in "strip one ability": without it, "no fireball appeared" is equally
#     satisfied by summon being broken, by the chunk never loading, and by a mod that
#     quietly stopped every projectile in the game -- and that last one would pass every
#     other assertion in this file;
#   * a fireball with NO owner still enters the world, because the rule must be about
#     being quelled and not about being a fireball;
#   * THE QUELLING FOLLOWS THE CREATURE. The quelled blaze is teleported thirty blocks
#     from where the spell was cast and still cannot shoot. That is the whole difference
#     between this and the two zone spells in the same school: a Hush is somewhere you
#     stand, and a quelling is something one thing carries. It has its OWN control -- the
#     unquelled blaze's fireball, summoned equally far from the cast -- because otherwise
#     "no fireball thirty blocks away" is satisfied by distance rather than by the spell.
#
# NOT ASSERTED, and stated rather than quietly skipped: anything audible. The Quiet One's
# most characteristic effects live on a client and this container has none -- the same wall
# hush_check.sh names, and for the same reason.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/quell.log; }

WHO=bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb

# Fixed UUIDs, so a fireball can be summoned with an Owner that names a specific blaze.
# There is no other way to make a headless server produce a mob's projectile: a blaze
# shoots at a player and this world has none.
A='[I;1,1,1,1]'
B='[I;2,2,2,2]'

COMMANDS="forceload add -16 -16 79 79
wait 3
summon minecraft:blaze 0 101 0 {UUID:$A,NoAI:1b,NoGravity:1b,Silent:1b,PersistenceRequired:1b}
summon minecraft:blaze 3 101 0 {UUID:$B,NoAI:1b,NoGravity:1b,Silent:1b,PersistenceRequired:1b}
wait 2
interregnum cast quell $WHO 0 100 0
interregnum learn $WHO silence
interregnum cast quell $WHO 0 100 0
say QUELL_CAST
summon minecraft:small_fireball 0 105 0 {Owner:$A}
summon minecraft:small_fireball 3 105 0 {Owner:$B}
summon minecraft:small_fireball 0 105 60 {}
tp @e[type=minecraft:blaze,limit=1,sort=nearest,x=0,y=101,z=0] 0 101 30
wait 2
summon minecraft:small_fireball 0 105 30 {Owner:$A}
summon minecraft:small_fireball 0 105 45 {Owner:$B}
wait 2
execute if entity @e[type=minecraft:small_fireball,x=-1,y=104,z=-1,dx=2,dy=2,dz=2] run say QUELLED_FIREBALL_EXISTS
execute if entity @e[type=minecraft:small_fireball,x=2,y=104,z=-1,dx=2,dy=2,dz=2] run say CONTROL_FIREBALL_EXISTS
execute if entity @e[type=minecraft:small_fireball,x=-1,y=104,z=59,dx=2,dy=2,dz=2] run say OWNERLESS_FIREBALL_EXISTS
execute if entity @e[type=minecraft:small_fireball,x=-1,y=104,z=29,dx=2,dy=2,dz=2] run say MOVED_FIREBALL_EXISTS
execute if entity @e[type=minecraft:small_fireball,x=-1,y=104,z=44,dx=2,dy=2,dz=2] run say FAR_CONTROL_FIREBALL_EXISTS" \
    LOG=/tmp/quell.log timeout 900 ./tools/server_smoke.sh > /tmp/qc.txt 2>&1 \
    || { tail -25 /tmp/qc.txt; fail "the run did not complete"; }

# --- nothing is known by default -------------------------------------------
grep -q 'cast=quell took=false subject= frayed=0 refused=unlearned' /tmp/qc.txt || {
    grep -oE 'cast=quell [a-z=0-9:_ ]+' /tmp/qc.txt | head -4 || true
    fail "an untaught caster quelled something. WORLD.md locks schools as learned in their worlds, and if casting works by default the successful cast below is evidence of nothing"; }

# --- the cast found one creature, and it was the near one ------------------
grep -q 'cast=quell took=true subject=minecraft:blaze' /tmp/qc.txt || {
    grep -oE 'cast=quell [a-z=0-9:_ ]+' /tmp/qc.txt | head -4 || true
    fail "the cast did not take a blaze, so nothing below is about a quelling"; }
grep -q 'cast=quell took=true subject=minecraft:blaze .* quelled=1' /tmp/qc.txt || {
    grep -oE 'cast=quell [a-z=0-9:_ ]+' /tmp/qc.txt | head -4 || true
    fail "one cast quelled more than one creature. 'Strip ONE ability' is singular twice over, and a Quell that takes the whole radius is a zone with extra steps -- this school already has two of those"; }

# --- THE CONTROL: the other blaze, three blocks away, still shoots ----------
# Asserted before the spell's own effect. Without it, every absence below is equally
# satisfied by a broken summon, an unloaded chunk, or a mod that stopped every projectile
# in the game -- and that last one passes every other assertion in this file.
mark CONTROL_FIREBALL_EXISTS || {
    grep -oE '[A-Z_]*(QUELLED|CONTROL|OWNERLESS|MOVED)_[A-Z_]+' /tmp/quell.log | sort | uniq -c || true
    fail "a fireball owned by the UNQUELLED blaze three blocks away also failed to appear. No projectile is entering this world at all, so 'the quelled blaze could not shoot' is not evidence of anything"; }
mark OWNERLESS_FIREBALL_EXISTS || \
    fail "a fireball with no owner at all was refused entry. The rule is supposed to be about being quelled, not about being a fireball"
mark FAR_CONTROL_FIREBALL_EXISTS || \
    fail "the UNQUELLED blaze's fireball did not appear forty-five blocks from the cast either, so the moved blaze's silence below would be evidence of distance rather than of the spell"

# --- a blaze that cannot ignite --------------------------------------------
if mark QUELLED_FIREBALL_EXISTS; then
    grep -oE '[A-Z_]*(QUELLED|CONTROL|OWNERLESS|MOVED)_[A-Z_]+' /tmp/quell.log | sort | uniq -c || true
    fail "a fireball owned by the quelled blaze entered the world. WORLD.md is explicit: a blaze that cannot ignite"
fi

# --- and it followed the creature ------------------------------------------
if mark MOVED_FIREBALL_EXISTS; then
    fail "the quelled blaze could shoot again thirty blocks from where the spell was cast. A quelling is not a zone -- it is something one creature carries, which is the entire difference between this spell and the two Silence spells that are places"
fi

echo
echo "OK: a quelled blaze cannot ignite, wherever it goes; the one beside it still can"
