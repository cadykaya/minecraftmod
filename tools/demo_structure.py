"""Two stone textures, same palette, same pixel size, same number of values.

One is painted by a hash. One is painted from structure. The difference is the
whole argument of docs/ARTSTYLE.md section 5, and it is much easier to look at
than to read about.

    python3 tools/demo_structure.py <outdir>
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from paintkit import Cells, Image, h2, pal, SIZE   # noqa: E402
from pngio import write                            # noqa: E402

FAMILY = "stone"
LIGHT = (-1, -1)          # from up-left, in image coords where +y is down


def hash_painted(seed=7):
    """The failure mode. Every pixel's value chosen by a random number.

    Right palette, right pixel size, four values -- and it reads as digital
    camouflage, because no cluster sits anywhere for a reason.
    """
    img = Image(SIZE, SIZE)
    for y in range(SIZE):
        for x in range(SIZE):
            r = h2(x, y, seed)
            step = 0 if r < 0.18 else 1 if r < 0.58 else 2 if r < 0.9 else 3
            img.set(x, y, pal(FAMILY, step) + (255,))
    return img


def structure_painted(seed=3, sites=11):
    """The same four values, placed against a described surface.

    The painter can ask: which cell am I on, how near its edge, and which way does
    its interior lie. Then mortar goes in the gaps, highlight on up-left rims,
    shadow on down-right rims -- and every pixel means something.

    The tuning here was NOT free, and the numbers carry the scars:
      * 7 sites and a full-brightness highlight passed inspection as a single tile
        and produced a regular grid of bright dots across a wall. More, smaller
        cells and a highlight that only ever moves ONE step off the cell's own
        value is what fixed it. See docs/LESSONS.md #1.
      * The highlight is broken by a hash. That is the only legitimate use of a
        hash in this pipeline: a breaker INSIDE a zone that structure already chose.
    """
    cells = Cells(sites, seed=seed)
    img = Image(SIZE, SIZE)
    for y in range(SIZE):
        for x in range(SIZE):
            idx, _, edge, (nx, ny) = cells.at(x, y)

            # each stone is one of two mid values, so cells read as separate stones
            step = 1 if h2(idx, 0, seed) < 0.5 else 2

            if edge > 0.66:
                step = 0                                   # mortar: the gap between stones
            else:
                # a rim is a fixed width; only the outer band of a cell is a rim at all
                rim = edge > 0.34
                facing = nx * -LIGHT[0] + ny * -LIGHT[1]   # >0 => site is away from light
                lit_cell = h2(idx, 1, seed) < 0.45         # most stones are NOT highlighted
                breaker = h2(x, y, seed + 5) < 0.72        # hash only as a breaker
                if rim and facing > 0.62 and lit_cell and breaker:
                    step = step + 1                        # exactly one step, never more
                elif rim and facing < -0.62:
                    step = max(0, step - 1)                # down-right rim in shadow

            img.set(x, y, pal(FAMILY, min(3, step)) + (255,))
    return img


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "."
    os.makedirs(out, exist_ok=True)
    for name, img in (("hash", hash_painted()), ("structure", structure_painted())):
        p = os.path.join(out, f"stone_{name}.png")
        write(p, img)
        print(f"wrote {p}  ({len(img.colours())} colours)")


if __name__ == "__main__":
    main()
