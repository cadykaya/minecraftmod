#!/bin/bash
# Band 3: the overworld starts leaking other gods' law.
#
# WORLD.md, locked: "Not their blocks. Their RULES. The dead god's policy was what held
# the systems apart -- the Isolation was a policy, not a wall -- and with nobody
# enforcing it, patches of the overworld begin obeying somebody else's law."
#
# The patches sit on the SHRINES, because the shrines are already the mod's map of where
# the dead god's attention was: the places its authority was strongest are where its
# absence shows first.
#
# THE ASSERTION THAT MATTERS is not that something happens near a shrine. It is that the
# thing which happens is THE SAME LAW the god's own world runs -- "the apocalypse is
# teaching you the curriculum" -- so `Leaks` calls `Verdant.grow`, the same method the
# Verdant's dimension calls, and this file proves the effect shows up in the OVERWORLD.
#
# Which god sits at which shrine is ASKED OF THE SERVER (`exodus law`), never predicted
# here. A check that recomputed the hash would be a restatement of the implementation
# rather than a test of it -- and it would agree with a broken hash.
#
# Eight shrines, so all four laws are represented and the check has both cases: chunks
# that must grow, and chunks that must not. That second set is what makes this a test of
# a PATCH rather than of a switch.
#
# THE TRAP THIS FILE FELL INTO, recorded because I had documented it one increment
# earlier and walked into it anyway: the first version asserted that a shrine with a
# non-Verdant law greened ZERO of its eight targets. It ran, and it failed -- the two
# Verdant shrines greened 7/8 while six non-Verdant shrines greened 0, 0, 0, 0, 1 and 2.
# Which is a pass. Grass spreads in the OVERWORLD; `verdant_check.sh`'s own control
# measures 0-4 of 32 in 25 seconds and its header says in as many words that "a check
# demanding zero at home would be flaky by construction". Eight targets over 30 seconds
# is that same baseline, so the assertion below is a COMPARISON -- every Verdant shrine
# must out-green every other one, by a margin -- and never a threshold at zero.
#
# TWO LAWS ARE MEASURED HERE, and getting the Verdant's control right took three goes.
#
#   The ANCHORITE's rise was categorical from the start, because nothing in vanilla makes
#   sand go up. Sand landing at every shrine that was not named an Anchorite is not a
#   threshold, it is the definition of the overworld working.
#
#   The VERDANT's growth was not, because grass spreads in the overworld on its own. The
#   first version demanded zero at the other shrines and failed on a clean run
#   (docs/LESSONS.md #27). The second compared the two groups with a margin -- honest, but
#   the margin was thin: one run came back with a Verdant shrine at 5 of 16 and a control
#   at 4, which would have been a red build with nothing wrong.
#
#   The third version stops tolerating the confound and REMOVES it. `Verdant.grow` does
#   its own explicit random ticks and never consults `randomTickSpeed`; vanilla's spread
#   does nothing else. So `gamerule random_tick_speed 0` below switches off vanilla's
#   growth and leaves the mod's untouched, and the control becomes categorical: in THIS
#   WORLD, with that gamerule at zero, the only thing that can turn dirt into grass is
#   the leak. Not a weaker claim than the comparison -- a stronger one, and a stable one.
#
#   NOTE THE RULE'S NAME. 26.x renamed the whole gamerule set to snake_case behind a
#   registry: it is `random_tick_speed`, and `randomTickSpeed` is rejected outright.
#   The first version of this used the old name, the server answered "Incorrect argument
#   for command", nothing was switched off, and the check went red with a failure message
#   that said "in a world with randomTickSpeed at zero" -- describing a world that did
#   not exist. `deicide_check.sh` has carried a comment about this exact rename since it
#   was written. Which is why the rule is now read BACK from the server below rather
#   than assumed to have taken (docs/LESSONS.md #15).
#
# Which is docs/LESSONS.md #2: a confounded measurement is a false instrument, and the
# first move is to look for a way to remove the confound rather than to model it.
#
# NOT asserted: the Hearth-Turner's ageing showing up in a leak. Its rate is one sample
# per section per tick at 0.35, which is deliberately slow (see `Hearth`), and over any
# window CI will tolerate the expected count is about one -- so the assertion would be a
# coin flip. The method it calls is the same one `turning_check.sh` proves in the
# Hearth-Turner's own world, and `Leaks` calls it directly rather than reimplementing it.
# Stated rather than quietly omitted.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }

