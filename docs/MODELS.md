# Models

> **Provenance: REFERENCE + DOCTRINE.** JSON structure and engine behaviour are reference;
> the "when may I leave a cube" rules are ours.
>
> **`VERIFY:` markers flag things that churn.** This was written without access to the
> NeoForge docs — read [`INDEX.md`](INDEX.md)'s standing warning. The item-model system in
> particular was overhauled during the 1.21 line and again at the version-scheme change;
> check it against a real project before writing fifty files against a remembered shape.

---

## The prime directive

> **Default hard to the plain cube. A custom model must earn itself.**

This is DOWNTIME's triangle-budget rule, translated. There the rule was *"over budget means
delete geometry and paint it instead, never optimise the mesh."* Here the currency is not
triangles — the engine does not care about a few extra quads — it is **everything a
non-cube block silently gives up**:

| A full cube gets | A custom model loses |
|---|---|
| face culling against neighbours | every hidden face is still submitted |
| correct ambient occlusion | AO must be reasoned about, often disabled |
| clean light propagation | needs explicit light/opacity properties |
| a collision box that matches what you see | collision must be authored separately |
| a free item icon | usually needs its own item model |

A mod full of custom-model blocks runs badly and lights strangely, and **this is one of the
most common reasons a content mod feels worse than vanilla without the player being able to
say why.**

### The decision procedure

1. **Does the player collide with it differently?** → model it.
2. **Does it change the outline seen from across a room?** → model it.
3. **Is the shape itself the gameplay tell** — a machine's arm angle, a tank's fill level,
   a lid open or shut? → model it.
4. **Anything else** → **paint it.**

Panel lines, bolts, planks, grain, rust, mortar, ore specks, moss, cracks, labels: **always
painted, no exceptions.** If you are about to add an element to a model for a surface mark,
stop and put it in the texture.

---

## Block models

### The layers

```
blockstates/<name>.json   -> which model, at what rotation, for which state
models/block/<name>.json  -> the geometry + texture bindings
textures/block/<name>.png -> the pixels
```

Keep them one-to-one where you can. A blockstate that maps twelve states onto twelve
bespoke models is twelve files to keep in sync; **prefer rotation of one model**.

### Inherit, don't author

Most blocks are a `parent` line and a texture binding. Vanilla's parents cover the
overwhelming majority of what a mod needs:

| Parent | For |
|---|---|
| `block/cube_all` | same texture on all six faces |
| `block/cube_column` | side + end (logs, pillars) |
| `block/orientable` | a front face (furnaces, machines) |
| `block/cross` | flat X-cross (plants) |
| `block/slab`, `block/stairs`, `block/fence_*`, `block/wall_*` | the shape families |

```json
{ "parent": "minecraft:block/cube_all",
  "textures": { "all": "<mod_id>:block/<name>" } }
```

**Authoring elements by hand is the exception.** If you are writing `from`/`to` arrays for
something vanilla has a parent for, you are making future-you maintain geometry that Mojang
would have maintained for you.

### When you do author elements

Coordinates are in **sixteenths of a block**, `0..16`. Values from `-16` to `32` are
permitted, so a model may extend outside its own block — and if it does, the block needs
correct light and occlusion properties or it will look wrong from the outside.

```json
{ "elements": [
    { "from": [2, 0, 2], "to": [14, 10, 14],
      "faces": {
        "north": { "uv": [0, 0, 12, 10], "texture": "#body", "cullface": "north" },
        "up":    { "uv": [0, 0, 12, 12], "texture": "#top", "tintindex": 0 }
      } } ] }
```

Four things that bite:

- **`uv` is in texture-sixteenths, not pixels**, and it is `[x1, y1, x2, y2]`. Omitting it
  lets the engine guess from the element's size, which is usually right and occasionally
  surprising — **state it whenever the element is not a full face.**
- **`cullface`** tells the engine "skip this face when a solid block is on that side." Omit
  it and you pay for invisible faces forever. Set it wrongly and faces vanish in the open.
- **`tintindex`** hooks the face to a colour handler (foliage, water, dyed variants). It is
  the *only* sanctioned reason to have a greyscale texture — see the tint rule below.
