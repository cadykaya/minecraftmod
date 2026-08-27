#!/bin/bash
# Reading the dead god's own hand, and what it costs.
#
# WORLD.md, locked: "Raw god-script (letters, shrine inscriptions) read without
# transcription at the ferry's desk MARKS the reader. Knowledge-as-hazard; the codex desk
# is the safe path." And: "'marks' means one specific thing: THE GHOST GETS LOUDER. Reading
# raw script raises your manifestation rate -- the server-real one, the door that moves
# while somebody else is standing there. Nothing else changes. No affliction bar, no
# debuff, no visions system to build."
#
# THE HAZARD IS DELIBERATELY SILENT. A player is never told their manifestation rate has
# moved -- an announcement would be an affliction bar made of text, which the locked line
# rules out. So `interregnum script <who>` exists to report what nothing else can see, and
# every assertion below is about a number the fiction keeps to itself.
#
# WHAT IS ASSERTED:
#   * a carved shrine stone CAN BE READ. It has advertised "a band of the dead god's
#     script" in its own javadoc since the chapter-0 art pass and nothing could look at it;
#     the reachability audit found that and deliberately left it until WORLD.md had decided
#     what reading costs;
#   * reading it MARKS the reader, and the odds move;
#   * reading THE SAME STONE AGAIN changes nothing. Knowledge is the hazard and a second
#     look is not more knowledge -- and a hazard you can grind by right-clicking one block
#     is a meter, not a fact about what you know;
#   * a DIFFERENT stone does count;
#   * IN THE DARK, NOTHING IS READ AND NOTHING IS MARKED. The refusal must not cost you:
#     "you could not see it" and "you saw it" have to be different states, or a player is
#     marked for failing to read something;
#   * a reader who has read nothing has EXACTLY the Manifestation's own odds -- the hazard
#     is a substitution for one number, not a second mechanism;
#   * and it is BOUNDED. However much is read, the ghost stops getting louder, because a
#     door that swings every minute is weather and weather is not a credibility problem.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }

A=11111111-2222-4333-8444-555555555555

# The stones are placed by hand rather than found: ShrineFeature puts carved stone in
# shrines, and hunting for one on a flat world is a search this check has no business
# doing. What is under test is the reading, not the worldgen.
#
# y=-60 is one above the flat world's ground at y=-61, so these stand in open daylight --
# which the reading needs. The dark one is walled into a solid cube with no light on any
# face, and it is done by FILLING the cube and then replacing the middle: a stone placed in
# open air and then covered would be lit for the tick in between.
#
# THE BURIED CUBE SITS INSIDE THE WORLD. The first version put it at y=-70, below the flat
# world's floor at y=-64, so `fill` and the reading both failed and the "in the dark" probe
# produced no line at all -- which the check then read as a stone that had been read in the
# dark. An out-of-range coordinate and a broken light rule look identical from an empty
# line, and only one of them is a bug in the mod.
COMMANDS="forceload add -16 -16 31 31
wait 3
setblock 4 -60 4 interregnum:shrine_stone_carved replace
setblock 8 -60 8 interregnum:shrine_stone_carved replace
fill 14 -64 14 18 -58 18 minecraft:stone replace
setblock 16 -61 16 interregnum:shrine_stone_carved replace
wait 2

say UNREAD
interregnum script $A
say FIRST
interregnum script $A read 4 -60 4
say AGAIN
interregnum script $A read 4 -60 4
say SECOND_STONE
interregnum script $A read 8 -60 8
say IN_THE_DARK
interregnum script $A read 16 -61 16
say GLUTTED
execute positioned 0 -60 0 run interregnum record deicide $A" \
    LOG=/tmp/script.log timeout 900 ./tools/server_smoke.sh > /tmp/sc.txt 2>&1 \
    || { tail -25 /tmp/sc.txt; fail "the run did not complete"; }

reads=$(grep -oE 'script=[A-Z_]+ read=[0-9]+ odds=[0-9]+ mean=[0-9]+' /tmp/sc.txt || true)
[ -n "$reads" ] || { tail -20 /tmp/sc.txt; fail "the script command produced no answer at all"; }
unread=$(echo "$reads" | head -1)
first=$(echo "$reads" | sed -n 2p)
again=$(echo "$reads" | sed -n 3p)
second=$(echo "$reads" | sed -n 4p)
dark=$(echo "$reads" | sed -n 5p)

