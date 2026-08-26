#!/bin/bash
# Delivering a god's letter is a conversation that cannot end well, and the mechanics
# say so rather than the prose.
#
# WORLD.md locks the Verdant's reaction: "Delivered its letter it is immediately
# DEFENSIVE: it assumes it is being blamed, before anyone has said anything", and the
# estrangement is "professional rather than personal, which is worse."
#
# `RegardState.recordDeicide` drops every surviving god by 45 and locks a permanent cap
# at -10: "none of them will fully trust you again." The most generous path through this
# scene -- listen, wait, offer condolence, let it keep the answer it prepared, accept the
# errand -- is worth +29 and lands at -16. Still hostile. The scene has no branch that
# makes the Verdant comfortable.
#
# THE CAP ASSERTION BELOW IS WEAKER THAN IT LOOKS AND IS LABELLED AS SUCH. I wrote it
# believing it would catch a later scene with one over-generous branch, then tried it:
# raising this scene's most generous option to +25 produced exactly -10, not -1. The
# engine CLAMPS to the ceiling, so a scene physically cannot lift a god past it and the
# assertion cannot fail from the dialogue side. What it does prove is that the clamp
# survives the whole path -- JSON, conversation runtime, regard saved data, restart --
# rather than only inside `RegardState`, where core's own tests already cover it.
#
# Stated rather than quietly kept, because a check described as defending something it
# cannot defend is worse than no check: the next person writing a god scene would trust
# it. The real hazard it exposed is the opposite one and belongs in HANDOFF: an author
# can write +25 into an option, a capped player silently receives +0, and the choice
# reads as consequential while doing nothing.
#
# Also asserted, because a scene is data and data can be shipped broken:
#   * the scene LOADS on a real server. `dialogue_check.py` proves the JSON is
#     well-formed against the string table; it cannot prove the running game accepted it.
#   * it is playable end to end -- every node on the path resolves and the graph reaches
#     its ending. A dangling target passes the fast gate as a JSON fact and wedges a
#     table in front of real people.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }

SCENE=interregnum:verdant_delivery
# UUID participants, because `interregnum regard` takes a player id and the whole point
# of this file is reading the number back. Opaque string ids are fine for the table but
# have no regard to inspect.
#
# AND THE DEICIDE IS ATTRIBUTED TO A, which the first version of this file forgot. Bare
# `interregnum record deicide` records the milestone with no killer, so nobody's regard
# is touched: A came out of the scene at KNOWN(29) with no ceiling at all, and the cap
# assertion below -- the one this file exists for -- had nothing to test. It failed
# loudly rather than passing, which is the only reason it was caught. Naming the killer
# is what puts the ceiling in play.
A=11111111-1111-4111-8111-111111111111
B=22222222-2222-4222-8222-222222222222
C=33333333-3333-4333-8333-333333333333

# Every node answered by all three, whatever its rule: a node resolves when the LAST
# person picks, so a partial table simply hangs and the failure would look like a broken
# graph rather than a mis-written check.
COMMANDS="forceload add -16 -16 15 15
wait 3
execute positioned 0 -60 0 run interregnum record deicide $A
say BEFORE_SCENE
interregnum regard $A
interregnum talk start $SCENE $A+$B+$C
interregnum talk say $A letter
interregnum talk say $B letter
interregnum talk say $C letter
interregnum talk say $A wait
interregnum talk say $B wait
interregnum talk say $C wait
interregnum talk say $A condolence
interregnum talk say $B condolence
interregnum talk say $C condolence
interregnum talk say $A let_it_stand
interregnum talk say $B let_it_stand
interregnum talk say $C let_it_stand
interregnum talk say $A accept
interregnum talk say $B accept
interregnum talk say $C accept
say AFTER_SCENE
interregnum talk status $A
interregnum regard $A" \
    LOG=/tmp/delivery.log timeout 900 ./tools/server_smoke.sh > /tmp/dl.txt 2>&1 \
    || { tail -25 /tmp/dl.txt; fail "the run did not complete"; }

# --- the scene exists on a running server -----------------------------------
grep -q 'talk=open' /tmp/dl.txt || {
    grep -E 'talk=|verdant_delivery' /tmp/dl.txt | head -6 || true
    fail "the delivery scene did not start on a live server -- the fast gate proves the JSON is well-formed against the string table, not that the game accepted it"; }

# --- and it is playable to its end ------------------------------------------
# Asked as "the table is gone", which is what ending means: a scene that reached its
# last node closes its table, and a scene that wedged on an unresolvable node does not.
after=$(sed -n '/AFTER_SCENE/,$p' /tmp/dl.txt | grep -oE 'talk=[a-z]+' | head -1 || true)
[ "$after" = "talk=none" ] || {
    sed -n '/AFTER_SCENE/,$p' /tmp/dl.txt | grep -E 'talk=' | head -4 || true
    fail "the table was still open after the last answer ('$after') -- the scene did not reach an ending, so somewhere on the generous path a node does not resolve and three real people would be sitting there"; }

# --- the regard actually moved ----------------------------------------------
v_before=$(sed -n '/BEFORE_SCENE/,/AFTER_SCENE/p' /tmp/dl.txt | grep -oE 'VERDANT=[A-Z]+\(-?[0-9]+\)' | head -1 | grep -oE '\(-?[0-9]+\)' | tr -d '()')
v_after=$(sed -n '/AFTER_SCENE/,$p' /tmp/dl.txt | grep -oE 'VERDANT=[A-Z]+\(-?[0-9]+\)' | head -1 | grep -oE '\(-?[0-9]+\)' | tr -d '()')
cap=$(sed -n '/AFTER_SCENE/,$p' /tmp/dl.txt | grep -oE 'VERDANT=[A-Z]+\(-?[0-9]+\)cap-?[0-9]+' | head -1 | grep -oE 'cap-?[0-9]+$' | sed 's/cap//')
[ -n "$v_before" ] && [ -n "$v_after" ] && [ -n "$cap" ] || {
    grep -oE 'VERDANT=[A-Za-z]+\(-?[0-9]+\)(cap-?[0-9]+)?' /tmp/dl.txt | head -4 || true
    fail "could not read the Verdant's regard before and after -- the assertions below have nothing to compare"; }

echo "  VERDANT: $v_before -> $v_after   (permanent cap $cap)"

[ "$v_after" -gt "$v_before" ] || \
    fail "the most generous path through the delivery scene left the Verdant at $v_after, no better than the $v_before it started at -- the choices in this scene are decoration"

# --- and the ceiling holds all the way out to storage -----------------------
# See the header for what this does and does not prove. It is an end-to-end check on the
# clamp, not a guard against a generous scene -- the engine makes that impossible.
[ "$v_after" -le "$cap" ] || \
    fail "the delivery scene lifted the Verdant to $v_after, past the permanent cap of $cap that killing a god imposes. WORLD.md is explicit that the surviving gods write the killer off; a scene that can undo that in one conversation makes the deicide a debt instead of a scar"

echo
echo "OK: the Verdant's letter can be delivered, the scene plays to its end, and the most generous path still leaves a god that will not have you"
