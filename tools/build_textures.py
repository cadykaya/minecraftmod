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


def _script_column(img, x0, y0, height, seed, dim, bright):
    """A vertical run of god-script glyphs in a 3px-wide channel.

    Glyphs come from a fixed stroke grammar rather than scattered pixels, for the
    reason in docs/ARTSTYLE.md section 5: marks placed by a hash mean nothing, and
    at this size "means nothing" reads as dirt rather than as writing. Each glyph
    occupies a 3x3 cell with a 1px gap, so a column is legible as *lines of text*
    at play distance even though no glyph is readable.
    """
    grammar = [
        [(0, 0), (1, 0), (0, 1), (0, 2)],          # hook
        [(1, 0), (1, 1), (0, 2), (2, 2)],          # fork
        [(0, 0), (2, 0), (1, 1), (1, 2)],          # spine
        [(0, 1), (1, 1), (2, 1), (1, 2)],          # bar
        [(0, 0), (1, 1), (2, 2)],                  # slash
    ]
    cell = 0
    y = y0
    while y + 3 <= y0 + height:
        g = grammar[int(h2(cell, seed, 17) * len(grammar))]
        # one accent stroke per glyph, so the column has rhythm instead of an even grey
        accent = int(h2(cell, seed, 23) * len(g))
        for k, (mx, my) in enumerate(g):
            img.set(x0 + mx, y + my, (bright if k == accent else dim) + (255,))
        cell += 1
        y += 4


def stele_side(seed=21):
    """The warning stele's inscribed face.

    Chapter 0 furniture: players walk past these for hours reading them as ruin
    dressing. After the death the same text is the only instruction anyone left
    behind, which is why the script is legible-as-writing from the start and never
    explained.
    """
    img = Image(SIZE, SIZE)
    for y in range(SIZE):
        for x in range(SIZE):
            # a plain dressed slab, darker than shrine ashlar: this is a marker, not
            # masonry, and it should read as one object rather than as coursed blocks
            step = 1
            if x == 0 or x == SIZE - 1:
                step = 0                                    # chamfered edges
            elif y == 0 and h2(x, 0, seed) < 0.5:
                step = 2                                    # top edge catches light
            img.set(x, y, pal("stone", step) + (255,))
    # recessed panel the script sits in: x 3..12, y 2..13
    for y in range(2, SIZE - 2):
        for x in range(3, SIZE - 3):
            img.set(x, y, pal("stone", 0) + (255,))
    # TWO columns, centred in the panel. One column read as a single scratch and
    # left the panel visibly half-empty; two read as an inscription, which is the
    # whole job -- the player must recognise it as WRITING without ever being able
    # to read it. Columns are given different seeds so they are not twins.
    _script_column(img, 4, 3, 11, seed, pal("void", 1), pal("void", 2))
    _script_column(img, 9, 3, 11, seed + 31, pal("void", 1), pal("void", 2))
    return img


def stele_top(seed=21):
    """The stele's crown: plain dressed stone, no script. Weathered at the rim."""
    img = Image(SIZE, SIZE)
    for y in range(SIZE):
        for x in range(SIZE):
            rim = min(x, y, SIZE - 1 - x, SIZE - 1 - y)
            if rim == 0:
                step = 0
            elif rim == 1 and h2(x, y, seed + 4) < 0.5:
                step = 2
            else:
                step = 1
            img.set(x, y, pal("stone", step) + (255,))
    return img


def _statue_inset(y):
    """Half-width of the figure at row y. Narrow head, wide plinth."""
    if y < 3:
        return 4
    if y < 6:
        return 3
    if y < 12:
        return 2
    return 1


def _statue_body(seed):
    """The shared carcass of a Warden statue: a standing figure in cold iron.

    Wardens are the dead god's enforcement, so per the palette law they are HELD --
    cool metal, upright, symmetrical, unweathered. Everything about the silhouette
    should say "this was maintained", because that is exactly what stopped.
    """
    img = Image(SIZE, SIZE)
    for y in range(SIZE):
        for x in range(SIZE):
            inset = _statue_inset(y)
            if x < inset or x >= SIZE - inset:
                img.set(x, y, pal("stone", 0) + (255,))       # the gap around it
            else:
                edge = x == inset or x == SIZE - 1 - inset
                img.set(x, y, pal("metal", 1 if edge else 2) + (255,))
    return img


def warden_statue_front(seed=31, woken=False):
    """The face. One dominant cue and nothing competing with it.

    ARTSTYLE 3c: whatever a character's facing cue is, it wins, and secondary head
    detail is cut until it does. Here it is the eye band -- two slots on a dark
    visor. Dormant they are the darkest value in the palette; woken they are EMBER,
    because a Warden enforcing a dead god's law is running on the corpse. Warm means
    spent. The eyes are the only warm pixels on an otherwise entirely cool figure,
    which is the whole point: you can see one across a field.
    """
    img = _statue_body(seed)
    # Every mark below is CLIPPED to the body. A first pass drew the visor at a
    # fixed width and it overflowed the head into the empty margin, which made a
    # figure read as a box with ears -- ARTSTYLE rule 1: if the silhouette is wrong
    # no amount of painting fixes it, and paint that leaves the silhouette IS the
    # silhouette being wrong.
    def band(y, colour, margin=1):
        inset = _statue_inset(y)
        for x in range(inset + margin, SIZE - inset - margin):
            img.set(x, y, colour + (255,))

    for y in range(4, 7):
        band(y, pal("metal", 0), margin=0)          # recessed visor across the head
    eye = pal("ember", 2) if woken else pal("stone", 0)
    for ex in (5, 6, 9, 10):
        img.set(ex, 5, eye + (255,))
    band(10, pal("metal", 0), margin=2)             # chest seam
    band(13, pal("metal", 0), margin=2)             # belt line
    return img


def warden_statue_front_woken(seed=31):
    return warden_statue_front(seed, woken=True)


def warden_statue_side(seed=31):
    """Profile: same carcass, no face, one vertical seam so it reads as a figure
    rather than a post when seen edge-on."""
    img = _statue_body(seed)
    for y in range(4, SIZE - 2):
        img.set(SIZE // 2, y, pal("metal", 0) + (255,))
    inset = _statue_inset(13)
    for x in range(inset + 2, SIZE - inset - 2):
        img.set(x, 13, pal("metal", 0) + (255,))
    return img


def warden_statue_top(seed=31):
    """The crown of the head: a plain plate, rimmed."""
    img = Image(SIZE, SIZE)
    for y in range(SIZE):
        for x in range(SIZE):
            rim = min(x, y, SIZE - 1 - x, SIZE - 1 - y)
            if rim < 2:
                img.set(x, y, pal("stone", 0) + (255,))
            elif rim == 2:
                img.set(x, y, pal("metal", 1) + (255,))
            else:
                img.set(x, y, pal("metal", 2) + (255,))
    return img


ASSETS = {
    (BLOCK, "shrine_stone"): shrine_stone,
    (BLOCK, "shrine_stone_carved"): shrine_stone_carved,
    (BLOCK, "stele_side"): stele_side,
    (BLOCK, "stele_top"): stele_top,
    (BLOCK, "warden_statue_front"): warden_statue_front,
    (BLOCK, "warden_statue_front_woken"): warden_statue_front_woken,
    (BLOCK, "warden_statue_side"): warden_statue_side,
    (BLOCK, "warden_statue_top"): warden_statue_top,
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
