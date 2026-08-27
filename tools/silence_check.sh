#!/bin/bash
# The Quiet One's door: it opens when nothing near it makes a sound.
#
# WORLD.md, locked: "Opens when NOTHING NEAR IT MAKES A SOUND. The only one of the four a
# player can close by accident. Opened with Hush -- and Held-breath, for the last few
# steps."
#
# HOW A HEADLESS SERVER KNOWS WHAT A SOUND IS. It does not, and `Hush`'s own javadoc has
# said so since the spell was written: the audible half of this god's law lives on a client
# and is deliberately not claimed anywhere in this mod. What the game DOES have, entirely
# server-side, is the vibration model -- the game event a sculk sensor listens for, posted
# with a position every time something happens that a listening thing could notice. So this
# door listens the way sculk listens, and nothing about it depends on anybody hearing
# anything.
#
# WHAT IS ASSERTED:
#   * a place with no silence cast on it is not a door. The Quiet One's world is quiet
#     everywhere; quiet is not the condition, a CAST SILENCE that has held is;
#   * a fresh silence is SHUT, and becomes OPEN by nothing happening. This is the only
#     portal in the mod whose opening condition is the absence of events;
#   * A NOISE CLOSES IT, and closes it by RESETTING rather than delaying -- the held count
#     goes back to nearly nothing, which is the difference between "the only one you can
#     close by accident" and "the one that opens a moment later than you expected";
#   * it opens again once the noise stops, so an accident is a setback and not a ruined
#     cast;
#   * something inside an open silence CROSSES, into the Quiet One's under-layer;
#   * THE CONTROL -- an identical thing, in the identical world, sitting the identical
#     length of time where no silence was ever cast -- does not.
#
# Every entity probe carries a position and a `distance`, and every probe sits inside the
# forceloaded region, which is measured in BLOCKS. See docs/LESSONS.md #43 and #45.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/silence.log; }

WHO=66666666-6666-4666-8666-666666666666
D=interregnum:unresponsive

# THE NOISE IS A BLOCK BEING BROKEN, and the DESTROY is load-bearing.
#
# `setblock <pos> <block> replace` posts NO game event at all -- it writes the block and
# nothing else, so the first version of this check made a silent noise and reported that
# the door survived being built next to. `setblock <pos> minecraft:air destroy` calls
# `Level.destroyBlock`, which posts `block_destroy` at the position: a real vibration, the
# same one a sculk sensor would feel. The stone is put down BEFORE the cast, quietly, so
# that there is something to break later.
#
# Nothing here reaches into the mod to say "pretend something was loud". It does a thing to
# the world, and the world reports it.
#
# Hush's zone lasts 400 ticks and the silence needed is 100, so one cast affords four
# attempts. This spends three of them: hold, break, hold again.
COMMANDS="execute in $D run forceload add -16 -16 47 47
execute in interregnum:unresponsive_lower run forceload add -16 -16 47 47
forceload add -16 -16 15 15
wait 4

interregnum learn $WHO silence
execute in $D run interregnum silence 4 100 4
execute in $D run setblock 6 100 6 minecraft:stone replace
execute in $D run interregnum cast hush $WHO 4 100 4
execute in $D run interregnum silence 4 100 4
say HOLDING
wait 6
execute in $D run interregnum silence 4 100 4

say BREAKING_IT
execute in $D run setblock 6 100 6 minecraft:air destroy
execute in $D run interregnum silence 4 100 4
say HOLDING_AGAIN
wait 6
execute in $D run interregnum silence 4 100 4

