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
# Every diagnostic dump below ends in `|| true`, and it is load-bearing rather than
# defensive. Under `set -e -o pipefail`, a dump that MATCHES NOTHING exits non-zero,
# and because it sits in the last branch of an `||` the shell kills the script right
# there -- before the `fail` that was supposed to explain what happened. The symptom
# is a check that exits 1 having printed nothing at all, which is how this was found:
# a deliberately broken assertion failed silently and looked like it had passed.
want() { grep -qF "$2" "$1" || { echo "--- looked for: $2"; grep -E 'talk=' "$1" || true; fail "$3"; }; }

SCENE=interregnum:warden_intake
WARDEN='@e[limit=1,type=interregnum:warden]'

# Participants are separated with + rather than , because brigadier's unquoted
# strings do not accept commas: `a,b,c` parses as `a` and then fails on trailing
# data, blaming whatever happens to be at the end of the line.
# `wait` after the forceload: the chunk's entity storage loads asynchronously, and
# a Warden summoned before it arrives is invisible to every selector afterwards --
# the conversation would then be held in front of nobody. See docs/LESSONS.md #22.
G=44444444-4444-4444-8444-444444444444
H=55555555-5555-4555-8555-555555555555
COMMANDS="forceload add -32 -32 31 31
wait 5
summon interregnum:warden 8 -60 8
interregnum talk scene $WARDEN
execute positioned 0 -60 0 run interregnum record deicide
interregnum talk scene $WARDEN
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
interregnum reply comply
interregnum talk start interregnum:shrine_keeper s1+s2
interregnum talk say s1 whose
interregnum talk say s2 whose
interregnum talk say s1 pressed
interregnum talk say s2 pressed
interregnum talk say s1 then_tell
interregnum talk say s2 then_tell
interregnum talk say s1 press
interregnum talk say s2 press
interregnum talk start interregnum:dream_audience d1
interregnum talk say d1 refuse
interregnum talk say d1 no
interregnum talk start $SCENE $G
interregnum talk show $G
interregnum regard $G adjust WARDENATE 50
interregnum talk show $G
interregnum regard $G adjust WARDENATE -90
interregnum talk show $G
interregnum talk start interregnum:shrine_keeper $H
interregnum talk show $H
interregnum talk say $H admit
interregnum talk show $H
interregnum talk leave $H
interregnum regard $H adjust VILLAGES -50
interregnum talk start interregnum:shrine_keeper $H
interregnum talk show $H
interregnum talk say $H admit
interregnum talk show $H
interregnum talk leave $H" \
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

# --- standing decides what you are even offered -----------------------------
#
# The first thing in the mod that READS regard. Until this existed, standing was
# recorded, persisted, announced -- and consulted by nothing.
#
# One player, one node, three standings. `show` renders through the same
# ConversationView a real player gets, so this is literally what would be on their
# screen. The sequence-parsing lives in tools/standing_gate_check.py: the property is
# that the offered set CHANGES, which no single grep can express, and counting
# occurrences alone would pass an implementation that showed both gated options in
# one render and neither in the others.
python3 tools/standing_gate_check.py /tmp/tc.txt \
    || { grep -n "show| " /tmp/tc.txt | tail -24 || true; fail "an institution's opinion does not change what it offers you"; }

# And the adjustments that moved the standing actually landed. Without this the three
# renders above could be three identical renders agreeing with each other, which is
# the trap in docs/LESSONS.md #15.
want /tmp/tc.txt 'adjust= WARDENATE moved=50 now=TRUSTED' \
    "the standing never reached TRUSTED, so the gate above was never exercised"
want /tmp/tc.txt 'adjust= WARDENATE moved=-90 now=RESENTED' \
    "the standing never reached RESENTED, so the ceiling above was never exercised"

# --- the villages are the second institution that shows -------------------
#
# The keeper IS the villages: there is no separate village institution to meet, only
# its people. So VILLAGES standing reaches this scene, and it reaches it twice --
# once in what the keeper SAYS, once in what they are willing to offer.
#
# Exactly two renders of each node, so a count is a real assertion here rather than a
# proxy for one: the resented opening must appear once (it fired) and the courtesy
# must appear once across BOTH renders of `admit` (it was withdrawn the second time).

