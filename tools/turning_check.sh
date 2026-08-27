#!/bin/bash
# In the Hearth-Turner's world, things keep every past they have had.
#
# WORLD.md locks the reuse: "the block-aging registry powering the Turning IS the same
# system that runs the unraveling. One mechanism; a school and an apocalypse." So the
# ageing table is the unraveling's own ConversionDef shape, and the difference between
# the two systems is what they contain, not how they work: the unraveling LOOSENS
# (intact -> loosened -> dry, a world nobody is holding), the Turning WEATHERS (a thing
# acquiring its history, in order, and never losing it).
#
# WHY THIS CHECK IS CATEGORICAL WHERE THE VERDANT'S IS STATISTICAL, and it is worth
# saying because the difference is the point of having two gods rather than one:
#
#   The Verdant asks vanilla for MORE OF WHAT IT ALREADY DOES. Grass spreads in the
#   overworld too, so that check can only compare rates, and its overworld control
#   converts a few every run.
#
#   Nothing in vanilla ever turns stone into cobble, or cobble into mossy cobble. The
#   overworld will not do it in a hundred hours. So here the control is absolute: any
#   ageing at home at all is the law having escaped its dimension, and that is asserted
#   directly rather than as a ratio.
#
# Also asserted: the CHAIN. Stone does not arrive mossy -- it is stone, then cobble,
# then mossy cobble, because each rule's `to` is another rule's `from`. Finding mossy
# cobblestone where only plain stone was placed proves two links ran in order, which no
# single-step implementation can fake.
#
# And: a block somebody placed is never aged. Unlike the Verdant's, this promise is
# categorical -- ageing is applied to the block being aged, not to a source that reaches
# a neighbour, so there is no indirect path to a claimed block.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/turning.log; }

# Two halves, and they prove different things.
#
# DETERMINISTIC, through `interregnum turning age <pos>`: the chain, the claim refusal,
# and the dimension gate. Ageing is slow on purpose and its most important property is
# that stone reaches moss THROUGH cobble; waiting for two independent rolls to land on
# one block would turn a categorical fact into a statistical one for no benefit. The
# command bypasses only the chance roll -- every other gate still applies -- so this is
# testing the law rather than a path written for the test.
#
# PASSIVE, by waiting: that it happens at all without being asked. A row of sixteen,
# because one block sampled 3 times per 4096 positions per tick is not observable inside
# any window CI will tolerate (the first draft of this file waited 40 seconds for one
# and got nothing, while the law worked).
ROWS=""
for i in $(seq 0 15); do
    ROWS="$ROWS
setblock $i 100 8 minecraft:stone replace
execute in interregnum:temporal_authority run setblock $i 100 8 minecraft:stone replace"
done
AFTER=""
for i in $(seq 0 15); do
    AFTER="$AFTER
execute in interregnum:temporal_authority unless block $i 100 8 minecraft:stone run say THERE_AGED
execute in interregnum:temporal_authority if block $i 100 8 minecraft:mossy_cobblestone run say THERE_CHAINED
execute unless block $i 100 8 minecraft:stone run say HOME_AGED"
done

COMMANDS="execute in interregnum:temporal_authority run forceload add -16 -16 15 15
forceload add -16 -16 15 15
wait 3
$ROWS
execute in interregnum:temporal_authority run setblock 20 100 8 minecraft:stone replace
execute in interregnum:temporal_authority run interregnum claim record 20 100 8 20 100 8
execute if block 0 100 8 minecraft:stone run say HOME_STONE_PLACED
execute in interregnum:temporal_authority if block 0 100 8 minecraft:stone run say THERE_STONE_PLACED
execute in interregnum:temporal_authority if block 20 100 8 minecraft:stone run say CLAIMED_STONE_PLACED
interregnum turning age 0 100 8
execute in interregnum:temporal_authority run interregnum turning age 1 100 8
execute in interregnum:temporal_authority run interregnum turning age 1 100 8
execute in interregnum:temporal_authority run interregnum turning age 20 100 8
execute in interregnum:temporal_authority if block 1 100 8 minecraft:mossy_cobblestone run say CHAIN_REACHED_MOSS
execute if block 0 100 8 minecraft:stone run say HOME_UNAGED_BY_COMMAND
execute in interregnum:temporal_authority if block 20 100 8 minecraft:stone run say CLAIMED_UNAGED_BY_COMMAND
wait 40
$AFTER
execute in interregnum:temporal_authority if block 20 100 8 minecraft:stone run say CLAIMED_STONE_SPARED" \
    LOG=/tmp/turning.log timeout 2000 ./tools/server_smoke.sh > /tmp/tn.txt 2>&1 \
    || { tail -25 /tmp/tn.txt; fail "the run did not complete"; }

