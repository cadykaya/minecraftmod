"""Enforce the palette law. Exits non-zero on any violation.

Checked rather than trusted, because every rule here is one a person can break by
accident and nobody notices until a screenshot looks muddy:

  1. STALENESS  -- palette.json still matches what palette_build.py would produce.
                   A hand-edited or out-of-date palette is the failure that hides
                   longest, because everything still runs.
  2. SEPARATION -- adjacent ramp steps differ by >= min_separation_lstar.
  3. ARITHMETIC -- the recorded L* values are the ones the hex actually has.
  4. OFF-PALETTE-- every opaque colour in every texture exists in the palette.
  5. BREADTH    -- no texture uses more than max_families_per_asset families.

Usage:  python3 tools/palette_check.py [--textures DIR]
"""

import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from colorlab import hex_to_rgb, lstar          # noqa: E402
import pngio                                     # noqa: E402
import palette_build                             # noqa: E402

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PALETTE = os.path.join(REPO, "assets", "palette.json")
DEFAULT_TEXTURES = os.path.join(REPO, "src", "main", "resources", "assets")

fails = []
notes = []


def fail(msg):
    fails.append(msg)


def load():
    with open(PALETTE) as fh:
        return json.load(fh)


def check_staleness(doc):
    """Would palette_build.py produce this file today?"""
    spec_names = set(palette_build.FAMILIES)
    have_names = set(doc["families"])
    if spec_names != have_names:
        fail(f"family set differs from palette_build.FAMILIES: "
             f"only in json {sorted(have_names - spec_names)}, "
             f"only in spec {sorted(spec_names - have_names)}")
        return
    for name, spec in palette_build.FAMILIES.items():
        want = [palette_build.solve_step(spec["base"], t) for t in spec["steps"]]
        got = doc["families"][name]["ramp"]
        if want != got:
            fail(f"{name}: palette.json is stale or hand-edited.\n"
                 f"      json  {got}\n      spec  {want}\n"
                 f"      -> re-run tools/palette_build.py")


def check_separation(doc):
    floor = doc["min_separation_lstar"]
    for name, fam in doc["families"].items():
        ramp = fam["ramp"]
        for i in range(len(ramp) - 1):
            gap = lstar(ramp[i + 1]) - lstar(ramp[i])
            if gap < floor:
                fail(f"{name}: {ramp[i]} -> {ramp[i+1]} separated by only "
                     f"{gap:.4f} L*, floor is {floor}")
            if gap <= 0:
                fail(f"{name}: ramp is not monotonically lightening at index {i}")


def check_arithmetic(doc):
    for name, fam in doc["families"].items():
        for hexcol, recorded in zip(fam["ramp"], fam["lstar"]):
            actual = round(lstar(hexcol), 4)
            if abs(actual - recorded) > 0.0002:
                fail(f"{name}: {hexcol} recorded as L* {recorded} but measures {actual}")


def _all_palette_rgb(doc):
    lookup = {}
    for name, fam in doc["families"].items():
        for i, hexcol in enumerate(fam["ramp"]):
            rgb = tuple(round(c * 255) for c in hex_to_rgb(hexcol))
            lookup.setdefault(rgb, []).append(f"{name}[{i}]")
    return lookup


# Review renders produced by tools/texview.py are not assets: a greyscale or tiled
# contact sheet is off-palette BY CONSTRUCTION and flagging it is a false positive.
# Skipping is by filename suffix rather than by directory so that a review render
# dropped into the asset tree by mistake is still skipped rather than failing CI
# for the wrong reason -- and so the list is visible here rather than implied.
REVIEW_SUFFIXES = ("_view.png", "_grey.png")
REVIEW_PREFIXES = ("tiled_", "sheet_")


def is_review_artifact(name):
    return name.endswith(REVIEW_SUFFIXES) or name.startswith(REVIEW_PREFIXES)


def check_textures(doc, root):
    if not os.path.isdir(root):
        notes.append(f"no texture tree at {os.path.relpath(root, REPO)} yet -- "
                     f"checks 4 and 5 had nothing to inspect")
        return
    lookup = _all_palette_rgb(doc)
    cap = doc["max_families_per_asset"]
    seen = 0
    skipped = []
    for dirpath, _, names in os.walk(root):
        for n in sorted(names):
            if not n.endswith(".png"):
                continue
            if is_review_artifact(n):
                skipped.append(n)
                continue
            path = os.path.join(dirpath, n)
            rel = os.path.relpath(path, REPO)
            try:
                img = pngio.read(path)
            except Exception as exc:                       # noqa: BLE001
                fail(f"{rel}: could not decode ({exc})")
                continue
            seen += 1
            used, offenders = set(), set()
            for rgb in img.colours():
                if rgb in lookup:
                    used.add(lookup[rgb][0].split("[")[0])
                else:
                    offenders.add(rgb)
            if offenders:
                shown = ", ".join("#%02X%02X%02X" % c for c in sorted(offenders)[:6])
                more = f" (+{len(offenders)-6} more)" if len(offenders) > 6 else ""
                fail(f"{rel}: {len(offenders)} colour(s) not in palette.json: {shown}{more}")
            if len(used) > cap:
                fail(f"{rel}: uses {len(used)} families ({', '.join(sorted(used))}), cap is {cap}")
    notes.append(f"inspected {seen} texture(s) under {os.path.relpath(root, REPO)}")
    if skipped:
        notes.append(f"skipped {len(skipped)} review render(s): {', '.join(sorted(skipped))}")


def main():
    root = DEFAULT_TEXTURES
    if "--textures" in sys.argv:
        root = os.path.abspath(sys.argv[sys.argv.index("--textures") + 1])
    doc = load()
    check_staleness(doc)
    check_separation(doc)
    check_arithmetic(doc)
    check_textures(doc, root)

    for n in notes:
        print(f"note: {n}")
    if fails:
        print(f"\nFAIL: {len(fails)} palette violation(s)\n")
        for f in fails:
            print(f"  - {f}")
        return 1
    fam = doc["families"]
    steps = sum(len(f["ramp"]) for f in fam.values())
    print(f"\nOK: {len(fam)} families, {steps} steps, all separations >= "
          f"{doc['min_separation_lstar']} L*")
    return 0


if __name__ == "__main__":
    sys.exit(main())
