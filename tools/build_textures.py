"""Generate INTERREGNUM's textures from the palette. Deterministic; re-run any time.

Semantic law (docs/PALETTE.md): cool means held, warm means spent. Shrine stone is
HELD -- worked masonry under a god's attention: regular courses, thin mortar, barely
any wear. The heart and clasts are the other side of the law and glow warm/strange.

    python3 tools/build_textures.py
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from paintkit import pal, h2, Image, contour, SIZE
from pngio import write

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BLOCK = os.path.join(REPO, "src/main/resources/assets/interregnum/textures/block")
ITEM = os.path.join(REPO, "src/main/resources/assets/interregnum/textures/item")


def shrine_stone(seed=11):
    """Ashlar masonry: two 8px courses, offset, thin mortar, sparse top-edge light.
    Regularity is CORRECT here -- masonry is regular; that is what 'held' looks like.
    Wear is almost absent (one chipped pixel per course at most, hash as breaker
    inside a structural zone: only ever on a bottom corner of a block)."""
    img = Image(SIZE, SIZE)
    for y in range(SIZE):
        course = y // 8                       # two courses per tile
        cy = y % 8
        offset = 4 if course % 2 else 0       # running bond, wraps cleanly (16%8==0)
        for x in range(SIZE):
            bx = (x + offset) % SIZE
            block_i = bx // 8                 # two blocks per course
            px = bx % 8
            mortar = (cy == 7) or (px == 7)
            if mortar:
                step = 0
            else:
                # ONE base value for every stone. With only two stones per course
                # per tile, any per-stone value variation tiles into columns
                # (tried: strict alternation read as bathroom tile, a 60/40 hash
                # read as periodic light columns). Held means calm: uniform dark
                # ashlar, stones separated by seams, light, and rare chips only.
                step = 1
                _ = block_i
                if cy == 0 and h2(x, course, seed) < 0.45:
                    step = min(3, step + 1)   # top edge catches light, broken by hash
                if cy >= 5 and px in (0, 6) and h2(x, y, seed + 3) < 0.12:
                    step = max(1, step - 1)   # rare chip, only at low corners
            img.set(x, y, pal("stone", step) + (255,))
    return img


def shrine_stone_carved(seed=11):
    """Shrine stone with a chiseled band of god-script. Vanilla's chiseled blocks
    embrace an identical repeat as deliberate ornament, so the band repeats -- but
    the glyphs are abstract ornament, not letter-shapes (a first pass produced
    marks that read as ASCII "4>LL" across a wall). Script is VOID per the
    semantic law, kept one step dim with single bright accents."""
    img = shrine_stone(seed)
    band_top, band_h = 5, 6
    for y in range(band_top, band_top + band_h):
        for x in range(SIZE):
            edge = y == band_top or y == band_top + band_h - 1
            img.set(x, y, pal("stone", 0 if edge else 1) + (255,))
    # two 8px ornament cells: a diamond-knot motif, mostly dim void with one accent
    for cell in range(2):
        ox = cell * 8
        dim, bright = pal("void", 1), pal("void", 2)
        for mx, my, b in [(3,1,0),(4,1,0),(2,2,0),(5,2,0),(3,3,0),(4,3,0),
                          (1,2,1) if cell == 0 else (6,2,1)]:
            img.set(ox + mx, band_top + 1 + my, (bright if b else dim) + (255,))
    return img


def god_heart(seed=5):
    """Item icon: the warm gold thing. Rounded mass, brass family, ember core,
    dark brass contour. Judged at 16px only."""
    img = Image(SIZE, SIZE, fill=(0, 0, 0, 0))
    cx, cy, r = 7.5, 8.0, 4.6
    for y in range(SIZE):
        for x in range(SIZE):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = (dx * dx + (dy * 1.15) ** 2) ** 0.5
            if d < r:
                step = 2 if (dx - dy) > 1.2 else 1   # up-left lit, hard-stepped
                if d < 1.7:
                    img.set(x, y, pal("ember", 2) + (255,))   # the core, still beating
                else:
                    img.set(x, y, pal("brass", step) + (255,))
    img.set(5, 5, pal("bone", 2) + (255,))            # single specular glint
    for i, (gx, gy) in enumerate([(7, 3), (8, 2)]):    # aorta stub
        img.set(gx, gy, pal("brass", 1 - 0 if i else 1) + (255,))
    return contour(img, pal("brass", 0))


def clast(seed=9):
    """Item icon: an angular shard of god. Void family, one bone glint, void contour."""
    img = Image(SIZE, SIZE, fill=(0, 0, 0, 0))
    # a jagged quad defined by explicit vertices -- silhouette first
    verts = [(8, 2), (12, 7), (9, 13), (5, 9)]
    def inside(px, py):
        s = 0
        for i in range(4):
            x1, y1 = verts[i]; x2, y2 = verts[(i + 1) % 4]
            cross = (x2 - x1) * (py - y1) - (y2 - y1) * (px - x1)
            if cross == 0: continue
            if s == 0: s = 1 if cross > 0 else -1
            elif (cross > 0) != (s > 0): return False
        return True
    for y in range(SIZE):
        for x in range(SIZE):
            if inside(x + 0.5, y + 0.5):
                facet = 2 if (x - y) > -2 else 1      # two hard facets
                img.set(x, y, pal("void", facet) + (255,))
    img.set(8, 4, pal("bone", 2) + (255,))            # glint at the fresh break
    img.set(7, 5, pal("void", 2) + (255,))
    return contour(img, pal("void", 0))


ASSETS = {
    (BLOCK, "shrine_stone"): shrine_stone,
    (BLOCK, "shrine_stone_carved"): shrine_stone_carved,
    (ITEM, "god_heart"): god_heart,
    (ITEM, "clast"): clast,
}

if __name__ == "__main__":
    for (folder, name), fn in ASSETS.items():
        os.makedirs(folder, exist_ok=True)
        img = fn()
        p = os.path.join(folder, name + ".png")
        write(p, img)
        print(f"wrote {os.path.relpath(p, REPO)}  ({len(img.colours())} colours)")