- **`"ambientocclusion": false`** at model top level, for models whose shading goes blotchy.
  A last resort, not a default.

### Render type

`VERIFY:` — the mechanism moved between the JSON model and client-side registration during
the 1.21 line and may have moved again.

| Type | For | Cost |
|---|---|---|
| `solid` | opaque | cheapest |
| `cutout_mipped` | leaves-like, alpha at distance | mipmapped, cheap |
| `cutout` | crisp alpha edges (plants, glass panes) | no mipmap on alpha |
| `translucent` | actual partial transparency | **sorted, expensive** |

**Do not reach for `translucent` for something that is merely see-through in places** — that
is `cutout`. Translucent blocks are depth-sorted and a mod that ships a lot of them is a mod
that tanks framerate in a build made of them.

### Tinting, and the one exception to the palette law

A tinted texture is authored in **greyscale** and multiplied by a colour at runtime. This is
how vanilla does grass and leaves across biomes, and it is genuinely the right tool for
"same block, many colours" — a dyed set of sixteen is one texture, not sixteen.

It is also the **only** legitimate reason for a texture to be off-palette, since a greyscale
source is off-palette by construction.

> **If you tint, the tint colours come from `assets/palette.json` like every other colour**,
> and the greyscale source must be built from palette L\* values so the multiplied result
> lands on the ramp. A tint chosen by eye is the seven-greens failure with an extra step.

`VERIFY:` — and note the tinted texture must be excluded from `palette_check.py`, which
currently has no notion of tint sources. Extend the skip list deliberately when the first
one appears; do not quietly widen it.

---

## Item models

**VERIFIED against 26.2.0.67**, read out of the vanilla jar. The remembered shape turned
out to be right, and it is now evidence instead of memory.

There are **two layers**, and conflating them is the mistake:

| file | what it is |
|---|---|
| `assets/<ns>/items/<name>.json` | the **definition**. Names a model, and is where dispatch lives. |
| `assets/<ns>/models/item/<name>.json` | the **model**, for flat items. Parent + `layer0`. |

A flat item needs both:

```json
// items/clast.json                    // models/item/clast.json
{ "model": {                           { "parent": "minecraft:item/generated",
    "type": "minecraft:model",           "textures": {
    "model": "interregnum:item/clast"      "layer0": "interregnum:item/clast" } }
} }
```

A **block item needs only the definition**, pointing straight at the block model, with no
`models/item/` file at all — which is exactly how vanilla ships `stone`.

The `type` field is the dispatch system that replaced overrides. Counted across all 1538
vanilla item definitions in 26.2: `minecraft:model` (2131 uses), `special` (91), `select`
(71), `dye` (50), `composite` (33), `condition` (26), `constant` (12), plus item-specific
ones like `shulker_box` and `banner`. `range_dispatch` carries `entries` with `threshold`
and a `fallback` — a drawn bow is the canonical example.

> **This mod shipped six registered items and zero definitions**, and every check was
> green: a dedicated server never loads `assets/`, so nothing in CI could see it, and
> `registry_check.py` was asserting translation keys while its summary line claimed items
> "resolve models". All six would have been the missing-model cube in front of a player.
> That check now verifies the definition exists and the model it names resolves.
> [`LESSONS.md`](LESSONS.md) #5, in the one area this container cannot look at directly.

What is stable regardless of format:

- **A block's item usually just points at the block model.** Free, and always consistent.
- **A flat item icon is generated from its texture** — the classic `item/generated` +
  `layer0` shape — and gets extruded automatically.
- **Anything that changes appearance with state** (durability, fill level, active/inactive)
  goes through the dispatch mechanism of the day. Find its current name before designing
  around it.

### The 16 px rule

An item is judged as a **16 px icon in a hotbar**. Not in a preview, not zoomed.

```sh
python3 tools/texview.py item.png --icon
```

Per [`ARTSTYLE.md`](ARTSTYLE.md): items carry a dark contour (a darker step of their own
family, never `#000000`); blocks never do. Silhouette first — if it does not read as a solid
black shape at icon size, painting will not save it.

---

## Entity models

