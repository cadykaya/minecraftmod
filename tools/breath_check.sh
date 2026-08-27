#!/bin/bash
# Held-breath: your own sound, taken away -- and your voice with it.
#
# WORLD.md, locked: "Your own sound, taken away. Nothing tracks you while you hold it --
# AND YOU CANNOT CAST, BECAUSE CASTING IS A SPOKEN WORD. Power for silence, exactly."
#
# THIS SPELL COULD NOT HAVE BEEN WRITTEN BEFORE THE SPOKEN WORD WAS. WORLD.md lists three
# things that fall out of casting being "a word you are on record as having said", and the
# second is this one: "you cannot cast silently -- which is what makes Held-breath
# interesting rather than a stealth trinket: while you hold it you have no voice, so you
# have no spells." A stealth ability with no cost is a trinket. This one takes the school
# away for as long as it lasts, INCLUDING THE WORD THAT WOULD END IT. You do not put it
# down; you wait.
#
# WHAT IS ASSERTED:
#   * it cannot be cast untaught, like every other spell;
#   * once held, SAYING ANY WORD DOES NOTHING -- and the refusal is NO_VOICE rather than a
#     spell that failed, because the claim is not that the spells stopped working, it is
#     that nothing was said;
#   * INCLUDING ITS OWN WORD. You cannot put it down early, because putting it down would
#     take a word and the word is what was taken;
#   * A SPEAKER WHO IS NOT HOLDING THEIR BREATH IS UNAFFECTED, in the same world at the
#     same moment. Without this, "no spells work" is equally satisfied by a build where
#     casting broke;
#   * and IT WEARS OFF. Fifteen seconds later the same word from the same person casts.
#
# The vibration half -- that a breath-holder posts no game event, and so the Quiet One's
# door does not hear them walk in -- is asserted in tools/silence_check.sh's own terms
# there, and NOT here: this file is about the voice.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }

Q=cccccccc-3333-4333-8333-333333333333
R=dddddddd-4444-4444-8444-444444444444

# Both are taught Silence, so the ONLY difference between them is which one is holding a
# breath. `speak` takes the speaker, where they are, what they are looking at, and the word
# -- the same seam chat goes through, and the only one a headless server can reach.
COMMANDS="forceload add -16 -16 31 31
wait 3
setblock 4 -60 4 minecraft:stone replace

say UNTAUGHT
interregnum speak $Q 0 -60 0 0 -60 0 held-breath
interregnum learn $Q silence
interregnum learn $R silence

say HOLDING
interregnum cast held_breath $Q 0 -60 0
say MUTE
interregnum speak $Q 0 -60 0 4 -60 4 hush
say ITS_OWN_WORD
interregnum speak $Q 0 -60 0 0 -60 0 held-breath
say THE_OTHER_ONE
interregnum speak $R 8 -60 8 8 -60 8 hush
wait 17
say BREATHING_AGAIN
interregnum speak $Q 0 -60 0 4 -60 4 hush" \
    LOG=/tmp/breath.log timeout 900 ./tools/server_smoke.sh > /tmp/br.txt 2>&1 \
    || { tail -25 /tmp/br.txt; fail "the run did not complete"; }

heard=$(grep -oE 'spoke=[A-Z_]+ spell=[A-Za-z_]+' /tmp/br.txt || true)
[ -n "$heard" ] || {
    grep -oE 'spoke=[A-Za-z_]+' /tmp/br.txt | head -5 || true
    tail -20 /tmp/br.txt
    fail "the speak command produced no answer at all"; }

untaught=$(echo "$heard" | head -1)
mute=$(echo "$heard" | sed -n 2p)
ownword=$(echo "$heard" | sed -n 3p)
other=$(echo "$heard" | sed -n 4p)
again=$(echo "$heard" | sed -n 5p)

casts=$(grep -oE 'cast=held_breath held=[a-z]+ frayed=[0-9]+ refused=[a-z]*' /tmp/br.txt || true)
[ -n "$casts" ] || fail "the held_breath cast produced no answer at all"

# --- untaught, like every other spell ----------------------------------------
echo "$untaught" | grep -q 'spoke=UNLEARNED' || {
    echo "  saying the word before being taught: $untaught"
    fail "somebody who had never been taught Silence held their breath. Schools are learned in their gods' worlds, and a spell that skips that is a spell the journey was not needed for"; }

echo "$casts" | grep -q 'held=true' || {
    echo "  the cast: $casts"
    fail "a taught caster could not hold their breath"; }

# --- and then there is no voice ----------------------------------------------
# NO_VOICE and not a failed spell. The claim is not that the spells stopped working.
echo "$mute" | grep -q 'spoke=NO_VOICE' || {
    echo "  saying a word while holding a breath: $mute"
    fail "a word spoken by somebody holding their breath was heard. WORLD.md: 'while you hold it you have no voice, so you have no spells' -- and this is the entire cost of the spell, so without it Held-breath is the stealth trinket the locked text exists to rule out"; }

# --- INCLUDING ITS OWN WORD --------------------------------------------------
# You cannot put it down. Ending it would take a word, and the word is what was taken.
echo "$ownword" | grep -q 'spoke=NO_VOICE' || {
    echo "  trying to say held-breath again while holding one: $ownword"
    fail "the spell could be spoken while it was in force. That is the escape hatch that makes the cost nothing: a player who can end it on demand has paid for silence with nothing at all, and 'power for silence, exactly' stops being a trade"; }

# --- THE CONTROL: somebody else, same world, same moment ---------------------
# Without this, every assertion above is equally satisfied by a build where casting is
# simply broken.
echo "$other" | grep -q 'spoke=CAST' || {
    echo "  a different taught speaker, not holding a breath: $other"
    fail "a speaker who is NOT holding their breath could not cast either. Casting is broken, and nothing above this line is about Held-breath"; }

# --- and it wears off --------------------------------------------------------
# Fifteen seconds, and the wait is seventeen. A breath you cannot end AND that never ends
# would take the school away permanently for one cast.
echo "$again" | grep -q 'spoke=CAST' || {
    echo "  the same speaker, seventeen seconds later: $again"
    fail "the breath never ran out. It cannot be ended by speaking -- that is the design -- so if it also does not expire then one cast costs a player the whole school for the rest of the world"; }

echo
printf "OK: a held breath is learned like any spell, takes the voice that would end it,\n    leaves everybody else speaking, and gives the voice back on its own\n"
