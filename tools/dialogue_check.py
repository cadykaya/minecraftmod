"""Validate every dialogue data file. Exits non-zero on any violation.

Mirrors the core DialogueGraph's validation (dangling targets, unreachable nodes,
missing start, duplicate ids) so bad data fails in CI rather than in front of a
player -- and adds the data-side checks Java cannot do: schema shape, known rules,
and that every text_key resolves in the lang file (an untranslated key renders as
the raw key in game, which is a bug wearing a costume).
"""
import json, os, re, sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DLG = os.path.join(REPO, "src/main/resources/data/interregnum/dialogue")
LANG = os.path.join(REPO, "src/main/resources/assets/interregnum/lang/en_us.json")
RULES = {"INITIATOR", "VOTE", "ROLL", "UNANIMOUS"}
# Kept in step with core's Institution enum. A typo here is the realistic failure --
# VILLAGE for VILLAGES at two in the morning -- and the runtime rejects the file
# outright, so the whole scene disappears rather than one effect going quiet.
INSTITUTIONS = {"WARDENATE", "VILLAGES", "VERDANT", "ANCHORITE",
                "HEARTH_TURNER", "QUIET_ONE", "THE_GHOST"}
# Kept in step with core's Standing enum, and for a sharper reason than the above: a
# misspelt band does not fail the load, it decodes to an ABSENT gate, and an option
# nobody was supposed to see yet appears for everyone. That is the one bug a
# playtester cannot report, because it looks exactly like the game working.
STANDINGS = {"HATED", "RESENTED", "WARY", "KNOWN", "TRUSTED", "BELOVED"}
END = "$end"
fails = []

def check_file(path, lang):
    name = os.path.relpath(path, REPO)
    d = json.load(open(path))
    for req in ("id", "start", "nodes"):
        if req not in d: fails.append(f"{name}: missing '{req}'"); return
    ids = [n.get("id") for n in d["nodes"]]
    if len(ids) != len(set(ids)): fails.append(f"{name}: duplicate node ids")
    byid = {n["id"]: n for n in d["nodes"]}
    if d["start"] not in byid: fails.append(f"{name}: start '{d['start']}' missing")
    for n in d["nodes"]:
        if n.get("rule") not in RULES:
            fails.append(f"{name}/{n['id']}: unknown rule {n.get('rule')!r}")
        if n.get("text_key") not in lang:
            fails.append(f"{name}/{n['id']}: text_key not in en_us.json: {n.get('text_key')}")
        # Alternative wordings for a player an institution has a file on. These were
        # invisible to this check when they landed: a variant with a misspelt key
        # passed, and would have rendered as the raw key to exactly the players the
        # variant was written for -- the ones with a history, who are the least likely
        # to be a first playtester.
        for v in n.get("text_variants", []):
            where = f"{name}/{n['id']}/variant"
            if v.get("text_key") not in lang:
                fails.append(f"{where}: text_key not in en_us.json: {v.get('text_key')}")
            gates = {k: v.get(k, {}) for k in ("standing_at_least", "standing_at_most")}
            if not any(gates.values()):
                # An unconditional variant always wins, silently shadowing the node's
                # own line and every variant after it. The engine refuses this at
                # construction; saying so here names the file instead of the stack.
                fails.append(f"{where} {v.get('text_key')}: no standing condition, "
                             f"so it would always win -- put it in the node's text_key")
            for field, gate in gates.items():
                if not isinstance(gate, dict):
                    fails.append(f"{where}: {field} must be an object")
                    continue
                for inst, band in gate.items():
                    if inst not in INSTITUTIONS:
                        fails.append(f"{where}: unknown institution {inst!r}")
                    if band not in STANDINGS:
                        fails.append(f"{where}: unknown standing {band!r}")
        for o in n.get("options", []):
            if o.get("target") != END and o.get("target") not in byid:
                fails.append(f"{name}/{n['id']}/{o.get('id')}: dangling target {o.get('target')!r}")
            if o.get("text_key") not in lang:
                fails.append(f"{name}/{n['id']}/{o.get('id')}: text_key not in en_us.json")
            for field in ("standing_at_least", "standing_at_most"):
                gate = o.get(field, {})
                if not isinstance(gate, dict):
                    fails.append(f"{name}/{n['id']}/{o.get('id')}: {field} must be an object")
                    continue
                for inst, band in gate.items():
                    if inst not in INSTITUTIONS:
                        fails.append(f"{name}/{n['id']}/{o.get('id')}: "
                                     f"unknown institution {inst!r} in {field}")
                    if band not in STANDINGS:
                        fails.append(f"{name}/{n['id']}/{o.get('id')}: "
                                     f"unknown standing {band!r} in {field}")
            where = f"{name}/{n['id']}/{o.get('id')}"
            regard = o.get("regard", {})
            if not isinstance(regard, dict):
                fails.append(f"{where}: regard must be an object")
                regard = {}
            for inst, delta in regard.items():
                if inst not in INSTITUTIONS:
                    fails.append(f"{where}: unknown institution {inst!r}")
                if not isinstance(delta, int) or isinstance(delta, bool):
                    fails.append(f"{where}: regard {inst} must be a whole number")
                elif not -100 <= delta <= 100:
                    fails.append(f"{where}: regard {inst}={delta} outside [-100, 100]")
                elif delta == 0:
                    fails.append(f"{where}: regard {inst}=0 does nothing; "
                                 f"omit it rather than implying a consequence")
                elif abs(delta) > 25:
                    fails.append(f"{where}: regard {inst}={delta} is too large for one "
                                 f"line of dialogue; the band scale is 35 wide and a "
                                 f"single sentence should not cross one")
    seen, queue = set(), [d["start"]]
    while queue:
        i = queue.pop()
        if i in seen or i not in byid: continue
        seen.add(i)
        queue += [o["target"] for o in byid[i].get("options", []) if o.get("target") != END]
    for i in byid:
        if i not in seen: fails.append(f"{name}: unreachable node '{i}'")
    term = [i for i in seen if not byid[i].get("options")] + \
           [1 for n in d["nodes"] for o in n.get("options", []) if o.get("target") == END]
    if not term: fails.append(f"{name}: no reachable ending")

