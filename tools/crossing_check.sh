#!/bin/bash
# The Quiet One's world exists, and it is not a second copy of the overworld.
#
# The trap this file is built around: a dimension that merely LOADS proves almost
# nothing. A level stem mis-wired to `minecraft:overworld`, a dimension_type that
# failed to resolve and silently fell back, a datapack that did not load at all and
# left the id resolving to something else -- every one of those still lets
# `execute in interregnum:unresponsive` succeed, and a check that stops there passes
# against all of them. So the assertions here are all RELATIONSHIPS between the two
# worlds rather than facts about one (docs/LESSONS.md #19):
#
#   * y=-10 is legal at home and illegal there            -> our min_y is in force
#   * y=250 is legal in both                              -> and it is not merely small
#   * a block written there is NOT at the same coordinates at home
#                                                         -> they are separate levels,
#                                                            not two names for one
#
# Only the first of those can be got right by accident, and only by an overworld copy
# that someone had already edited to start at y=0.
#
# What this file does NOT claim: that a bed does nothing here, that no raid can start,
# that the world is silent. Those are the actual law, and a headless server exposes no
# command that reads them back -- so they are asserted where they live, as data, by
# tools/dimension_check.py. Saying which half proves what is the point; a summary line
# that implies this ran the law is how a check starts lying.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/crossing.log; }

# `forceload` is per-dimension, hence the `execute in` on each. `wait` is not needed
# here -- nothing in this check touches entity storage (docs/LESSONS.md #22).
COMMANDS='execute in interregnum:unresponsive run forceload add -16 -16 15 15
forceload add -16 -16 15 15
execute in interregnum:unresponsive run say REACHED_UNRESPONSIVE

execute in interregnum:unresponsive run setblock 4 -10 4 minecraft:diamond_block replace
execute in interregnum:unresponsive if block 4 -10 4 minecraft:diamond_block run say FLOOR_LEAKED
setblock 4 -10 4 minecraft:diamond_block replace
execute if block 4 -10 4 minecraft:diamond_block run say HOME_ALLOWS_BELOW_ZERO

execute in interregnum:unresponsive run setblock 4 250 4 minecraft:diamond_block replace
execute in interregnum:unresponsive if block 4 250 4 minecraft:diamond_block run say CEILING_OK_THERE
setblock 4 250 4 minecraft:diamond_block replace
execute if block 4 250 4 minecraft:diamond_block run say CEILING_OK_HOME

execute in interregnum:unresponsive run setblock 8 100 8 minecraft:gold_block replace
execute in interregnum:unresponsive if block 8 100 8 minecraft:gold_block run say WROTE_THERE
execute if block 8 100 8 minecraft:gold_block run say SAME_WORLD_AFTER_ALL' \
    LOG=/tmp/crossing.log timeout 2000 ./tools/server_smoke.sh > /tmp/cx.txt 2>&1 \
    || { tail -25 /tmp/cx.txt; fail "the run did not complete"; }

# --- the dimension is there at all ------------------------------------------
mark REACHED_UNRESPONSIVE || {
    grep -iE "unresponsive|dimension|level stem" /tmp/cx.txt | tail -8 || true
    fail "interregnum:unresponsive did not load -- no command could be run in it"; }

# --- ...and it has OUR floor, which the overworld does not -------------------
# The control half first: if y=-10 stopped working at home the assertion below would
# pass for a reason that has nothing to do with our dimension_type.
mark HOME_ALLOWS_BELOW_ZERO || \
    fail "y=-10 is not writable in the overworld either, so the floor test proves nothing"
# `if mark ...; then fail; fi` rather than `mark ... && fail`. The && form does
# survive `set -e` here -- checked, not assumed -- but only because something follows
# it: as the last line of a script it would exit 1 on the PASSING path. The explicit
# form has no such trap for whoever edits this next (docs/LESSONS.md #23).
if mark FLOOR_LEAKED; then
    fail "a block landed at y=-10 in interregnum:unresponsive, which has min_y=0 -- the stem is not using our dimension_type"
fi

# --- and it is not merely a small world -------------------------------------
mark CEILING_OK_HOME || fail "y=250 is not writable at home -- the control is broken"
mark CEILING_OK_THERE || \
    fail "y=250 is not writable in interregnum:unresponsive, so its height is wrong, not just its floor"

# --- two worlds, not two names for one --------------------------------------
mark WROTE_THERE || fail "a block written in interregnum:unresponsive is not there"
if mark SAME_WORLD_AFTER_ALL; then
    fail "the same block is at those coordinates in the overworld -- the id resolves to the overworld"
fi

echo
echo "OK: interregnum:unresponsive is a separate world with its own floor and ceiling"