say CROSSING
execute in $D run interregnum cast hush $WHO 4 100 4
execute in $D run summon minecraft:armor_stand 4.5 100 4.5 {Tags:[\"listener\"],Marker:1b,Invisible:1b,NoGravity:1b}
execute in $D run summon minecraft:armor_stand 30.5 100 30.5 {Tags:[\"alone\"],Marker:1b,Invisible:1b,NoGravity:1b}
wait 8

execute in interregnum:unresponsive_lower positioned 4 128 4 if entity @e[tag=listener,distance=..400] run say LISTENER_ARRIVED
execute in $D positioned 4 128 4 if entity @e[tag=listener,distance=..400] run say LISTENER_STAYED
execute in $D positioned 30 128 30 if entity @e[tag=alone,distance=..400] run say ALONE_STAYED
execute in interregnum:unresponsive_lower positioned 30 128 30 if entity @e[tag=alone,distance=..400] run say ALONE_WENT" \
    LOG=/tmp/silence.log timeout 900 ./tools/server_smoke.sh > /tmp/sl.txt 2>&1 \
    || { tail -25 /tmp/sl.txt; fail "the run did not complete"; }

reads=$(grep -oE 'silence=[A-Z]+ held=-?[0-9]+ leads=[a-z_]+ broken=[0-9]+' /tmp/sl.txt || true)
[ -n "$reads" ] || { tail -20 /tmp/sl.txt; fail "the silence command produced no answer at all"; }
nothing=$(echo "$reads" | head -1)
fresh=$(echo "$reads" | sed -n 2p)
held=$(echo "$reads" | sed -n 3p)
broken=$(echo "$reads" | sed -n 4p)
regained=$(echo "$reads" | sed -n 5p)

heldticks() { echo "$1" | grep -oE 'held=-?[0-9]+' | grep -oE '\-?[0-9]+'; }

# --- an uncast place is not a door -------------------------------------------
# The Quiet One's world has no mob spawns and no ambience: it is quiet EVERYWHERE. If
# quiet alone were the condition, the whole dimension would be a portal, and this is the
# probe that says it is not.
echo "$nothing" | grep -q 'silence=SHUT' || {
    echo "  before any cast: $nothing"
    fail "a place with no silence cast on it is already a door. This world is quiet everywhere -- quiet is not the condition, a CAST silence that has HELD is, and if the bare world qualifies then the whole dimension is a portal"; }
[ "$(heldticks "$nothing")" = "-1" ] || \
    fail "a position with no silence over it reported a held time ('$nothing') -- there is nothing there to have held"
echo "$nothing" | grep -q 'leads=unresponsive_lower' || \
    fail "the Quiet One's surface does not know where its doors lead ('$nothing')"

# --- a fresh silence is shut, and opens by nothing happening -----------------
echo "$fresh" | grep -q 'silence=SHUT' || {
    echo "  the instant it was cast: $fresh"
    fail "a silence is a door the moment it is cast. The waiting IS the spell here -- an instantly-open Hush is a teleport with a cooldown"; }
echo "$held" | grep -q 'silence=OPEN' || {
    echo "  after six seconds of nothing: $held"
    fail "a silence nobody disturbed for six seconds did not open. This is the only door in the mod whose condition is the ABSENCE of events, so if it never opens on its own it cannot open at all"; }

# --- A NOISE CLOSES IT, BY RESETTING ----------------------------------------
# The assertion that carries the locked line. `setblock` posts a block_place game event --
# a real vibration, not a hook the check reached into the mod to pull.
echo "$broken" | grep -q 'silence=SHUT' || {
    echo "  immediately after a block was placed inside it: $broken"
    fail "a block placed inside the silence did not close the door. WORLD.md calls this the only one of the four a player can close by accident, and a door that survives somebody building next to it cannot be closed by accident at all"; }
before=$(heldticks "$held")
after=$(heldticks "$broken")
[ "$after" -lt "$before" ] && [ "$after" -lt 20 ] || {
    echo "  held before the noise: $before ticks; after: $after ticks"
    fail "the noise did not RESET the silence, it only delayed the door (held went from $before to $after). Those are different mechanics: a reset means an accident costs you the whole wait, and a delay means it costs you a moment and the accident is free"; }

# --- and it comes back ------------------------------------------------------
# So an accident is a setback rather than a wasted cast, which is why the silence needed is
# a quarter of the spell that holds it.
echo "$regained" | grep -q 'silence=OPEN' || {
    echo "  six seconds after the noise stopped: $regained"
    fail "the silence never recovered after being broken. One cast is meant to afford four attempts; if a single accident ends the cast then the spell is spent by any passer-by"; }

# --- something inside an open silence crosses --------------------------------
mark LISTENER_ARRIVED || {
    grep -oE '(LISTENER|ALONE)_[A-Z_]+' /tmp/silence.log | sort -u || true
    grep -oE 'silence=[A-Z]+ held=-?[0-9]+' /tmp/sl.txt | tail -3 || true
    fail "a thing sitting inside a silence that had held did not cross into the under-layer"; }
if mark LISTENER_STAYED; then
    fail "it is in the under-layer AND still on the surface -- something copied it rather than moving it"
fi

# --- THE CONTROL: sitting still is not enough -------------------------------
# Without this, every assertion above is satisfied by a world that takes anything that
# holds still -- which is the VERDANT'S door, and would mean this god had been given
# somebody else's law.
mark ALONE_STAYED || \
    fail "the control -- same world, same tick, sitting equally still where no silence was ever cast -- is not where it was put"
if mark ALONE_WENT; then
    fail "a thing sitting where NO silence was cast crossed anyway. Then the door is not the spell's zone, it is the whole world, and this god has been given the Verdant's law: hold still and you are taken"
fi

echo
printf "OK: a cast silence is shut until it has held, a block placed inside resets it to\n    nothing, it recovers, what sits in an open one crosses, and sitting where nobody\n    cast anything takes you nowhere\n"
