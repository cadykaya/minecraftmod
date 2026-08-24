# The palette

> **Provenance: PORTED.** Every rule here was paid for in DOWNTIME (`cadykaya/mario-3`),
> where a texture pass shipped **seven ground greens all within 0.16 L\* of each other** —
> not from bad taste, but because the values had been typed in seven places. The machinery
> below exists so that cannot happen again.
>
> One section is new: **vanilla adjacency**. That problem does not exist in a standalone
> game and is arguably the hardest colour problem in modding.

---

## The law

> **`assets/palette.json` is the single source of truth. Nothing hard-codes a colour.**

Not the texture scripts, not the Java, not the block model JSON, not a particle colour, not
a tooltip, not a map colour. If a colour appears anywhere else in this repository as a
literal, that is a bug, and it is the specific bug that produced the seven greens.

The file is **generated**. Do not hand-edit it — `tools/palette_check.py` fails if you do,
and it detects an edit as small as one hex digit.

```sh
python3 tools/palette_build.py     # regenerate (deliberate, manual, rare)
python3 tools/palette_check.py     # enforce (cheap, run constantly, wire into CI)
```

To change the palette, edit `FAMILIES` in `tools/palette_build.py` and re-run the build.
**Regenerating is deliberate on purpose.** A palette that regenerates automatically on
every build is a palette that has quietly stopped being locked.

---

## Values are sRGB hex

Because a palette you have to mentally convert is a palette that drifts. What a colour
picker shows is what the file contains.

---

## The three enforced rules

### 1. Adjacent steps differ by at least **0.12 in CIE L\***

Silhouette and readability are carried by **value** far more than by hue. Two colours the
same lightness are the same colour at play distance, whatever their hue says.

**L\*, not relative luminance, and the distinction is load-bearing.** Most of this palette
lives in the dark half of the range, where linear luminance is wildly non-uniform: a flat
threshold on it would demand enormous gaps in the shadows and wave through mush in the
highlights, forcing the whole palette pale to satisfy a number. L\* is perceptually
uniform, so 0.12 means the same thing at the bottom of a ramp as at the top.

L\* is stored **normalised to 0..1**, not the conventional 0..100. Every threshold in this
project is written that way; a mixed convention is a bug waiting to be typed.

*Current state: every ramp clears the floor with margin — the tightest gap in the shipped
palette is **0.159**.*

### 2. No asset may use more than **five families**

Running out of colours is the constraint doing its job. An asset reaching for a sixth
family is almost always an asset that has not decided what it is.

### 3. Every colour in every texture must exist in `palette.json`

Checked by scanning the actual PNGs. This is the rule that catches the realistic failure —
not a wild colour, but a colour *one digit off* a legal one, which no human eye will ever
spot in a diff. The checker catches `#6E502D` against a legal `#6D4F2D`.

---

## How the ramps are made — not by eye

`tools/palette_build.py` takes **one base colour per family** (used only for its hue and
saturation — its own lightness is discarded) plus **the L\* each step must hit**, and
solves for the rest. Spacing is therefore exact by construction rather than by luck.

The solver works in **HSV**:

- **Darken** by dropping V.
- **Lighten** by pushing V to 1.0 first, and only desaturating once there is no value
  headroom left.

> **Blending toward white instead — the obvious approach — spends saturation first and
> produces pastel mush.** In DOWNTIME it turned a soil colour into `#B9A89C`, which the
> saturated references would have rejected on sight.

---

## The families

Ten families. Each has a **job**, and a family without a job is a colour, not a family — it
will get used wherever it happens to look nice, which is how palettes rot.

| Family | Means | Steps |
|---|---|---|
| `stone` | inert structural mass; the default world material | 4 |
| `earth` | diggable, soft, disturbed ground | 4 |
| `wood` | grown then worked; anything a person shaped by hand | 3 |
| `foliage` | alive and rooted | 4 |
| `metal` | refined, cold, manufactured; holds its shape | 4 |
| `brass` | mechanisms a player can operate | 3 |
| `bone` | pale, dead, dry; the lightest values in the game | 3 |
| `ember` | heat, danger, energy in transit — the loudest colour, reserved for that tell | 3 |
| `void` | the strange thing; wrongness with a rule behind it | 3 |
| `sky` | atmosphere, distance, fog, horizon | 3 |

