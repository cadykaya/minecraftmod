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
SCENE2=interregnum:anchorite_delivery
SCENE3=interregnum:hearth_turner_delivery
SCENE4=interregnum:quiet_one_delivery
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
interregnum status
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
interregnum regard $A
interregnum talk start $SCENE2 $A+$B+$C
interregnum talk say $A heavy
interregnum talk say $B heavy
interregnum talk say $C heavy
interregnum talk say $A letter
interregnum talk say $B letter
interregnum talk say $C letter
interregnum talk say $A wait
interregnum talk say $B wait
interregnum talk say $C wait
interregnum talk say $A thanks
interregnum talk say $B thanks
interregnum talk say $C thanks
interregnum talk say $A put_it_down
interregnum talk say $B put_it_down
interregnum talk say $C put_it_down
interregnum talk say $A onward
interregnum talk say $B onward
interregnum talk say $C onward
interregnum talk say $A accept
interregnum talk say $B accept
interregnum talk say $C accept
say AFTER_SCENE2
interregnum talk status $A
interregnum regard $A
interregnum talk start $SCENE3 $A+$B+$C
interregnum talk say $A how
interregnum talk say $B how
interregnum talk say $C how
interregnum talk say $A show_me
interregnum talk say $B show_me
interregnum talk say $C show_me
interregnum talk say $A keep_going
interregnum talk say $B keep_going
interregnum talk say $C keep_going
interregnum talk say $A onward
interregnum talk say $B onward
interregnum talk say $C onward
interregnum talk say $A give_it
interregnum talk say $B give_it
interregnum talk say $C give_it
interregnum talk say $A onward
interregnum talk say $B onward
interregnum talk say $C onward
interregnum talk say $A accept
interregnum talk say $B accept
interregnum talk say $C accept
say AFTER_SCENE3
interregnum talk status $A
interregnum regard $A
interregnum talk start $SCENE4 $A+$B+$C
interregnum talk say $A wait
interregnum talk say $B wait
interregnum talk say $C wait
interregnum talk say $A read_aloud
interregnum talk say $B read_aloud
interregnum talk say $C read_aloud
interregnum talk say $A count_them
interregnum talk say $B count_them
interregnum talk say $C count_them
interregnum talk say $A onward
interregnum talk say $B onward
interregnum talk say $C onward
interregnum talk say $A sign_for_it
interregnum talk say $B sign_for_it
interregnum talk say $C sign_for_it
interregnum talk say $A onward
interregnum talk say $B onward
interregnum talk say $C onward
interregnum talk say $A come_back
interregnum talk say $B come_back
interregnum talk say $C come_back
say AFTER_SCENE4
interregnum talk status $A
interregnum regard $A
interregnum status" \
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

# --- and the second god's scene, which is a different shape ------------------
# The Verdant opens mid-argument, defending an arrangement nobody mentioned. The
# Anchorite does not defend anything: it has been holding something for an age and asks
# what the load is before it asks who you are. Two gods, two openings, one machinery --
# and this asserts the second one is reachable, plays to its end, and pays.
after2=$(sed -n '/AFTER_SCENE2/,$p' /tmp/dl.txt | grep -oE 'talk=[a-z]+' | head -1 || true)
[ "$after2" = "talk=none" ] || {
    sed -n '/AFTER_SCENE2/,$p' /tmp/dl.txt | grep -E 'talk=' | head -4 || true
    fail "the Anchorite's table was still open after the last answer ('$after2') -- the scene did not reach an ending, so somewhere on the generous path a node does not resolve"; }

a_before=$(sed -n '/AFTER_SCENE/,/AFTER_SCENE2/p' /tmp/dl.txt | grep -oE 'ANCHORITE=[A-Z]+\(-?[0-9]+\)' | head -1 | grep -oE '\(-?[0-9]+\)' | tr -d '()')
a_after=$(sed -n '/AFTER_SCENE2/,$p' /tmp/dl.txt | grep -oE 'ANCHORITE=[A-Z]+\(-?[0-9]+\)' | head -1 | grep -oE '\(-?[0-9]+\)' | tr -d '()')
a_cap=$(sed -n '/AFTER_SCENE2/,$p' /tmp/dl.txt | grep -oE 'ANCHORITE=[A-Z]+\(-?[0-9]+\)cap-?[0-9]+' | head -1 | grep -oE 'cap-?[0-9]+$' | sed 's/cap//')
[ -n "$a_before" ] && [ -n "$a_after" ] && [ -n "$a_cap" ] || {
    grep -oE 'ANCHORITE=[A-Za-z]+\(-?[0-9]+\)(cap-?[0-9]+)?' /tmp/dl.txt | head -4 || true
    fail "could not read the Anchorite's regard before and after its scene"; }

