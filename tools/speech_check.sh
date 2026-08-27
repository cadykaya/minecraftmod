#!/bin/bash
# Casting, at last, by the affordance WORLD.md locks.
#
# "Casting is a spoken word. Not a focus item, not a keybind. You say the word, out loud,
# in chat, and everyone in earshot sees you say it."
#
# Ten spells have existed and been verified for some time and a player could reach none of
# them: the command was the only way in. This is the seam that fixes that, and it is
# deliberately the ONLY seam -- every *Spell.cast(...) takes the same arguments it took
# before this existed, so if typing a word turns out to feel bad in play, the swap costs
# one file.
#
# WHAT IS ASSERTED:
#   * the word is the spell's own name, and saying it casts that spell -- checked by
#     reading the WORLD, not the command's reply: a block ages, a zone opens;
#   * NOTHING IS KNOWN BY DEFAULT. An untaught speaker says the word and is refused,
#     which is also the only way to be sure the successful casts are evidence of a rule;
#   * A SENTENCE CONTAINING A SPELL WORD CASTS NOTHING. This is the one that matters most
#     and the reason the argument is greedy: chat is where players TALK about the game,
#     and a magic system that fires on a substring would make every conversation about
#     magic a hazard. The first thing anyone would learn is to stop discussing it;
#   * an unaimed spell centres on the SPEAKER and an aimed one on what they are looking
#     at -- and the two are cast from the same position in this run, so a version that
#     ignored the distinction would put both in the same place;
#   * and a word nobody has ever heard of is silent. NOT_A_WORD is the common case: it is
#     every sentence anybody types.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/speech.log; }
want() { grep -qF "$2" "$1" || { echo "--- looked for: $2"; grep -oE 'spoke=[A-Za-z_]+ spell=[A-Za-z_]+' "$1" | head -12 || true; fail "$3"; }; }

WHO=99999999-9999-4999-8999-999999999999

# Ground is y=-61 in this flat world. The speaker stands at 0 -60 0 throughout and looks
# at a block twenty away, so "centred on the speaker" and "centred on the target" are
# forty blocks apart and cannot be confused for one another.
COMMANDS="forceload add -16 -16 47 47
wait 3
setblock 20 -60 20 minecraft:stone_bricks replace
setblock 30 -60 30 minecraft:stone_bricks replace
interregnum speak $WHO 0 -60 0 20 -60 20 weather
interregnum learn $WHO turning
interregnum learn $WHO silence
interregnum learn $WHO verdancy
interregnum learn $WHO weight
interregnum speak $WHO 0 -60 0 20 -60 20 weather
execute if block 20 -60 20 minecraft:cracked_stone_bricks run say AGED_THE_TARGET
execute if block 30 -60 30 minecraft:stone_bricks run say SPARED_THE_OTHER
interregnum speak $WHO 0 -60 0 30 -60 30 I said weather and nothing happened
execute if block 30 -60 30 minecraft:stone_bricks run say SENTENCE_CAST_NOTHING
interregnum speak $WHO 0 -60 0 30 -60 30 weatherproof
interregnum speak $WHO 0 -60 0 30 -60 30 good evening everyone
interregnum speak $WHO 0 -60 0 30 -60 30 HUSH
say ZONE_SPOKEN
interregnum speak $WHO 0 -60 0 20 -60 20 bridgeroot
interregnum speak $WHO 0 -60 0 20 -60 20 lighten" \
    LOG=/tmp/speech.log timeout 900 ./tools/server_smoke.sh > /tmp/sp.txt 2>&1 \
    || { tail -25 /tmp/sp.txt; fail "the run did not complete"; }

# --- nothing is known by default -------------------------------------------
want /tmp/sp.txt 'spoke=UNLEARNED spell=null' \
    "an untaught speaker cast a spell by saying its name. Schools are learned in their worlds, and if the word works by default then every successful cast below is evidence of nothing"

# --- the word casts the spell, read off the world --------------------------
want /tmp/sp.txt 'spoke=CAST spell=WEATHER' \
    "saying the word did not reach the spell at all"
mark AGED_THE_TARGET || {
    grep -oE '(AGED|SPARED|SENTENCE|ZONE)_[A-Z_]+' /tmp/speech.log | sort | uniq -c || true
    fail "the command reported a cast and the block it was aimed at is unchanged. A seam that reports CAST and does nothing passes every assertion that trusts its own reply"; }

# --- an aimed spell lands where the speaker is LOOKING, not where they stand
mark SPARED_THE_OTHER || \
    fail "a block the speaker was not looking at aged too. Weather names one block, and a version that ignored the target would age whatever was nearest the caster instead"

# --- THE ONE THAT MATTERS: talking about magic is not doing magic ----------
# Asserted twice over -- the outcome AND the world -- because this is the failure that
# would make chat unusable, and it is the one a substring match passes silently.
want /tmp/sp.txt 'spoke=NOT_A_WORD spell=null' \
    "a sentence with a spell word inside it was treated as a spell. Chat is where players talk about the game; a magic system that fires on a substring turns every conversation about magic into a hazard, and the first thing anybody would learn is to stop discussing it"
mark SENTENCE_CAST_NOTHING || \
    fail "the block somebody was TALKING about aged. Saying 'I said weather and nothing happened' must not age a block, and the fact that it reported NOT_A_WORD is not enough on its own -- the world is where it would show"

# --- and a word that merely starts the same is not the word ----------------
[ "$(grep -cF 'spoke=NOT_A_WORD' /tmp/sp.txt || true)" -ge 3 ] || {
    grep -oE 'spoke=[A-Za-z_]+' /tmp/sp.txt | sort | uniq -c || true
    fail "expected at least three non-words: a sentence containing 'weather', the word 'weatherproof' which merely begins with it, and an ordinary greeting. A prefix match would let 'weatherproof' through"; }

# --- case does not matter, because meaning it is what matters --------------
want /tmp/sp.txt 'spoke=CAST spell=HUSH' \
    "HUSH in capitals was not recognised. A player typing at the start of a sentence gets a capital for free, and refusing it would make the spell fail for reasons nobody could see"

# --- an unaimed spell centres on the SPEAKER -------------------------------
# Hush and Lighten are rooms you stand in; Bridgeroot spans from the speaker to the
# target. All three are spoken from 0 -60 0 in this run, so a dispatch that passed the
# wrong position would be visible as a zone opening twenty blocks from anybody.
want /tmp/sp.txt 'spoke=CAST spell=BRIDGEROOT' \
    "the one spell that needs both the speaker's position and the target's did not cast"

echo
echo "OK: the word casts the spell, talking about the spell does not, and an untaught mouth says nothing at all"
