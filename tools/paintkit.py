"""Primitives for painting 16x16 textures from the palette.

Contains NO textures and NO per-block constants, deliberately, for the same reason
DOWNTIME's `charkit.py` contains no proportions: a shared module that holds one
asset's numbers is a module every later asset copies. Reach for the *method* here;
keep the numbers in the builder.

Everything is wrap-aware. A block face is seen tiled, so an operation that clamps
at the edge instead of wrapping produces a lattice across every wall built from it.
See docs/ARTSTYLE.md section 4.
"""

import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from colorlab import hex_to_rgb        # noqa: E402
from pngio import Image                # noqa: E402

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
_PALETTE = None
SIZE = 16


def palette():
    global _PALETTE
    if _PALETTE is None:
        with open(os.path.join(REPO, "assets", "palette.json")) as fh:
            _PALETTE = json.load(fh)
    return _PALETTE


def pal(family, step):
    """Palette lookup as an (r,g,b) 0-255 tuple. The ONLY way to obtain a colour.

    A literal colour anywhere in this repository is a bug -- see docs/PALETTE.md.
    """
    fams = palette()["families"]
    if family not in fams:
        raise KeyError(f"no palette family {family!r}; have {sorted(fams)}")
    ramp = fams[family]["ramp"]
    if not -len(ramp) <= step < len(ramp):
        raise IndexError(f"{family} has {len(ramp)} steps, asked for {step}")
    return tuple(round(c * 255) for c in hex_to_rgb(ramp[step]))


# --- wrapping geometry -------------------------------------------------------

def wrap(v, n=SIZE):
    return v % n


def tdist(ax, ay, bx, by, n=SIZE):
    """Toroidal distance. Straight-line distance is wrong on a tiling surface:
    two pixels on opposite edges are neighbours in the wall, not far apart."""
    dx = abs(ax - bx)
    dy = abs(ay - by)
    dx = min(dx, n - dx)
    dy = min(dy, n - dy)
    return math.hypot(dx, dy)


def tdelta(ax, ay, bx, by, n=SIZE):
    """Shortest signed vector from a to b across the wrap."""
    dx, dy = bx - ax, by - ay
    if dx > n / 2: dx -= n
    if dx < -n / 2: dx += n
    if dy > n / 2: dy -= n
    if dy < -n / 2: dy += n
    return dx, dy


# --- deterministic noise -----------------------------------------------------
#
# Textures must be byte-identical across machines and runs, so `random` is banned:
# a seeded PRNG still drifts when call order changes, and a texture that changes
# under an unrelated edit is a texture nobody can review in a diff.

def h2(x, y, seed=0):
    """Stable hash of a lattice point -> float in [0,1)."""
    n = (int(x) * 374761393 + int(y) * 668265263 + seed * 1442695040888963407) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFFFF) / 0x1000000


def scatter(count, seed=0, n=SIZE):
    """`count` well-separated points on the torus, deterministically."""
    pts = []
    tries = 0
    while len(pts) < count and tries < count * 400:
        x = h2(len(pts) * 7 + tries, seed, 11) * n
        y = h2(seed, len(pts) * 13 + tries, 29) * n
        if all(tdist(x, y, px, py, n) > n / (count ** 0.5) * 0.55 for px, py in pts):
            pts.append((x, y))
        tries += 1
    return pts


# --- structure ---------------------------------------------------------------

class Cells:
    """A Voronoi partition of the tiling face -- the 'what am I painting on' index.

    This is the whole point of structure-aware painting. A paint function handed
    only (x, y) has no idea what it is on, so a hash is the only tool left, and a
    hash on a broad surface is digital camouflage. Handed a Cells, a painter can
    ask which cell, how near its edge, and which way its interior lies -- and then
    every decision means something. See docs/ARTSTYLE.md section 5.
    """

    def __init__(self, count, seed=0, n=SIZE):
        self.sites = scatter(count, seed, n)
        self.n = n
        if len(self.sites) < 2:
            raise ValueError("need at least two sites to have edges")

    def at(self, x, y):
        """-> (index, distance_to_nearest_site, edge_proximity 0..1, (nx, ny))

        edge_proximity is 1 at a cell boundary and 0 deep inside a cell, computed
        from the gap between the two nearest sites rather than an absolute radius:
        a rim is a fixed width in the world, never a fraction of the cell.
        """
        px, py = x + 0.5, y + 0.5
        best = second = (1e9, -1)
        for i, (sx, sy) in enumerate(self.sites):
            d = tdist(px, py, sx, sy, self.n)
            if d < best[0]:
                second, best = best, (d, i)
            elif d < second[0]:
                second = (d, i)
        d0, idx = best
        gap = second[0] - d0
        edge = max(0.0, 1.0 - gap / 1.6)          # 1.6 px rim, fixed width
        sx, sy = self.sites[idx]
        dx, dy = tdelta(px, py, sx, sy, self.n)
        m = math.hypot(dx, dy) or 1.0
        return idx, d0, edge, (dx / m, dy / m)


# --- painting ----------------------------------------------------------------

def solid(colour, size=SIZE):
    img = Image(size, size)
    for y in range(size):
        for x in range(size):
            img.set(x, y, colour + (255,))
    return img


def contour(img, colour, size=SIZE):
    """Dark outline around opaque pixels -- items only, never blocks."""
    out = Image(size, size, img.px)
    for y in range(size):
        for x in range(size):
            if img.get(x, y)[3] != 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < size and 0 <= ny < size and img.get(nx, ny)[3] != 0:
                    out.set(x, y, colour + (255,))
                    break
    return out


def step_shade(family, base_step, amount):
    """Move along a ramp by whole steps and clamp. Values are stepped, never blended:
    an interpolated colour is off-palette by definition and the checker will say so."""
    ramp_len = len(palette()["families"][family]["ramp"])
    return pal(family, max(0, min(ramp_len - 1, base_step + amount)))
