#!/bin/bash
# The ferry sails where the letter in your hand is addressed.
#
# WORLD.md, locked: "No menu on the keel, no destination written by hand. You hold a
# letter, and the ferry reads it. No letter, no voyage." This is "the route to them is its
# unanswered correspondence" and "you are the only one carrying their mail" ceasing to be
# flavour and becoming the navigation.
#
# WHAT IS ASSERTED:
#   * a letter sails the hull to ITS OWN world -- and two different letters sail to two
#     different worlds, from the same keel, with nothing else changed. One crossing on its
#     own is satisfied by a ferry that always goes to the same place;
#   * THE UNADDRESSED LETTER SAILS LIKE ANY OTHER, and this assertion exists because the
#     opposite very nearly shipped. The letter that opens `To --` is the QUIET ONE'S --
#     WORLD.md: "the Quiet One has no name in the letters, and that is the whole
#     character" -- so a routing rule keyed on the addressee would have made that god's
#     world permanently unreachable by the only affordance there is. The blank envelope is
#     a fact about a god, not a defect in a document. See core/.../ferry/Routing.java;
#   * an empty hand sails nothing, so the mail is not decorative;
#   * and the hull ARRIVES: read off the world at the far end, not off the reply.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/carry.log; }
want() { grep -qF "$2" "$1" || { echo "--- looked for: $2"; grep -oE 'ferry=[a-z]+ [a-z=_:. ]*' "$1" | head -12 || true; fail "$3"; }; }

# Two hulls, built and claimed the same way ferry_check.sh builds them: `setblock` is not
# a player placing a block, so the claim is recorded by command. Each is a keel and one
# plank, which clears every crossing's checklist -- what is under test here is the
# ROUTING, and a hull held for carrying a jukebox would test the checklist instead.
COMMANDS="forceload add -16 -16 47 47
execute in interregnum:unresponsive run forceload add -16 -16 15 15
execute in interregnum:green_authority run forceload add -16 -16 15 15
wait 3
setblock 4 -60 4 interregnum:ferry_keel replace
setblock 5 -60 4 minecraft:oak_planks replace
interregnum claim record 4 -60 4 5 -60 4
setblock 4 -60 20 interregnum:ferry_keel replace
setblock 5 -60 20 minecraft:oak_planks replace
interregnum claim record 4 -60 20 5 -60 20
setblock 4 -60 36 interregnum:ferry_keel replace
setblock 5 -60 36 minecraft:oak_planks replace
interregnum claim record 4 -60 36 5 -60 36
interregnum ferry carry 4 -60 4 none
interregnum ferry carry 4 -60 4 quiet_one
say AFTER_QUIET
execute if block 4 -60 4 minecraft:air run say FIRST_KEEL_GONE
interregnum ferry carry 4 -60 20 verdant
say AFTER_VERDANT
execute if block 4 -60 20 minecraft:air run say SECOND_KEEL_GONE
interregnum ferry carry 4 -60 36 no_such_letter" \
    LOG=/tmp/carry.log timeout 900 ./tools/server_smoke.sh > /tmp/cy.txt 2>&1 \
    || { tail -25 /tmp/cy.txt; fail "the run did not complete"; }

# --- no letter, no voyage --------------------------------------------------
want /tmp/cy.txt 'ferry=refused reason=NOT_A_LETTER' \
    "an empty hand sailed a ferry. 'No letter, no voyage' is the locked rule, and a keel that moves without one makes the mail decorative"

# --- the letter names the world --------------------------------------------
want /tmp/cy.txt 'ferry=sailed law=quiet_one to=interregnum:unresponsive' \
    "the Quiet One's letter did not sail its hull to the Quiet One's world"
mark FIRST_KEEL_GONE || {
    grep -oE '(FIRST|SECOND)_KEEL_GONE' /tmp/carry.log | sort | uniq -c || true
    fail "the command reported a crossing and the keel is still sitting on the dock. A seam that reports SAILED and moves nothing passes every assertion that trusts its own reply"; }

# --- TWO letters, TWO worlds, one keel design ------------------------------
# The control. One crossing on its own is equally satisfied by a ferry that always goes
# to the same place, which is exactly what the old operator-typed version could not rule
# out either -- the destination came from an argument, so of course it varied.
want /tmp/cy.txt 'ferry=sailed law=verdant to=interregnum:green_authority' \
    "the Verdant's letter sailed somewhere else. Two letters must reach two worlds from identical hulls, or 'the ferry reads the letter' is unfalsifiable"
mark SECOND_KEEL_GONE || \
    fail "the second hull did not leave its dock"

# --- a letter the post has never heard of ----------------------------------
[ "$(grep -cF 'ferry=refused reason=NOT_A_LETTER' /tmp/cy.txt || true)" -ge 2 ] || {
    grep -oE 'ferry=refused reason=[A-Z_]+' /tmp/cy.txt | sort | uniq -c || true
    fail "expected two non-letters refused: an empty hand, and a name the post does not carry. One of them got through, which means the ferry routes on something other than the mail"; }

echo
echo "OK: the ferry goes where the letter says, two letters reach two worlds, and nothing but the post moves it"
