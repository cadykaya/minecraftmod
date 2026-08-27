"""Look at an entity model assembled, before it ever reaches a game.

docs/MODELS.md is unambiguous about how mob design fails: silhouette is the whole
design, the front view is the one that lies, and you judge in rotation. None of
that is possible from a texture sheet -- an unwrapped net tells you nothing about
what the boxes look like stacked, and it is exactly the view a texture editor gives
you. DOWNTIME lost a character to this.

So this renders the spec in tools/entity_specs.py directly: orthographic, one ray
per pixel against the box list, sampling the real texture through Minecraft's own
unwrap. What comes out is what the model will look like, because it is built from
the same numbers the game gets.

    python3 tools/entity_view.py warden
    python3 tools/entity_view.py warden --scale 6 --out /tmp/w.png
    python3 tools/entity_view.py warden --silhouette     # the test that matters

Ray casting rather than rasterising on purpose: six boxes and 60k pixels is nothing,
and a slab test that is wrong produces obvious garbage, while a scanline rasteriser
that is subtly wrong produces something that looks plausible and is off by a pixel.
"""
import argparse
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from entity_specs import SPECS            # noqa: E402
from pngio import Image, read, write      # noqa: E402

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# Flat shading per face normal. Minecraft does the same thing, and without it a
# three-quarter view of grey boxes is an unreadable grey blob.
FACE_SHADE = {
    "up": 1.00, "north": 0.86, "south": 0.86,
    "east": 0.70, "west": 0.70, "down": 0.55,
}
BACKDROP = (150, 150, 150, 255)


def _boxes(spec):
    """Flatten the part tree into absolute-space boxes carrying their net."""
    out = []
    for part in spec.parts:
        ox, oy, oz = spec.origin_of(part)
        for b in part.boxes:
            x, y, z = b.origin
            w, h, d = b.size
            out.append({
                "lo": (ox + x, oy + y, oz + z),
                "hi": (ox + x + w, oy + y + h, oz + z + d),
                "size": (w, h, d),
                "faces": b.faces(),
                "mirror": b.mirror,
            })
    return out


def _hit(box, origin, direction):
    """Slab test. Returns (distance, axis, sign) of the nearest entry face."""
    tmin, tmax = -1e9, 1e9
    axis, sign = 0, 1
    for i in range(3):
        o, dd = origin[i], direction[i]
        lo, hi = box["lo"][i], box["hi"][i]
        if abs(dd) < 1e-9:
            if o < lo or o > hi:
                return None
            continue
        t1, t2 = (lo - o) / dd, (hi - o) / dd
        s = 1
        if t1 > t2:
            t1, t2 = t2, t1
            s = -1
        if t1 > tmin:
            tmin, axis, sign = t1, i, s
        tmax = min(tmax, t2)
        if tmin > tmax:
            return None
    return (tmin, axis, sign) if tmax >= 0 else None


def _sample(box, point, axis, sign, tex):
    """Which texel of the sheet this point on this face shows."""
    lo, hi = box["lo"], box["hi"]
    w, h, d = box["size"]
    # u, v run across the face in the same order the net does.
    if axis == 1:                                   # a horizontal face
        name = "up" if sign > 0 else "down"
        fu = (point[0] - lo[0]) / w
        fv = (point[2] - lo[2]) / d
        if name == "down":
            fv = 1.0 - fv
    elif axis == 2:                                 # facing the camera or away
        name = "north" if sign > 0 else "south"
        fu = (point[0] - lo[0]) / w
        if name == "south":
            fu = 1.0 - fu
        fv = (point[1] - lo[1]) / h
    else:                                           # the sides
        name = "east" if sign > 0 else "west"
        fu = (point[2] - lo[2]) / d
        if name == "west":
            fu = 1.0 - fu
        fv = (point[1] - lo[1]) / h
    if box["mirror"] and name in ("north", "south"):
        fu = 1.0 - fu
    if box["mirror"] and name in ("east", "west"):
        name = "west" if name == "east" else "east"

    rx, ry, rw, rh = box["faces"][name]
    tx = min(rw - 1, max(0, int(fu * rw)))
    ty = min(rh - 1, max(0, int(fv * rh)))
    px = tex.get(rx + tx, ry + ty)
    return px, FACE_SHADE[name]


