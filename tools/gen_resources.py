"""Generate model/blockstate/pack JSON for interregnum. Deterministic, like all art.

Only formats stable for years are generated here (blockstates, block models,
pack.mcmeta shell). Item MODEL definitions are deliberately NOT generated: the
item-model system churned during the 1.21 line and its 26.x shape is unverified
(docs/MODELS.md). They get written when the real toolchain can confirm the format.
"""
import json, os

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(REPO, "src/main/resources/assets/interregnum")

CUBE_ALL_BLOCKS = ["shrine_stone", "shrine_stone_carved"]

def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(obj, fh, indent=2)
        fh.write("\n")
    print("wrote", os.path.relpath(path, REPO))

for name in CUBE_ALL_BLOCKS:
    w(os.path.join(ASSETS, "models/block", name + ".json"),
      {"parent": "minecraft:block/cube_all",
       "textures": {"all": f"interregnum:block/{name}"}})
    w(os.path.join(ASSETS, "blockstates", name + ".json"),
      {"variants": {"": {"model": f"interregnum:block/{name}"}}})
