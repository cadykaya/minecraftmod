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
# NOT asserted: the Hearth-Turner's ageing showing up in a leak. Its rate is one sample
# per section per tick at 0.35, which is deliberately slow (see `Hearth`), and over any
# window CI will tolerate the expected count is about one -- so the assertion would be a
# coin flip. The method it calls is the same one `turning_check.sh` proves in the
# Hearth-Turner's own world, and `Leaks` calls it directly rather than reimplementing it.
# Stated rather than quietly omitted.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }

# Per shrine: masonry to anchor the leak, and eight grass/dirt pairs for the Verdant's
# growth to act on. One target block would be unobservable -- see docs/LESSONS.md and
# the Verdant's own check, which learned this the hard way.
SETUP=""
LAWQ=""
AFTER=""
for c in 0 1 2 3 4 5 6 7; do
    x=$((c * 32))
    SETUP="$SETUP
setblock $x -60 0 interregnum:shrine_stone replace"
    for t in $(seq 0 15); do
        SETUP="$SETUP
setblock $((x + t)) 100 0 minecraft:grass_block replace
setblock $((x + t)) 100 1 minecraft:dirt replace"
    done
    LAWQ="$LAWQ
interregnum exodus law $((c * 2)) 0"
    AFTER="$AFTER
say SHRINE_$c"
    for t in $(seq 0 15); do
        AFTER="$AFTER
execute if block $((x + t)) 100 1 minecraft:grass_block run say GREENED_$c"
    done
done

# `forceload add` caps at 256 chunks and refuses the whole command over it -- the first
# version asked for 289, nothing loaded, and every setblock below answered "that position
# is not loaded". x chunks -1..15 and z chunks -1..5 is 119.
COMMANDS="forceload add -16 -16 255 95
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
wait 30
say AFTER_WAIT
$AFTER" \
    LOG=/tmp/exodus.log timeout 2000 ./tools/server_smoke.sh > /tmp/ex.txt 2>&1 \
    || { tail -25 /tmp/ex.txt; fail "the run did not complete"; }

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
# is only a patch if the boundary between them is unambiguous, so the assertion is that
# those two do not come near each other.
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
    elif [ -n "$law" ] && [ "$law" != "hearth_turner" ]; then
        # anchorite and quiet_one are not block-level laws, so nothing here should grow
        # beyond what the overworld grows on its own.
        other_shrines=$((other_shrines + 1))
        [ "$greened" -le "$other_max" ] || other_max=$greened
    fi
done

[ "$verdant_shrines" -gt 0 ] || \
    fail "not one of eight shrines drew the Verdant, which with four laws is a 1-in-1600 accident -- suspect the hash before the luck"
[ "$other_shrines" -gt 0 ] || \
    fail "no shrine drew a non-block-level law, so there is no control: 'growth happened at the Verdant's shrines' would also be true of growth happening everywhere"

echo "  worst Verdant shrine: $verdant_min/16    best non-Verdant shrine: $other_max/16"

# The shape assertion. Measured: 7/8 at both Verdant shrines, 0-2/8 at the other six.
# The margin of two is the same one the Verdant's own check uses, set the same way -- from
# the observed spread rather than from taste. A slower CI runner ticks less in the same
# thirty seconds and lowers BOTH numbers together, which is exactly why this compares them
# instead of testing either against a constant.
[ "$verdant_min" -gt $((other_max + 2)) ] || \
    fail "the worst Verdant shrine greened $verdant_min of 16 and the best non-Verdant one greened $other_max of 16 -- there is no boundary between the patches, so either the leak applies one god's law everywhere (and the patches are not shaped like anybody) or it applies none (and this file would be green with Leaks deleted)"

# And the absolute ceiling, for the same reason `verdant_check.sh` carries one: a mutation
# that leaks the Verdant's law at EVERY patch raises both numbers together, and while the
# comparison above catches that today, it catches it by a margin rather than by kind. Four
# of eight is triple the measured baseline and half the measured leak.
[ "$other_max" -le 4 ] || \
    fail "a shrine leaking a non-block-level law greened $other_max of 16, which is far past what the overworld grows on its own -- the Verdant's law is being applied where another god's was named"

echo
echo "OK: the overworld leaks only at band 3, only at shrines, always the same god in the same place, and the god it names is the one whose law runs"
