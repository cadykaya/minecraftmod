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
from entity_specs import WARDEN, KEEPER

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


# --- the walking Warden ------------------------------------------------------
#
# An ENTITY sheet, not a block texture: one 128x128 atlas holding the unwrapped net
# of every box in the model. Nothing here tiles, so none of the wrap-aware machinery
# above applies -- but the palette law still does, and so does the rule that made
# the statue work: the eyes are the only warm pixels on an entirely cool figure.
#
# The box sizes below are duplicated in WardenModel.java, and that is a real seam.
# tools/entity_sheet_check.py exists to make it a checked one: if a box moves in the
# model and not here, its net lands on somebody else's pixels and the check says so.

ENTITY = os.path.join(REPO, "src/main/resources/assets/interregnum/textures/entity")


def _plate(img, rect, base=2, rim=1, family="metal"):
    """A panel: flat interior, one pixel of darker rim.

    The rim is what stops a box reading as a flat sticker at distance -- every
    vanilla mob does some version of this, and without it the silhouette is the
    only thing carrying the model.
    """
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            edge = x in (x0, x0 + w - 1) or y in (y0, y0 + h - 1)
            img.set(x, y, pal(family, rim if edge else base) + (255,))


def _row(img, rect, ry, colour, margin=1):
    """A horizontal seam across a face, inset from its edges."""
    x0, y0, w, _ = rect
    for x in range(x0 + margin, x0 + w - margin):
        img.set(x, y0 + ry, colour + (255,))


def warden_entity(seed=31):
    """The Warden on its feet.

    Same species as the statue and it has to read that way instantly, so it
    inherits the statue's two decisions wholesale: cold worked metal everywhere
    (HELD -- this was maintained, and that is what stopped), and a recessed dark
    visor whose two ember slots are the ONLY warm pixels on the entire sheet.

    docs/MODELS.md: one dominant facing cue, and everything that competes with it
    gets cut. The rank plate on the left shoulder is therefore metal and not brass,
    even though brass is what an insignia wants to be -- a second warm mark on the
    body would split the read at exactly the distance the cue has to survive.
    """
    spec = WARDEN
    img = Image(*spec.sheet)           # transparent; only the nets are painted
    box = {}                           # part name -> face rects, for the detail pass
    for part in spec.parts:
        for i, b in enumerate(part.boxes):
            key = part.name if i == 0 else f"{part.name}:{i}"
            box[key] = b.faces()
            for rect in box[key].values():
                _plate(img, rect)

    # The mantle (the head's second box): darker underside, so the brim reads as an
    # overhang shading the face rather than as a hat sitting on top of it.
    _plate(img, box["head:1"]["down"], base=0, rim=0)

    # The face. Three rows of visor recess across the head's north face, two ember
    # slots in the middle. Clipped inside the plate rim on purpose -- the statue's
    # first version overflowed its face band and read as "a box with ears".
    front = box["head"]["north"]
    for ry in (2, 3, 4):
        _row(img, front, ry, pal("metal", 0), margin=1)
    fx, fy = front[0], front[1]
    for ex in (2, 4):
        img.set(fx + ex, fy + 3, pal("ember", 2) + (255,))
    # A dimmer pixel under each slot: at 16 blocks the eyes are the whole mob, and
    # a single lit pixel disappears into the dark band around it.
    for ex in (2, 4):
        img.set(fx + ex, fy + 4, pal("ember", 0) + (255,))

    # Chest seam and belt, matching the statue's front face.
    _row(img, box["torso"]["north"], 4, pal("metal", 0), margin=2)
    _row(img, box["torso"]["north"], 10, pal("metal", 0), margin=1)
    _row(img, box["torso"]["south"], 4, pal("metal", 0), margin=2)

    # The hem, on every side of the robe's lower step, so the flare reads from any
    # angle -- including from directly behind, which is the view a player following
    # a Warden gets for minutes at a time.
    for face in ("north", "south", "east", "west"):
        _row(img, box["robe_lower"][face], 8, pal("metal", 0), margin=0)

    # The rank plate: brighter than the body it sits on, which is the only way a
    # 5x4 box says "raised" instead of "painted on".
    for rect in box["pauldron"].values():
        _plate(img, rect, base=3, rim=1)

    return img


def shrine_keeper_entity(seed=17):
    """A person, warm, next to a Warden that is not.

    The palette law does the characterisation on its own. The Warden is HELD -- cool
    metal, maintained, and the maintenance is what stopped. The keeper is the other
    side of the same law: earth and bone and wood, WARM, which the palette reserves
    for what is being spent. That is exactly what they are doing. Somebody is still
    reconciling a ledger for a reader who is dead, quarterly, and it is costing them.
    Nobody says so and the colours do not have to.

    No ember anywhere. The ember step is the dead god's, and a living person who
    happens to be sad is not running on the corpse.
    """
    spec = KEEPER
    img = Image(*spec.sheet)
    box = {}
    for part in spec.parts:
        for i, b in enumerate(part.boxes):
            key = part.name if i == 0 else f"{part.name}:{i}"
            box[key] = b.faces()

    # Cloth, not plate: a softer rim than the Warden's, one step rather than two.
    for name in ("robe_lower", "torso", "right_arm"):
        for rect in box[name].values():
            _plate(img, rect, base=1, rim=0, family="earth")
    for rect in box["head"].values():
        _plate(img, rect, base=1, rim=0, family="bone")   # skin, lighter than cloth

    # The hood, in the darkest earth: it is what the face sits inside.
    for rect in box["head:1"].values():
        _plate(img, rect, base=0, rim=0, family="earth")

    # The face. Two dark eyes and nothing else -- no mouth, because at 16 pixels a
    # mouth is one ambiguous smudge that reads as an expression the scene has not
    # earned. The keeper is not sad AT you.
    #
    # Row 5, not row 3. The hood is a box over the head's top half, so eyes painted
    # in the upper rows are geometrically INSIDE it and simply never render -- the
    # first version produced a hooded figure with a blank pale bandage for a face and
    # nothing at all to look back with. Paint has to be told where the geometry is.
    front = box["head"]["north"]
    fx, fy = front[0], front[1]
    for ex in (2, 5):
        img.set(fx + ex, fy + 5, pal("earth", 0) + (255,))

    # The ledger: bone pages, dark board. The one bright thing in the silhouette,
    # which is the point -- it is why they are here.
    for rect in box["ledger"].values():
        _plate(img, rect, base=0, rim=0, family="wood")
    # Page edges along the bottom of every face a player can actually see. On the
    # top face alone they were invisible from every angle in the contact sheet --
    # nobody looks down on a shrine-keeper.
    for face in ("north", "south", "east", "west"):
        rect = box["ledger"][face]
        for x in range(rect[0], rect[0] + rect[2]):
            for dy in (rect[3] - 2, rect[3] - 1):
                img.set(x, rect[1] + dy, pal("bone", 2 if x % 2 else 1) + (255,))

    # A belt, so the robe does not read as one poured shape.
    _row(img, box["torso"]["north"], 8, pal("wood", 0), margin=0)
    _row(img, box["torso"]["south"], 8, pal("wood", 0), margin=0)
    return img


ASSETS = {
    (ENTITY, "warden"): warden_entity,
    (ENTITY, "shrine_keeper"): shrine_keeper_entity,
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