# The worst any Verdant shrine may do over the sixty-second window with vanilla's ticking
# off. Set from measurement, not taste -- see docs/LOG.md for the run it came from.
VERDANT_FLOOR=8

# Per shrine: masonry to anchor the leak, sixteen grass/dirt pairs for the Verdant's
# growth to act on, and a stone floor for the Anchorite's sand to fail to land on. One
# target block would be unobservable -- see docs/LESSONS.md #27 and the Verdant's own
# check, which learned this the hard way and then had to learn it again here.
SETUP=""
LAWQ=""
AFTER=""
SAND=""
SANDPROBE=""
for c in 0 1 2 3 4 5 6 7; do
    x=$((c * 32))
    SETUP="$SETUP
setblock $x -60 0 interregnum:shrine_stone replace
setblock $((x + 4)) 100 8 minecraft:stone replace"
    for t in $(seq 0 15); do
        SETUP="$SETUP
setblock $((x + t)) 100 0 minecraft:grass_block replace
setblock $((x + t)) 100 1 minecraft:dirt replace"
    done
    LAWQ="$LAWQ
interregnum exodus law $((c * 2)) 0"
    AFTER="$AFTER
say SHRINE_$c"
    # Sand goes in LAST, and that ordering is the whole reason it is a separate string.
    # A rising block never lands, so nothing ever stops it: at terminal rise it clears
    # the build height in about ten seconds and vanilla discards it. Placed before the
    # thirty-second growth wait, every Anchorite shrine would report no falling block at
    # all -- which is also what a deleted block looks like, and the check could not tell
    # the two apart.
    #
    # VANILLA'S TICKING GOES BACK ON for this phase, and the restore is deliberate
    # rather than tidy. The gamerule exists to isolate the VERDANT measurement, which is
    # the only one with a vanilla confound; weight has none, and a falling block is the
    # single most ordinary thing in Minecraft. Measuring it in a world with the tick rate
    # set to zero would be testing the mod under conditions no player will ever have --
    # and when sand did fail to fall under that gamerule at full scale, the honest answer
    # was to stop asking the question in a strange world rather than to explain the
    # strangeness away. The rule is read back again so a rejected restore is not silent.
    #
    # FOUR seconds, and the window is bounded at BOTH ends, which is why it is not just
    # "a bit". Falling sand needs ~34 ticks to cover the 19 blocks down to its floor, so
    # a two-second probe was passing with 6 ticks to spare and would go red on a runner
    # that ticks slowly. Rising sand at terminal speed is near y=216 after four seconds
    # and does not clear the build height until about eight, so the entity is still there
    # to be found.
    SAND="$SAND
setblock $((x + 4)) 120 8 minecraft:sand replace"
    SANDPROBE="$SANDPROBE
execute if block $((x + 4)) 120 8 minecraft:sand run say STUCK_$c
execute if block $((x + 4)) 101 8 minecraft:sand run say LANDED_$c
execute if entity @e[type=minecraft:falling_block,x=$x,y=125,z=4,dx=16,dy=200,dz=8] run say ROSE_$c"
    for t in $(seq 0 15); do
        AFTER="$AFTER
execute if block $((x + t)) 100 1 minecraft:grass_block run say GREENED_$c"
    done
done

