"""The dead god's mail says what it is supposed to say, and no more.

`WORLD.md` builds one reveal on these four files and it is the mid-game's best beat:

> You spend a hundred hours calling it The Verdant. Then you open the mail you are
> carrying and it says **"Rill --"**, and you understand for the first time that you are
> holding a stranger's correspondence about people you have never met.

That reveal has an unusual property: **it can be destroyed from anywhere in the mod.**
Not by touching the letters — by a villager mentioning Rill, a Warden docket carrying
Ballast, a scene that has a character say Ash. Any of those spend the name early, the
letter lands as recognition instead of as a stranger's mail, and nothing anywhere fails.
Nobody would ever find out; the reveal would just quietly stop being one.

So the load-bearing assertion here is a NEGATIVE one, about the whole shipped string
table rather than about the letters: the three names appear in their own letters and
absolutely nowhere else.

Also asserted:

  * **Three letters open with a name and the fourth does not.** Enforced in `core/` by
    Post as well, because it is a rule about a set; checked here too so it fails in the
    fast gate rather than only when a server loads.
  * The Quiet One's letter has NO `addressee` key at all -- not `""`. `To --` is a
    decision; `To ` is a typo, and the two look nearly identical in a JSON file.
  * Every key resolves. A letter that renders as `interregnum.letter.verdant.2` is worse
    than no letter, because it happens at the exact moment a player is being asked to
    care.
  * No letter is a lament. The dread covenant forbids the grief being a punchline and
    the register is procedure -- so every letter must carry its `SUBJECT:` line, which
    is the tell that this is filed correspondence and not a farewell.

    python3 tools/letters_check.py
"""
import json
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
POST = os.path.join(REPO, "src/main/resources/data/interregnum/letters/post.json")
LANG = os.path.join(REPO, "src/main/resources/assets/interregnum/lang/en_us.json")

fails = []

with open(POST) as fh:
    post = json.load(fh)["letters"]
with open(LANG) as fh:
    lang = json.load(fh)

# --- the set-level rule -----------------------------------------------------
unnamed = [l for l in post if "addressee" not in l]
if len(unnamed) != 1:
    fails.append(
        f"{len(unnamed)} letter(s) open unaddressed; exactly one must. WORLD.md: three "
        f"letters open with a name, the fourth opens `To --`. If the Quiet One has been "
        f"given a name, or another god has lost one, the reveal is gone and nothing "
        f"else in the mod will notice.")

for l in post:
    if "addressee" in l and not l["addressee"].strip():
        fails.append(
            f"letter {l['id']} has a blank addressee. An unaddressed letter OMITS the "
            f"key and renders `To --`; a blank string renders `To ` and is a typo.")

# --- every key resolves -----------------------------------------------------
for l in post:
    keys = [l["subject_key"]] + l["body_keys"]
    for k in keys:
        if k not in lang:
            fails.append(f"letter {l['id']}: {k} has no translation, so it renders raw "
                         f"at the exact moment a player is being asked to care")
    subject = lang.get(l["subject_key"], "")
    if "SUBJECT:" not in subject:
        fails.append(
            f"letter {l['id']}: the subject line does not carry `SUBJECT:`. That prefix "
            f"is the tell that this is filed correspondence rather than a farewell -- "
            f"the dread covenant's register, and where the Wardens got theirs.")

# --- THE reveal: the names appear nowhere else ------------------------------
names = [l["addressee"] for l in post if "addressee" in l]
own_keys = set()
for l in post:
    own_keys.update([l["subject_key"]] + l["body_keys"])

for name in names:
    # A blank name matches every string in the file, so it would bury the two accurate
    # failures above under a list of every key in the mod. The blank case is already
    # reported, precisely, by the addressee check -- this scan has nothing to add to it.
    # A failure path that produces unreadable output is barely better than one that does
    # not fire (docs/LESSONS.md #23).
    if not name.strip():
        continue
    # Word-boundary, case-sensitive: these are proper nouns and a lowercase "ash" in
    # some unrelated line is not a leak.
    pattern = re.compile(r"\b" + re.escape(name) + r"\b")
    leaked = sorted(k for k, v in lang.items()
                    if k not in own_keys and pattern.search(v))
    if leaked:
        # Capped, for the same reason: a name that leaked into fifty lines is one
        # problem, and printing fifty keys hides the next finding rather than helping.
        shown = ", ".join(leaked[:5])
        more = f" (and {len(leaked) - 5} more)" if len(leaked) > 5 else ""
        fails.append(
            f"the name {name!r} appears outside its own letter, in: {shown}{more}. "
            f"The whole point is that the mail uses names the player has never heard; a "
            f"name spent early lands as recognition instead of as a stranger's "
            f"correspondence, and nothing else would ever fail.")

if fails:
    print()
    for f in fails:
        print("  - " + f)
    print(f"\nFAIL: {len(fails)} problem(s) with the mail")
    sys.exit(1)

print(f"\nOK: {len(post)} letter(s), {len(names)} named and one not; "
      f"{', '.join(names)} appear nowhere else in the mod")
