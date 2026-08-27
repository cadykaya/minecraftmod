#!/bin/bash
# Becoming a Theoclast, and the person who decides whether you may.
#
# WORLD.md, locked: "a rite at a shrine, and the keeper has to agree to witness it. Not a
# right-click, and not the crater... Standing that was previously a matter of prices and
# greetings now decides whether you can hold the class at all."
#
# THE FIRST TIME REGARD GATES SOMETHING A PLAYER WANTS. Standing has decided prices,
# greetings and which replies are offered; none of those is a door. This one is.
#
# WHAT IS ASSERTED:
#   * a stranger is refused. Nobody has a record until they have dealt with somebody, and
#     an institution's opinion of a person it has never met is an ABSENCE -- so a player
#     who has walked past every village cannot become a Theoclast, which is the point;
#   * asking does NOT create a record. `peek`, not `of`: being refused by a keeper must
#     not bring a file into existence for somebody they have never dealt with;
#   * the same player, once the villages think well enough of them, IS witnessed -- and
#     nothing else about them changed, so standing is what moved;
#   * it does not take twice;
#   * and THE TAG APPEARS. `class/theoclast` has gated a reply in the Warden's intake
#     scene since long before anything could be attuned, correctly invisible because
#     nobody could truthfully hold it. The scene is not edited here. It simply starts
#     offering the line, which is the whole proof that the class exists rather than that
#     a boolean was set somewhere.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
want() { grep -qF "$2" "$1" || { echo "--- looked for: $2"; grep -oE 'rite=[A-Z_]+ theoclasts=[0-9]+|regard=[a-z]+' "$1" | head -12 || true; fail "$3"; }; }

S=33333333-3333-4333-8333-333333333333
V=44444444-4444-4444-8444-444444444444

# `regard <who> adjust` moves a record by hand, the same gamemaster affordance `record deicide`
# is: there is no way to earn village standing on a server with no players, and what is
# under test is the GATE rather than how anybody gets through it.
COMMANDS="forceload add -16 -16 31 31
wait 3
interregnum rite $S
execute positioned 0 -60 0 run interregnum record deicide $V
say AFTER_THE_DEATH
interregnum rite $S
interregnum regard $S
interregnum regard $S adjust VILLAGES 20
interregnum rite $S
interregnum rite $S
interregnum talk start interregnum:warden_intake $S
interregnum talk show $S
interregnum talk leave $S
interregnum talk start interregnum:warden_intake $V
interregnum talk show $V" \
    LOG=/tmp/rite.log timeout 900 ./tools/server_smoke.sh > /tmp/rt.txt 2>&1 \
    || { tail -25 /tmp/rt.txt; fail "the run did not complete"; }

# --- nothing to attune while the god is whole ------------------------------
want /tmp/rt.txt 'rite=NO_GOD theoclasts=0' \
    "a rite was witnessed before the god had shattered. There are no clasts yet, and a class created out of nothing is a class that did not cost the thing WORLD.md says it costs"

# --- a stranger is refused, and asking leaves no file behind ---------------
want /tmp/rt.txt 'rite=REFUSED theoclasts=0' \
    "the villages vouched for somebody they have never dealt with. An institution's opinion of a stranger is an absence, not a nought, and a player who walked past every village must not be able to hold the class"
grep -qF 'regard=none' /tmp/rt.txt || {
    grep -oE 'regard=[a-z]+[A-Za-z0-9()=,_ -]*' /tmp/rt.txt | head -4 || true
    fail "being refused a rite created a record for somebody the villages had never dealt with. 'peek' exists precisely so that asking cannot bring a file into existence"; }

# --- the same player, once they are known ----------------------------------
# Nothing about them changed except the villages' opinion, which is the assertion: the
# gate is standing and not the deicide, not the item, not who they are.
want /tmp/rt.txt 'rite=ATTUNED theoclasts=1' \
    "a player the villages think well of was still refused. Standing is the whole gate, and if moving it changes nothing then the keeper agreeing is decoration"

# --- and it does not take twice --------------------------------------------
want /tmp/rt.txt 'rite=ALREADY theoclasts=1' \
    "a second rite attuned the same person again. A clast attunes a person, and this one is attuned"

# --- THE TAG APPEARS, in a scene nothing edited ----------------------------
# The reply gated on class/theoclast in warden_intake. It has been in that file since
# before attunement existed and has never once been visible.
#
# Asserted on the SENTENCE and not on the option id, because `show` prints what a player
# reads. Looking for the id passed nothing and cost this check a run -- the id is how the
# scene refers to the option and the text is the only thing that ever reaches anybody.
MARK='Say nothing. The warmth in your chest says it for you.'
grep -qF "$MARK" /tmp/rt.txt || {
    grep -oE 'show\|[^|]*' /tmp/rt.txt | head -8 || true
    fail "the Theoclast reply did not appear in the Warden's intake scene for an attuned player. PlayerTags is the seam every scene reads, and a class that does not reach it is a boolean somebody set rather than a thing the world knows"; }

# --- and NOT for the one who was never attuned -----------------------------
# The control. Without it, "the option is visible" is equally satisfied by a tag gate that
# stopped gating -- which would show the line to everybody and look identical from the
# attuned player's side. Both players are shown the same scene in this run.
[ "$(grep -cF "$MARK" /tmp/rt.txt || true)" = "1" ] || {
    grep -cF "$MARK" /tmp/rt.txt || true
    fail "the Theoclast-only reply was offered to somebody who has not attuned anything. The gate has stopped gating, and from one player's side that looks exactly like the class working"; }

echo
echo "OK: the villages decide who may hold the class, asking leaves no file, and the reply that was waiting for a Theoclast has one"
