# Art style

> **Provenance: PORTED + DOCTRINE.** The thesis and the failure modes come from DOWNTIME
> (`cadykaya/mario-3`), where they cost rebuilds and at least one entire discarded
> character. The Minecraft-specific rules — resolution, tiling, vanilla consistency — are
> new and are marked where they appear.
>
> Judged the same way as everything else here: **a rule either constrains a decision or it
> is decoration.** Every rule below is one you can fail.

---

## The inheritance, and the one big inversion

DOWNTIME's entire art thesis is four words from its owner:

> **"paint on all details"**

Geometry stays cheap; the texture carries everything. That project had to *fight* for it —
a 3,000-triangle ceiling enforced at build time, a rule that going over budget means
"delete geometry and paint it instead, never optimise the mesh," and a build-time assertion
that no organic asset is flat-shaded.

**Minecraft gives you that for free.** Block models are axis-aligned boxes. Entity models
are boxes with a fixed vertex format. There is no triangle budget to enforce because there
is no triangle budget to spend. The medium has already made the decision DOWNTIME had to
defend.

So the inversion is this:

| DOWNTIME | Here |
|---|---|
| The hard part was **restraining geometry** | Geometry is restrained by the engine |
| The hard part was **earning the right to paint** | Painting is all there is |
| Triangle budget was the enforced constraint | **Pixel budget is the enforced constraint** |

**Everything that made DOWNTIME's art good now lands on a 16×16 grid.** That is a smaller
canvas than a 256px character texture by a factor of 256. It is not an easier problem. It
is the same problem with less room to hide.

---

## The three families

DOWNTIME split assets into *hard-surface* and *organic* because applying prop rules to
characters produced "Minecraft cube-men" — which is a funny thing to inherit into a
Minecraft project, and it means the split has to be redrawn. Here it is by **what the
renderer does with it**, because that is what actually changes the rules.

| | **Block** | **Item** | **Entity** |
|---|---|---|---|
| Seen as | a tiling face in a wall, usually many at once | a ~16px icon in a hotbar, and in-hand | a moving figure at 3–30 m |
| Read at | 1–2 blocks to 40+ m | **16 px, always** | silhouette first, detail never |
| Must | **tile with itself invisibly** | read as one shape at icon size | read as a shape in motion |
| Outline | **never** | **yes, a dark contour** | no |
| Detail budget | low — it repeats | highest — it is looked at | low — it moves |
| Failure mode | **grid pattern / noise mush** | **unreadable at 16px** | **silhouette soup** |

**Applying the wrong family's rules is the most expensive mistake available**, exactly as
it was in DOWNTIME. A block textured like an item icon produces a wall of loud repeating
contours. An item textured like a block produces a grey smudge in the hotbar.

---

## 1. Resolution — 16×16, and this is not negotiable

> **Every texture is 16×16. Landmarks and multi-block structures get 16 per block face,
> not a bigger texture.**

Reasons, in order of how much they matter:

1. **Mixed resolution is the single loudest amateur tell in modding.** A 32px or 64px block
   next to vanilla's 16px reads as a foreign object before the player has consciously
   noticed why. It is the same failure as DOWNTIME's cube-men: instantly legible as wrong,
   and not fixable by making the high-res texture *better*.
2. **Resource packs.** Players run 16×, 32×, 64× packs. A mod at native 16 scales into all
   of them; a mod at 64 is permanently inconsistent with every pack.
3. **It forces the discipline.** 256 pixels and five families is a real constraint, and
   constraints are why DOWNTIME's art holds together.

**Exceptions, and they are narrow:** entity textures follow the vanilla atlas convention
for their model (64×64 and 64×32 are the common ones), and GUI textures follow whatever the
screen layout needs. Neither is a licence to upscale a block.

### The pixel budget

At 16×16 you have **256 pixels**, and one pixel is **6.25% of the width**. DOWNTIME's rule
"no geometry smaller than 10% of the object's height" has a direct translation:

> **A feature smaller than 2 pixels does not exist.** It will not survive mipmapping, it
> will shimmer at distance, and at any sane render distance it is gone.

Measure a detail in pixels *before* designing it. DOWNTIME learned this on a character
mouth that was twelve texels wide; here the whole face of a block is sixteen.

