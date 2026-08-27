#!/bin/bash
# Band 4: the world forgets what it was -- but not where somebody is looking after it.
#
# WORLD.md, locked: "It frays where nobody tends. Regions people visit and keep hold
# their definition. This makes the 'take the job' ending literal rather than thematic --
# holding the world together shrine by shrine is exactly the counter-move."
#
# THE THING THIS FILE DEFENDS is a gap between two distances, and it is the whole reason
# band 4 can exist at all. There is a contradiction under it:
#
#   * attrition must act where NOBODY TENDS
#   * but like the unraveling it can only act on LOADED ground, because placement
#     tracking answers "claimed" for an unloaded chunk and so protects it absolutely --
#     and loaded, in practice, means near a player
#
# Taken naively those cancel and the band is inert forever. The resolution is that
# tending is intimate (two chunks) while loading reaches much further, so the ring
# between them is ground that is present but unattended. The fringe of your world frays
# while its heart holds.
#
# A single number -- TEND_RADIUS_CHUNKS raised to whatever the view distance is -- turns
# band 4 into a no-op, and nothing crashes, nothing logs, and the world simply never
# forgets anything. The core self-test asserts the rule; this file asserts that the ring
# is real in a running world, through the same method the tick handler calls.
#
# WHAT THIS DELIBERATELY DOES NOT ASSERT, stated rather than quietly omitted:
#
#   The threshold being CROSSED. Ground frays after twenty minutes untended, and no CI
#   run is going to wait that out. `/time add` cannot help: it moves dayTime, while the
#   stamp is compared against gameTime, which only advances by ticking. The arithmetic of
#   crossing is pure and is covered in core's self-test, including the tick either side
#   of the boundary. What is asserted here is the half core cannot see: that the stamp is
#   really written, really persists, and really ages.
#
#   The TICK HANDLER itself. It walks `level.players()`, and a headless server has none.
#   So the command seam is what runs here -- and it is not a parallel implementation: the
#   command and the tick handler call the same `Tending.tendAround`, the way
#   `interregnum record deicide` is the deicide path's second legitimate caller rather
#   than a hook written for tests.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }

# Chunk 0 is tended from a standing position; chunk 4 is four chunks away -- outside the
# tended square, inside the forceloaded region. That pair IS the mechanic.
COMMANDS="forceload add -16 -16 127 127
wait 3
say LOADED
interregnum attrition at 0 -60 0
interregnum attrition at 64 -60 0
interregnum attrition tend 0 -60 0
say TENDED
interregnum attrition at 0 -60 0
interregnum attrition at 64 -60 0
wait 8
say AGED
interregnum attrition at 0 -60 0
interregnum attrition at 64 -60 0" \
    LOG=/tmp/attrition.log timeout 900 ./tools/server_smoke.sh > /tmp/at.txt 2>&1 \
    || { tail -25 /tmp/at.txt; fail "the run did not complete"; }

field() { sed -n "/$1/,\$p" /tmp/at.txt | grep -oE 'attrition sinceTended=-?[0-9]+ stale=[a-z]+ fraying=[a-z]+' | sed -n "$2p" | grep -oE "$3=[-a-z0-9]+" | cut -d= -f2; }

# --- first sight counts as tending ------------------------------------------
# Ground nobody has visited is not ancient ground, it is ground nobody has looked at.
# If an unloaded-then-loaded chunk reported "never tended", a player exploring at band 4
# would meet fresh land that had already gone generic -- which reads as broken worldgen
# rather than as a world forgetting, and is the one way this mechanic can look like a bug.
home_first=$(field LOADED 1 sinceTended)
far_first=$(field LOADED 2 sinceTended)
[ -n "$home_first" ] && [ -n "$far_first" ] || {
    grep -E 'attrition' /tmp/at.txt | head -6 || true
    fail "the attrition probe produced nothing readable -- nothing below has anything to compare"; }
[ "$home_first" != "-1" ] && [ "$far_first" != "-1" ] || \
    fail "a freshly loaded chunk reports never having been tended (home=$home_first far=$far_first) -- first sight is supposed to stamp it, and without that a player exploring at band 4 finds land that is already generic, which looks like broken worldgen instead of a world losing its distinctions"

# --- asking does not change the answer --------------------------------------
# An earlier version stamped unstamped chunks inside the staleness read, so the same
# probe answered "never tended" and then "tended just now". A check asking twice would
# have been measuring itself.
[ "$(field LOADED 1 stale)" = "false" ] || \
    fail "freshly loaded ground reports itself already stale -- the world would start forgetting the moment it was looked at"

