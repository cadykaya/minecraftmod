#!/bin/bash
# Are the mod's people the things the design says they are?
#
# Two mobs, opposite in every way that shows: a Warden that will not fight you and a
# keeper who cannot. Neither has an ATTACK_DAMAGE attribute at all, which is the
# design enforced rather than intended -- there is nothing for a future careless goal
# to reach for.
#
# An entity is the easiest content in a mod to ship broken, because nothing about
# it fails at boot. A missing attribute supplier, a renderer that was never
# registered, a despawn flag that does not stick -- the server starts clean every
# time and the fault appears when somebody is standing in front of one.
#
# So this asserts the four properties that carry actual design weight:
#
#   * it exists and can be summoned at all      (registration + attributes)
#   * it has NO attack damage                   (the dread covenant, in code)
#   * it is immovable and hard to kill          (hitting one must be uninformative)
#   * it does not despawn, across a restart     (an institution, not weather)
#
# The attack-damage assertion is the load-bearing one. "Wardens never attack" is
# the single most important fact about them, and the way it silently stops being
# true is somebody adding a goal that needs a damage value and adding the attribute
# to make it work.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
want() { grep -qF "$2" "$1" || { echo "--- looked for: $2"; grep -E 'Warden|attribute' "$1" || true; fail "$3"; }; }

SEL='@e[type=interregnum:warden,limit=1]'
KEEP='@e[type=interregnum:shrine_keeper,limit=1]'
BYSTANDER=88888888-8888-4888-8888-888888888888

echo "1/2 summon one of each, and interrogate them"
# `wait` after the forceload, for the reason worldgen_check found the hard way:
# the chunk arrives asynchronously, and an entity added before its ENTITY storage
# has loaded is accepted and then invisible to every selector for the rest of the
# run. The summons below would succeed and every `data get entity` would answer
# "No entity was found". See docs/LESSONS.md #22.
COMMANDS="forceload add -32 -32 31 31
wait 5
summon interregnum:warden 8 -60 8
data get entity $SEL Health
data get entity $SEL PersistenceRequired
attribute $SEL minecraft:max_health get
attribute $SEL minecraft:knockback_resistance get
attribute $SEL minecraft:attack_damage get
summon interregnum:shrine_keeper 12 -60 12
data get entity $KEEP Health
attribute $KEEP minecraft:attack_damage get
interregnum talk start interregnum:shrine_keeper $BYSTANDER $KEEP
interregnum talk say $BYSTANDER restore
interregnum talk say $BYSTANDER give
interregnum regard $BYSTANDER
kill @e[type=interregnum:shrine_keeper]
interregnum regard $BYSTANDER" \
    LOG=/tmp/warden1.log timeout 2000 ./tools/server_smoke.sh > /tmp/wd1.txt 2>&1 \
    || { tail -25 /tmp/wd1.txt; fail "run 1 did not complete"; }

want /tmp/wd1.txt 'Summoned new Warden' \
    "the Warden could not be summoned -- unregistered, or it has no attribute supplier"
want /tmp/wd1.txt 'Warden has the following entity data: 100.0f' \
    "the Warden did not spawn at full health; its attributes are not being applied"
want /tmp/wd1.txt 'The value of attribute Max Health for entity Warden is 100.0' \
    "max health is not 100 -- a Warden must not be a thing you can casually kill"
want /tmp/wd1.txt 'The value of attribute Knockback Resistance for entity Warden is 1.0' \
    "a Warden can be knocked around; hitting one has to be uninformative, not fun"

# THE assertion. Note it passes on an ERROR reply: the command is supposed to fail,
# because the attribute is supposed to be absent.
want /tmp/wd1.txt 'Entity Warden has no attribute Attack Damage' \
    "a Warden has an attack damage attribute -- Wardens do not fight, and an attribute nobody uses today is what a melee goal uses tomorrow"