---

## 2. Colour

All of it lives in [`PALETTE.md`](PALETTE.md). The short version:

- **Nothing hard-codes a colour.** `assets/palette.json` or it is a bug.
- **Five families maximum per texture**, enforced.
- **Adjacent values differ by ≥ 0.12 L\***, enforced.
- **Never pure black (`#000000`) and never pure white (`#FFFFFF`).** Vanilla essentially
  never uses either. They read as holes. `stone[0]` and `bone[2]` are the ends of the world.
- Typical block texture: **3–5 values from 1–2 families.** If you need six, the block has
  not decided what it is.

---

## 3. Silhouette

DOWNTIME's rules, translated to the grid. All three still bite.

**1. Every item must be identifiable as a solid black shape at 16 px.** If it is not, the
silhouette is wrong and no amount of painting fixes it. This is the item family's version
of DOWNTIME's *"judge in silhouette, with the head hidden"* — and it is the same exam: strip
everything that flatters and see whether the shape survives.

**2. Exaggerate the load-bearing masses.** A tool's head reads bigger than a real tool's
head. A thin, correctly-proportioned object vanishes at icon size. DOWNTIME's silo is
fatter than a real silo for exactly this reason.

**3. Asymmetry.** A perfectly mirrored texture reads as cheap at any resolution. This
matters *more* at 16×16, not less, because mirroring is so easy to do accidentally.

**4. Items get a contour; blocks never do.** Vanilla items carry a dark outline (a darker
value of their own family, not black) which is what makes them pop against any hotbar
background. Blocks carry none — an outlined block tiles into a visible grid, which is the
next section.

---

## 4. Tiling — the rule DOWNTIME never needed

> **Provenance: DOCTRINE.** New. This is the constraint that makes block texturing a
> genuinely different discipline from every other kind of texture painting.

A block face is not seen once. It is seen **in a wall, hundreds of times, at once**. Two
distinct failures follow, and they are separate problems:

### 4a. Seam tiling

The texture must wrap: the right edge must continue into the left, the bottom into the top.
Any generator must be written modulo 16 rather than clamped, or every wall grows a visible
lattice.

### 4b. Pattern tiling — the harder one

A texture can tile *seamlessly* and still be terrible, because the eye finds the repeat.
Two specific tells:

- **A strong feature near the centre** becomes a regular grid of dots across a wall. Any
  feature the eye can latch onto must be either broken by the edge or repeated enough
  within the tile that no instance is special.
- **A bright or dark outlier pixel** becomes a starfield. This is the single most common
  reason modded stone looks wrong at distance: one over-bright speck, invisible on its own,
  becomes a rash across a cliff face.

**The test — and it is mechanical, not a matter of taste:** render the texture tiled 8×8
and look at *that*, never at the single tile. Anything that resolves into a pattern is a
failure. `TEXTURING.md` has the helper.

> This is the direct analogue of DOWNTIME's **"judge in rotation, always"**: a model passed
> review for weeks because every review was a front view, and the front is the view
> everything gets tuned in. **The single tile is the front view.** It is the one that lies.

---

## 5. Painting rules

The full method is in [`TEXTURING.md`](TEXTURING.md). The two doctrinal points belong here
because they are the difference between generated-looking and painted-looking art:

### A hash on a broad surface is digital camouflage

> **The most transferable lesson DOWNTIME produced, and it applies to every painted asset.**

Its first pixel pass placed wear and highlight clusters with a hash. The pixel size was
right, the palette was right, and it read as **digital camouflage** — because every cluster
sat where a random number put it, so **no cluster meant anything**.

This is not an aesthetic failure, it is a structural one: a paint function handed only a
coordinate has no idea what it is painting on, so a hash is the only tool left.

**Fix: give the painter the structure.** Painting is done *against a described surface* —
which plank am I on, how far from its end, is this a mortar line, is this an edge of the
block face. Then highlights go on rims, darks in overlaps, wear at edges, and noise
survives only as a *breaker* inside a zone that structure already chose.

At 16×16 this is even more true than at 1024, because you have **256 chances** to mean
something and a hash spends all of them meaning nothing.