# --- tending writes, and only where it reaches ------------------------------
home_tended=$(field TENDED 1 sinceTended)
far_tended=$(field TENDED 2 sinceTended)
echo "  after tending:  home(chunk 0)=$home_tended ticks   far(chunk 4)=$far_tended ticks   gap=$((far_tended - home_tended))"
# Tolerant of a tick of scheduling jitter for the same reason the gap assertion is: the
# probe is a separate command from the tend, and both are exact only on average.
[ "$home_tended" -le 2 ] || \
    fail "standing on ground did not tend it: chunk 0 still reports $home_tended ticks since anybody was near. The counter-move to band 4 is unavailable even to somebody performing it deliberately"

# THE ASSERTION, and it is a COMPARISON rather than a threshold for a reason that cost
# a run to find. The first version asked `far_tended -gt 0`, on the reasoning that ground
# tended this instant reads 0. It escaped the mutation it exists to catch: with tending
# widened to twelve chunks BOTH chunks are stamped in the same instant, but the two
# probes are separate RCON commands and can land one tick apart, so the far one read 1
# and 1 is greater than 0. A threshold sitting exactly on the boundary of ordinary
# jitter -- docs/LESSONS.md #27, arrived at from a third direction.
#
# Honestly, the far chunk is stamped at LOAD and the near one at TEND, which are about
# eighty ticks apart. Mutated, the gap is zero or one. Twenty is far above the jitter and
# far below the real separation.
gap=$((far_tended - home_tended))
[ "$gap" -ge 20 ] || \
    fail "ground four chunks from the tended position was stamped $gap tick(s) apart from the ground under it, so tending reached it too. Tending has to be strictly more intimate than loading, or the ring of present-but-unattended ground vanishes and band 4 can never act anywhere -- silently, with nothing failing and the world simply never forgetting anything"

# --- and the stamp ages ------------------------------------------------------
# Staleness is a comparison against a clock, so the clock has to be seen to move. Without
# this, a stamp frozen at the moment of writing would satisfy everything above and never
# cross any threshold, and band 4 would be inert for a completely different reason.
home_aged=$(field AGED 1 sinceTended)
far_aged=$(field AGED 2 sinceTended)
echo "  eight seconds later:  home=$home_aged ticks   far=$far_aged ticks"
[ "$home_aged" -gt "$home_tended" ] || \
    fail "eight seconds passed and tended ground still reports $home_aged ticks since the visit -- the stamp is not ageing, so nothing can ever go stale and the threshold is unreachable by construction"
[ "$far_aged" -gt "$far_tended" ] || \
    fail "untended ground did not age either -- the clock the staleness comparison reads is not advancing"

# ==========================================================================
# The other half: what "generalised" actually means.
# ==========================================================================
# WORLD.md: "Biome-specific detail reverts to its plainest equivalent. Your forest stops
# being a forest -- not destroyed, GENERALISED. Ores return to stone."
#
# Every assertion below is CATEGORICAL, and it is worth saying why this half gets to be
# when the Verdant's growth did not: nothing in vanilla turns a birch log into an oak log,
# or podzol into dirt, or diamond ore into stone. There is no background process to
# separate the mod's effect from, so a single conversion where none was permitted is the
# law having escaped its gates -- not a rate to compare.
#
# The staleness THRESHOLD is not asserted here and is not asserted anywhere live: ground
# frays after twenty minutes and no CI run waits that out, and `/time add` cannot help
# because it moves dayTime while the stamp reads gameTime. `interregnum attrition abandon`
# sets the stamp, the way an operator marking a region abandoned would; every gate after
# it still applies. The threshold itself is arithmetic, proven in core's self-test on both
# sides of the boundary.
G="/tmp/generalise.log"
COMMANDS="forceload add -16 -16 63 63
wait 3
setblock 0 100 40 minecraft:birch_log replace
setblock 1 100 40 minecraft:podzol replace
setblock 2 100 40 minecraft:diamond_ore replace
setblock 3 100 40 minecraft:birch_log replace
setblock 4 100 40 minecraft:birch_log replace
interregnum claim record 4 100 40 4 100 40
execute if block 0 100 40 minecraft:birch_log run say SETUP_BIRCH
execute if block 2 100 40 minecraft:diamond_ore run say SETUP_ORE
execute if block 4 100 40 minecraft:birch_log run say SETUP_CLAIMED
interregnum attrition abandon 0 100 40
interregnum attrition generalise 0 100 40
say BEFORE_BAND4
execute positioned 0 -60 0 run interregnum record deicide
interregnum record warden_contact
interregnum record first_crossing
interregnum record letter_delivered
say AT_BAND4
interregnum attrition abandon 0 100 40
interregnum attrition generalise 0 100 40
interregnum attrition generalise 1 100 40
interregnum attrition generalise 1 100 40
interregnum attrition generalise 2 100 40
interregnum attrition generalise 4 100 40
interregnum attrition tend 3 100 40
interregnum attrition generalise 3 100 40
execute if block 0 100 40 minecraft:oak_log run say BIRCH_GENERALISED
execute if block 1 100 40 minecraft:dirt run say PODZOL_CHAINED
execute if block 2 100 40 minecraft:stone run say ORE_GENERALISED
execute if block 3 100 40 minecraft:birch_log run say TENDED_SPARED
execute if block 4 100 40 minecraft:birch_log run say CLAIMED_SPARED" \
    LOG="$G" timeout 900 ./tools/server_smoke.sh > /tmp/gen.txt 2>&1 \
    || { tail -25 /tmp/gen.txt; fail "the generalisation run did not complete"; }

