"""Validate the god-worlds' biomes. Exits non-zero on any violation.

A SKY IS ART DIRECTION. `palette_check.py` has been enforcing the shared palette on
textures since before there was a mod, and stopping at textures was arbitrary: a fog
colour decided by whoever happened to be in the file is exactly the drift the palette
system exists to prevent, and it is worse than a texture drifting because a sky is the
one colour a player cannot look away from.

So every colour in every generated biome must be a literal step off a ramp in
`assets/palette.json`. Not "close to" and not "in the family" -- the same hex.

Also checked, because they are the reasons these biomes exist at all:

  * all four god-worlds use one of THESE biomes, not a vanilla one. Three used
    `the_void` and the Verdant's used `plains`, which is where its mob spawns came from.
  * nothing spawns in any of them. A skeleton wandering through a letter delivery is
    scenery nobody wrote.
  * only the Verdant's generates features. A world whose law is growth needs something
    that can grow; the other three are deliberately bare, and a stray `addDefaultOres`
    in one of them would be terrain nobody designed arriving by accident.
"""
import json, os, sys, glob

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BIOMES = os.path.join(REPO, "src/generated/resources/data/interregnum/worldgen/biome")
DIMS = os.path.join(REPO, "src/generated/resources/data/interregnum/dimension")
PALETTE = os.path.join(REPO, "assets/palette.json")

# The Verdant's, and only the Verdant's.
GROWS = "long_green"

fails = []


def palette_hexes():
    doc = json.load(open(PALETTE))
    out = {}
    for name, fam in doc["families"].items():
        for i, step in enumerate(fam["ramp"]):
            out.setdefault(step.lower(), []).append(f"{name}[{i}]")
        out.setdefault(fam["base"].lower(), []).append(f"{name}.base")
    return out


def colours(node, where, found):
    """Every string that looks like #rrggbb, anywhere in the document."""
    if isinstance(node, dict):
        for k, v in node.items():
            colours(v, f"{where}.{k}", found)
    elif isinstance(node, list):
        for i, v in enumerate(node):
            colours(v, f"{where}[{i}]", found)
    elif isinstance(node, str) and node.startswith("#") and len(node) == 7:
        found.append((where, node.lower()))


def main():
    known = palette_hexes()
    files = sorted(glob.glob(os.path.join(BIOMES, "*.json")))
    if not files:
        print("FAIL: no generated biomes at all -- datagen did not run, or the "
              "registry was never added to DataGenerators")
        return 1

    names = set()
    total_colours = 0
    for path in files:
        name = os.path.basename(path)[:-5]
        names.add(name)
        doc = json.load(open(path))

        found = []
        colours(doc, name, found)
        if not found:
            fails.append(f"{name}: has no colours at all, so it is a biome that "
                         f"designs nothing -- which is what `the_void` already was")
        for where, hexcode in found:
            total_colours += 1
            if hexcode not in known:
                fails.append(f"{where}: {hexcode} is not a step in assets/palette.json. "
                             f"A sky is art direction and the palette is where art "
                             f"direction is decided, not the file somebody was in")

        spawners = doc.get("spawners", {})
        for group, entries in spawners.items():
            if entries:
                fails.append(f"{name}: {len(entries)} {group} spawn(s). Nothing spawns "
                             f"in a god's world -- a mob in the middle of a letter "
                             f"delivery is scenery nobody wrote")

        steps = sum(len(s) for s in doc.get("features", []))
        if name == GROWS and steps == 0:
            fails.append(f"{name}: generates nothing. This is the Verdant's world and "
                         f"its law is growth; accelerating bare stone is nothing")
        if name != GROWS and steps > 0:
            fails.append(f"{name}: generates {steps} feature(s). Only the Verdant's "
                         f"world is meant to, and terrain nobody designed arriving by "
                         f"accident is exactly how these worlds started")

    # every god-world points at one of ours
    dims = sorted(glob.glob(os.path.join(DIMS, "*.json")))
    if not dims:
        fails.append("no generated dimensions -- nothing is using these biomes")
    for path in dims:
        dim = os.path.basename(path)[:-5]
        doc = json.load(open(path))
        biome = doc.get("generator", {}).get("biome_source", {}).get("biome")
        if not biome:
            fails.append(f"dimension {dim}: no fixed biome, so this check cannot see "
                         f"what it generates")
        elif not biome.startswith("interregnum:"):
            fails.append(f"dimension {dim}: still generates the vanilla biome {biome!r}. "
                         f"Three of these used `the_void` and one used `plains`, which "
                         f"is where a whole vanilla spawn list came in")
        elif biome.split(":", 1)[1] not in names:
            fails.append(f"dimension {dim}: names {biome!r}, which is not generated")

    if fails:
        print(f"FAIL: {len(fails)} biome violation(s)")
        for f in fails:
            print("  -", f)
        return 1
    print(f"OK: {len(files)} biome(s), {total_colours} colour(s), every one a palette "
          f"step; nothing spawns in any of them")
    return 0


if __name__ == "__main__":
    sys.exit(main())
