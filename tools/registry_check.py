"""Every registered thing resolves a model, its textures, and a name.

VERIFICATION.md ranks this the highest value-per-line test in a content mod,
because it catches the three commonest silent failures at once:

  * the purple-and-black cube      (a model or texture that does not resolve)
  * the raw `block.modid.thing`    (a missing translation key)
  * "my block vanished"            (a block with no loot table)

None of these crash. None fail a compile. The server starts perfectly and the
player finds them. So they get checked here instead.

Loot tables are reported as WARN until datagen exists, because nothing generates
them yet -- reporting a known-absent thing as a hard failure would train everyone
to ignore this tool's output, which is worse than not checking.
"""
import json
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
A = os.path.join(REPO, "src/main/resources/assets/interregnum")
D = os.path.join(REPO, "src/main/resources/data/interregnum")
GEN = os.path.join(REPO, "src/generated/resources/data/interregnum")
REG = os.path.join(REPO, "src/main/java/com/cadykaya/interregnum/registry")

fails, warns = [], []


def registered(java_file, *patterns):
    """Pull registry names out of a registry class. Deliberately regex over the
    source rather than reflection: this runs with no Minecraft on the classpath."""
    src = open(os.path.join(REG, java_file)).read()
    names = []
    for p in patterns:
        names += re.findall(p, src)
    return sorted(set(names))


def model_textures(path):
    """Every texture id a model references, following its parent if it is ours."""
    with open(path) as fh:
        m = json.load(fh)
    out = set(v for v in m.get("textures", {}).values() if isinstance(v, str))
    parent = m.get("parent", "")
    if parent.startswith("interregnum:"):
        p = os.path.join(A, "models", parent.split(":", 1)[1] + ".json")
        if os.path.exists(p):
            out |= model_textures(p)
    return out


def texture_path(tex_id):
    ns, _, path = tex_id.partition(":")
    if ns != "interregnum":
        return None                      # vanilla textures are not ours to check
    return os.path.join(A, "textures", path + ".png")


def check_blocks(lang):
    names = registered("ModBlocks.java",
                       r'registerSimpleBlock\(\s*\n?\s*"([a-z_0-9]+)"',
                       r'BLOCKS\.register\(\s*\n?\s*"([a-z_0-9]+)"')
    if not names:
        fails.append("ModBlocks.java: no registered blocks found -- has the "
                     "registration API changed? This check would then be blind.")
    for n in names:
        bs = os.path.join(A, "blockstates", n + ".json")
        if not os.path.exists(bs):
            fails.append(f"block {n}: no blockstate")
            continue
        with open(bs) as fh:
            state = json.load(fh)
        models = set()
        for v in state.get("variants", {}).values():
            for entry in (v if isinstance(v, list) else [v]):
                models.add(entry["model"])
        for part in state.get("multipart", []):
            models.add(part["apply"]["model"])
        for mid in models:
            mp = os.path.join(A, "models", mid.split(":", 1)[1] + ".json")
            if not os.path.exists(mp):
                fails.append(f"block {n}: blockstate points at missing model {mid}")
                continue
            for tex in model_textures(mp):
                tp = texture_path(tex)
                if tp and not os.path.exists(tp):
                    fails.append(f"block {n}: model {mid} references missing texture {tex}")
        if f"block.interregnum.{n}" not in lang:
            fails.append(f"block {n}: no translation key (renders as the raw key in game)")
        loot = [os.path.join(GEN, "loot_table/blocks", n + ".json"),
                os.path.join(D, "loot_table/blocks", n + ".json")]
        if not any(os.path.exists(p) for p in loot):
            fails.append(f"block {n}: no loot table -- it will drop nothing when mined. "
                         f"Run `gradle runServerData`.")
    return names


def check_items(lang, block_names):
    src = open(os.path.join(REG, "ModItems.java")).read()
    simple = re.findall(r'registerSimpleItem\(\s*\n?\s*"([a-z_0-9]+)"', src)
    block_items = re.findall(r'registerSimpleBlockItem\(ModBlocks\.([A-Z_0-9]+)\)', src)
    for n in sorted(set(simple)):
        if f"item.interregnum.{n}" not in lang:
            fails.append(f"item {n}: no translation key")
    for const in sorted(set(block_items)):
        n = const.lower()
        if n not in block_names:
            fails.append(f"block item for {const}: no such registered block {n}")
    return sorted(set(simple)), sorted(set(block_items))


def check_entities(lang):
    """An entity needs four things, and missing any of them is invisible at boot.

    A block with no model is a purple cube the moment you look at a wall. An entity
    with no renderer, or a renderer whose layer definition was never registered,
    starts a server and a client perfectly happily and then throws the first time
    one walks into view -- so the pairing gets asserted here instead, statically,
    with nothing on the classpath.
    """
    names = registered("ModEntities.java", r'registerEntityType\(\s*\n?\s*"([a-z_0-9]+)"')
    if not names:
        fails.append("ModEntities.java: no registered entities found -- has the "
                     "registration API changed? This check would then be blind.")
    client = os.path.join(REPO, "src/main/java/com/cadykaya/interregnum/client")
    setup = ""
    setup_path = os.path.join(client, "ClientSetup.java")
    if os.path.exists(setup_path):
        setup = open(setup_path).read()
    for n in names:
        const = n.upper()
        if f"entity.interregnum.{n}" not in lang:
            fails.append(f"entity {n}: no translation key")
        tex = os.path.join(A, "textures/entity", n + ".png")
        if not os.path.exists(tex):
            fails.append(f"entity {n}: no texture at textures/entity/{n}.png")
        if f"ModEntities.{const}" not in setup:
            fails.append(f"entity {n}: no renderer registered in ClientSetup "
                         f"-- it will throw the first time one is seen")
        geometry = os.path.join(client, "".join(
            p.capitalize() for p in n.split("_")) + "Geometry.java")
        if not os.path.exists(geometry):
            fails.append(f"entity {n}: no generated geometry -- run gen_resources.py")
        elif "registerLayerDefinition" not in setup:
            fails.append(f"entity {n}: geometry exists but no layer definition is "
                         f"registered; baking its model will throw")
    return names


def main():
    lang_path = os.path.join(A, "lang/en_us.json")
    lang = json.load(open(lang_path)) if os.path.exists(lang_path) else {}
    if not lang:
        fails.append("no en_us.json -- every name in the game would render as a raw key")

    blocks = check_blocks(lang)
    items, block_items = check_items(lang, blocks)
    entities = check_entities(lang)

    for w in warns:
        print(f"warn: {w}")
    if fails:
        print(f"\nFAIL: {len(fails)} registry violation(s)")
        for f in fails:
            print("  -", f)
        return 1
    print(f"\nOK: {len(blocks)} block(s), {len(items)} item(s), "
          f"{len(block_items)} block item(s), {len(entities)} entity(s); "
          f"all resolve models, textures and names")
    return 0


if __name__ == "__main__":
    sys.exit(main())
