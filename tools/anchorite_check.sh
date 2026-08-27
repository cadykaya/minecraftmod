#!/bin/bash
# In the Anchorite's world, unanchored things rise.
#
# This is not a new idea being tested. The ferry has been printing it as a boarding
# refusal since before the world existed:
#
#   "Refused for the crossing to the Mass Authority. Nothing that pours. Where you
#    are going, unanchored things go up, and they do not stop."
#
# A player reads that before they arrive, so the world has to mean it. What this file
# asserts is that the promise and the behaviour are the same thing.
#
# Both halves are measured, because either alone is worthless:
#
#   * at home, sand dropped in mid-air LANDS      -- the control. Without it, "no sand
#                                                    on the floor" is satisfied by sand
#                                                    that never spawned, by a chunk that
#                                                    never loaded, and by a setblock
#                                                    that silently failed.
#   * in the Anchorite's, the same sand DOES NOT land, is no longer sitting where it
#     was put, and a falling-block entity is found ABOVE where it started.
#
# The last two clauses are what make this an assertion about RISING rather than about
# vanishing. "Did not land" is also satisfied by sand that was deleted, sand that fell
# through the void, and sand that never became an entity because gravity was switched
# off rather than reversed -- and all three are bugs that would ship. The first draft
# of this file waited six seconds and then could not distinguish "rose out of the
# world" from "never moved", which is why the wait is short and why the block's
# original position is checked as well as the floor.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/anchorite.log; }

# `wait` after forceload: chunk loading is asynchronous and entity storage arrives
# later still, so a falling block spawned into the gap is accepted and then invisible
# to every selector for the rest of the run (docs/LESSONS.md #22).
#
# Sand is placed at y=120 with air beneath it, so it becomes a FallingBlockEntity
# immediately. The stone slab at y=100 is the floor it would land on at home.
COMMANDS='execute in interregnum:mass_authority run forceload add -16 -16 15 15
forceload add -16 -16 15 15
wait 3

setblock 4 100 4 minecraft:stone replace
setblock 4 120 4 minecraft:sand replace
execute in interregnum:mass_authority run setblock 4 100 4 minecraft:stone replace
execute in interregnum:mass_authority run setblock 4 120 4 minecraft:sand replace
wait 2

execute in interregnum:mass_authority if block 4 120 4 minecraft:sand run say NEVER_LEFT_THE_SPOT
execute if block 4 101 4 minecraft:sand run say HOME_SAND_LANDED
execute in interregnum:mass_authority if block 4 101 4 minecraft:sand run say SAND_LANDED_THERE_TOO
execute in interregnum:mass_authority if entity @e[type=minecraft:falling_block,x=-64,y=125,z=-64,dx=128,dy=200,dz=128] run say SAND_ROSE
execute in interregnum:mass_authority if entity @e[type=minecraft:falling_block,x=-64,y=-64,z=-64,dx=128,dy=400,dz=128] run say SAND_STILL_EXISTS' \
    LOG=/tmp/anchorite.log timeout 2000 ./tools/server_smoke.sh > /tmp/an.txt 2>&1 \
    || { tail -25 /tmp/an.txt; fail "the run did not complete"; }

# --- the control: gravity still works at home -------------------------------
mark HOME_SAND_LANDED || {
    grep -iE "sand|falling|forceload" /tmp/an.txt | tail -10 || true
    fail "sand dropped in the overworld did not land on the floor beneath it -- the setup is broken, so nothing below this line means anything"; }

# --- the law ----------------------------------------------------------------
if mark SAND_LANDED_THERE_TOO; then
    fail "sand landed on the floor in interregnum:mass_authority -- it is still falling, and the ferry's boarding notice is now a lie"
fi

# Not merely "did not land": it is HIGHER than it started and it is still a thing.
if mark NEVER_LEFT_THE_SPOT; then
    fail "the sand is still sitting at 4 120 4 in interregnum:mass_authority -- it never became a falling block, so gravity there is OFF rather than REVERSED"
fi
mark SAND_STILL_EXISTS || \
    fail "there is no falling block in interregnum:mass_authority at all -- the sand was deleted or never spawned, which satisfies 'did not land' for entirely the wrong reason"
mark SAND_ROSE || {
    grep -iE "falling_block" /tmp/an.txt | tail -5 || true
    fail "a falling block exists but is not above y=125, where it was placed at y=120 -- it is hanging, not rising"; }

echo
echo "OK: in the Anchorite's world, what is not held goes up"