# `forceload add` caps at 256 chunks and refuses the whole command over it -- the first
# version asked for 289, nothing loaded, and every setblock below answered "that position
# is not loaded". x chunks -1..15 and z chunks -1..5 is 119.
# `random_tick_speed 0` is load-bearing, not tidiness -- see the header. Vanilla's grass
# spread is driven by this gamerule and the mod's growth is not, so setting it to zero
# separates the two cleanly. It goes in BEFORE the setup so no target has a head start,
# and it is queried straight back because a rejected gamerule is silent apart from one
# line in a log nobody reads.
COMMANDS="gamerule random_tick_speed 0
gamerule random_tick_speed
forceload add -16 -16 255 95
wait 3
$SETUP
say BEFORE_BAND3
interregnum exodus at 0 -60 0
execute positioned 0 -60 0 run interregnum record deicide
interregnum record warden_contact
interregnum record first_crossing
say AT_BAND3
interregnum exodus at 0 -60 0
interregnum exodus at 0 -60 0
interregnum exodus at 0 -60 80
$LAWQ
wait 60
say AFTER_WAIT
$AFTER
gamerule random_tick_speed 3
gamerule random_tick_speed
time query gametime
setblock 0 100 80 minecraft:stone replace
setblock 0 120 80 minecraft:sand replace
$SAND
wait 4
time query gametime
execute if block 0 101 80 minecraft:sand run say CONTROL_LANDED
$SANDPROBE" \
    LOG=/tmp/exodus.log timeout 2000 ./tools/server_smoke.sh > /tmp/ex.txt 2>&1 \
    || { tail -25 /tmp/ex.txt; fail "the run did not complete"; }

# --- the setup, before anything that rests on it ----------------------------
# The whole Verdant half is categorical ONLY because vanilla's ticking is off. If the
# gamerule did not take, every number below is measuring vanilla and the mod together
# and the assertions are lies with confident wording. So it is read back from the server
# rather than assumed -- the mistake this replaced was a failure message that described
# a world state which had been rejected two minutes earlier.
grep -q 'random_tick_speed is currently set to: 0' /tmp/ex.txt || {
    grep -iE 'random_tick_speed|gamerule' /tmp/ex.txt | head -4 || true
    fail "vanilla's random ticking is not off in this world -- the gamerule was rejected or ignored, so every growth number below is vanilla and the mod mixed together and no assertion about them means what it says"; }

# --- the gate: nothing leaks before band 3 ----------------------------------
before=$(sed -n '/BEFORE_BAND3/,/AT_BAND3/p' /tmp/ex.txt | grep -oE 'exodus=[a-z_]+ band=[0-9]+' | head -1 || true)
[ -n "$before" ] || fail "the exodus probe produced nothing at all before band 3"
echo "$before" | grep -q 'exodus=none' || \
    fail "the overworld was leaking at band 0 ($before) -- band 3 is when the ways open, and a world leaking before its god has died has no escalation left"

# --- and it does at band 3, beside a shrine ---------------------------------
at3=$(sed -n '/AT_BAND3/,$p' /tmp/ex.txt | grep -oE 'exodus=[a-z_]+ band=[0-9]+' | head -1 || true)
echo "$at3" | grep -q 'band=3' || {
    grep -oE 'exodus=[a-z_]+ band=[0-9]+' /tmp/ex.txt | head -4 || true
    fail "the world did not reach band 3, so nothing below is about the exodus"; }
case "$at3" in
    *exodus=none*) fail "nothing leaks beside a shrine at band 3 -- the patches are supposed to sit on the shrines" ;;
esac

# --- the same place leaks the same god ---------------------------------------
# A patch that changed god between visits would be weather, not a place to learn a law
# in. Two probes at one position, back to back, must agree.
twice=$(sed -n '/AT_BAND3/,$p' /tmp/ex.txt | grep -oE 'exodus=[a-z_]+ band=3' | head -2 | sort -u | wc -l)
[ "$twice" = "1" ] || {
    sed -n '/AT_BAND3/,$p' /tmp/ex.txt | grep -oE 'exodus=[a-z_]+ band=3' | head -2 || true
    fail "one position reported two different gods -- the leak is rolled rather than derived, so a player cannot learn anything by standing in it"; }

# --- and nowhere near a shrine leaks at all ---------------------------------
# The probe position has to be LOADED (BlockPosArgument refuses an unloaded one) and
# still outside every patch. Shrines sit in z chunk 0 and reach one chunk, so z=80 --
# chunk 5 -- is the nearest place that is both.
far=$(sed -n '/AT_BAND3/,$p' /tmp/ex.txt | grep -oE 'exodus=[a-z_]+ band=3' | sed -n '3p' || true)
[ "$far" = "exodus=none band=3" ] || {
    sed -n '/AT_BAND3/,$p' /tmp/ex.txt | grep -oE 'exodus=[a-z_]+ band=3' | head -4 || true
    fail "a loaded position five chunks from any shrine reported '$far' -- the whole overworld is leaking, so there is no patch to walk out of and no way to learn it has edges"; }

