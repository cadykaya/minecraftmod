"""Validate the crushing table. Exits non-zero on any violation.

Every other block table in this mod is applied BY THE WORLD, and the runtime asks
the claim ledger before touching anything -- so a rule that named a player's
building would still never eat one. Crushing is the exception on purpose: a crush
only happens because somebody cast Drop-forge here and then dropped something into
it, and `Crush` therefore has no claim check at all. The ledger gates the world,
not the caster (docs/LESSONS.md #35).

That makes this file the ONLY thing standing between a misfired drop and somebody's
wall, and it used to say so in a comment. A comment is not a guard. So the rule
that matters here -- nothing a player builds out of may appear on the left of an
arrow -- is checked, and it is checked on `from` only: what a crush PRODUCES may be
anything, because producing it is the point.
"""
import json, os, sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(REPO, "src/main/resources/data/interregnum/crushing/crushing.json")
KNOWN = os.path.join(REPO, "tools/vanilla_blocks.txt")

# The player's stuff, on either side of the arrow. Same list the unraveling uses,
# and for the same reason: converting one of these destroys something that cannot
# be got back.
FORBIDDEN_SUBSTRINGS = (
    "chest", "shulker", "barrel", "furnace", "hopper", "dropper", "dispenser",
    "bed", "anvil", "beacon", "spawner", "lectern", "jukebox", "brewing",
    "enchanting", "campfire", "sign", "banner", "frame", "armor_stand",
    "respawn", "conduit", "smoker", "crafter", "vault",
)

# Blocks somebody BUILT WITH, on the left of an arrow only. A drop-forge is aimed
# by hand and ignores the ledger, so the reason a stray drop cannot eat a wall is
# that walls are not in this file -- and that reason is now enforced rather than
# asserted in prose.
WORKED_SUBSTRINGS = (
    "planks", "brick", "glass", "wool", "carpet", "concrete", "terracotta",
    "polished", "chiseled", "cut_", "smooth_", "_slab", "_stairs", "_wall",
    "_fence", "_door", "_gate", "_tiles", "quartz", "prismarine", "copper",
    "iron", "gold", "diamond", "emerald", "netherite", "log", "_wood",
    "leaves", "planks", "purpur", "mosaic",
)

# Ore is out for a different reason, and it is not safety: crushing an ore block
# into its mineral would make Drop-forge the best pickaxe in the game, and the
# spell is a workbench, not a mining rig.
ORE_SUBSTRINGS = ("_ore", "ore_", "raw_")

fails = []


def main():
    known = {l.strip() for l in open(KNOWN) if l.strip() and not l.startswith("#")}
    doc = json.load(open(DATA))
    rules = doc.get("conversions", [])
    if not rules:
        print("FAIL: no conversions"); return 1

    ids, froms, pairs = set(), set(), {}
    for c in rules:
        cid = c.get("id", "?")
        src, dst = c.get("from"), c.get("to")

        if cid in ids:
            fails.append(f"{cid}: duplicate conversion id")
        ids.add(cid)

        # Two rules claiming one `from` is refused at load time too, loudly. It is
        # checked here as well so it fails before a server ever starts.
        if src in froms:
            fails.append(f"{cid}: {src} already has a crushing rule; "
                         f"which one applies would be decided by file order")
        froms.add(src)

        for role, v in (("from", src), ("to", dst)):
            if v not in known:
                fails.append(f"{cid}: {role} {v!r} is not in tools/vanilla_blocks.txt")
            if v and any(s in v for s in FORBIDDEN_SUBSTRINGS):
                fails.append(f"{cid}: {role} {v!r} holds items or marks a spawn; "
                             f"nothing may convert it")

        if src and not src.startswith("minecraft:"):
            fails.append(f"{cid}: from must be a vanilla block, got {src!r}")

        # THE ONE THAT MATTERS. `from` only -- what a crush produces may be
        # anything, because producing it is what the spell is for.
        if src and any(s in src for s in WORKED_SUBSTRINGS):
            fails.append(f"{cid}: from {src!r} is something a player BUILDS WITH. "
                         f"Drop-forge is aimed by hand and does not consult the "
                         f"claim ledger, so a rule naming a worked block is a "
                         f"stray drop away from eating somebody's wall")
        if src and any(s in src for s in ORE_SUBSTRINGS):
            fails.append(f"{cid}: from {src!r} is ore. Crushing ore into its "
                         f"mineral makes this the best pickaxe in the game; the "
                         f"spell is a workbench, not a mining rig")

        # A crush is an ACT, not weather. The three tables that run on a clock use
        # `chance` to write a rate; here a weight that landed and did nothing part
        # of the time would not read as a rate, it would read as broken.
        if c.get("chance") != 1.0:
            fails.append(f"{cid}: chance {c.get('chance')!r} must be exactly 1.0 -- "
                         f"a crush is an act, and an act that sometimes does "
                         f"nothing reads as a broken spell rather than as a rate")

        if src and src == dst:
            fails.append(f"{cid}: converts {src} to itself")
        if src and dst:
            if pairs.get(dst) == src:
                fails.append(f"{cid}: {src} -> {dst} reverses an existing "
                             f"{dst} -> {src}; a weight dropped twice would "
                             f"undo itself and the chain would never end")
            pairs[src] = dst

    comment = " ".join(doc.get("_comment", []))
    if "claim ledger" not in comment:
        fails.append("the note that this table does NOT consult the claim ledger is "
                     "missing from _comment; it has to stay in front of whoever "
                     "adds the next rule")

    if fails:
        print(f"FAIL: {len(fails)} crushing violation(s)")
        for f in fails:
            print("  -", f)
        return 1
    print(f"OK: {len(rules)} crushing rule(s), none names anything a player built")
    return 0


if __name__ == "__main__":
    sys.exit(main())