# The setup, asserted before anything that depends on it. Three identical renders
# agreeing with each other is the trap in docs/LESSONS.md #15.
want /tmp/tc.txt 'adjust= VILLAGES moved=-50 now=RESENTED' \
    "the keeper's regard never fell, so nothing below was exercised"

n_known=$(grep -c 'in another keeper.s hand' /tmp/tc.txt || true)
[ "$n_known" = "1" ] || { grep -n 'show|' /tmp/tc.txt | tail -8 || true;
    fail "the keeper's opening does not change for a party the villages resent (matched $n_known time(s), want 1)"; }

# The courtesy: writing a theft up as an authorised withdrawal. Offered to an
# ordinary party, withdrawn from one the villages resent. Two renders of `admit`,
# so twice means the gate did nothing and zero means the setup never got there.
n_courtesy=$(grep -c 'Write it however it balances' /tmp/tc.txt || true)
[ "$n_courtesy" = "1" ] || { grep -n 'show|' /tmp/tc.txt | tail -16 || true;
    fail "the keeper offered the kind label $n_courtesy time(s) across two standings, want exactly 1"; }

# And the node did not simply empty out. Standing costs you the easy way out, never
# the content -- a scene with no replies left is a wedged table, not a consequence.
n_truth=$(grep -c 'There is nothing on the other end of that slot' /tmp/tc.txt || true)
[ "$n_truth" -ge 2 ] \
    || fail "the resented party has no way through the admit node -- gating removed content, not a courtesy"

# --- the same unit, one question changed ----------------------------------
#
# A Warden conducts a census of the living before the death and takes statements
# about it afterwards. Same mob, same manner; what moves is what the procedure is
# FOR, and that pairing is the reason the interrogation scene works at all.
#
# `talk scene` asks the question a right-click asks. A headless server can never
# reach mobInteract, so without it an NPC's choice of opening is observable only by
# playing -- which is to say, not observable from CI.
want /tmp/tc.txt 'scene=interregnum:warden_intake' \
    "a Warden did not open with the census before the god died"
want /tmp/tc.txt 'scene=interregnum:warden_interrogation' \
    "a Warden still opens with the census of the living after the death it is investigating"

# Order matters and counting does not prove it: one of each, and the census FIRST.
# Two scene lines that happened to be the same would satisfy two greps.
first_scene=$(grep -oE 'scene=interregnum:warden_(intake|interrogation)' /tmp/tc.txt | head -1)
[ "$first_scene" = "scene=interregnum:warden_intake" ] \
    || fail "the Warden's first answer was $first_scene, so the pair is the wrong way round"

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

# --- a scene's LAST line reaches the table ----------------------------------
#
# The terminal node is the payoff of every branch, and for a while it was resolved,
# recorded and thrown away without ever being shown: the table closed the instant
# the conversation ended, so nothing pushed the final view. Nothing about that is
# wrong from the inside -- the state machine was perfectly correct -- and it was
# only found by playing a scene through and reading the output.
want /tmp/tc.txt 'ended=true final=remarks' \
    "the run did not reach the shrine-keeper's ending"
#
# HONEST LIMIT, stated so nobody reads more into these than they carry: the lines
# below come from the COMMAND rendering the finished table, not from the push that
# sends it to players. `push` only writes to real ServerPlayers and this server has
# none, so the push itself cannot be observed here at all -- a mutation that deletes
# it passes this file. What IS proved is that a terminal node renders, with its text,
# to the end, and that the command shows it. The push is one line over a render path
# that is covered.
want /tmp/tc.txt 'show| SHRINE-KEEPER  Noted. Under remarks.' \
    "a terminal node rendered nothing -- the payoff of every branch is missing"
want /tmp/tc.txt 'show| The quarter closes at moonrise. You are welcome to attend. Most people do not.' \
    "a multi-line beat lost everything after its first newline"
want /tmp/tc.txt 'ended=true final=decline' \
    "the dream-audience did not reach its refusal ending"

# A finished conversation has nothing outstanding. Telling somebody the scene they
# just ended is "waiting on 1 other" would be its last impression.
[ "$(grep -c 'show| waiting on' /tmp/tc.txt)" = "3" ] \
    || { grep -n 'show| waiting on' /tmp/tc.txt;
         fail "wrong number of waiting lines -- either one was rendered for a conversation that has already ended, or the reader is being counted among the people being waited for"; }

echo
echo "OK: tables resolve on the last word, survive dissent, cannot be left hanging, and render to the end"
