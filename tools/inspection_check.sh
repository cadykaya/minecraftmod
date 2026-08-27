#!/bin/bash
# The ferry's checklist, in front of a player at last.
#
# WORLD.md, locked: "a keel block captures the structure, validates it against the
# destination's law, and re-places it at the far pad. THE VALIDATION CHECKLIST TEACHES
# EACH WORLD'S RULE BEFORE ARRIVAL -- the Quiet One's crossing: no note blocks, no
# jukebox, muffle your animals."
#
# The middle clause shipped a long time ago and was reachable only from a command, which
# means the beat the locked sentence is actually about -- a player learning what a god is
# like by being refused by its paperwork -- had never once happened in play. Touching the
# keel produces the docket now.
#
# WHAT IS BEING PROVED:
#
#   * the page names ALL FOUR crossings              (a player told about one crossing
#                                                     learns one rule; the whole page is
#                                                     the reconnaissance)
#   * and the four DO NOT AGREE about one hull       (the load-bearing one -- a docket
#                                                     where every destination says the
#                                                     same thing teaches nothing at all)
#   * a refusal names the block AND the count        (Manifest's own javadoc: "there is a
#                                                     problem" tells a player nothing;
#                                                     two note blocks is a different
#                                                     search from twelve)
#   * a refusal carries that god's reason line       (the teaching itself)
#   * EVERY violation is listed, not the first       (being sent back twice for one
#                                                     mistake is the design's fault)
#   * a bare keel answers on one line                (silence from a desk is
#                                                     indistinguishable from a mod that
#                                                     has stopped working)
#   * the same hull produces the same page twice     (a bill of lading that reorders
#                                                     itself between two identical
#                                                     inspections is one nobody can trust)
#
# The command is the seam, not the feature: `interregnum ferry inspect` and the keel's
# right-click both call FerryDocket.of and nothing else. A right-click cannot be driven
# from a headless server, so a docket only the block could produce would be a docket no
# check could read -- the same arrangement `interregnum learn` has with the dialogue node
# that teaches a school.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }

# One hull that each god feels differently about, which is the entire point of showing
# all four. Two sound-makers so the Quiet One has TWO findings and the "every violation"
# claim has something to be about; sand for the Anchorite; nothing stripped and nothing
# waxed, so the Verdant and the Hearth-Turner both clear it.
COMMANDS='forceload add -32 -32 31 31
wait 3
setblock 4 100 4 interregnum:ferry_keel replace
setblock 5 100 4 minecraft:oak_planks replace
setblock 6 100 4 minecraft:note_block replace
setblock 7 100 4 minecraft:jukebox replace
setblock 5 101 4 minecraft:sand replace
interregnum claim record 4 100 4 7 101 4
say DOCKET_ONE
interregnum ferry inspect 4 100 4
say DOCKET_TWO
interregnum ferry inspect 4 100 4
say BARE_KEEL
setblock 20 100 4 interregnum:ferry_keel replace
interregnum ferry inspect 20 100 4
say DONE' \
    LOG=/tmp/inspection.log timeout 900 ./tools/server_smoke.sh > /tmp/insp.txt 2>&1 \
    || { tail -25 /tmp/insp.txt; fail "the run did not complete"; }

# `|| true` on every extraction: under `set -o pipefail` a sed or grep that matches
# nothing kills this script before the message it exists to print (docs/LESSONS.md #23).
page() { sed -n "/$1/,/$2/p" /tmp/insp.txt || true; }
first=$(page DOCKET_ONE DOCKET_TWO)
second=$(page DOCKET_TWO BARE_KEEL)
bare=$(page BARE_KEEL DONE)

[ -n "$first" ] || fail "the inspection produced no output at all -- the command did not run, or the docket came back empty, and a desk that says nothing is indistinguishable from a mod that has stopped working"

