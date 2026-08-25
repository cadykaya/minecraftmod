#!/bin/bash
# The dead god's mail loads on a real server, and reads the way it is meant to.
#
# tools/letters_check.py proves the data is right -- the set-level rule, the keys, and
# the reveal-protecting scan that the names appear nowhere else. What it cannot prove is
# that a running server accepts the file and renders it, because a codec mismatch is a
# load-time failure and a missing translation is a render-time one.
#
# The assertion that matters most here is the SHAPE OF THE FOURTH LETTER. Three open with
# a name; the Quiet One's opens `To --`. In the data that is an absent key, which is easy
# to check. On a server it has to come out as a decision rather than as a blank -- and
# `To ` and `To --` differ by two characters in a place nobody is looking.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "FAIL: $1"; exit 1; }

COMMANDS="interregnum letter read verdant
interregnum letter read quiet_one
interregnum letter read nobody" \
    LOG=/tmp/mail.log timeout 2000 ./tools/server_smoke.sh > /tmp/ml.txt 2>&1 \
    || {
        # Look for OUR failure before reporting a generic one. A broken post makes the
        # loader log an ERROR and degrade to no mail -- which is correct, and which
        # server_smoke.sh then fails the whole run for, because it fails on any ERROR.
        # Reporting "the run did not complete" there is true and useless: the run did
        # not complete BECAUSE the mail is broken, and saying so points at the right
        # file. `|| true` on the grep, per docs/LESSONS.md #23.
        broken=$(grep -oE "exactly [0-9]+ letter must open unaddressed, and [0-9]+ do" /tmp/ml.txt | head -1 || true)
        [ -z "$broken" ] || fail "the post is invalid and the server refused it: $broken"
        grep -iE "mail is broken|letter" /tmp/ml.txt | tail -6 || true
        fail "the run did not complete"; }

# --- the mail loaded at all -------------------------------------------------
grep -q 'letter(s) loaded' /tmp/mail.log || {
    grep -iE "letter|mail" /tmp/mail.log | tail -5 || true
    fail "no letter loaded, so nothing below proves anything"; }
grep -q '4 letter(s) loaded, 3 named' /tmp/mail.log || {
    grep -oE '[0-9]+ letter\(s\) loaded, [0-9]+ named' /tmp/mail.log || true
    fail "the server did not load four letters with three named -- the set-level rule in core/Post either did not run or the file changed"; }

# --- a named letter opens with its name -------------------------------------
grep -q 'letter=verdant to=Rill' /tmp/ml.txt || {
    grep -oE 'letter=[a-z_]+ to=[^ ]*' /tmp/ml.txt || true
    fail "the Verdant's letter does not open with Rill -- the reveal that the mail uses names nobody has heard is the whole point of it"; }

# --- and the fourth opens with a decision, not a blank ----------------------
grep -q 'letter=quiet_one to=--' /tmp/ml.txt || {
    grep -oE 'letter=quiet_one to=[^ ]*' /tmp/ml.txt || true
    fail "the Quiet One's letter does not open 'To --'. If it rendered blank it reads as a bug rather than as the character: whether the dead god never had a name for it, or struck one out, is the ambiguity the whole god is built on"; }

# --- the text is text, not keys ---------------------------------------------
# A raw key here is worse than no letter at all: it happens at the exact moment a
# player is being asked to care.
# Dots ESCAPED, and -E. Unescaped, `interregnum.letter.` matches the echoed command
# line `$ interregnum letter read verdant` -- the dots match the spaces -- so the check
# fired on its own input and reported a raw key while the letter had rendered perfectly.
# A false positive that names the wrong culprit costs exactly as much as a missed bug.
if grep -qE 'interregnum\.letter\.' /tmp/ml.txt; then
    grep -oE 'interregnum\.letter\.[a-z_.0-9]+' /tmp/ml.txt | sort -u | head -5 || true
    fail "a letter rendered as a raw translation key"
fi
grep -q 'SUBJECT: GREEN AUTHORITY' /tmp/ml.txt || \
    fail "the Verdant's subject line did not render -- the SUBJECT: prefix is the tell that this is filed correspondence rather than a farewell"

# --- an unknown addressee is refused, not invented ---------------------------
grep -q 'letter=none for nobody' /tmp/ml.txt || \
    fail "asking for a letter to a god that does not exist did not refuse cleanly"

echo
echo "OK: four letters load, three open with a name, and the fourth opens To --"
