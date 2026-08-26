#!/bin/bash
# The warning steles say something.
#
# WORLD.md, chapter 0: "the mod adds only content: shrines, WARNING STELES, sealed
# sanctums, and Warden statues -- inert, decorative, screenshot-bait", and the block's own
# javadoc: "Chapter 0 dressing that players read as ruin flavour for hours, and which after
# the death is the only instruction anyone left behind."
#
# The block, its texture and its model had existed since the chapter-0 art pass. There was
# NO TEXT ON IT ANYWHERE -- and the shrine-keeper has been telling players for just as long
# that "the steles are readable if you have the light for it; most people don't bother, and
# I have never held it against anybody." A shipped line of dialogue describing a rule that
# nothing implements is worse than a missing feature: it is the mod lying in its own voice.
#
# WHAT IS BEING PROVED:
#
#   * a stele in the light reads out a notice, with its header
#   * the SAME stele reads the SAME notice twice     (the choice is a function of position
#                                                     precisely so a ruin is not a slot
#                                                     machine and a quotation is not a lie)
#   * different steles can read DIFFERENTLY          (without this, one notice repeated
#                                                     forever passes every line above)
#   * a stele with no light on it says so            (the keeper's line, made true)
#
# NOT asserted: that the text changes after the death. It does not, and that is the joke
# WORLD.md names when it lists "steles that re-read differently" -- what re-reads
# differently is the reader. There is nothing here for a check to watch.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }

# Six steles in open daylight, spread out, plus one walled into stone with no light on any
# face. Six because the notices are chosen by a hash of position: two would prove nothing
# about spread, and the core self-test already proves every notice is reachable somewhere.
COMMANDS='forceload add -64 -64 63 63
wait 3
setblock 0 100 0 interregnum:warning_stele replace
setblock 10 100 0 interregnum:warning_stele replace
setblock 20 100 0 interregnum:warning_stele replace
setblock 30 100 0 interregnum:warning_stele replace
setblock 40 100 0 interregnum:warning_stele replace
setblock 50 100 0 interregnum:warning_stele replace
fill 30 60 30 34 64 34 minecraft:stone replace
setblock 32 62 32 interregnum:warning_stele replace
wait 3
say LIT
interregnum stele read 0 100 0
interregnum stele read 10 100 0
interregnum stele read 20 100 0
interregnum stele read 30 100 0
interregnum stele read 40 100 0
interregnum stele read 50 100 0
say AGAIN
interregnum stele read 0 100 0
say DARK
interregnum stele read 32 62 32
say DONE' \
    LOG=/tmp/stele.log timeout 900 ./tools/server_smoke.sh > /tmp/st.txt 2>&1 \
    || { tail -25 /tmp/st.txt; fail "the run did not complete"; }

# `|| true` on every extraction: a grep that matches nothing would otherwise take this
# script down before the message it exists to print (docs/LESSONS.md #23).
lit=$(sed -n '/say LIT/,/say AGAIN/p' /tmp/st.txt || true)
again=$(sed -n '/say AGAIN/,/say DARK/p' /tmp/st.txt || true)
dark=$(sed -n '/say DARK/,/say DONE/p' /tmp/st.txt || true)

# --- a stele in the light says something ------------------------------------
headers=$(echo "$lit" | grep -c 'WARNING STELE' || true)
[ "$headers" -eq 6 ] || {
    echo "$lit" | sed 's/^/    /' | head -12 || true
    fail "$headers of 6 steles standing in open daylight produced a notice. A stele that cannot be read in the middle of a field cannot be read anywhere, and the keeper has been promising players otherwise since the first dialogue pass"; }

# --- and it is not the header alone -----------------------------------------
# The header is the mod's own chrome. If the body is missing, every count above still
# passes and the stele is a blank sign with a title on it.
echo "$lit" | grep -qE 'Persons|Sleep is scheduled|Light is provided|The floor of the world|In the event' || {
    echo "$lit" | sed 's/^/    /' | head -8 || true
    fail "the steles produced a header and no notice. The five inscriptions are the whole feature; a header with nothing under it is the sign without the writing"; }

# --- the same stele reads the same twice ------------------------------------
# The body line only: markers, echoed commands and the header all stripped. The first
# version took the last line of the range, which is the `say` marker that BOUNDS the range
# -- `sed -n '/A/,/B/p'` keeps both ends -- so it compared a notice against the word
# "DARK". The same mistake return_check.sh made, in the same shape.
body() { echo "$1" | grep -vE '^\$|WARNING STELE|say ' | grep -v '^$' | tail -1 || true; }
first=$(body "$(echo "$lit" | sed -n '/stele read 0 100 0/,+2p')")
repeat=$(body "$again")
[ -n "$first" ] && [ "$first" = "$repeat" ] || {
    printf '    first:  %s\n    repeat: %s\n' "$first" "$repeat"
    fail "one stele gave two different notices. The inscription is a pure function of the stele's coordinates for exactly this reason -- a stele you read yesterday says the same thing today, or the ruin is a slot machine and every quotation of it is a lie"; }

# --- and different steles can differ ----------------------------------------
# The load-bearing negative. One notice repeated at every stele in the world satisfies
# every assertion above, and a hash that has collapsed to a constant is invisible without
# this line.
distinct=$(echo "$lit" | grep -vE 'WARNING STELE|^\$|stele read' | grep -v '^$' | sort -u | wc -l || true)
echo "  six steles, $distinct distinct notice(s) between them"
[ "$distinct" -gt 1 ] || \
    fail "all six steles read out the same notice. Either the position hash has collapsed to a constant, or there is only one inscription -- and reading a second stele then teaches a player nothing they did not already have"

# --- a stele nobody can see says so -----------------------------------------
# The keeper's line, made true. Also the guard on the bug this file caught: the light was
# first read AT the stele, which is inside an opaque block and therefore always dark --
# so every stele in the world reported itself unreadable, and one buried in stone
# reported itself fine.
echo "$dark" | grep -q 'not the light to make it out' || {
    echo "$dark" | sed 's/^/    /' | head -6 || true
    fail "a stele walled into stone with no light on any face read out perfectly. The keeper says the steles are readable IF YOU HAVE THE LIGHT FOR IT, and a rule the dialogue describes and the world does not apply is the mod contradicting itself in front of the player"; }

echo
printf "OK: a lit stele reads out its notice, the same stele reads the same twice, six steles\n    do not all say one thing, and an unlit one says it cannot be made out\n"