**These are a starting set, not a finished one.** They are deliberately generic because the
mod's subject is not decided yet. Re-theming is cheap — edit `FAMILIES`, re-run the build —
and is *expected* once we know what we are making.

### What is still missing: the semantic law

DOWNTIME's palette has one sentence that does more work than the whole ramp table:

> **Warm means it is leaving. Cool means it is holding.**

It falls straight out of that game's fiction (gravity is a metered utility; everything cold
and blue-grey is something a person drove into the ground to stop the ground leaving), and
its payoff is that **a player who reads no text still knows which rock will betray them.**

This palette does not have its law yet, because the mod does not have its fiction yet. The
`means` column above is a placeholder for one.

> **OPEN — BLOCKING FOR ANY SERIOUS ART PASS.** Write the semantic law once the mod's
> subject exists. A palette whose families mean "brown things" and "grey things" is a
> colour picker. A palette whose families mean something is a game mechanic the player
> reads without being told.

---

## Vanilla adjacency — the problem DOWNTIME never had

> **Provenance: DOCTRINE.** New to this project. In a standalone game every pixel on screen
> is yours. In a mod, your block sits in a wall next to Mojang's, in a hotbar next to
> Mojang's, and is judged against thirteen years of art nobody here made.

Three failure modes, and mods die of all three:

**1. Invisible.** Your stone variant is within a hair of vanilla stone, so the player cannot
see that anything was added. Value separation solves this — but it must be measured against
*vanilla*, not only within our own ramps.

**2. Clashing.** Your block is separated from vanilla by being *more saturated and brighter
than anything Mojang ships*, which is why so much modded content reads as tacky. Vanilla's
palette is desaturated and mid-valued almost everywhere; the loud outlier always looks
like an import.

**3. Uncanny.** Close enough to vanilla to read as a texture-pack inconsistency rather than
a deliberate addition. The worst of the three, because it reads as a *bug*.

### The rule

> **A new block must differ from its nearest vanilla neighbour by at least 0.12 L\* *or* be
> unmistakably a different hue family — and must not exceed vanilla's saturation ceiling
> for that material class.**

Separate by **value and structure** (the pattern painted into it), not by turning the
saturation up. If your block is only distinguishable because it is louder, it is not
distinguishable, it is intrusive.

### The test

Screenshot your block **in a wall, next to the vanilla block it is closest to**, at normal
play distance. Not in the inventory, not zoomed in, not on its own. Inventory-only review
is the modding equivalent of DOWNTIME's *"judge in rotation, always"* lesson: the flattering
view is the one everything gets tuned in, and it is the one that lies.

> **TODO:** once textures exist, extend `palette_check.py` with a vanilla-neighbour report —
> for each of our textures, name the closest vanilla block by mean L\* and hue, and flag any
> pair closer than the floor. This is mechanisable and should not stay a matter of opinion.

---

## The greyscale test

The cheapest honest check in the whole project, ported directly:

> **Desaturate a screenshot. If the composition falls apart, the palette is not working.**

It takes ten seconds and it catches what staring at a colour picker never will. If two
things that need to be told apart become the same grey, no amount of hue fixes it.

---

## What the checker actually enforces

`tools/palette_check.py`, exits non-zero on any violation:

| # | Check | Catches |
|---|---|---|
| 1 | **Staleness** | `palette.json` hand-edited, or older than `palette_build.py`'s spec |
| 2 | **Separation** | any ramp gap under 0.12 L\*, and any non-monotonic ramp |
| 3 | **Arithmetic** | a recorded L\* that the hex does not actually have |
| 4 | **Off-palette** | any opaque colour in any texture that is not in the palette |
| 5 | **Breadth** | any texture using more than five families |

**All five have been verified by reintroducing the bug they catch and confirming they
fire.** Per `VERIFICATION.md`: a check that has never failed is unverified. If you add a
sixth check, break something on purpose before you trust it.
