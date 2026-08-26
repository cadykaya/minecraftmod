#!/bin/bash
# The one thing the dead god does that a second person can see.
#
# WORLD.md, locked: "Rarely, a manifestation is SERVER-REAL -- a bystander sees the door
# move too. Not a sanity bar: a CREDIBILITY PROBLEM."
#
# That distinction is the whole feature. Everything else on the Haunt's list is rendered
# for one player, and a mod that only ever did that has built a sanity meter: the killer
# sees things, everybody knows the killer sees things, and nobody has to decide anything.
# The rare ones being REAL is what turns it into a question two people can disagree about.
#
# WHAT IS ASSERTED:
#   * nothing moves while the god is alive;
#   * ONLY the killer -- the binding is personal, and a haunting that followed anybody
#     standing nearby would be weather;
#   * the door ACTUALLY MOVES, read back off the block state rather than off the command's
#     own reply. A method that returned MOVED and changed nothing would pass every
#     assertion that trusted the reply;
#   * it toggles rather than opens: the ghost is moving the door, not opening doors;
#   * A DOOR OUT OF REACH IS NEVER TOUCHED. This is the control, and without it "the near
#     door moved" is equally satisfied by every door in the world moving -- which would
#     also look, from one door, exactly like the feature working;
#   * and an empty field produces NOTHING_TO_MOVE rather than something invented. A
#     haunting that supplied its own evidence would be the mod talking about itself.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/manifest.log; }
want() { grep -qF "$2" "$1" || { echo "--- looked for: $2"; grep -oE 'manifest=[A-Za-z_ =]+' "$1" | head -8 || true; fail "$3"; }; }

K=77777777-7777-4777-8777-777777777777
O=88888888-8888-4888-8888-888888888888

# Ground in this flat world is at y=-61, so a door stands at -60 and -59. Both halves are
# placed explicitly: `setblock` of one half alone leaves a door that vanilla will break.
# The near door is two blocks from where the ghost reaches; the far one is forty, well
# outside REACH, and is the control.
COMMANDS="forceload add -16 -16 63 63
wait 3
setblock 2 -60 2 minecraft:oak_door[half=lower,facing=north,hinge=left,open=false] replace
setblock 2 -59 2 minecraft:oak_door[half=upper,facing=north,hinge=left,open=false] replace
setblock 40 -60 40 minecraft:oak_door[half=lower,facing=north,hinge=left,open=false] replace
setblock 40 -59 40 minecraft:oak_door[half=upper,facing=north,hinge=left,open=false] replace
interregnum haunt manifest $K 0 -60 0
execute positioned 0 -60 0 run interregnum record deicide $K
interregnum haunt manifest $O 0 -60 0
interregnum haunt manifest $K 32 -60 0
interregnum haunt manifest $K 0 -60 0
say AFTER_FIRST
execute if block 2 -60 2 minecraft:oak_door[open=true] run say NEAR_OPENED
execute if block 40 -60 40 minecraft:oak_door[open=false] run say FAR_UNTOUCHED_AFTER_FIRST
interregnum haunt manifest $K 0 -60 0
say AFTER_SECOND
execute if block 2 -60 2 minecraft:oak_door[open=false] run say NEAR_CLOSED_AGAIN
execute if block 40 -60 40 minecraft:oak_door[open=false] run say FAR_UNTOUCHED_AFTER_SECOND
interregnum haunt manifest notauuid 0 -60 0" \
    LOG=/tmp/manifest.log timeout 900 ./tools/server_smoke.sh > /tmp/mn.txt 2>&1 \
    || { tail -25 /tmp/mn.txt; fail "the run did not complete"; }

want /tmp/mn.txt 'manifest=NO_GHOST' \
    "a door moved while the god was still alive. There is nothing to be haunted by yet"
want /tmp/mn.txt 'manifest=NOT_THE_KILLER' \
    "the ghost manifested around somebody who did not kill it. The binding is personal, and a haunting that follows whoever is standing nearby is weather"
want /tmp/mn.txt 'manifest=NOTHING_TO_MOVE' \
    "a haunting in an empty field found something to move. Inventing an object would be the mod supplying its own evidence"
want /tmp/mn.txt 'manifest=refused reason=not a player id' \
    "a non-player id was accepted"

# --- the door actually moved, read off the world and not off the reply ------
want /tmp/mn.txt 'manifest=MOVED' \
    "the ghost never moved anything at all"
mark NEAR_OPENED || {
    grep -oE '(NEAR|FAR)_[A-Z_]+' /tmp/manifest.log | sort | uniq -c || true
    fail "the command reported MOVED and the door beside it is still shut. A method that returns an outcome and changes nothing passes every assertion that trusts its own reply"; }

# --- it toggles: the ghost moves the door, it does not open doors -----------
mark NEAR_CLOSED_AGAIN || \
    fail "a second manifestation left the door open. The beat is that the door MOVED -- a spell that only ever opens things is a lock-pick, and after an hour every door in the world would be standing open"

# --- THE CONTROL: reach is real --------------------------------------------
# Asserted at both moments rather than once at the end. A far door that opened and shut
# again between the two manifestations would satisfy a single check at the end while every
# door in the world had in fact been moving.
mark FAR_UNTOUCHED_AFTER_FIRST || \
    fail "a door forty blocks away moved too. Reach is what makes the killer plainly the centre of it; a door moving across the valley is just a door moving"
mark FAR_UNTOUCHED_AFTER_SECOND || \
    fail "the far door had moved by the second manifestation. Whatever the near door is doing, it is not the only thing being moved"

echo
echo "OK: the dead god moves one door beside its killer, moves it back, and never touches the one across the field"