# Persistence is asserted through NBT rather than through behaviour on purpose: a
# headless server has no player, and Mob#checkDespawn returns early when there is
# nobody to be far away from, so the despawn path is unreachable here. What IS
# reachable is whether the flag survives being read back out of the save -- which
# is exactly the thing that was broken (docs/LESSONS.md #17).
want /tmp/wd1.txt 'Warden has the following entity data: 1b' \
    "PersistenceRequired came back false -- the constructor's setPersistenceRequired() is being overwritten by the NBT read, and this Warden will evaporate"

# --- the keeper ------------------------------------------------------------
want /tmp/wd1.txt 'Summoned new Shrine-Keeper' \
    "the shrine-keeper could not be summoned -- unregistered, or no attribute supplier"
want /tmp/wd1.txt 'Shrine-Keeper has the following entity data: 20.0f' \
    "the keeper is not an ordinary person's worth of health"
want /tmp/wd1.txt 'Entity Shrine-Keeper has no attribute Attack Damage' \
    "the keeper has attack damage -- they tend a shrine, they do not brawl"
want /tmp/wd1.txt 'talk=open scene=interregnum:shrine_keeper' \
    "a keeper cannot be spoken to, so the written scene has no way to reach anybody"

# The bystander squares the ledger honestly first, so a record EXISTS at +8 --
# "no record" would be a much weaker way to prove nothing was charged. Then the
# keeper is killed by a COMMAND rather than by a player. The villages do not blame
# anybody for that, and charging a player for a creeper's work is the kind of
# unfairness that teaches people never to walk near an NPC again.
[ "$(grep -c 'VILLAGES=WARY(8)' /tmp/wd1.txt)" = "2" ] \
    || { grep -E 'regard=' /tmp/wd1.txt;
         fail "a keeper's death by no player's hand still moved somebody's regard (or the honest ledger fix did not land in the first place)"; }

echo "2/2 restart: is it still standing there?"
# Same wait, mirrored reason: here the Warden is being read back OFF DISK, and the
# chunk's entities arrive after the chunk's blocks do. Asking too early answers
# "No entity was found", which this check would have reported as a Warden that did
# not survive the restart -- the loudest possible wrong answer.
KEEP_WORLD=1 COMMANDS="forceload add -32 -32 31 31
wait 5
data get entity $SEL Pos
data get entity $SEL PersistenceRequired" \
    LOG=/tmp/warden2.log timeout 2000 ./tools/server_smoke.sh > /tmp/wd2.txt 2>&1 \
    || { tail -25 /tmp/wd2.txt; fail "run 2 did not complete"; }

# Assert that it EXISTS and is roughly where it was left -- not that it is at the
# exact summon coordinate. The first version of this line pinned 8.5d and failed on
# a perfectly healthy Warden that had simply walked four blocks, which is the
# behaviour the stroll goal is there to produce. A check that fails on the feature
# working is measuring the wrong thing (docs/VERIFICATION.md rule 1).
pos=$(grep -oE 'Warden has the following entity data: \[[-0-9.]+d, [-0-9.]+d, [-0-9.]+d\]' /tmp/wd2.txt | head -1)
[ -n "$pos" ] || { grep -E 'Warden|No entity|Found no' /tmp/wd2.txt || true;
                   fail "no Warden answered after the restart -- it did not survive the save"; }
python3 - "$pos" <<'PY' || fail "the Warden came back somewhere it should not be"
import re, sys
x, y, z = (float(v) for v in re.findall(r'(-?[0-9.]+)d', sys.argv[1]))
if abs(y + 60.0) > 1.0:
    print(f"  y={y}: not standing on the flat world's surface"); sys.exit(1)
if max(abs(x - 8.5), abs(z - 8.5)) > 24.0:
    print(f"  ({x},{z}): too far from where it was left to be the same Warden"); sys.exit(1)
print(f"  still there, at ({x:.1f}, {z:.1f})")
PY
want /tmp/wd2.txt 'Warden has the following entity data: 1b' \
    "the Warden survived the restart but came back non-persistent"

echo
echo "OK: both mobs exist, neither can attack, and a death nobody caused costs nobody"