**VERIFIED by this repo's own compiling code** on 26.2.0.67: entity models are **Java**,
not JSON — `WardenModel` and `ShrineKeeperModel` are built from `LayerDefinition` /
`MeshDefinition` and registered against `EntityRenderersEvent.RegisterLayerDefinitions`.
The builder APIs still move between versions; read the sources rather than a tutorial.

### The bench: `tools/entity_view.py`

Everything below says *look at it*, and until this existed there was no way to. A texture
sheet shows unwrapped nets, which is exactly the view that tells you nothing about the
assembled figure, and the game is the only other renderer available.

So the geometry lives in `tools/entity_specs.py` and three things read it: the texture
painter (to place each box's net), `gen_resources.py` (which writes the `*Geometry.java`
the game bakes), and the viewer, which ray-casts the boxes orthographically and samples the
real texture through Minecraft's own unwrap. What it draws is what the model will be,
because it is built from the numbers the model is built from.

```sh
python3 tools/entity_view.py warden                 # front / three-quarter / side / rear
python3 tools/entity_view.py warden --silhouette    # the test that actually decides
```

**It earned its keep on the first mob.** The Warden's robe was one box, and it looked
plausible as a net and plausible from the front. Assembled, in profile, it was a bollard —
the "judge it with the head hidden" failure below, arriving exactly as advertised. Splitting
the robe into two stepped boxes fixed it, and that decision was only available because the
figure could be seen.

**Known limit, stated rather than discovered later:** the Warden is weakest in pure profile,
where the arms sit inside the torso's depth and contribute nothing to the outline. Vanilla
humanoids have the same property, so this is accepted rather than solved — but it is the
first thing to fix if a Warden ever needs to read side-on.

Two things the viewer will not tell you: how the model looks **animated**, and how it looks
**lit**. Both still need a client.

The shape of it is stable even when the API is not: a hierarchy of boxes, defined once as a
layer definition with a fixed texture size, then posed per-frame. Boxes are placed in model
space, each carrying a UV origin into a single atlas texture — commonly 64×64 or 64×32.

### The rules that actually matter

DOWNTIME spent an entire character learning these and they are cheap to inherit:

- **Silhouette is the whole design.** An entity is seen moving, at distance, often against
  a busy background. Detail is invisible; outline is everything.
- **Judge it with the head hidden.** DOWNTIME's body passed review for weeks as "stacked
  cylinders with clothes painted on" because the head was doing all the work. If your mob
  only becomes a character when the head goes on, the body is not finished.
- **Asymmetry.** A perfectly mirrored mob reads as a placeholder.
- **One dominant facing cue, and nothing competes with it.** Walker reads as facing a
  direction because of *one* feature — a visor slit at full face width — and the brow,
  cheek and jaw plates were removed *because* they competed for the same 20 pixels. If the
  player must read which way a mob faces before deciding whether to swing, that cue wins
  and everything else is cut until it does.
- **Judge in rotation, always.** Front, three-quarter, side, rear. The front is the view
  everything gets tuned in, and it is the one that lies.

### Proportion

Vanilla mobs are **chunky and short** — roughly 2–4 heads tall, with oversized heads and
hands. This is not a stylistic accident, it is what survives being 30 blocks away.

DOWNTIME's mistake here is worth quoting because it is so easy to repeat: a character built
at a literal 6.5 heads with limbs sized like limbs came out *"a thin green army man,"*
because **a narrow body reads taller than it measures.** The ratio came down and every
cross-section went up by about a third. Read proportion as a *look*, not as arithmetic.

---

## Verification

- **`ARTSTYLE.md` §7's failure catalogue** — the tells, all checkable.
- **Missing-model check.** A model that fails to load is a purple-and-black cube in game and
  a silent warning in the log. DOWNTIME's `test/Units.tscn` fails if any expected `.glb` is
  missing; the equivalent here is a test that every registered block and item resolves a
  model and every model resolves its textures. **Write it early** — it is the single highest
  value-per-line test in a content mod, because this failure is common, silent, and
  embarrassing.
- **Collision is authored separately from visuals, always.** DOWNTIME's rule: *"Collision is
  always built separately from the visuals, so swapping a model can never change how the
  game plays."* A shape used for both is a shape that changes gameplay when an artist edits
  it.
