#!/bin/bash
# Can three people have an argument in front of a Warden?
#
# The dialogue engine in core/ is tested with no game running, and that covers who
# WINS a node. What it cannot cover is the part that only exists on a server: who is
# at which table, when a node resolves, and what happens when somebody walks off
# mid-sentence. This is the multiplayer half, and every rule in it is a way a table
# can wedge in front of real people:
#
#   * a node resolves the moment the LAST person picks -- not the first, not on a timer
#   * one player leaving must not deadlock the rest, and must not leave their vote behind
#   * the initiator leaving ends it; there is no INITIATOR node without an initiator
#   * a UNANIMOUS node that fails re-prompts instead of picking for people
#   * nobody can be at two tables, and no table survives its scene ending
#
# Participants are opaque ids by design, which is what lets all of that be asserted
# on a server with no players in existence.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
want() { grep -qF "$2" "$1" || { echo "--- looked for: $2"; grep -E 'talk=' "$1" || true; fail "$3"; }; }

SCENE=interregnum:warden_intake
WARDEN='@e[limit=1,type=interregnum:warden]'

# Participants are separated with + rather than , because brigadier's unquoted
# strings do not accept commas: `a,b,c` parses as `a` and then fails on trailing
# data, blaming whatever happens to be at the end of the line.
COMMANDS="forceload add -32 -32 31 31
summon interregnum:warden 8 -60 8
execute positioned 0 -60 0 run interregnum record deicide
interregnum status
interregnum talk start $SCENE kaya+p2+p3 $WARDEN
interregnum status
interregnum talk say p2 refuse
interregnum talk say p3 refuse
interregnum talk say kaya comply
interregnum talk say kaya mining
interregnum talk say p2 mining
interregnum talk say p3 home
interregnum talk say kaya hold
interregnum talk say p2 hold
interregnum talk say p3 look_away
interregnum talk say kaya attest_yes
interregnum talk say p2 attest_no
interregnum talk say p3 attest_yes
interregnum talk say kaya attest_yes
interregnum talk say p2 attest_yes
interregnum talk say p3 attest_yes
interregnum talk status kaya
interregnum talk start $SCENE r1+r2+r3
interregnum talk say r1 comply
interregnum talk say r2 comply
interregnum talk say r3 comply
interregnum talk say r1 mining
interregnum talk say r2 mining
interregnum talk say r3 mining
interregnum talk say r1 hold
interregnum talk say r2 hold
interregnum talk say r3 hold
interregnum talk start $SCENE solo
interregnum talk say solo nonsense
interregnum talk start $SCENE solo
interregnum talk start $SCENE q+r
interregnum talk say q comply
interregnum talk leave r
interregnum talk leave q
interregnum talk status q
interregnum talk start $SCENE z1+z2
interregnum talk leave z1
interregnum talk status z2
interregnum talk start $SCENE v1+v2
interregnum talk show v1
interregnum talk show v1 \"class/theoclast\"
interregnum talk say v1 comply
interregnum talk show v1
interregnum talk show v2
interregnum talk show nobody
interregnum reply comply" \
    LOG=/tmp/talk_check.log timeout 2000 ./tools/server_smoke.sh > /tmp/tc.txt 2>&1 \
    || { tail -25 /tmp/tc.txt; fail "the run did not complete"; }

# --- first contact ends Chapter 1 -------------------------------------------
# Being ADDRESSED by a Warden is the milestone, not seeing one. Asserted through
# the chapter it unlocks rather than through a log line, because the chapter is the
# thing anything else in the mod actually reads.
want /tmp/tc.txt 'chapter=VIGIL band=1' "the deicide did not happen, so this proves nothing about what follows"
want /tmp/tc.txt 'chapter=ENFORCEMENT band=2' \
    "talking to a Warden did not record WARDEN_CONTACT -- the world cannot reach band 2 by playing"

# --- INITIATOR: the initiator wins, and dissent survives as stances ----------
want /tmp/tc.txt 'talk=ADVANCED chose=comply stances={p2=refuse, p3=refuse, kaya=comply}' \
    "the initiator did not win an INITIATOR node, or the others' dissent was thrown away"

# Resolution happens on the LAST submission, not the first. Two pendings then an
# advance is the only shape that proves it.
[ "$(grep -c 'talk=pending' /tmp/tc.txt)" -ge 8 ] \
    || fail "nodes are resolving before everyone has picked"

# --- VOTE: majority carries, and the loser's stance is still recorded --------
want /tmp/tc.txt 'talk=ADVANCED chose=mining stances={kaya=mining, p2=mining, p3=home}' \
    "the majority did not carry a VOTE node"

# --- ROLL: the dice pick from what was actually SAID, not from the node ------
# Two passes. The first has a split table, and only proves the node resolves at
# all. The second has all three saying `hold`, so the pool of submitted picks
# contains one option while the node still offers two -- an implementation that
# rolled across the node's options rather than across what people actually said
# would return `look_away` here.
#
# Stated plainly: that second case catches the bug about half the time, not always.
# The deterministic coverage of ROLL's semantics is in the core self-test, with an
# injected generator; what this proves is that the server wired the dice up at all.
roll=$(grep -oE 'talk=ADVANCED chose=(hold|look_away)' /tmp/tc.txt | head -1)
[ -n "$roll" ] \
    || { grep 'talk=' /tmp/tc.txt | tail -20;
         fail "the ROLL node never resolved"; }
