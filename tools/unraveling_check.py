"""Validate the unraveling table. Exits non-zero on any violation.

The unraveling is the one system in this mod that changes a world players live
in, so its data gets the strictest checking in the repository. Four of the six
checks exist to protect the player's build and the player's stuff -- those are
design invariants from docs/WORLD.md, not preferences, and a data file is exactly
where they would quietly get broken later.
"""
import json, os, sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(REPO, "src/main/resources/data/interregnum/unraveling/bands.json")
KNOWN = os.path.join(REPO, "tools/vanilla_blocks.txt")

# Blocks the unraveling may NEVER touch, whatever a future band says. Anything
# holding items, marking a spawn, or representing work the player did by hand.
# Converting one of these destroys something the player cannot get back.
FORBIDDEN_SUBSTRINGS = (
    "chest", "shulker", "barrel", "furnace", "hopper", "dropper", "dispenser",
    "bed", "anvil", "beacon", "spawner", "lectern", "jukebox", "brewing",
    "enchanting", "campfire", "sign", "banner", "frame", "armor_stand",
    "respawn", "conduit", "smoker", "crafter", "vault",
)
VALID_SCOPES = {"thin_places", "overworld"}
VALID_CHAPTERS = {"VIGIL", "ENFORCEMENT", "EXODUS", "ATTRITION"}

fails = []


def main():
    known = {l.strip() for l in open(KNOWN) if l.strip() and not l.startswith("#")}
    doc = json.load(open(DATA))
    bands = doc.get("bands", [])
    if not bands:
        print("FAIL: no bands"); return 1

    seen_bands = set()
    pairs = {}          # (from -> to) across all bands, for the cycle check
    for b in bands:
        n = b.get("band")
        tag = f"band {n}"
        if n in seen_bands:
            fails.append(f"{tag}: duplicate band number")
        seen_bands.add(n)
        if b.get("chapter") not in VALID_CHAPTERS:
            fails.append(f"{tag}: unknown chapter {b.get('chapter')!r}")
        if b.get("scope") not in VALID_SCOPES:
            fails.append(f"{tag}: unknown scope {b.get('scope')!r}")

        ids = set()
        for c in b.get("conversions", []):
            cid = c.get("id", "?")
            where = f"{tag}/{cid}"
            if cid in ids:
                fails.append(f"{where}: duplicate conversion id")
            ids.add(cid)

            src, dst = c.get("from"), c.get("to")

            # 1. every id must be one we have actually written down
            for role, v in (("from", src), ("to", dst)):
                if v not in known:
                    fails.append(f"{where}: {role} {v!r} is not in tools/vanilla_blocks.txt")

            # 2. sources must be vanilla, naturally-generated blocks. A mod block
            #    as a source would mean the unraveling eating our OWN structures.
            if src and not src.startswith("minecraft:"):
                fails.append(f"{where}: from must be a vanilla block, got {src!r}")

            # 3. the player's stuff is untouchable, on both sides of the arrow
            for role, v in (("from", src), ("to", dst)):
                if v and any(s in v for s in FORBIDDEN_SUBSTRINGS):
                    fails.append(f"{where}: {role} {v!r} is a protected block type "
                                 f"-- the unraveling may never convert it")

            # 4. rates must be real probabilities, and small: this runs per random
            #    tick over a whole world, so a large number is a world-eater.
            ch = c.get("chance")
            if not isinstance(ch, (int, float)) or not (0 < ch <= 1):
                fails.append(f"{where}: chance {ch!r} must be in (0, 1]")
            elif ch > 0.35:
                fails.append(f"{where}: chance {ch} is too high; "
                             f"cap is 0.35 (this fires per random tick, world-wide)")

            # 5. no oscillation: if A -> B exists anywhere, B -> A must not.
            if src and dst:
                if pairs.get(dst) == src:
                    fails.append(f"{where}: {src} -> {dst} reverses an existing "
                                 f"{dst} -> {src}; the unraveling never runs backwards")
                pairs[src] = dst

            # 6. a conversion to itself is a no-op that looks like a rule
            if src and src == dst:
                fails.append(f"{where}: converts {src} to itself")

    # the guarantee has to stay written down where an author will see it
    comment = " ".join(doc.get("_comment", []))
    if "NEVER converts a player-placed block" not in comment:
        fails.append("the player-placed-block guarantee is missing from _comment; "
                     "it must stay stated in the file authors edit")

    if fails:
        print(f"FAIL: {len(fails)} unraveling violation(s)")
        for f in fails:
            print("  -", f)
        return 1
    total = sum(len(b.get("conversions", [])) for b in bands)
    print(f"OK: {len(bands)} band(s), {total} conversions, all safe")
    return 0


if __name__ == "__main__":
    sys.exit(main())