mark() { grep -q "$1" "$G"; }

# --- the table loaded, and the setup exists ---------------------------------
grep -q 'generalisation rule(s) loaded' "$G" || {
    grep -iE "generalis|attrition" "$G" | tail -5 || true
    fail "band 4's generalisation table did not load, so nothing below proves anything"; }
mark SETUP_BIRCH   || fail "no birch log was placed -- the control does not exist"
mark SETUP_ORE     || fail "no diamond ore was placed -- the control does not exist"
mark SETUP_CLAIMED || fail "the claimed birch log was never placed, so sparing it proves nothing"

# --- the gate: nothing generalises before band 4 ----------------------------
before=$(sed -n '/SETUP_CLAIMED/,/BEFORE_BAND4/p' /tmp/gen.txt | grep -oE 'attrition-step=[a-z:_-]+' | head -1 || true)
[ "$before" = "attrition-step=not-fraying" ] || \
    fail "abandoned ground generalised at band 0 (reported '$before') -- band 4 is when the world starts forgetting, and a world forgetting before its god has died has no escalation left"

# --- and it does at band 4 --------------------------------------------------
mark BIRCH_GENERALISED || {
    grep -oE 'attrition-step=[a-z:_-]+' /tmp/gen.txt | head -8 || true
    fail "an abandoned birch log did not become an oak log at band 4 -- the table is loaded and the gates are open, so the law is not being applied"; }

# --- the chain: podzol -> coarse dirt -> dirt -------------------------------
# Two steps on one block. Nothing in the table takes podzol to dirt directly, so finding
# dirt proves two links ran IN ORDER -- which a single-step implementation, or a table
# whose chains do not join up, cannot fake. The same property turning_check.sh asserts
# for the Turning, because it is the same machinery.
mark PODZOL_CHAINED || {
    grep -oE 'attrition-step=[a-z:_-]+' /tmp/gen.txt | head -8 || true
    fail "two generalisation passes on podzol did not reach plain dirt -- the chain is not joining up, so the world loses one distinction and then stops forgetting"; }

# --- ores return to stone ---------------------------------------------------
# Locked in WORLD.md, and the harshest rule in the table. If it stops working the band
# loses the consequence that makes tending worth anything.
mark ORE_GENERALISED || \
    fail "abandoned diamond ore did not return to stone. WORLD.md locks it, and it is the rule that gives tending a price: without it the world stops being worth anything only cosmetically"

# --- THE COUNTER-MOVE: tended ground keeps its definition -------------------
# The assertion band 4 exists for. Its control is BIRCH_GENERALISED above -- identical
# block, identical command, one chunk tended and one not -- so this is a decision rather
# than a world where nothing happened to anything.
mark TENDED_SPARED || \
    fail "a birch log in TENDED ground was generalised anyway. Regions people visit are supposed to hold their definition; without that the apocalypse is weather rather than something a player can argue with, and the 'take the job' ending has nothing behind it"

# --- and never anyone's work ------------------------------------------------
mark CLAIMED_SPARED || \
    fail "a block a player had placed was generalised -- the oldest promise in the mod is that the world may warp but a player's work may not"

echo
printf "OK: standing somewhere keeps it, four chunks out is loaded and unattended, the stamp ages,\n    and abandoned ground forgets what it was -- through every step, never where somebody\n    tends, and never anything anyone built\\n"
