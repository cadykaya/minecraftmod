"""Solve the palette ramps and write assets/palette.json.

Ramps are NOT typed by hand. Each family supplies one base colour (for its hue and
saturation) plus the L* each step must hit; the solver finds the rest. Spacing is
then exact by construction rather than by luck.

Run deliberately, not automatically:

    python3 tools/palette_build.py

The palette should change when somebody means it to. Regenerating on every build
is how a locked palette quietly stops being locked.

See docs/PALETTE.md for the rules this enforces and why they exist.
"""

import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from colorlab import hex_to_rgb, rgb_to_hex, rgb_to_hsv, hsv_to_rgb, lstar  # noqa: E402

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(REPO, "assets", "palette.json")

# --- the specification -------------------------------------------------------
#
# base:    one colour per family. Only its HUE and SATURATION are used; its own
#          lightness is irrelevant and is discarded.
# steps:   the L* each ramp entry must hit, dark -> light. Adjacent entries must
#          differ by at least MIN_SEPARATION; palette_check.py enforces it.
# means:   what the family is FOR. A family without a job is a colour, not a
#          family, and will be used wherever it happens to look nice.

MIN_SEPARATION = 0.12

FAMILIES = {
    "stone": {
        "base": "#8A8F94",
        "steps": [0.18, 0.34, 0.52, 0.70],
        "means": "inert structural mass; the default world material",
    },
    "earth": {
        "base": "#A77844",
        "steps": [0.20, 0.36, 0.54, 0.72],
        "means": "diggable, soft, disturbed ground",
    },
    "wood": {
        "base": "#B5793C",
        "steps": [0.24, 0.42, 0.60],
        "means": "grown then worked; anything a person shaped by hand",
    },
    "foliage": {
        "base": "#6B9E3E",
        "steps": [0.22, 0.38, 0.56, 0.74],
        "means": "alive and rooted",
    },
    "metal": {
        "base": "#7B93A6",
        "steps": [0.18, 0.34, 0.52, 0.70],
        "means": "refined, cold, manufactured; holds its shape",
    },
    "brass": {
        "base": "#C99A3F",
        "steps": [0.34, 0.52, 0.70],
        "means": "mechanisms a player can operate",
    },
    "bone": {
        "base": "#CFC8B4",
        "steps": [0.58, 0.76, 0.94],
        "means": "pale, dead, dry; the lightest values in the game",
    },
    "ember": {
        "base": "#E8641F",
        "steps": [0.32, 0.50, 0.68],
        "means": "heat, danger, and energy in transit -- the loudest colour, "
                 "reserved for that tell",
    },
    "void": {
        "base": "#6E3FA8",
        "steps": [0.20, 0.38, 0.56],
        "means": "the strange thing; wrongness with a rule behind it",
    },
    "sky": {
        "base": "#5C7CA8",
        "steps": [0.22, 0.42, 0.62],
        "means": "atmosphere, distance, fog and horizon",
    },
}


def solve_step(base_hex, target_l, tol=0.0005, iters=64):
    """Find the colour with base's hue that lands on target_l.

    Darken by dropping V. Lighten by pushing V to 1.0 first, and only desaturate
    once there is no value headroom left. Blending toward white instead is the
    obvious approach and it spends saturation first, which produces pastel mush.
    """
    h, s, _ = rgb_to_hsv(hex_to_rgb(base_hex))
    if s == 0.0:  # a grey base has no hue to preserve; solve V directly
        lo, hi = 0.0, 1.0
        for _ in range(iters):
            mid = (lo + hi) / 2
            if lstar(hsv_to_rgb(h, 0.0, mid)) < target_l:
                lo = mid
            else:
                hi = mid
        return rgb_to_hex(hsv_to_rgb(h, 0.0, (lo + hi) / 2))

    if lstar(hsv_to_rgb(h, s, 1.0)) >= target_l:
        lo, hi = 0.0, 1.0                      # headroom exists: solve on V
        for _ in range(iters):
            mid = (lo + hi) / 2
            if lstar(hsv_to_rgb(h, s, mid)) < target_l:
                lo = mid
            else:
                hi = mid
        v, sat = (lo + hi) / 2, s
    else:
        v = 1.0                                # capped: desaturate toward white
        lo, hi = 0.0, s
        for _ in range(iters):
            mid = (lo + hi) / 2
            if lstar(hsv_to_rgb(h, mid, 1.0)) < target_l:
                hi = mid
            else:
                lo = mid
        sat = (lo + hi) / 2

    out = rgb_to_hex(hsv_to_rgb(h, sat, v))
    if abs(lstar(out) - target_l) > tol * 6:
        # 8-bit quantisation, not solver failure -- but say so rather than hide it
        print(f"  note: {out} lands at L* {lstar(out):.4f}, asked {target_l:.4f}")
    return out


def build():
    families = {}
    for name, spec in FAMILIES.items():
        ramp = [solve_step(spec["base"], t) for t in spec["steps"]]
        families[name] = {
            "means": spec["means"],
            "base": spec["base"],
            "targets": spec["steps"],
            "ramp": ramp,
            "lstar": [round(lstar(c), 4) for c in ramp],
        }
        gaps = [round(lstar(ramp[i + 1]) - lstar(ramp[i]), 4) for i in range(len(ramp) - 1)]
        print(f"{name:8s} {' '.join(ramp)}   gaps {gaps}")

    doc = {
        "_comment": "GENERATED by tools/palette_build.py -- do not hand-edit. "
                    "Edit FAMILIES in that script and re-run.",
        "min_separation_lstar": MIN_SEPARATION,
        "max_families_per_asset": 5,
        "families": families,
    }
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w") as fh:
        json.dump(doc, fh, indent=2)
        fh.write("\n")
    print(f"\nwrote {os.path.relpath(OUT, REPO)}  ({len(families)} families)")


if __name__ == "__main__":
    build()