field() { echo "$1" | grep -oE "$2=[0-9]+" | grep -oE '[0-9]+'; }

# --- nobody has read anything, and the odds are the Manifestation's own ------
echo "$unread" | grep -q 'read=0' || {
    echo "  before anything was read: $unread"
    fail "somebody who has never looked at anything is already marked"; }
base=$(field "$unread" odds)
[ "$base" = "90" ] || \
    fail "an unread reader's odds are $base, not the Manifestation's own 90. This hazard is a substitution for one existing number; if the unread case has drifted then every world is being punished for a thing nobody did"

# --- the stone can be read, and reading it marks -----------------------------
echo "$first" | grep -q 'script=MARKED' || {
    echo "  reading a carved stone in daylight: $first"
    fail "a carved shrine stone could not be read. It has advertised a band of the dead god's script in its own javadoc since the chapter-0 art pass, and this is the increment that was supposed to make that true"; }
echo "$first" | grep -q 'read=1' || fail "the reading was reported but the ledger did not take it ('$first')"
one=$(field "$first" odds)
[ "$one" -lt "$base" ] || \
    fail "reading a piece of raw god-script did not move the odds ($base -> $one). WORLD.md locks 'marks' as meaning exactly one thing -- the ghost gets louder -- and if nothing moves then reading is decoration"

# --- reading it AGAIN changes nothing ----------------------------------------
# The assertion that keeps this a fact about knowledge rather than a meter. A hazard you
# can deepen by right-clicking the same block is a hazard with a grind in it.
echo "$again" | grep -q 'script=ALREADY' || {
    echo "  second look at the same stone: $again"
    fail "reading the same stone twice was recorded as a second reading"; }
[ "$(field "$again" read)" = "1" ] || \
    fail "the same stone read twice counts twice ('$again'). Knowledge is the hazard and a second look is not more knowledge -- and a player who can grind this by right-clicking one block has a meter rather than a world"
[ "$(field "$again" odds)" = "$one" ] || \
    fail "the odds moved on a re-reading even though the count did not ('$again') -- two things that must agree are being computed separately"

# --- a different stone does count --------------------------------------------
# The control for the one above: without it, "the count did not move" is equally satisfied
# by a ledger that stopped recording anything after the first entry.
echo "$second" | grep -q 'script=MARKED' || \
    fail "a DIFFERENT carved stone was not a new reading ('$second'). Either the mark is not the stone's position, or the ledger stopped taking entries after the first"
[ "$(field "$second" read)" = "2" ] || fail "a second distinct stone did not raise the count ('$second')"
[ "$(field "$second" odds)" -lt "$one" ] || \
    fail "a second distinct reading did not make the ghost louder ('$second')"

# --- in the dark, nothing is read and nothing is marked ----------------------
# The refusal must not cost you. If a failed reading marked, a player would be punished
# for NOT having read something, which is the exact inverse of the locked hazard.
echo "$dark" | grep -q 'script=TOO_DARK' || {
    echo "  a stone buried in stone with no light on any face: $dark"
    fail "a carved stone with no light on it was read anyway. The light rule is the steles' own and the same worn carving has the same problem; if it does not apply then the darkness has stopped meaning anything"; }
[ "$(field "$dark" read)" = "2" ] || \
    fail "a reading that was refused for darkness marked the reader anyway ('$dark'). That punishes somebody for failing to read something, which is the inverse of the hazard WORLD.md locks"

# --- and it is bounded --------------------------------------------------------
# Asserted against the core rule rather than by reading a hundred stones, and the check
# says so: what a live run can show is that the odds MOVE and that the seam agrees with
# the rule. The floor itself is a pure function and is guarded in the self-test, where a
# mutation watches it.
floor=$(grep -oE 'LOUDEST = [0-9]+' core/src/main/java/com/cadykaya/interregnum/core/haunt/Script.java | grep -oE '[0-9]+')
[ -n "$floor" ] && [ "$floor" -gt 1 ] || \
    fail "the loudest the ghost gets is $floor -- at one it manifests every interval, which is a metronome, and Manifestation's whole argument is that a thing on a schedule is a mechanic rather than a rumour"
[ "$(field "$second" mean)" -gt 0 ] || \
    fail "the reported mean between manifestations is not a positive number of ticks ('$second')"

echo
printf "OK: a carved stone reads, reading marks and the odds move, the same stone twice is\n    the same knowledge, a different one is not, and a stone nobody could see leaves\n    the reader unmarked\n"
