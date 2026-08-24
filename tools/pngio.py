"""Minimal dependency-free PNG read/write.

Pillow is not assumed. Every texture in this project is generated, checked and
diffed by scripts, and a pipeline that needs a pip install to run is a pipeline
that stops running. This module is deliberately small: 8-bit RGB/RGBA/grey/
palette in, 8-bit RGB/RGBA out, plus nearest-neighbour scaling for review.

Anything here that grows a third caller belongs in a real function, not copied.
"""

import struct
import zlib

_CHANNELS = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}


class Image:
    """8-bit RGBA pixel buffer. `px` is a flat bytearray, 4 bytes per pixel."""

    __slots__ = ("w", "h", "px")

    def __init__(self, w, h, px=None, fill=(0, 0, 0, 0)):
        self.w, self.h = w, h
        self.px = bytearray(px) if px is not None else bytearray(bytes(fill) * (w * h))

    def get(self, x, y):
        i = (y * self.w + x) * 4
        return tuple(self.px[i:i + 4])

    def set(self, x, y, rgba):
        if not (0 <= x < self.w and 0 <= y < self.h):
            raise IndexError(f"({x},{y}) outside {self.w}x{self.h}")
        i = (y * self.w + x) * 4
        self.px[i:i + 4] = bytes(rgba if len(rgba) == 4 else tuple(rgba) + (255,))

    def colours(self, opaque_only=True):
        """Set of (r,g,b) actually present. Used by the palette checker."""
        out = set()
        for i in range(0, len(self.px), 4):
            r, g, b, a = self.px[i:i + 4]
            if opaque_only and a == 0:
                continue
            out.add((r, g, b))
        return out

    def scaled(self, factor):
        """Nearest-neighbour upscale. For looking at 16x16 art, never for assets."""
        out = Image(self.w * factor, self.h * factor)
        for y in range(out.h):
            row = (y // factor) * self.w
            for x in range(out.w):
                i = (row + x // factor) * 4
                j = (y * out.w + x) * 4
                out.px[j:j + 4] = self.px[i:i + 4]
        return out


def _unfilter(raw, w, h, ch):
    stride = w * ch
    out = bytearray()
    prev = bytearray(stride)
    pos = 0
    for _ in range(h):
        f = raw[pos]
        pos += 1
        line = bytearray(raw[pos:pos + stride])
        pos += stride
        if f == 1:
            for x in range(ch, stride):
                line[x] = (line[x] + line[x - ch]) & 255
        elif f == 2:
            for x in range(stride):
                line[x] = (line[x] + prev[x]) & 255
        elif f == 3:
            for x in range(stride):
                a = line[x - ch] if x >= ch else 0
                line[x] = (line[x] + ((a + prev[x]) >> 1)) & 255
        elif f == 4:
            for x in range(stride):
                a = line[x - ch] if x >= ch else 0
                b = prev[x]
                c = prev[x - ch] if x >= ch else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[x] = (line[x] + pr) & 255
        elif f != 0:
            raise ValueError(f"unknown PNG filter {f}")
        out += line
        prev = line
    return out


def read(path):
    with open(path, "rb") as fh:
        b = fh.read()
    if b[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(f"{path} is not a PNG")
    i, idat, plte, trns = 8, bytearray(), None, None
    w = h = bd = ct = None
    while i < len(b):
        ln = struct.unpack(">I", b[i:i + 4])[0]
        typ = b[i + 4:i + 8]
        data = b[i + 8:i + 8 + ln]
        if typ == b"IHDR":
            w, h, bd, ct, _, _, il = struct.unpack(">IIBBBBB", data[:13])
            if bd != 8:
                raise ValueError(f"{path}: only 8-bit supported, got {bd}")
            if il:
                raise ValueError(f"{path}: interlaced PNG unsupported")
        elif typ == b"PLTE":
            plte = data
        elif typ == b"tRNS":
            trns = data
        elif typ == b"IDAT":
            idat += data
        elif typ == b"IEND":
            break
        i += 12 + ln
    ch = _CHANNELS[ct]
    raw = _unfilter(zlib.decompress(bytes(idat)), w, h, ch)
    img = Image(w, h)
    for n in range(w * h):
        s = raw[n * ch:(n + 1) * ch]
        if ct == 0:
            v = s[0]; rgba = (v, v, v, 255)
        elif ct == 2:
            rgba = (s[0], s[1], s[2], 255)
        elif ct == 3:
            k = s[0]
            rgba = (plte[k * 3], plte[k * 3 + 1], plte[k * 3 + 2],
                    trns[k] if trns and k < len(trns) else 255)
        elif ct == 4:
            v = s[0]; rgba = (v, v, v, s[1])
        else:
            rgba = (s[0], s[1], s[2], s[3])
        img.px[n * 4:n * 4 + 4] = bytes(rgba)
    return img


def write(path, img, rgba=True):
    ch, ct = (4, 6) if rgba else (3, 2)
    stride = img.w * ch
    raw = bytearray()
    for y in range(img.h):
        raw.append(0)                       # filter 0; art this small does not need more
        row = img.px[y * img.w * 4:(y + 1) * img.w * 4]
        if rgba:
            raw += row
        else:
            for x in range(img.w):
                raw += row[x * 4:x * 4 + 3]
    def chunk(typ, data):
        return (struct.pack(">I", len(data)) + typ + data
                + struct.pack(">I", zlib.crc32(typ + data) & 0xFFFFFFFF))
    hdr = struct.pack(">IIBBBBB", img.w, img.h, 8, ct, 0, 0, 0)
    with open(path, "wb") as fh:
        fh.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", hdr)
                 + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b""))
