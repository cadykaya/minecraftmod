#!/bin/bash
# The four god-worlds are four places, and each one is its own.
#
# Until this increment three of them generated `minecraft:the_void` and the Verdant's
# generated `minecraft:plains`, so the register's four distinct worlds were, on the
# ground, three identical grey rooms and one meadow carrying vanilla's entire mob spawn
# list. Each now has a biome of its own, named the way people name these places rather
# than the way the dead god's letters do -- and the Quiet One's is named for the silence,
# because WORLD.md gives that god no common name and its register column reads "they will
# not say it".
#
# WHAT IS ASSERTED, and all of it is a RELATIONSHIP rather than a fact about one world,
# for the reason crossing_check.sh gives at length (docs/LESSONS.md #19): a dimension
# that merely loads proves almost nothing, and four `if biome` probes that each pass
# would also pass if all four stems pointed at the same biome.
#
#   * each world reports ITS OWN biome                    (the four names)
#   * and reports NONE OF THE OTHER THREE                 (they are four, not one with
#                                                          four ids)
#   * the overworld is none of them                       (a biome modifier or a stray
#                                                          FixedBiomeSource reaching
#                                                          home would be silent)
#   * the Verdant's world generates vegetation and the Quiet One's generates NONE
#                                                         (the only one with a feature
#                                                          list, and the reason it has
#                                                          one: accelerating bare stone
#                                                          is nothing)
#
# NOT asserted here: any colour. Sky, fog and water are client-side and a headless server
# renders nothing, so a summary line claiming these worlds LOOK different would be a lie
# this container cannot detect. The colours are checked where they can be -- as data, by
# tools/biome_check.py, against assets/palette.json. Saying which half proves what is the
# point; see the same note at the top of crossing_check.sh.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
mark() { grep -q "$1" /tmp/worlds.log; }

COMMANDS='execute in interregnum:unresponsive run forceload add -16 -16 15 15
execute in interregnum:mass_authority run forceload add -16 -16 15 15
execute in interregnum:green_authority run forceload add -16 -16 15 15
execute in interregnum:temporal_authority run forceload add -16 -16 15 15
forceload add -16 -16 15 15
wait 4

execute in interregnum:unresponsive if biome 0 100 0 interregnum:unanswered run say QUIET_OWN
execute in interregnum:mass_authority if biome 0 100 0 interregnum:old_heavy run say HEAVY_OWN
execute in interregnum:green_authority if biome 0 100 0 interregnum:long_green run say GREEN_OWN
execute in interregnum:temporal_authority if biome 0 100 0 interregnum:the_turning run say TURNING_OWN

execute in interregnum:unresponsive if biome 0 100 0 interregnum:old_heavy run say QUIET_IS_HEAVY
execute in interregnum:unresponsive if biome 0 100 0 interregnum:long_green run say QUIET_IS_GREEN
execute in interregnum:unresponsive if biome 0 100 0 interregnum:the_turning run say QUIET_IS_TURNING
execute in interregnum:temporal_authority if biome 0 100 0 interregnum:old_heavy run say TURNING_IS_HEAVY
execute if biome 0 100 0 interregnum:unanswered run say HOME_IS_QUIET
execute if biome 0 100 0 interregnum:long_green run say HOME_IS_GREEN

say COUNTING_GREEN
execute in interregnum:green_authority run fill -16 60 -16 15 90 15 minecraft:air replace minecraft:short_grass
say COUNTING_QUIET
execute in interregnum:unresponsive run fill -16 60 -16 15 90 15 minecraft:air replace minecraft:short_grass
say COUNTED' \
    LOG=/tmp/worlds.log timeout 900 ./tools/server_smoke.sh > /tmp/wd.txt 2>&1 \
    || { tail -25 /tmp/wd.txt; fail "the run did not complete"; }

# --- each world is its own ---------------------------------------------------
for m in QUIET_OWN HEAVY_OWN GREEN_OWN TURNING_OWN; do
    mark "$m" || {
        grep -oE '(QUIET|HEAVY|GREEN|TURNING|HOME)_[A-Z_]+' /tmp/worlds.log | sort -u || true
        fail "a god-world is not generating its own biome: $m never fired. Either the biome did not register, or its level stem is still pointing at whatever it pointed at before"; }
done

# --- and is not any of the others --------------------------------------------
# The load-bearing half. Four passing `if biome` probes would ALSO pass if every stem
# resolved to the same biome and the four ids were aliases, which is exactly the kind of
# silent fallback a dimension can do.
for m in QUIET_IS_HEAVY QUIET_IS_GREEN QUIET_IS_TURNING TURNING_IS_HEAVY; do
    if mark "$m"; then
        fail "two god-worlds are reporting the same biome ($m). They are not four places, they are one place with four names -- and every probe above would still pass"
    fi
done

# --- and none of them has reached home ---------------------------------------
if mark HOME_IS_QUIET || mark HOME_IS_GREEN; then
    fail "the overworld is generating one of the god-worlds' biomes. Something is applying a fixed biome source or a biome modifier where it was never meant to reach, and the overworld's own generation is gone"
fi

# --- vegetation: only the Verdant's world has any ----------------------------
# Read off `fill ... replace`, which answers with how many blocks it changed. Counted
# over 1024 columns rather than probed at one, because features are placed sparsely and
# a single column proves nothing either way (docs/LESSONS.md #31).
#
# `|| true` on the pipeline, and it is not decoration: under `set -o pipefail` a grep
# that matches nothing kills this script before any message it exists to print
# (docs/LESSONS.md #23), and that is exactly what the first version of this line did --
# it looked for "Changed N blocks" and the server says "Successfully filled N block(s)".
# The check went red with no output at all.
count() {
    sed -n "/$1/,/$2/p" /tmp/wd.txt \
        | grep -oE 'filled [0-9]+ block' | grep -oE '[0-9]+' | head -1 || true
}
green=$(count COUNTING_GREEN COUNTING_QUIET)
quiet=$(count COUNTING_QUIET COUNTED)
# "No blocks were filled" carries no number, and it means zero.
green=${green:-0}
quiet=${quiet:-0}
echo "  the Long Green grew $green plant(s) in 1024 columns; the Quiet One's world, $quiet"

[ "$quiet" -eq 0 ] || \
    fail "$quiet plant(s) generated in the Quiet One's world. Its biome has no feature list at all, so this is not a tuning problem -- something is adding terrain to a world nobody designed terrain for"
[ "$green" -gt 0 ] || \
    fail "nothing at all grew in the Verdant's world. Its law is growth and it is the only one of the four given anything to grow; accelerating bare stone is nothing, and verdant_check.sh would be measuring an empty room"

echo
printf "OK: four worlds, four biomes, none of them each other and none of them the\n    overworld's; only the Verdant's grows anything\n"
