#!/bin/bash
# The codex desk: the safe way to read a dead god's handwriting.
#
# WORLD.md, locked: "Raw god-script ... read WITHOUT TRANSCRIPTION at the ferry's desk marks
# the reader. Knowledge-as-hazard; the codex desk is the safe path." And: "The safe path
# costs time and a trip to the desk. The unsafe path costs nothing at all, which is exactly
# why people will take it."
#
# THE DESK IS A LECTERN, and that is an argument this codebase has already made in blocks.
# FerryPad: "an institution does not redesign its dock for each god. It has a standard
# dock." The Post used what was to hand. A lectern does nothing unusual unless somebody
# offers it one of four letters.
#
# WHAT IS ASSERTED:
#   * a letter read with no copy in the Post MARKS the reader -- the hazard still works,
#     and this probe is the control for everything below it;
#   * a desk TAKES TIME. Lodged and immediately asked, it is not finished; the letter
#     cannot be collected, and a desk that copied on the click would make the hazard a
#     formality nobody notices;
#   * after the wait it CAN be collected, and the Post has a copy;
#   * READING THE SAME LETTER IS THEN FREE. Not "cheaper" -- free, and for a reader who has
#     never been near the desk, because a transcription belongs to the world rather than to
#     whoever paid for it;
#   * A DIFFERENT LETTER IS STILL DANGEROUS. Without this, "reading is free now" is equally
#     satisfied by a transcription that switched the whole hazard off;
#   * A CARVED STONE IS STILL DANGEROUS. Stones cannot be brought to a desk and never stop
#     being the god's own hand, which is why letters are finite and stones are not;
#   * and a lectern with nothing on it says so rather than inventing a letter.
#
# The reading half is driven through `interregnum script`, which reports the count and the
# odds -- the hazard is deliberately silent in play and there is nothing else to look at.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }

A=aaaaaaaa-1111-4111-8111-111111111111
B=bbbbbbbb-2222-4222-8222-222222222222

# 30 seconds of clerk's work, so the wait is 33 to clear it with room. `wait` is the rcon
# driver's own directive and counts seconds.
COMMANDS="forceload add -16 -16 31 31
wait 3
setblock 4 -60 4 minecraft:lectern replace
setblock 8 -60 8 minecraft:lectern replace
setblock 12 -60 12 interregnum:shrine_stone_carved replace
wait 2

say RAW_FIRST
interregnum script $A read_letter anchorite
say EMPTY_DESK
interregnum desk 8 -60 8
say LODGING
interregnum desk 4 -60 4 lodge anchorite
say TOO_SOON
interregnum desk 4 -60 4 collect
wait 33
say COLLECTING
interregnum desk 4 -60 4 collect
say AFTER
interregnum script $B read_letter anchorite
say OTHER_LETTER
interregnum script $B read_letter verdant
say A_STONE
interregnum script $B read 12 -60 12" \
    LOG=/tmp/desk.log timeout 900 ./tools/server_smoke.sh > /tmp/dk.txt 2>&1 \
    || { tail -25 /tmp/dk.txt; fail "the run did not complete"; }

scripts=$(grep -oE 'script=[A-Z_]+ read=[0-9]+ odds=[0-9]+ mean=[0-9]+' /tmp/dk.txt || true)
desks=$(grep -oE 'desk=[A-Z_]+( [a-z_]+)? left=[0-9]+ copies=[0-9]+ working=[0-9]+' /tmp/dk.txt || true)
[ -n "$scripts" ] || { tail -20 /tmp/dk.txt; fail "the script command produced no answer at all"; }
[ -n "$desks" ] || { tail -20 /tmp/dk.txt; fail "the desk command produced no answer at all"; }

rawfirst=$(echo "$scripts" | head -1)
after=$(echo "$scripts" | sed -n 2p)
other=$(echo "$scripts" | sed -n 3p)
stone=$(echo "$scripts" | sed -n 4p)

empty=$(echo "$desks" | head -1)
lodged=$(echo "$desks" | sed -n 2p)
toosoon=$(echo "$desks" | sed -n 3p)
collected=$(echo "$desks" | sed -n 4p)

field() { echo "$1" | grep -oE "$2=[0-9]+" | grep -oE '[0-9]+'; }

