#!/bin/bash
# The dead god's mail changes hands.
#
# WORLD.md, locked: the letters were sent, none was answered, and they came back. A
# shrine-keeper has been holding them ever since, waiting for somebody to give them to.
# "You do not find the mail. It is GIVEN to you, by someone who has been keeping it."
#
# Four letters have existed, been validated and been readable for some time, and NOTHING
# IN THE WORLD PRODUCED ONE. This is the scene that does -- and it is also the gate on the
# ferry, which sails where the letter in your hand is addressed.
#
# WHAT IS ASSERTED:
#   * a keeper does not offer the post while the god is alive. There is no vacancy yet,
#     and a keeper handing out the round on a Tuesday gives away the whole opening;
#   * once the god is dead, the mail scene is what the keeper opens with -- ahead of both
#     of the other two, because a person holding a box they have kept for years does not
#     open with the housekeeping;
#   * ACCEPTING MARKS IT AND REFUSING DOES NOT. This is the one that matters: there is one
#     set of letters in a world, so a refusal that spent the milestone would destroy the
#     mail permanently and silently, and the keeper would go back to talking about the
#     offering box forever;
#   * and after it is taken, the keeper stops offering. Once, forever, server-wide -- the
#     same rule the clasts run on, and for the same reason.
#
# NOT ASSERTED, and stated rather than skipped: the letters ARRIVING IN AN INVENTORY. A
# headless server has no players, so the hand-over has nobody to hand to; `Conversations`
# logs that case and records the milestone anyway, which is what keeps the beat once-only.
# What is checked here is every decision around the transfer. Clearing it needs one player
# and a client.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
want() { grep -qF "$2" "$1" || { echo "--- looked for: $2"; grep -oE 'scene=[a-z_:]+|MAIL_RECEIVED[a-z ]*' "$1" | head -12 || true; fail "$3"; }; }

K=12121212-1212-4121-8121-121212121212

# `talk scene` asks the keeper the same question a right-click asks, which is the only way
# to reach `openingScene` on a server with nobody to click anything.
COMMANDS="forceload add -16 -16 31 31
wait 3
summon interregnum:shrine_keeper 4 -60 4 {NoAI:1b,PersistenceRequired:1b}
wait 2
say BEFORE_THE_DEATH
execute positioned 4 -60 4 run interregnum talk scene @e[type=interregnum:shrine_keeper,limit=1,sort=nearest]
execute positioned 0 -60 0 run interregnum record deicide $K
say AFTER_THE_DEATH
execute positioned 4 -60 4 run interregnum talk scene @e[type=interregnum:shrine_keeper,limit=1,sort=nearest]
interregnum talk start interregnum:keeper_mail $K
interregnum talk say $K not_mine
say AFTER_REFUSING
execute positioned 4 -60 4 run interregnum talk scene @e[type=interregnum:shrine_keeper,limit=1,sort=nearest]
interregnum talk start interregnum:keeper_mail $K
interregnum talk say $K how_long
interregnum talk say $K take
say AFTER_TAKING
execute positioned 4 -60 4 run interregnum talk scene @e[type=interregnum:shrine_keeper,limit=1,sort=nearest]" \
    LOG=/tmp/handover.log timeout 900 ./tools/server_smoke.sh > /tmp/ho.txt 2>&1 \
    || { tail -25 /tmp/ho.txt; fail "the run did not complete"; }

# --- the keeper's opening line, at each of the four moments -----------------
# BY POSITION, not by marker. `talk scene` replies with sendSuccess(..., false), which
# reaches the rcon reply and NOT the log -- so the `say` markers and the answers live in
# different files and no marker can say which answer it belongs to. The four calls are the
# only `talk scene` calls in the run and they happen in a known order, so the Nth answer is
# the Nth moment. `describe` also emits a scene= but always with ` node=` after it, which
# is what the END anchor excludes -- and there is no start anchor because rcon indents
# every reply by two spaces, which cost this check one run to find out.
nth() { grep -oE 'scene=interregnum:[a-z_]+$' /tmp/ho.txt | sed -n "$1p"; }

BEFORE=$(nth 1 || true)
AFTER=$(nth 2 || true)
REFUSED=$(nth 3 || true)
TAKEN=$(nth 4 || true)

[ -n "$BEFORE" ] && [ -n "$TAKEN" ] || {
    grep -oE 'scene=[a-z_:]+' /tmp/ho.txt | head
    fail "fewer than four opening scenes were reported, so the four moments below do not line up with the four answers and every comparison after this is meaningless"; }

# --- nothing is offered while the god is alive -----------------------------
[ "$BEFORE" != "scene=interregnum:keeper_mail" ] || \
    fail "a keeper offered the dead god's returned post while the god was still alive. There is no vacancy yet, and a keeper handing out the round on a Tuesday gives away the whole opening"

# --- once it is dead, the mail outranks the housekeeping -------------------
[ "$AFTER" = "scene=interregnum:keeper_mail" ] || {
    echo "--- opened with: $AFTER"
    fail "after the death the keeper still opened with the offering box. A person holding a box they have kept for years does not open with the housekeeping"; }

# --- REFUSING MUST NOT SPEND IT --------------------------------------------
# The sharpest assertion in the file. There is one set of letters in a world; a refusal
# that marked the milestone would destroy the mail permanently, silently, and the keeper
# would go back to talking about the offering box forever.
[ "$REFUSED" = "scene=interregnum:keeper_mail" ] || {
    echo "--- opened with: $REFUSED"
    fail "refusing the post consumed it. The milestone hangs on the ACCEPTING node for exactly this reason -- there is no second set of letters, and a keeper who stopped offering after a no would have lost them for good"; }

# --- taking it ends the offer ----------------------------------------------
# In the server LOG, not the rcon replies: recording a milestone is something the world
# does, and `Conversations` logs it. The command that advanced the conversation only ever
# reports which node it landed on.
want /tmp/handover.log 'marked MAIL_RECEIVED' \
    "accepting the post did not record that the mail changed hands"
[ "$TAKEN" != "scene=interregnum:keeper_mail" ] || \
    fail "the keeper offered the post again after handing it over. Once, forever, server-wide -- the same rule the clasts run on, and a keeper who can produce another set on request makes the mail scenery"

echo
echo "OK: the post is offered only once the job exists, survives being refused, and is offered no more once it is taken"
