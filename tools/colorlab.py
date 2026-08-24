"""sRGB <-> CIE L* conversion, shared by palette_build and palette_check.

L* is returned normalised to 0..1 rather than 0..100, because every threshold in
this project is written that way and a mixed convention is a bug waiting to be
typed. See docs/PALETTE.md for why L* and not relative luminance.
"""

_D65_Yn = 1.0
_DELTA = 6.0 / 29.0


def hex_to_rgb(h):
    """'#A77844' or 'A77844' -> (0.655, 0.470, 0.267) floats in 0..1."""
    h = h.lstrip("#")
    if len(h) != 6:
        raise ValueError(f"expected 6 hex digits, got {h!r}")
    return tuple(int(h[i:i + 2], 16) / 255.0 for i in (0, 2, 4))


def rgb_to_hex(rgb):
    return "#" + "".join(f"{max(0, min(255, round(c * 255))):02X}" for c in rgb)


def _linearise(c):
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def relative_luminance(rgb):
    r, g, b = (_linearise(c) for c in rgb)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def lstar(rgb):
    """Perceptual lightness, 0..1. Accepts an (r,g,b) tuple or a hex string."""
    if isinstance(rgb, str):
        rgb = hex_to_rgb(rgb)
    y = relative_luminance(rgb) / _D65_Yn
    f = y ** (1.0 / 3.0) if y > _DELTA ** 3 else y / (3 * _DELTA ** 2) + 4.0 / 29.0
    return (116.0 * f - 16.0) / 100.0


def rgb_to_hsv(rgb):
    r, g, b = rgb
    mx, mn = max(rgb), min(rgb)
    d = mx - mn
    if d == 0:
        h = 0.0
    elif mx == r:
        h = ((g - b) / d) % 6
    elif mx == g:
        h = (b - r) / d + 2
    else:
        h = (r - g) / d + 4
    return h * 60.0, (0.0 if mx == 0 else d / mx), mx


def hsv_to_rgb(h, s, v):
    h = h % 360.0
    c = v * s
    x = c * (1 - abs((h / 60.0) % 2 - 1))
    m = v - c
    i = int(h // 60) % 6
    r, g, b = [(c, x, 0), (x, c, 0), (0, c, x), (0, x, c), (x, 0, c), (c, 0, x)][i]
    return r + m, g + m, b + m
