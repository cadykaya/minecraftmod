"""What a block would look like assembled, since this container cannot run a client.

The review bench (`texview.py`) shows a texture tiled flat. That answers "does this
paint read at 16px and does it tile", which is the question while you are painting it.
It does not answer the question anybody actually asks, which is *what does the block
look like*: a flat sheet of gravel and a flat sheet of shrine stone can both look fine
and still be indistinguishable once they are cubes in a world.

So this assembles the cube. It reads the block's real model JSON -- the same file the
game reads -- resolves its textures the same way, and composites the three visible
faces in isometric with **vanilla's own directional shading**: top 1.0, south 0.8,
east 0.6. Those multipliers are why a Minecraft cube reads as a solid object rather
than as three squares, and a mock without them lies in the flattering direction.

What this is NOT, stated plainly so nobody quotes it as more than it is:

  * no lighting, no ambient occlusion, no shadows, no sky tint;
  * no perspective -- true isometric, where the game uses a perspective camera;
  * nothing about how a block looks NEXT to vanilla blocks in a real biome;
  * nothing at all about animation, particles, or item-in-hand rendering.

It is a geometry-and-paint check. **The first person to open a client will still see
things this cannot show**, and that limitation is in HANDOFF for a reason.

    python3 tools/blockview.py                    # every registered block, one sheet
    python3 tools/blockview.py ferry_keel         # just this one, larger
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from paintkit import Image                          # noqa: E402
from pngio import read, write                       # noqa: E402

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
A = os.path.join(REPO, "src/main/resources/assets/interregnum")
N = 16                                              # texels per block edge

# Vanilla's directional shading, read off the game's own constants rather than eyeballed.
# The down face is 0.5 and never visible from this angle.
SHADE = {"top": 1.0, "south": 0.8, "east": 0.6}


def resolve(model_id):
    """Follow a model's parent chain and return its texture map, as the game does."""
    ns, _, path = model_id.partition(":")
    if ns != "interregnum":
        return {}
    p = os.path.join(A, "models", path + ".json")
    if not os.path.exists(p):
        return {}
    with open(p) as fh:
        m = json.load(fh)
    out = dict(resolve(m.get("parent", "")))
    out.update({k: v for k, v in m.get("textures", {}).items() if isinstance(v, str)})
    out["__parent__"] = m.get("parent", "")
    return out


def faces_for(name):
    """(top, side) texture ids for a block, from its blockstate's first model.

    Handles the two parents this mod uses: `cube_all` (one texture everywhere) and
    `cube_column` (end/side). Anything else returns None rather than guessing, because
    a mock that quietly substitutes the wrong face is worse than no mock.
    """
    bs = os.path.join(A, "blockstates", name + ".json")
    if not os.path.exists(bs):
        return None
    with open(bs) as fh:
        state = json.load(fh)
    variants = state.get("variants", {})
    if not variants:
        return None
    first = next(iter(variants.values()))
    model_id = (first[0] if isinstance(first, list) else first)["model"]
    tex = resolve(model_id)
    parent = tex.get("__parent__", "")
    if "cube_all" in parent and "all" in tex:
        return tex["all"], tex["all"]
    if "cube_column" in parent and "end" in tex and "side" in tex:
        return tex["end"], tex["side"]
    return None


def load(tex_id):
    ns, _, path = tex_id.partition(":")
    p = os.path.join(A, "textures", path + ".png")
    return read(p) if os.path.exists(p) else None


def shade(rgba, factor):
    r, g, b, a = rgba
    return (int(r * factor), int(g * factor), int(b * factor), a)


def cube(top_img, side_img, s):
    """One block, isometric, s pixels per texel along each screen axis.

    Output pixels are inverse-mapped into each face's (u, v) rather than each texel
    being drawn as a parallelogram: the parallelogram approach leaves seams between
    texels at non-integer scales, and seams in a review render read as texture detail
    that is not there.
    """
    w = int(2 * N * s)
    h = int(N * s + N * s)                          # cube height + the two half-diamonds
    img = Image(w, h)
    ox, oy = w / 2.0, 0.0                           # screen origin: the top vertex

    def project(x, y, z):
        return (ox + (x - z) * s, oy + (x + z) * (s / 2.0) + (N - y) * s)

    # Each face: its screen origin and the two screen-space edge vectors of its
    # (u, v) parameterisation, plus how to read a texel out of the source image.
    def plane(p0, pu, pv):
        ax, ay = pu[0] - p0[0], pu[1] - p0[1]
        bx, by = pv[0] - p0[0], pv[1] - p0[1]
        det = ax * by - ay * bx
        return p0, ax, ay, bx, by, det

    top = plane(project(0, N, 0), project(N, N, 0), project(0, N, N))
    south = plane(project(0, N, N), project(N, N, N), project(0, 0, N))
    east = plane(project(N, N, N), project(N, N, 0), project(N, 0, N))

    for name, (p0, ax, ay, bx, by, det), src in (
            ("top", top, top_img), ("south", south, side_img), ("east", east, side_img)):
        if src is None or det == 0:
            continue
        f = SHADE[name]
        for py in range(h):
            for px in range(w):
                dx, dy = px + 0.5 - p0[0], py + 0.5 - p0[1]
                u = (dx * by - dy * bx) / det
                v = (ax * dy - ay * dx) / det
                if 0.0 <= u < 1.0 and 0.0 <= v < 1.0:
                    tx = min(N - 1, int(u * N))
                    ty = min(N - 1, int(v * N))
                    rgba = src.get(tx, ty)
                    if rgba[3]:
                        img.set(px, py, shade(rgba, f))
    return img


def blit(dst, src, x0, y0):
    for y in range(src.h):
        for x in range(src.w):
            rgba = src.get(x, y)
            if rgba[3]:
                dst.set(x0 + x, y0 + y, rgba)


def blocks():
    d = os.path.join(A, "blockstates")
    return sorted(f[:-5] for f in os.listdir(d) if f.endswith(".json"))


def main(argv):
    wanted = argv[1:] or blocks()
    s = 8.0 if len(wanted) == 1 else 4.0
    made = []
    for name in wanted:
        pair = faces_for(name)
        if pair is None:
            print(f"skip {name}: model parent is not cube_all or cube_column")
            continue
        top, side = load(pair[0]), load(pair[1])
        if top is None or side is None:
            print(f"skip {name}: a texture is missing")
            continue
        made.append((name, cube(top, side, s)))

    if not made:
        print("nothing to draw")
        return 1

    pad = int(6 * s)
    cw = max(c.w for _, c in made) + pad
    ch = max(c.h for _, c in made) + pad
    cols = min(len(made), 4)
    rows = (len(made) + cols - 1) // cols
    # A mid-grey ground rather than transparent: these are dark blocks, and a
    # checkerboard or white page flatters them in a way a lit world will not.
    sheet = Image(cols * cw, rows * ch, fill=(70, 74, 78, 255))
    for i, (_, c) in enumerate(made):
        col, row = i % cols, i // cols
        blit(sheet, c, col * cw + (cw - c.w) // 2, row * ch + (ch - c.h) // 2)

    out = os.path.join(REPO, "build", "blockview.png")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    write(out, sheet)
    print(f"wrote {os.path.relpath(out, REPO)}  ({', '.join(n for n, _ in made)})")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