GODS = {"VERDANT", "ANCHORITE", "HEARTH_TURNER", "QUIET_ONE"}


def deicide_numbers():
    """The two constants a deicide imposes, read from the source rather than remembered.

    A regex over Java is a fragile way to learn a number, and it is deliberate here: if
    `recordDeicide` is reshaped, this returns None and the check below says so instead of
    quietly validating against numbers that no longer exist.
    """
    src = os.path.join(REPO, "core/src/main/java/com/cadykaya/interregnum/core/regard/"
                             "RegardState.java")
    text = open(src).read()
    drop = re.search(r"adjust\(i,\s*(-?\d+)\)", text)
    cap = re.search(r"lowerCeiling\(i,\s*(-?\d+)\)", text)
    return (int(drop.group(1)), int(cap.group(1))) if drop and cap else (None, None)


def best_path(nodes, start, inst):
    """The most regard any single play-through can earn with one institution.

    Longest path over a graph that is small and usually a DAG, with a visited set so a
    scene that loops back cannot make this run forever. Only POSITIVE deltas are summed:
    the question is what the most generous possible route is worth, and a player taking
    it would not choose the options that cost them.
    """
    best = {}

    def walk(node_id, seen):
        if node_id in best and node_id not in seen:
            return best[node_id]
        node = nodes.get(node_id)
        if node is None or node_id in seen:
            return 0
        seen = seen | {node_id}
        top = 0
        for o in node.get("options", []):
            gain = max(0, o.get("regard", {}).get(inst, 0))
            top = max(top, gain + walk(o.get("target"), seen))
        best[node_id] = top
        return top

    return walk(start, frozenset())


def check_ceilings(scenes):
    """No scene may promise a capped player more than the cap can deliver.

    A deicide drops every surviving god and locks a permanent ceiling, and `adjust`
    CLAMPS to that ceiling. So an option worth +25 written past the cap gives a capped
    player +0 -- the choice reads as consequential and does nothing, which is the same
    defect this file already rejects for `regard: 0` ("omit it rather than implying a
    consequence") wearing a number instead of a zero.

    This was written off as uncheckable, on the grounds that whether a player is capped
    is runtime state. That is true and beside the point: the WORST case is arithmetic.
    If the floor plus the most generous route exceeds the ceiling, then for the players
    the scene is really about -- the ones who killed a god -- some of its choices are
    decoration. Found by walking into it: the Anchorite's delivery scene was written at
    +40 against a floor of -45 and a ceiling of -10, so its final choice was worth
    nothing to the only audience that matters.
    """
    drop, cap = deicide_numbers()
    if drop is None:
        fails.append(
            "could not read the deicide's drop and ceiling out of RegardState.java -- "
            "`recordDeicide` has been reshaped, so the ceiling check below is not "
            "running and no scene is being checked against it.")
        return
    for name, d in scenes:
        nodes = {n["id"]: n for n in d.get("nodes", [])}
        for inst in sorted(GODS):
            top = best_path(nodes, d.get("start"), inst)
            if top and drop + top > cap:
                fails.append(
                    f"{name}: the most generous route earns {inst} +{top}, which from the "
                    f"post-deicide floor of {drop} reaches {drop + top} -- past the "
                    f"permanent ceiling of {cap} that killing a god imposes. `adjust` "
                    f"clamps, so a player who killed a god silently receives less than "
                    f"the scene offers and its later choices do nothing. Keep the best "
                    f"route at or under {cap - drop}.")


def main():
    lang = json.load(open(LANG)) if os.path.exists(LANG) else {}
    n = 0
    scenes = []
    for root, _, files in os.walk(DLG):
        for f in sorted(files):
            if f.endswith(".json"):
                path = os.path.join(root, f)
                check_file(path, lang); n += 1
                scenes.append((os.path.relpath(path, REPO), json.load(open(path))))
    check_ceilings(scenes)
    if fails:
        print(f"FAIL: {len(fails)} dialogue violation(s)")
        for f in fails: print("  -", f)
        return 1
    effects = 0
    for root, _, files in os.walk(DLG):
        for f in files:
            if f.endswith(".json"):
                doc = json.load(open(os.path.join(root, f)))
                effects += sum(1 for nd in doc["nodes"]
                               for o in nd.get("options", []) if o.get("regard"))
    drop, cap = deicide_numbers()
    print(f"OK: {n} dialogue file(s) valid, all keys resolve, "
          f"{effects} option(s) carry consequences; no scene offers a god more than "
          f"the {cap} ceiling a deicide leaves")
    return 0

if __name__ == "__main__":
    sys.exit(main())