# --- all four crossings are on the page --------------------------------------
for where in "THE UNRESPONSIVE" "THE MASS AUTHORITY" "THE GREEN AUTHORITY" "THE TEMPORAL AUTHORITY"; do
    echo "$first" | grep -qF "$where" || {
        echo "$first" | sed 's/^/    /' || true
        fail "the docket never mentions $where. A player handed one destination learns one rule; the whole page is what teaches that the four gods refuse DIFFERENT things, which is the reconnaissance band 3 exists to begin"; }
done

# --- and they disagree about this hull ---------------------------------------
# The assertion that matters. Four destinations that all say the same thing is a page
# that teaches nothing, and it is exactly what a checklist wired to one shared rule set,
# or to the wrong law, or to no law at all, would produce.
cleared=$(echo "$first" | grep -c "CLEARED" || true)
refused=$(echo "$first" | grep -c "REFUSED" || true)
echo "  one hull, four desks: $cleared cleared, $refused refused"
[ "$cleared" -gt 0 ] && [ "$refused" -gt 0 ] || {
    echo "$first" | sed 's/^/    /' || true
    fail "every destination reached the same verdict on one hull ($cleared cleared, $refused refused). The gods are supposed to refuse different things -- a page where they agree is a page with one law behind it, and it teaches a player nothing about where they are going"; }

# --- a refusal names the block and how many ----------------------------------
echo "$first" | grep -qF "minecraft:note_block" || \
    fail "a refusal did not name the block that caused it. Manifest's own reasoning: a player who built a ferry out of two hundred blocks and is told 'there is a problem' has been told nothing"
# The separator between the count and the id is a multi-byte '×', which a single `.`
# in a byte-mode regex does not match -- the first version of this line used one and
# failed against a docket that was entirely correct.
echo "$first" | grep -qE '[0-9]+ .+ minecraft:note_block' || \
    fail "a refusal named the block but not how many. Two note blocks is a different search from twelve, and the count is the closest this can get to answering the question actually in the player's head"

# --- and carries the god's own reason ----------------------------------------
# The teaching. Without this the docket is a rejection slip; with it, it is the thing
# WORLD.md means by "the checklist teaches each world's rule before arrival".
echo "$first" | grep -qF "no procedure for apologising after" || \
    fail "the Quiet One's refusal arrived without its reason line. The docket is then a rejection slip rather than the checklist WORLD.md locks -- a player learns that they cannot go, and nothing about where they were going"

# --- every violation, not the first ------------------------------------------
sound=$(echo "$first" | grep -cE 'minecraft:(note_block|jukebox)' || true)
[ "$sound" -ge 2 ] || {
    echo "$first" | sed 's/^/    /' || true
    fail "the Quiet One's crossing reported $sound of the two sound-makers aboard. A player sent back to fix one thing, who then finds a second, has been made to cross twice for one mistake of the design's"; }

# --- a bare keel says so -----------------------------------------------------
echo "$bare" | grep -qF "not a vessel" || {
    echo "$bare" | sed 's/^/    /' || true
    fail "a keel with nothing attached produced no explanation. The refusal reasons are named in Ferry.Refusal precisely so that 'it did not work' is never the answer"; }

# --- and the page is the same page twice -------------------------------------
# Both `say` markers are stripped from both pages -- `sed -n '/A/,/B/p'` keeps the lines
# that bound the range, and comparing those compares the marker names rather than the
# dockets. The first version of this line did exactly that and reported a stable page as
# unstable.
body() { echo "$1" | tail -n +2 | sed '$d'; }
[ "$(body "$first")" = "$(body "$second")" ] || {
    diff <(echo "$first") <(echo "$second") | head -12 || true
    fail "two inspections of one unchanged hull produced different pages. The manifest is a TreeMap and the destinations are iterated in load order for exactly this reason: a bill of lading that reorders itself between two identical crossings is one nobody can trust"; }

echo
printf "OK: the keel hands back one page naming all four crossings, they disagree about the\n    same hull, and every refusal says what, how many, and why\n"
