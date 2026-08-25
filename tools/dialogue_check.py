"""Validate every dialogue data file. Exits non-zero on any violation.

Mirrors the core DialogueGraph's validation (dangling targets, unreachable nodes,
missing start, duplicate ids) so bad data fails in CI rather than in front of a
player -- and adds the data-side checks Java cannot do: schema shape, known rules,
and that every text_key resolves in the lang file (an untranslated key renders as
the raw key in game, which is a bug wearing a costume).
"""
import json, os, sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DLG = os.path.join(REPO, "src/main/resources/data/interregnum/dialogue")
LANG = os.path.join(REPO, "src/main/resources/assets/interregnum/lang/en_us.json")
RULES = {"INITIATOR", "VOTE", "ROLL", "UNANIMOUS"}
# Kept in step with core's Institution enum. A typo here is the realistic failure --
# VILLAGE for VILLAGES at two in the morning -- and the runtime rejects the file
# outright, so the whole scene disappears rather than one effect going quiet.
INSTITUTIONS = {"WARDENATE", "VILLAGES", "VERDANT", "ANCHORITE",
                "HEARTH_TURNER", "QUIET_ONE", "THE_GHOST"}
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
        for o in n.get("options", []):
            if o.get("target") != END and o.get("target") not in byid:
                fails.append(f"{name}/{n['id']}/{o.get('id')}: dangling target {o.get('target')!r}")
            if o.get("text_key") not in lang:
                fails.append(f"{name}/{n['id']}/{o.get('id')}: text_key not in en_us.json")
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

def main():
    lang = json.load(open(LANG)) if os.path.exists(LANG) else {}
    n = 0
    for root, _, files in os.walk(DLG):
        for f in sorted(files):
            if f.endswith(".json"):
                check_file(os.path.join(root, f), lang); n += 1
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
    print(f"OK: {n} dialogue file(s) valid, all keys resolve, "
          f"{effects} option(s) carry consequences")
    return 0

if __name__ == "__main__":
    sys.exit(main())