# --- the law the server names is the law that RUNS --------------------------
# Two numbers, not eight: the WORST Verdant shrine and the BEST non-Verdant one. A patch
# is only a patch if the boundary between them is unambiguous, and with vanilla's ticking
# off the boundary is total -- one number must be a clear majority and the other exactly
# zero.
verdant_shrines=0
verdant_min=99
other_shrines=0
other_max=-1
for c in 0 1 2 3 4 5 6 7; do
    law=$(grep -oE "exodus-law $((c * 2)) 0 = [a-z_]+" /tmp/ex.txt | head -1 | awk '{print $NF}' || true)
    greened=$(grep -c "GREENED_$c\$" /tmp/exodus.log || true)
    printf '  shrine %d (chunk %d) leaks %-14s greened %d/16\n' "$c" "$((c * 2))" "${law:-?}" "$greened"
    if [ "$law" = "verdant" ]; then
        verdant_shrines=$((verdant_shrines + 1))
        [ "$greened" -ge "$verdant_min" ] || verdant_min=$greened
    elif [ -n "$law" ]; then
        # EVERY other law, the Hearth-Turner's included. It was excluded at first on the
        # reasoning that it is a block-level law too -- but read the table: `turning.json`
        # converts stone, deepslate and brick, and nothing in it has grass or dirt as a
        # `from`. It cannot move this measurement, so excluding it was throwing away four
        # of the six available controls for no reason at all.
        other_shrines=$((other_shrines + 1))
        [ "$greened" -le "$other_max" ] || other_max=$greened
    fi
done

[ "$verdant_shrines" -gt 0 ] || \
    fail "not one of eight shrines drew the Verdant, which with four laws is a 1-in-1600 accident -- suspect the hash before the luck"
[ "$other_shrines" -gt 0 ] || \
    fail "no shrine drew another law, so there is no control: 'growth happened at the Verdant's shrines' would also be true of growth happening everywhere"

echo "  worst Verdant shrine: $verdant_min/16    best non-Verdant shrine: $other_max/16"

# CATEGORICAL, because the gamerule removed the only other thing that grows grass. A
# single conversion at a shrine the server named for somebody else is the Verdant's law
# being applied where another god's was named -- there is no vanilla baseline left for it
# to hide in.
[ "$other_max" -eq 0 ] || \
    fail "$other_max of 16 dirt block(s) turned to grass at a shrine leaking a law that is not the Verdant's, in a world whose random_tick_speed was read back as zero -- nothing but the leak can do that, so the leak is applying one god's law at every patch and the patches are not shaped like anybody"

# And the floor, so "nothing grew anywhere" cannot pass the line above. Measured rather
# than chosen: see the run recorded in docs/LOG.md.
[ "$verdant_min" -ge "$VERDANT_FLOOR" ] || \
    fail "the worst Verdant shrine greened only $verdant_min of 16 with vanilla's own ticking switched off -- the leak names the Verdant and does not run its law, so this file would be green with Leaks deleted"

# --- and the second law ------------------------------------------------------
# Its own setup assertion first: the sand phase runs with vanilla ticking restored, and
# a rejected restore would leave weight being measured in a world no player will have.
# DID THE WORLD TICK AT ALL? Asked before anything is concluded from the sand, because
# the answer was once no. A dedicated server with nobody on it pauses after sixty seconds
# by default; this check is the only one that waits longer than that, and when it did, the
# world froze and the sand table read exactly like a broken law. `server_smoke.sh` now
# switches the pause off, and this assertion is what makes sure it stayed off.
before=$(grep -oE 'time is [0-9]+' /tmp/ex.txt | tail -2 | head -1 | awk '{print $NF}')
after=$(grep -oE 'time is [0-9]+' /tmp/ex.txt | tail -1 | awk '{print $NF}')
[ -n "$before" ] && [ -n "$after" ] || fail "no gametime readings in the output -- the sand window cannot be shown to have happened in a running world"
[ "$((after - before))" -ge 40 ] || \
    fail "the world advanced $((after - before)) tick(s) during the four-second sand window, and four seconds is eighty ticks -- the server is paused or crawling, so nothing about falling blocks below is a statement about the mod. Check pause-when-empty-seconds in tools/server_smoke.sh"