want /tmp/tc.txt 'talk=ADVANCED chose=hold stances={r1=hold, r2=hold, r3=hold}' \
    "a ROLL node with only one option on the table returned something else -- the dice are drawing from the node, not from what was said"

# --- UNANIMOUS: dissent re-prompts, it does not decide for people ------------
want /tmp/tc.txt 'talk=REPROMPT chose=none stances={kaya=attest_yes, p2=attest_no, p3=attest_yes}' \
    "a UNANIMOUS node resolved without unanimity"
want /tmp/tc.txt 'talk=ADVANCED chose=attest_yes stances={kaya=attest_yes, p2=attest_yes, p3=attest_yes} ended=true' \
    "the scene never reached its end"
want /tmp/tc.txt 'talk=none active=0' "the table outlived the conversation"

# --- refusals are answers, not crashes --------------------------------------
want /tmp/tc.txt 'talk=refused reason=no option nonsense on node open' \
    "an option that does not exist on this node was accepted"
want /tmp/tc.txt 'talk=refused reason=solo is already in a conversation' \
    "one participant was seated at two tables at once"

# --- walking away -----------------------------------------------------------
# q has picked, r has not. r leaving completes the table, so the node must resolve
# ON THE WAY OUT. Without this the remaining players wait on someone who has gone.
want /tmp/tc.txt 'talk=left resolved=ADVANCED/comply' \
    "a departure that completed the table did not resolve the node -- the rest are stuck"
# ...and then the initiator leaves, which ends it rather than orphaning the node.
# q left, then the initiator: `status q` must find nothing. Counting `active` here
# would be wrong -- this script deliberately leaves earlier tables open, so the
# tally is not zero and never should be.
grep -q 'talk=none' /tmp/tc.txt \
    || fail "a participant whose table ended still reports as being in a conversation"

# The initiator leaving must not strand the others: z2 is left with no table at all,
# rather than sitting alone at one that can never resolve an INITIATOR node.
[ "$(grep -c 'talk=none' /tmp/tc.txt)" -ge 3 ] \
    || { grep -nE 'talk=(left|none|open)' /tmp/tc.txt;
         fail "z2 was left sitting at a table whose initiator had gone"; }

# --- what a player actually sees --------------------------------------------
#
# The conversation renders to CHAT, with clickable options, which is why the mod
# is playable at all right now: no client code exists and none is needed. These
# assert the view because a table that is correct and invisible is not a feature.

# The scene's text reaches the player, not a raw translation key. (The dedicated
# server does resolve the mod's lang file -- checked, not assumed.)
want /tmp/tc.txt 'show| WARDEN  This unit is conducting a census of the living.' \
    "the speaker line did not render, or came out as an unresolved translation key"
want /tmp/tc.txt "show|   > We're here. We're present." \
    "the options did not render, so there is nothing for a player to click"

# THE gating assertion. `mark` requires class/theoclast, which no player can hold
# until attunement exists, so it must be absent from an untagged view and present
# from a tagged one. Both halves matter: absence alone would also be produced by
# the option failing to render at all.
untagged=$(grep -c 'show|   > Say nothing. The warmth in your chest says it for you.' /tmp/tc.txt || true)
[ "$untagged" = "1" ] \
    || { grep -c 'show|' /tmp/tc.txt; fail "a tag-gated option appeared $untagged time(s); it must be hidden from the untagged view and shown in exactly the tagged one"; }

# The waiting line counts OTHER people, not the reader. Getting this wrong told one
# of two participants that the table was waiting on two of them.
#
# Asserted as an exact tally plus an absence, because merely asking whether the line
# EXISTS does not catch it: a view that counts the reader still emits "waiting on 1"
# from some seats, and the first version of this check passed the mutation happily.
# Four `show` calls on a two-person table: v1 untagged, v1 tagged, v1 after picking,
# and v2. Correct code says "1 other" for the three v1 views and nothing for v2,
# whose only outstanding participant is v2.
ones=$(grep -c 'show| waiting on 1 other(s)...' /tmp/tc.txt || true)
twos=$(grep -c 'show| waiting on 2 other(s)...' /tmp/tc.txt || true)
[ "$ones" = "3" ] && [ "$twos" = "0" ] \
    || { grep 'show| waiting' /tmp/tc.txt;
         fail "waiting counts are wrong (got $ones ones, $twos twos; want 3 and 0) -- the reader is being counted among the people the table is waiting for"; }
want /tmp/tc.txt "show| you said: We're here. We're present." \
    "a participant who has answered is not told what they said"

want /tmp/tc.txt 'show=none' "a view was rendered for somebody who is not in a conversation"

# `reply` is the unprivileged, player-facing half: it can only ever speak for whoever
# ran it, which is what makes it safe to hang off a clickable chat option.
want /tmp/tc.txt 'talk=refused reason=only a player can reply' \
    "reply accepted a caller with no player behind it"

echo
echo "OK: tables resolve on the last word, survive dissent, cannot be left hanging, and render"