# --- the hazard still works ---------------------------------------------------
# The control for everything below. If reading an untranscribed letter were already free,
# every "it is free now" assertion would pass against a mod that never marked anybody.
echo "$rawfirst" | grep -q 'script=MARKED' || {
    echo "  reading a letter with no copy in the Post: $rawfirst"
    fail "reading a letter that has never been near a desk did not mark the reader. The hazard is the thing the desk is the safe path AWAY from; if it is already off, nothing below this line means anything"; }

# --- an empty lectern says so -------------------------------------------------
echo "$empty" | grep -q 'desk=EMPTY' || {
    echo "  an untouched lectern: $empty"
    fail "a lectern with nothing on it did not report itself empty ('$empty')"; }
[ "$(field "$empty" copies)" = "0" ] || \
    fail "the Post has copies of letters nobody has transcribed ('$empty')"

# --- lodging takes the letter, and the clerk takes time -----------------------
echo "$lodged" | grep -q 'desk=LODGED' || {
    echo "  offering a letter to a lectern: $lodged"
    fail "a letter could not be left at a desk"; }
[ "$(field "$lodged" working)" = "1" ] || fail "the letter was accepted and the desk is not working ('$lodged')"
[ "$(field "$lodged" left)" -gt 0 ] || \
    fail "a letter just put down has no work left on it ('$lodged'). A desk that copies on the click is a free lever, and a hazard with a free lever beside it is a formality"

echo "$toosoon" | grep -q 'desk=NOT_YET' || {
    echo "  asking for it back immediately: $toosoon"
    fail "the letter came back before the clerk had finished ('$toosoon'). The waiting IS the cost -- WORLD.md's whole account of why anybody takes the unsafe path is that the safe one takes the letter out of your hands"; }
[ "$(field "$toosoon" copies)" = "0" ] || \
    fail "the Post recorded a copy before the work was done ('$toosoon')"

# --- and then it can be collected --------------------------------------------
echo "$collected" | grep -q 'desk=COLLECTED anchorite' || {
    echo "  after the wait: $collected"
    fail "the letter could not be collected after the clerk's work was done ('$collected'). Four letters exist in a world; a desk that keeps one is a desk holding a piece of the endgame"; }
[ "$(field "$collected" copies)" = "1" ] || fail "the letter came back and no copy was recorded ('$collected')"
[ "$(field "$collected" working)" = "0" ] || fail "the letter came back and the desk still has it ('$collected')"

# --- READING IT IS NOW FREE, for somebody who never went near the desk --------
# The whole point. A transcription belongs to the world rather than to whoever paid for it.
echo "$after" | grep -q 'script=TRANSCRIBED' || {
    echo "  a DIFFERENT reader opening the same letter afterwards: $after"
    fail "reading a transcribed letter still marked the reader. WORLD.md's hazard is raw script read WITHOUT transcription -- if a copy in the Post changes nothing then the desk is scenery and the locked choice between paths has one option"; }
[ "$(field "$after" read)" = "0" ] || \
    fail "the second reader was marked by a transcribed letter ('$after')"

# --- A DIFFERENT LETTER IS STILL DANGEROUS -----------------------------------
# The control. Without it, "reading is free now" is equally satisfied by a transcription
# that switched the hazard off for everything.
echo "$other" | grep -q 'script=MARKED' || {
    echo "  the same reader, a letter nobody transcribed: $other"
    fail "transcribing ONE letter made every letter safe. The Post copies what it is given; a copy of the Anchorite's letter says nothing about the Verdant's"; }

# --- AND SO IS A CARVED STONE -------------------------------------------------
# Stones cannot be brought to a desk. That asymmetry is why there are four letters that can
# be made safe and any number of inscriptions that cannot.
echo "$stone" | grep -q 'script=MARKED' || {
    echo "  a carved shrine stone, after a letter was transcribed: $stone"
    fail "transcribing a letter made a carved stone safe to read. A stone cannot be carried to a desk and never stops being the god's own hand -- if the Post's copies reach them, the hazard has an end and WORLD.md's 'most people don't bother' describes nothing"; }

echo
printf "OK: an untranscribed letter marks its reader, a desk takes the letter and takes time,\n    a copy makes that letter free for everybody, and neither another letter nor a\n    carved stone is covered by it\n"