echo "  ANCHORITE: $a_before -> $a_after   (permanent cap $a_cap)"
[ "$a_after" -gt "$a_before" ] || \
    fail "the most generous path through the Anchorite's scene left it at $a_after, no better than the $a_before it started at -- the choices in that scene are decoration"

# STRICTLY under, not merely at. `dialogue_check.py` now refuses a scene whose best route
# would reach the ceiling, because `adjust` clamps and the tail of such a scene pays a
# capped player nothing. Landing exactly ON the cap here would mean the fast gate's
# arithmetic and the running game disagree about what the best route is worth.
[ "$a_after" -lt "$a_cap" ] || \
    fail "the Anchorite finished at $a_after, at or past its permanent cap of $a_cap. dialogue_check.py computes the best route statically and refuses one that reaches the ceiling; if the live number gets there anyway, the two disagree and the static guard is not guarding what it thinks"

# --- the third, and the one that is mostly exposition -----------------------
# The Hearth-Turner is the exposition god, which makes its scene the one most likely to
# wedge: the generous route runs through every optional beat rather than skipping them,
# so if any of those nodes fails to resolve this is where it shows.
after3=$(sed -n '/AFTER_SCENE3/,$p' /tmp/dl.txt | grep -oE 'talk=[a-z]+' | head -1 || true)
[ "$after3" = "talk=none" ] || {
    sed -n '/AFTER_SCENE3/,$p' /tmp/dl.txt | grep -E 'talk=' | head -4 || true
    fail "the Hearth-Turner's table was still open after the last answer ('$after3') -- the longest generous path in the mod does not reach its ending"; }

h_before=$(sed -n '/AFTER_SCENE2/,/AFTER_SCENE3/p' /tmp/dl.txt | grep -oE 'HEARTH_TURNER=[A-Z]+\(-?[0-9]+\)' | head -1 | grep -oE '\(-?[0-9]+\)' | tr -d '()')
h_after=$(sed -n '/AFTER_SCENE3/,$p' /tmp/dl.txt | grep -oE 'HEARTH_TURNER=[A-Z]+\(-?[0-9]+\)' | head -1 | grep -oE '\(-?[0-9]+\)' | tr -d '()')
h_cap=$(sed -n '/AFTER_SCENE3/,$p' /tmp/dl.txt | grep -oE 'HEARTH_TURNER=[A-Z]+\(-?[0-9]+\)cap-?[0-9]+' | head -1 | grep -oE 'cap-?[0-9]+$' | sed 's/cap//')
[ -n "$h_before" ] && [ -n "$h_after" ] && [ -n "$h_cap" ] || {
    grep -oE 'HEARTH_TURNER=[A-Za-z]+\(-?[0-9]+\)(cap-?[0-9]+)?' /tmp/dl.txt | head -4 || true
    fail "could not read the Hearth-Turner's regard before and after its scene"; }

echo "  HEARTH_TURNER: $h_before -> $h_after   (permanent cap $h_cap)"
[ "$h_after" -gt "$h_before" ] || \
    fail "the most generous path through the Hearth-Turner's scene left it at $h_after, no better than the $h_before it started at -- the choices in that scene are decoration"
[ "$h_after" -lt "$h_cap" ] || \
    fail "the Hearth-Turner finished at $h_after, at or past its permanent cap of $h_cap -- the static guard in dialogue_check.py and the running game disagree about what the best route is worth"

