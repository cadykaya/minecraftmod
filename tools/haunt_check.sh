#!/bin/bash
# Does the dead god reach the right person, once?
#
# The Haunt is a [LOCKED] headline beat -- the owner asked for the dead god to haunt
# the one who killed it -- and it is delivered by exactly one event: a player waking
# up. A headless server has no sleeping players, so every decision lives in
# `TheHaunt.offer` where a command can reach it, and the event handler is three lines.
# What is asserted here is the gate, which is the whole feature:
#
#   * nothing haunts anybody while the god is alive
#   * ONLY the killer -- this is the ghost's private conversation and an admin with
#     good intentions must not be able to hand it to somebody else
#   * once, because that is what "first dream-audience" means
#   * and a deferral must not silently spend it
#
# It also asserts the SECOND dream, which is gated on the chapter rather than on the
# killer: the god comes back once the world reaches ENFORCEMENT and not before. Two
# failures that look identical from outside are separated here on purpose -- the
# second dream not being due yet (NOT_YET) and the second dream having been spent
# (ALREADY) are different facts, and a run that collapsed them would let a lost
# scene look like a delivered one.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }
want() { grep -qF "$2" "$1" || { echo "--- looked for: $2"; grep -E 'haunt=' "$1" || true; fail "$3"; }; }
count() { grep -cF "$2" "$1" || true; }

K=55555555-5555-4555-8555-555555555555
O=66666666-6666-4666-8666-666666666666

COMMANDS="forceload add -32 -32 31 31
interregnum haunt dream $K
execute positioned 0 -60 0 run interregnum record deicide $K
interregnum haunt dream $O
interregnum talk start interregnum:shrine_keeper $K
interregnum haunt dream $K
interregnum talk leave $K
interregnum haunt dream $K
interregnum talk show $K
interregnum talk say $K refuse
interregnum talk say $K no
interregnum haunt dream $K
interregnum haunt dream $K force
interregnum regard $K
interregnum regard $O
interregnum haunt dream notauuid
interregnum talk say $K refuse
interregnum talk say $K no
interregnum haunt dream $K
interregnum record warden_contact
interregnum haunt dream $K
interregnum talk show $K
interregnum talk say $K how
interregnum talk say $K what
interregnum talk say $K on
interregnum talk say $K why
interregnum talk say $K on
interregnum talk say $K yes
interregnum haunt dream $K
interregnum regard $K" \
    LOG=/tmp/haunt.log timeout 2000 ./tools/server_smoke.sh > /tmp/hc.txt 2>&1 \
    || { tail -25 /tmp/hc.txt; fail "the run did not complete"; }

want /tmp/hc.txt 'haunt=NO_GHOST' \
    "something haunted a player while the god was still alive"
want /tmp/hc.txt 'haunt=NOT_THE_KILLER' \
    "the ghost's private conversation was offered to somebody who did not kill it"

# A player already at a table is not evicted for the dream -- and, the part that
# matters, the deferral must NOT record the milestone. The OPENED below comes after
# a BUSY, so if BUSY had spent it this would read ALREADY and the killer would have
# lost the scene to a coincidence of timing.
want /tmp/hc.txt 'haunt=BUSY' \
    "the dream trampled a conversation the player was already in"
[ "$(count /tmp/hc.txt 'haunt=OPENED')" = "3" ] \
    || { grep -n 'haunt=' /tmp/hc.txt;
         fail "expected exactly three openings (one after the deferral, one forced, one for the second dream); a BUSY that consumed the milestone would leave fewer"; }

want /tmp/hc.txt 'show| ...  Executor. Sit.' \
    "the dream opened but the ghost's scene is not what the player was shown"

# The first dream is spent and the second is not due: NOT_YET, twice. Once before
# the forced re-issue and once after it, which is the sharper of the two -- forcing a
# scene must not leave the gate open behind it.
[ "$(count /tmp/hc.txt 'haunt=NOT_YET')" = "2" ] \
    || { grep -n 'haunt=' /tmp/hc.txt;
         fail "the first dream-audience can happen again before the world reaches ENFORCEMENT"; }
want /tmp/hc.txt 'haunt=refused reason=not a player id' \
    "a non-player id was accepted"

# The second dream, gated on ENFORCEMENT and NOT on anything the killer did. Before
# the Warden contact it is NOT_YET (asserted above); after it, the god returns. What
# is looked for is the COLD opening rather than any line of the scene: this killer has
# now refused the god twice, the record says RESENTED, and the neutral wording landing
# here would mean the variant machinery never reached this scene at all. It is also
# the only string that distinguishes dream two from dream one -- both open on
# "Executor", which is the point of the word and useless as an assertion.
want /tmp/hc.txt 'I have not improved it. Sit anyway.' \
    "the second dream did not open, or the ghost greeted a killer it resents in the neutral wording"
want /tmp/hc.txt 'haunt=ALREADY' \
    "the second dream-audience can happen more than once"

# The dream's choices land on THE_GHOST, which is the one relationship a deicide
# leaves open -- and which nobody but the killer has at all. Refusing at `wake` (-5)
# and again at `estate` (-10) puts them at -15.
want /tmp/hc.txt 'killer=true' "the killer's record does not know they are the killer"
want /tmp/hc.txt 'THE_GHOST=WARY(-15)' \
    "the dream's answers did not move the one relationship the killer still has"
# Refusing the whole of the forced first dream again takes them to -30 (RESENTED),
# which is what the cold opening above is reading; agreeing to be the god's eyes in
# the second dream is +14, and lands them back at -16.
want /tmp/hc.txt 'THE_GHOST=WARY(-16)' \
    "the second dream's answers did not move the relationship"
# The bystander has NO RECORD AT ALL, which is stronger than having an empty one.
# Being offered the dream and refused must not bring a file into existence -- an
# institution's opinion of somebody it has never dealt with is an absence, not a
# nought, and `peek` exists precisely so reading cannot create one.
grep -qF 'regard=none' /tmp/hc.txt \
    || { grep -E 'regard=' /tmp/hc.txt;
         fail "a record was created for a bystander who was merely offered the dream and refused"; }

echo
echo "OK: the dead god reaches its killer, twice and in order, and nobody else ever"