Two numbers from that pass, both of which were quietly producing the noise:

- An edge band expressed as a **fraction of the surface** makes every pixel on a small
  face an "edge". **A rim is a fixed width** — here, 1 pixel.
- A wear radius expressed as a **flat distance** was wider than the whole part. Scale it to
  the feature it belongs to.

### One dominant cue per asset, and nothing competes with it

DOWNTIME's Walker reads as facing a direction because of **one** feature — a visor slit at
full face width — and the brow, cheek and jaw plates were *removed* because they competed
for the same 20 pixels.

> **If every edge is lit, none of them is the one to look at.**

At 16×16 you have room for exactly one idea. Decide what the block *is* — the ore's specks,
the machine's vent, the log's bark ring — and cut everything that competes with it.

---

## 6. Model it, or paint it?

The decision procedure. In Minecraft the geometry question is not "how many triangles" but
"is this a plain cube, or a custom model," and custom models cost far more than triangles:
they cost occlusion, lighting behaviour, collision surprises, and render performance.

Ask in this order:

1. **Does the player collide with it differently?** → model it.
2. **Does it change the outline seen from across a room?** → model it.
3. **Is the shape itself the gameplay tell?** (a machine's arm angle, a tank's fill level)
   → model it.
4. **Anything else** → **paint it.**

**Always painted, no exceptions:** panel lines, bolts, rivets, planks, grain, rust, dirt,
wear, cracks, mortar, ore specks, labels, stencilled text, screw threads, moss, frost, and
every other mark on a surface.

**Always modelled:** anything the player stands on or walks into, anything whose *shape*
changes with state, and anything that must be visible in silhouette from outside the block
grid.

> **Default hard to the plain cube.** A mod full of custom-model blocks is a mod that runs
> badly and lights strangely. DOWNTIME's rule was "over budget means delete geometry and
> paint it instead, never optimise the mesh." The translation is: **when in doubt, it is a
> cube with a good texture.**

---

## 7. Failure catalogue

The specific tells of amateur modded art. Every one of these is checkable.

| Tell | What causes it | Fix |
|---|---|---|
| **Resolution mismatch** | a 32/64px texture beside vanilla's 16 | 16×16, always |
| **Gradient smear** | interpolated shading, an airbrush habit | hard-stepped values only, 3–5 of them |
| **Digital camouflage** | hash-placed noise | paint from structure |
| **Grid rash** | one bright outlier pixel, tiled | check tiled 8×8, never single |
| **Mud** | too many values too close together | the 0.12 L\* rule, enforced |
| **Neon** | separating from vanilla by saturation | separate by value and structure instead |
| **Holes** | pure `#000000` / `#FFFFFF` | ends of the world are `stone[0]` / `bone[2]` |
| **Sticker** | an item with no contour, or a block *with* one | contour items, never blocks |
| **Shimmer** | features under 2 px | measure in pixels before designing |
| **Mirror** | accidental symmetry | asymmetry is a rule, not a preference |

---

## 8. Verification

Full doctrine in [`VERIFICATION.md`](VERIFICATION.md). The art-specific gate:

1. **`python3 tools/palette_check.py`** — machine-checkable rules, exits non-zero.
2. **Tiled 8×8 render** — never judge a block from one tile.
3. **16 px icon render** — never judge an item zoomed in.
4. **In-wall vanilla adjacency screenshot** — beside the nearest vanilla block, at play
   distance.
5. **Greyscale** — desaturate it; if the composition falls apart, the palette is not working.

### The lesson that outranks the gate

> **A check can only tell you the thing matches its description. It cannot tell you the
> description was worth matching.**

This one cost DOWNTIME an entire character. Walker v1 met every ratio its spec asked for,
passed two purpose-written build-time guards, and cleared the whole seven-shot review gate —
and the owner rejected it on sight as a *"clickbait-thick Among Us character."* They were
right. Reading the spec's six constraints together, there was exactly **one** silhouette
that satisfied all of them, and it was not a maintenance worker. Every check was measuring
conformance to a specification that had already decided the wrong answer.

**So when something passes everything and still looks wrong, the spec is the suspect** —
not the execution, and certainly not the eye that flagged it.