# --- the fourth, where nothing in the text ever says it worked ---------------
# The Quiet One never speaks. Every line in its scene is a description of what does not
# happen, because the whole world is built on the difference between refusing LOUDLY --
# which is what the Nether and the End do to a bed -- and declining to react at all.
#
# WHICH MAKES THIS THE ONE ASSERTION THAT SEES SOMETHING THE PLAYER CANNOT. Regard with
# this god moves exactly as it does in the other three scenes, and nothing in the text
# ever acknowledges it: you are treated differently afterwards by something that never
# told you it noticed. That is the intended experience, and it is indistinguishable from
# the consequences being silently broken unless a check reads the number.
after4=$(sed -n '/AFTER_SCENE4/,$p' /tmp/dl.txt | grep -oE 'talk=[a-z]+' | head -1 || true)
[ "$after4" = "talk=none" ] || {
    sed -n '/AFTER_SCENE4/,$p' /tmp/dl.txt | grep -E 'talk=' | head -4 || true
    fail "the Quiet One's table was still open after the last answer ('$after4') -- the scene did not reach an ending"; }

q_before=$(sed -n '/AFTER_SCENE3/,/AFTER_SCENE4/p' /tmp/dl.txt | grep -oE 'QUIET_ONE=[A-Z]+\(-?[0-9]+\)' | head -1 | grep -oE '\(-?[0-9]+\)' | tr -d '()')
q_after=$(sed -n '/AFTER_SCENE4/,$p' /tmp/dl.txt | grep -oE 'QUIET_ONE=[A-Z]+\(-?[0-9]+\)' | head -1 | grep -oE '\(-?[0-9]+\)' | tr -d '()')
q_cap=$(sed -n '/AFTER_SCENE4/,$p' /tmp/dl.txt | grep -oE 'QUIET_ONE=[A-Z]+\(-?[0-9]+\)cap-?[0-9]+' | head -1 | grep -oE 'cap-?[0-9]+$' | sed 's/cap//')
[ -n "$q_before" ] && [ -n "$q_after" ] && [ -n "$q_cap" ] || {
    grep -oE 'QUIET_ONE=[A-Za-z]+\(-?[0-9]+\)(cap-?[0-9]+)?' /tmp/dl.txt | head -4 || true
    fail "could not read the Quiet One's regard before and after its scene"; }

echo "  QUIET_ONE: $q_before -> $q_after   (permanent cap $q_cap)"
[ "$q_after" -gt "$q_before" ] || \
    fail "the Quiet One's regard did not move ($q_before -> $q_after). Its scene deliberately never acknowledges the player, so a silently broken consequence here looks EXACTLY like the scene working as designed -- this is the only place in the mod where the check sees something the player is not shown, and it is the whole reason the assertion exists"
[ "$q_after" -lt "$q_cap" ] || \
    fail "the Quiet One finished at $q_after, at or past its permanent cap of $q_cap -- the static guard and the running game disagree about the best route"

# --- and delivering actually MOVES THE WORLD ---------------------------------
# The gap this half was written for. `LETTER_DELIVERED` existed in core, `ChapterState`
# counted letters, and `Chapter` gated the back half of the game on the count -- and
# nothing anywhere recorded one. All four letters could be delivered and the world would
# not move. Four scenes shipped and the milestone they exist to produce was never set.
#
# Nothing failed, because nothing was looking: the scenes played, the regard moved, every
# assertion above passed. Only the count is evidence.
letters_before=$(sed -n '/BEFORE_SCENE/,/AFTER_SCENE$/p' /tmp/dl.txt | grep -oE 'letters=[0-9]+' | head -1 | cut -d= -f2)
letters_after=$(sed -n '/AFTER_SCENE4/,$p' /tmp/dl.txt | grep -oE 'letters=[0-9]+' | head -1 | cut -d= -f2)
[ -n "$letters_before" ] && [ -n "$letters_after" ] || {
    grep -oE 'letters=[0-9]+|chapter=[A-Z]+' /tmp/dl.txt | head -6 || true
    fail "the status probe does not report a letter count -- this assertion has nothing to read, so nothing below it is evidence that delivering a letter does anything at all"; }

echo "  letters delivered: $letters_before -> $letters_after"
[ "$letters_before" = "0" ] || \
    fail "the world already had $letters_before letter(s) delivered before any scene ran -- the control does not exist"
[ "$letters_after" = "4" ] || \
    fail "four delivery scenes were played to their accepting endings and the world records $letters_after letter(s) delivered. The milestone is what Chapter gates the back half of the game on, so a scene that does not record it is a questline opener that opens nothing -- and every other assertion in this file passes while it is broken"

echo
printf "OK: all four gods' letters can be delivered, every scene plays to its end, the most\n    generous path through any of them still leaves a god that will not have you, the one\n    that never answers still forms an opinion, and the world counts every delivery\n"