# THE CONTROL THIS FILE WENT WITHOUT FOR TOO LONG. `anchorite_check.sh` has always
# carried it and says why: "did not land" is also satisfied by sand that never became a
# falling block, and without a place where sand demonstrably DOES fall, every reading of
# the sand table is guesswork. One column at 0,120,80 -- loaded, and five chunks from any
# shrine, so no leak reaches it.
grep -q 'CONTROL_LANDED' /tmp/exodus.log || \
    fail "sand dropped five chunks from any shrine did not land on the floor beneath it -- gravity is not working anywhere in this world, so the sand table below is not about the leak at all and the fault is in the harness rather than the mod"

grep -q 'random_tick_speed is currently set to: 3' /tmp/ex.txt || {
    grep -iE 'random_tick_speed' /tmp/ex.txt | head -4 || true
    fail "vanilla's random ticking was not restored before the sand phase -- weight is being measured in a world with the tick rate at zero, which is not a world anybody plays in"; }


# Categorical without needing a gamerule to make it so: nothing in vanilla makes sand
# rise, at any tick speed. Sand lands at every shrine that was not named an Anchorite,
# and rises at every shrine that was.
anchorite_shrines=0
other_shrines=0
stuck=""
wrong=""
for c in 0 1 2 3 4 5 6 7; do
    law=$(grep -oE "exodus-law $((c * 2)) 0 = [a-z_]+" /tmp/ex.txt | head -1 | awk '{print $NF}' || true)
    grep -q "STUCK_$c\$" /tmp/exodus.log && stuck="$stuck $c"
    landed=no; grep -q "LANDED_$c\$" /tmp/exodus.log && landed=yes
    rose=no;   grep -q "ROSE_$c\$"   /tmp/exodus.log && rose=yes
    printf '  shrine %d (chunk %d) leaks %-14s sand landed=%-3s rose=%s\n' \
        "$c" "$((c * 2))" "${law:-?}" "$landed" "$rose"
    if [ "$law" = "anchorite" ]; then
        anchorite_shrines=$((anchorite_shrines + 1))
        [ "$rose" = yes ] && [ "$landed" = no ] || wrong="$wrong $c(anchorite)"
    elif [ -n "$law" ]; then
        other_shrines=$((other_shrines + 1))
        [ "$landed" = yes ] && [ "$rose" = no ] || wrong="$wrong $c($law)"
    fi
done

# Setup before law, so a failure says what actually went wrong. Sand still sitting where
# it was put never became a falling block at all -- which is gravity switched OFF rather
# than reversed, and satisfies "did not land" for entirely the wrong reason.
[ -z "$stuck" ] || \
    fail "sand never left its placed position at shrine(s):$stuck -- it did not become a falling block, so nothing below is a statement about which way things fall"

[ "$anchorite_shrines" -gt 0 ] || \
    fail "not one of eight shrines drew the Anchorite, which with four laws is a 1-in-1600 accident -- suspect the hash before the luck"
[ "$other_shrines" -gt 0 ] || \
    fail "every shrine drew the Anchorite, so there is no control: 'sand rose at the Anchorite patches' would also be true of sand rising everywhere"

[ -z "$wrong" ] || \
    fail "shrine(s)$wrong disagreed with the law the server named for them. An Anchorite patch must send sand up and never let it land; every other patch must let it land and never send it up -- and vanilla has no mechanism that makes sand rise, so this is not a threshold, it is the overworld either working or not"

echo
echo "OK: the overworld leaks only at band 3, only at shrines, always the same god in the same place, and both laws it names -- growth and weight -- are the ones that run"
