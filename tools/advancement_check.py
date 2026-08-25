"""The advancement exists, resolves, and never announces itself.

`WORLD.md` locks two things that collide here. The advancement at the moment of death
is called **Deicide**. And: *the mod never announces who did it. There is simply a
player online who has gone quiet.*

Minecraft broadcasts advancements to chat by default. Shipping this with the default
flag would print "<player> has made the advancement [Deicide]" to everybody on the
server at the exact instant the design says nobody is told -- the loudest possible
violation of the mod's central beat, delivered by a boolean nobody looked at.

So `announce_to_chat: false` is not a preference, it is the feature, and it is checked
here. The rest of this file exists because the same JSON carries three other things
that can silently drift:

  * the CRITERION NAME, which Java awards by string and JSON declares by string --
    two copies of one name, and the award silently does nothing if they disagree
  * the ADVANCEMENT ID, same problem one level up
  * the title and description keys, which render as raw keys if absent

The award path itself cannot be checked here: it needs a real player and a headless
server has none, the same wall `mobInteract` sits behind. What is checkable is that
everything the award depends on is correct and agrees with itself.

    python3 tools/advancement_check.py
"""
import json
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GEN = os.path.join(REPO, "src/generated/resources/data/interregnum/advancement")
LANG = os.path.join(REPO, "src/main/resources/assets/interregnum/lang/en_us.json")
JAVA = os.path.join(REPO, "src/main/java/com/cadykaya/interregnum/data/ModAdvancements.java")
ITEMS = os.path.join(REPO, "src/main/java/com/cadykaya/interregnum/registry/ModItems.java")


def main():
    src = open(JAVA).read()
    # The two strings Java uses. Parsed rather than hardcoded so this check cannot
    # drift from the code it is checking -- a hardcoded copy is a third copy.
    m_id = re.search(r'MOD_ID,\s*"([a-z_0-9]+)"\)', src)
    m_crit = re.search(r'CRITERION\s*=\s*"([a-z_0-9]+)"', src)
    if not m_id or not m_crit:
        print("  could not parse the advancement id or criterion out of ModAdvancements.java")
        return 1
    adv_id, criterion = m_id.group(1), m_crit.group(1)

    path = os.path.join(GEN, f"{adv_id}.json")
    if not os.path.exists(path):
        print(f"FAIL: Java awards interregnum:{adv_id} and no such advancement is generated.")
        print(f"  expected {path}")
        print("  Run `gradle runServerData` and commit.")
        return 1
    adv = json.load(open(path))
    display = adv.get("display", {})
    fails = []

    # THE rule. Everything else in this file is hygiene; this is the design.
    if display.get("announce_to_chat", True) is not False:
        fails.append("announce_to_chat is not false -- the death would be broadcast to "
                     "the whole server, and WORLD.md locks that the mod never announces "
                     "who did it")
    if display.get("hidden") is not True:
        fails.append("hidden is not true -- the advancement tree would show everybody "
                     "that killing the god is a thing that can be done")

    # The two copies of the criterion name. If these disagree the award is a silent
    # no-op: `award()` returns false and nothing anywhere says so.
    criteria = adv.get("criteria", {})
    if criterion not in criteria:
        fails.append(f"Java awards criterion {criterion!r}, and the advancement declares "
                     f"{sorted(criteria) or 'none'} -- the award would do nothing, quietly")

    lang = json.load(open(LANG))
    for field in ("title", "description"):
        key = display.get(field, {}).get("translate")
        if not key:
            fails.append(f"{field} is not a translation key")
        elif key not in lang:
            fails.append(f"{field} key {key!r} is not in en_us.json -- it renders raw")

    # The icon has to be an item this mod actually registers, or the toast is empty.
    icon = display.get("icon", {}).get("id", "")
    if icon.startswith("interregnum:"):
        name = icon.split(":", 1)[1]
        items = open(ITEMS).read()
        if f'"{name}"' not in items:
            fails.append(f"icon {icon} is not a registered item")
    elif not icon:
        fails.append("no icon: the toast would be blank")

    if fails:
        print(f"FAIL: {len(fails)} advancement violation(s)")
        for f in fails:
            print("  -", f)
        return 1

    print(f"OK: interregnum:{adv_id} resolves, is hidden, and never reaches chat")
    return 0


if __name__ == "__main__":
    sys.exit(main())