# --- the table loaded at all ------------------------------------------------
grep -q 'ageing rule(s) loaded' /tmp/turning.log || {
    grep -iE "ageing|Turning" /tmp/turning.log | tail -5 || true
    fail "the ageing table did not load, so nothing below proves anything"; }

# --- the setup, before anything that depends on it --------------------------
# The setup probes run BEFORE the `turning age` calls, and that ordering is load-bearing
# rather than tidy. In the first version they ran after, so a mutation that let the
# command age the overworld made HOME_STONE_PLACED fail -- and the check reported "no
# stone was placed in the overworld", which is false and points at the wrong file. A
# diagnostic that misattributes is not much better than one that does not fire.
mark HOME_STONE_PLACED || fail "no stone was placed in the overworld -- the control does not exist"
mark THERE_STONE_PLACED || {
    grep -iE "temporal_authority|dimension" /tmp/tn.txt | tail -8 || true
    fail "no stone was placed in interregnum:temporal_authority -- the dimension may not have loaded"; }
mark CLAIMED_STONE_PLACED || fail "the claimed stone was never placed, so sparing it proves nothing"

# --- the law ----------------------------------------------------------------
aged=$(grep -c "THERE_AGED" /tmp/turning.log || true)
chained=$(grep -c "THERE_CHAINED" /tmp/turning.log || true)
home=$(grep -c "HOME_AGED" /tmp/turning.log || true)
echo "aged in 40s:  interregnum:temporal_authority=$aged/16 (of which mossy: $chained)   overworld=$home/16"

[ "$aged" -gt 0 ] || {
    grep -oE "(THERE|HOME|CLAIMED)_[A-Z_]+" /tmp/turning.log | sort | uniq -c || true
    fail "no stone aged in interregnum:temporal_authority in 40 seconds -- the world has no law"; }

# --- the chain: stone -> cobble -> mossy cobble, driven deterministically ---
# Two `turning age` calls on one block. Mossy cobblestone cannot be reached from stone
# in one step by any rule in the table, so finding it proves two links ran IN ORDER --
# which a single-step implementation, or a table whose chains do not join up, cannot fake.
mark CHAIN_REACHED_MOSS || {
    grep -oE "turning=[a-z:_]+" /tmp/tn.txt | head -6 || true
    fail "two ageing passes on one stone block did not reach mossy cobblestone -- the chain is not joining up, so the world ages one step and then stops keeping its past"; }

# --- and the law stayed in its own world ------------------------------------
# Categorical, not a ratio: NOTHING in vanilla turns stone into cobble, so a single
# conversion at home is the law having escaped its dimension.
[ "$home" = "0" ] || \
    fail "$home stone block(s) aged in the OVERWORLD, and vanilla never does that -- the Turning's law has leaked out of its own dimension"

# The command was also aimed at the overworld and at a claimed block, and must have
# refused both. Asked as direct questions rather than inferred from a count, and asked
# AFTER the setup has been confirmed, so a failure here means what it says.
mark HOME_UNAGED_BY_COMMAND || \
    fail "'interregnum turning age' aged a block in the OVERWORLD -- the dimension gate is on the passive tick only, so anything calling the law directly can age the overworld"
mark CLAIMED_UNAGED_BY_COMMAND || \
    fail "'interregnum turning age' aged a block a player had placed -- the claim ledger is consulted on the passive path only"

#
# NOTE the single quotes in the failure message below. The first version used backticks,
# inside a double-quoted string, which is command substitution: bash ran `interregnum
# turning age` as a shell command, printed "command not found", and delivered a failure
# message with its own subject missing. Only on the failure path, so only when somebody
# most needed to read it. docs/LESSONS.md #23 -- a failure path must not contain anything
# that can fail -- with a new way to break the same rule.
grep -q "turning=nothing" /tmp/tn.txt || {
    grep -oE "turning=[a-z:_]+" /tmp/tn.txt | head -6 || true
    fail "'interregnum turning age' in the OVERWORLD did not refuse -- the dimension gate is on the passive tick only, so anything that calls the law directly can age the overworld"; }

# --- and it did not touch what somebody built -------------------------------
# THERE_AGED above is this one's control: it proves stone DOES age here, so the claimed
# block staying stone is a decision rather than a world where nothing happened.
mark CLAIMED_STONE_SPARED || \
    fail "stone a player placed was aged -- the mod broke the guarantee that the world may warp but the player's work may not"

echo
echo "OK: the Hearth-Turner's world ages its own stone through every step, and nowhere else does"
