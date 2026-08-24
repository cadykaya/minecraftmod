"""Look at a texture the way the game shows it, not the way a file browser does.

Judging a block texture from one tile is the same mistake as judging a character
from a front view: it is the view everything gets tuned in, and it is the one that
lies. See docs/ARTSTYLE.md section 4b.

    python3 tools/texview.py block.png                  # 8x8 tiled, 6x scale
    python3 tools/texview.py block.png --tile 4 --scale 12
    python3 tools/texview.py item.png --icon            # what a 16px hotbar shows
    python3 tools/texview.py a.png b.png --sheet out.png
    python3 tools/texview.py block.png --grey           # the desaturation test

Output goes to <name>_view.png next to the source unless --out is given.
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pngio import Image, read, write     # noqa: E402
from colorlab import lstar               # noqa: E402


def tiled(img, n):
    out = Image(img.w * n, img.h * n)
    for ty in range(n):
        for tx in range(n):
            for y in range(img.h):
                src = y * img.w * 4
                dst = ((ty * img.h + y) * out.w + tx * img.w) * 4
                out.px[dst:dst + img.w * 4] = img.px[src:src + img.w * 4]
    return out


def greyscale(img):
    """Desaturate by L*, not by naive average -- the average lies in the shadows."""
    out = Image(img.w, img.h)
    for i in range(0, len(img.px), 4):
        r, g, b, a = img.px[i:i + 4]
        v = round(lstar((r / 255, g / 255, b / 255)) * 255)
        v = max(0, min(255, v))
        out.px[i:i + 4] = bytes((v, v, v, a))
    return out


def checkerboard(img, cell=8, light=(200, 200, 200), dark=(160, 160, 160)):
    """Composite over a checker so transparent regions are visible, not guessed."""
    out = Image(img.w, img.h)
    for y in range(img.h):
        for x in range(img.w):
            r, g, b, a = img.get(x, y)
            bg = light if ((x // cell) + (y // cell)) % 2 == 0 else dark
            f = a / 255
            out.set(x, y, tuple(round(c * f + d * (1 - f)) for c, d in zip((r, g, b), bg)) + (255,))
    return out


def seam_report(img):
    """Does it wrap? Compare opposite edges. A wrapping texture has continuity."""
    dh = sum(abs(img.get(0, y)[i] - img.get(img.w - 1, y)[i])
             for y in range(img.h) for i in range(3)) / (img.h * 3)
    dv = sum(abs(img.get(x, 0)[i] - img.get(x, img.h - 1)[i])
             for x in range(img.w) for i in range(3)) / (img.w * 3)
    return dh, dv


def stats(img, label):
    cols = img.colours()
    ls = sorted(lstar((c[0] / 255, c[1] / 255, c[2] / 255)) for c in cols)
    dh, dv = seam_report(img)
    print(f"{label}: {img.w}x{img.h}  {len(cols)} colours  "
          f"L* {ls[0]:.3f}..{ls[-1]:.3f} (range {ls[-1]-ls[0]:.3f})")
    print(f"  edge delta  horizontal {dh:6.1f}   vertical {dv:6.1f}   "
          f"(lower = wraps better; these are mean 0-255 per channel)")
    if len(cols) > 8:
        print(f"  note: {len(cols)} colours is high for 16x16 -- see ARTSTYLE.md section 2")


def main(argv):
    args = [a for a in argv if not a.startswith("--")]
    def opt(name, default=None, cast=str):
        if name in argv:
            return cast(argv[argv.index(name) + 1])
        return default

    if not args:
        print(__doc__)
        return 2

    tile = opt("--tile", 1 if "--icon" in argv else 8, int)
    scale = opt("--scale", 6, int)
    out_path = opt("--out")

    if "--sheet" in argv:
        out_path = opt("--sheet")
        imgs = [read(p) for p in args]
        for p, im in zip(args, imgs):
            stats(im, os.path.basename(p))
        pad, cell = 4, max(i.w for i in imgs) * scale
        sheet = Image(len(imgs) * (cell + pad) + pad, cell + 2 * pad, fill=(30, 30, 34, 255))
        for k, im in enumerate(imgs):
            s = checkerboard(im).scaled(scale)
            ox = pad + k * (cell + pad)
            for y in range(s.h):
                d = ((pad + y) * sheet.w + ox) * 4
                sheet.px[d:d + s.w * 4] = s.px[y * s.w * 4:(y + 1) * s.w * 4]
        write(out_path, sheet)
        print(f"\nwrote {out_path}  ({sheet.w}x{sheet.h})")
        return 0

    src = args[0]
    img = read(src)
    stats(img, os.path.basename(src))
    view = greyscale(img) if "--grey" in argv else img
    view = checkerboard(view)
    if tile > 1:
        view = tiled(view, tile)
    view = view.scaled(scale)
    if out_path is None:
        base, _ = os.path.splitext(src)
        suffix = "_grey" if "--grey" in argv else "_view"
        out_path = base + suffix + ".png"
    write(out_path, view)
    print(f"wrote {out_path}  ({view.w}x{view.h}, {tile}x{tile} tiled at {scale}x)")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
