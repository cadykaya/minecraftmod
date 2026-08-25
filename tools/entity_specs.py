"""Entity geometry, once, for the three things that need it.

A mob's box list is needed in three places and they fail silently against each
other: the TEXTURE painter has to know where each box's net lands, the JAVA model
has to declare the same sizes at the same UV offsets, and anyone judging the design
has to be able to look at it assembled. Keep those in three hand-maintained copies
and the first resize garbles the skin with nothing raising an error anywhere -- the
game does not check that a net fits, it just samples whatever pixels are there.

So the spec lives here, and:

    tools/build_textures.py   paints the nets from it
    tools/gen_resources.py    writes WardenModel.java from it
    tools/entity_view.py      renders it, so it can be judged in rotation

Coordinates are Minecraft entity-model units: 16 to a block, origin at the entity's
feet, and **y increases downward**, so y=24 is the ground and the top of a tall mob
is a negative number. Every vanilla model reads this way; fighting it is worse.
"""


def net_size(w, h, d):
    """The width and height of a box's unwrapped net on the sheet."""
    return 2 * (w + d), h + d


class Box:
    """One cube: where its net sits on the sheet, and where the box sits in space."""

    def __init__(self, uv, origin, size, mirror=False):
        self.uv = tuple(uv)
        self.origin = tuple(float(v) for v in origin)
        self.size = tuple(size)
        self.mirror = mirror

    @property
    def net(self):
        return net_size(*self.size)

    def faces(self):
        """The six face rectangles of this box's net, in Minecraft's unwrap order.

        Top and bottom sit above the belt of side faces; the side order left to
        right is right, front, left, back. Getting this wrong produces a model that
        looks *almost* right, which is the hardest kind to debug.
        """
        u, v = self.uv
        w, h, d = self.size
        return {
            "up":     (u + d, v, w, d),
            "down":   (u + d + w, v, w, d),
            "east":   (u, v + d, d, h),
            "north":  (u + d, v + d, w, h),
            "west":   (u + d + w, v + d, d, h),
            "south":  (u + d + w + d, v + d, w, h),
        }


class Part:
    """A named, pivoted group of boxes, optionally hung off another part."""

    def __init__(self, name, pivot, boxes, parent=None):
        self.name = name
        self.pivot = tuple(float(v) for v in pivot)
        self.boxes = boxes
        self.parent = parent


class Spec:
    def __init__(self, texture, sheet, parts):
        self.texture = texture
        self.sheet = tuple(sheet)
        self.parts = parts
        self._validate()

    def part(self, name):
        for p in self.parts:
            if p.name == name:
                return p
        raise KeyError(name)

    def origin_of(self, part):
        """Absolute position of a part's pivot, walking up through its parents."""
        x, y, z = part.pivot
        while part.parent is not None:
            part = self.part(part.parent)
            x += part.pivot[0]
            y += part.pivot[1]
            z += part.pivot[2]
        return x, y, z

    def _validate(self):
        """Refuse to exist if two nets overlap or one runs off the sheet.

        This is the check that makes the shared spec worth having. Overlapping nets
        do not crash: they render, wrongly, with one box wearing a slice of
        another's paint -- and at 16 blocks away that reads as "the texture is a bit
        noisy" rather than as a bug.
        """
        sw, sh = self.sheet
        claimed = {}
        for part in self.parts:
            for i, box in enumerate(part.boxes):
                nw, nh = box.net
                u, v = box.uv
                where = f"{part.name}[{i}]"
                if u + nw > sw or v + nh > sh:
                    raise ValueError(
                        f"{where}: net {nw}x{nh} at ({u},{v}) runs off the {sw}x{sh} sheet")
                for y in range(v, v + nh):
                    for x in range(u, u + nw):
                        if box.mirror and (x, y) in claimed:
                            continue          # a mirrored box shares its twin's net
                        prev = claimed.get((x, y))
                        if prev is not None and prev != where:
                            raise ValueError(
                                f"{where}: net overlaps {prev} at ({x},{y})")
                        claimed[(x, y)] = where
        self.used = len(claimed)


# --- the Warden --------------------------------------------------------------
#
# A tall narrow figure under a wide flat mantle. The brim is the silhouette: 13
# units across on a 7-unit head, so the shape reads as "official" from far enough
# away that no paint detail survives. docs/MODELS.md: silhouette is the whole
# design, and the one facing cue -- the ember visor slots -- is the only warm mark
# anywhere on the sheet.
#
# The asymmetry that keeps this from reading as a placeholder is the rank plate,
# which exists on the left shoulder only. The arm net is shared and mirrored, as
# vanilla does for every humanoid; mirroring an arm is not the mirrored-mob problem.

WARDEN = Spec(
    texture="warden",
    sheet=(64, 64),
    parts=[
        # The robe is TWO boxes, and that is the whole difference between a
        # character and a stack of cubes. A single skirt box rendered as a bollard
        # in profile -- the first assembled view showed a featureless post with a
        # hat, which is exactly the "judge it with the head hidden" failure
        # docs/MODELS.md warns about. Stepping 9 wide to 12 wide gives the figure a
        # base, and the step is visible from every angle including directly behind.
        Part("robe_upper", (0, 8, 0), [
            Box(uv=(0, 52), origin=(-4.5, 0, -3), size=(9, 6, 6)),
        ]),
        Part("robe_lower", (0, 14, 0), [
            Box(uv=(0, 0), origin=(-6, 0, -4.5), size=(12, 10, 9)),
        ]),
        Part("torso", (0, 8, 0), [
            Box(uv=(0, 33), origin=(-4, -13, -3), size=(8, 13, 6)),
        ]),
        # Head and brim rotate together: the mantle is part of the head, so it
        # swings when the Warden looks at you. That motion is most of the menace.
        Part("head", (0, -5, 0), [
            Box(uv=(28, 33), origin=(-3.5, -8, -3.5), size=(7, 8, 7)),
            Box(uv=(0, 19), origin=(-6.5, -10, -6), size=(13, 2, 12)),
        ]),
        Part("right_arm", (-5.5, -4, 0), [
            Box(uv=(42, 0), origin=(-1.5, 0, -1.5), size=(3, 11, 3)),
        ]),
        Part("left_arm", (5.5, -4, 0), [
            Box(uv=(42, 0), origin=(-1.5, 0, -1.5), size=(3, 11, 3), mirror=True),
        ]),
        # Sits over the left arm rather than beside it: fully enclosing, so no two
        # faces are coplanar and nothing z-fights.
        Part("pauldron", (0, -2, 0), [
            Box(uv=(30, 52), origin=(-2.5, 0, -3), size=(5, 4, 6)),
        ], parent="left_arm"),
    ],
)

SPECS = {"warden": WARDEN}
