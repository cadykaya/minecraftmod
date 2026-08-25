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

# name -> (side_texture, end_texture). Pillar-shaped blocks; the blockstate carries
# the axis, which is why these get variants rather than a single model.
COLUMN_BLOCKS = {"warning_stele": ("stele_side", "stele_top")}

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

# The Warden statue: 4 facings x 2 states. Two models (asleep / awake) differing
# only in the front texture, and the blockstate rotates them.
_STATUE_ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
for suffix, front in (("", "warden_statue_front"), ("_woken", "warden_statue_front_woken")):
    w(os.path.join(ASSETS, "models/block", "warden_statue" + suffix + ".json"),
      {"parent": "minecraft:block/orientable",
       "textures": {
           "front": f"interregnum:block/{front}",
           "side": "interregnum:block/warden_statue_side",
           "top": "interregnum:block/warden_statue_top",
       }})
_statue_variants = {}
for facing, rot in _STATUE_ROT.items():
    for woken in (False, True):
        model = "interregnum:block/warden_statue" + ("_woken" if woken else "")
        entry = {"model": model}
        if rot:
            entry["y"] = rot
        _statue_variants[f"facing={facing},woken={str(woken).lower()}"] = entry
w(os.path.join(ASSETS, "blockstates", "warden_statue.json"), {"variants": _statue_variants})

for name, (side, end) in COLUMN_BLOCKS.items():
    tex = {"side": f"interregnum:block/{side}", "end": f"interregnum:block/{end}"}
    w(os.path.join(ASSETS, "models/block", name + ".json"),
      {"parent": "minecraft:block/cube_column", "textures": tex})
    w(os.path.join(ASSETS, "models/block", name + "_horizontal.json"),
      {"parent": "minecraft:block/cube_column_horizontal", "textures": tex})
    w(os.path.join(ASSETS, "blockstates", name + ".json"),
      {"variants": {
          "axis=y": {"model": f"interregnum:block/{name}"},
          "axis=z": {"model": f"interregnum:block/{name}_horizontal", "x": 90},
          "axis=x": {"model": f"interregnum:block/{name}_horizontal", "x": 90, "y": 90},
      }})