def render(spec, yaw_deg, size=192, silhouette=False):
    """One orthographic view, yaw in degrees (0 = facing the camera)."""
    tex = read(os.path.join(
        REPO, "src/main/resources/assets/interregnum/textures/entity",
        spec.texture + ".png"))
    boxes = _boxes(spec)

    # Frame the whole model with a small margin, so proportion is not distorted by
    # the crop -- the point of the exercise is to judge proportion.
    xs = [b["lo"][0] for b in boxes] + [b["hi"][0] for b in boxes]
    ys = [b["lo"][1] for b in boxes] + [b["hi"][1] for b in boxes]
    zs = [b["lo"][2] for b in boxes] + [b["hi"][2] for b in boxes]
    cx, cy, cz = (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2, (min(zs) + max(zs)) / 2
    span = max(max(xs) - min(xs), max(ys) - min(ys), max(zs) - min(zs)) * 1.5
    if span <= 0:
        span = 1.0

    yaw = math.radians(yaw_deg)
    cos, sin = math.cos(yaw), math.sin(yaw)
    img = Image(size, size)
    for py in range(size):
        # +y is DOWN in model space and down the image too, so no flip here.
        wy = cy + (py / size - 0.5) * span
        for px in range(size):
            wx = (px / size - 0.5) * span
            # Camera on -z looking toward +z, orbited by yaw about the model's
            # vertical axis: forward is (sin, 0, cos) and the screen's right is
            # perpendicular to it. Stepping the ORIGIN along forward (rather than
            # only rotating the direction) is the part that is easy to drop, and
            # dropping it aims every ray away from the model -- an empty frame.
            direction = (sin, 0.0, cos)
            right = (cos, 0.0, -sin)
            origin = (cx - direction[0] * span + right[0] * wx,
                      wy,
                      cz - direction[2] * span + right[2] * wx)

            best = None
            for b in boxes:
                hit = _hit(b, origin, direction)
                if hit and (best is None or hit[0] < best[0][0]):
                    best = (hit, b)
            if best is None:
                img.set(px, py, BACKDROP)
                continue
            (t, axis, sign), b = best
            if silhouette:
                img.set(px, py, (0, 0, 0, 255))
                continue
            point = tuple(origin[i] + direction[i] * t for i in range(3))
            (r, g, bl, a), shade = _sample(b, point, axis, sign, tex)
            if a == 0:
                # A hole in the net. Loud magenta, because the alternative is a
                # transparent patch that reads as "shadow" and ships.
                img.set(px, py, (255, 0, 255, 255))
            else:
                img.set(px, py, (int(r * shade), int(g * shade), int(bl * shade), 255))
    return img


def contact_sheet(spec, size=192, silhouette=False):
    """Front, three-quarter, side, rear -- the four docs/MODELS.md asks for."""
    views = [render(spec, a, size, silhouette) for a in (0, 45, 90, 180)]
    sheet = Image(size * len(views) + 4 * (len(views) - 1), size)
    for y in range(sheet.h):
        for x in range(sheet.w):
            sheet.set(x, y, (40, 40, 40, 255))
    for i, v in enumerate(views):
        x0 = i * (size + 4)
        for y in range(size):
            for x in range(size):
                sheet.set(x0 + x, y, v.get(x, y))
    return sheet


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("entity", choices=sorted(SPECS))
    ap.add_argument("--scale", type=int, default=1)
    ap.add_argument("--size", type=int, default=192)
    ap.add_argument("--silhouette", action="store_true",
                    help="black on grey: the only test that says whether it reads")
    ap.add_argument("--out")
    args = ap.parse_args()

    spec = SPECS[args.entity]
    sheet = contact_sheet(spec, args.size, args.silhouette)
    if args.scale > 1:
        sheet = sheet.scaled(args.scale)
    out = args.out or os.path.join(
        REPO, f"{args.entity}_view{'_silhouette' if args.silhouette else ''}.png")
    write(out, sheet)
    print(f"wrote {out}  ({sheet.w}x{sheet.h})  front / three-quarter / side / rear")


if __name__ == "__main__":
    main()
