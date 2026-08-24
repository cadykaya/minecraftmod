# Worldgen

> **Provenance: REFERENCE + DOCTRINE.** Engine behaviour is reference; the design rules at
> the end are ours and lean on [`AESTHETIC.md`](AESTHETIC.md).
>
> **`VERIFY:` markers flag specifics.** Worldgen JSON schemas move between versions more
> than almost anything else — density functions and noise settings especially. Read a
> vanilla datapack from the actual jar before authoring: it is the ground truth, it is
> right there, and it is always current.

---

## Worldgen is data, not code

Almost all of it is **JSON in datapack registries**, not Java. Dimensions, dimension types,
biomes, noise settings, density functions, placed and configured features, structures,
structure sets, processor lists — all data.

Java appears in exactly three places:

1. **A new *kind* of feature or structure** — behaviour vanilla cannot express. Rare.
2. **Codecs** for the config of such a thing.
3. **Datagen providers** that emit the JSON (see [`DATAGEN.md`](DATAGEN.md)).

> **If you are writing Java to place ores, you have taken a wrong turn.** Vanilla's feature
> and placement vocabulary covers a very large space, and every bit of it you use is a bit
> you do not maintain.

### Read the vanilla datapack. Constantly.

The single most useful worldgen habit. Every vanilla biome, feature and noise setting ships
as JSON inside the client jar. It is current by definition, and copying a working file and
editing it beats authoring from a schema you half remember.

```sh
# after one successful setup
find ~/.gradle/caches -name 'client-*.jar' -o -name 'minecraft-*.jar' 2>/dev/null
# then: unzip -l <jar> | grep worldgen
```

---

## The layers, outermost in

```
dimension              which generator + which dimension_type
  dimension_type       the physics: height, light, ceiling, time, portals
  noise_settings       the shape of the terrain
    density_function   the maths that produces that shape
  biome_source         which biome goes where
    biome              climate, colours, mob spawns, and which features
      placed_feature   a configured_feature + where/how often
        configured_feature   what to actually place
```

**Work outside in, and stop as early as you can.** A great many "new dimension" ideas are
better served by a new *biome* in the overworld, and a great many "new biome" ideas are
better served by a *feature* placed in existing biomes. Each layer outward is a large step
up in cost, in worldgen time, and in ways to be wrong.

---

## Adding to existing biomes: biome modifiers

`VERIFY:` — this is a NeoForge mechanism and its JSON shape is loader-specific.

To put your ore in vanilla stone, **do not edit the vanilla biome.** Editing it means
fighting every other mod that also edits it, and last-loaded wins. NeoForge provides
**biome modifiers**: data files declaring "add this placed feature to biomes matching this
tag," applied additively at load.

> **Target biome *tags*, never biome lists.** A modifier that names twelve biomes misses the
> thirteenth, misses every modded biome, and breaks when Mojang adds one. A modifier that
> targets `#is_overworld` or `#is_forest` keeps working.

---

## Features and placement — the split that matters

- **Configured feature** = *what*. An ore blob of size 9, a tree of this shape.
- **Placed feature** = *where and how often*. Count, height range, rarity, biome filter.

The same configured feature is reused by many placements. **Do not bake rarity into the
configured feature.** Ore in one place and ore in another are one *what* and two *wheres*.

### Placement order is not decorative

Placement modifiers run **in sequence**, each transforming the set of positions, and the
order is the semantics. Putting the cheap filter last means the expensive one ran on every
candidate. Getting height and count the wrong way round changes both the distribution and
the cost. `VERIFY:` the modifier names, but the ordering principle holds everywhere:

> **Cheapest and most eliminating first.** Rarity, then count, then height, then the
> per-position predicates.

### Worldgen cost is real and it is paid by the player

Features run for **every chunk generated, forever**, on a thread the player is waiting on. A
feature that scans a large area, or one placed with a high count and rejected late, is a
chunk-loading stutter that will be blamed on the pack rather than on you.

Structures are worse — they run over large areas and can pin chunks. **Measure generation
time before shipping anything ambitious.** `VERIFY:` the current profiling command.

---

## The gotcha that will cost a session

> **Worldgen settings are baked into a world when it is created.** Changing a biome, a
> noise setting, or a dimension does **not** affect an existing world's generated chunks,
> and often not its ungenerated ones either — the settings were copied into the save.

So during worldgen iteration: **create a new world every time.** A "my change did nothing"
report is this, nine times out of ten, and it is indistinguishable from a real bug.

Corollaries:

- Test with a **fixed seed** so two runs are comparable. Comparing different seeds tells you
  nothing and feels like it tells you something.
- Adding a feature to a *released* mod affects only newly generated chunks. Existing bases
  sit in a world that predates it — a real design constraint, not a bug.

---

## Dimensions

A dimension is cheap to *declare* and expensive to *make good*. The declaration is a
dimension type (height, light, ceiling, portal behaviour) plus a generator. What is
expensive is everything that makes it worth visiting.

### The `dimension_type` fields that shape the whole feel

`VERIFY:` names. Conceptually: build height and minimum Y (the volume you get to design
in), whether there is a ceiling, whether there is skylight, ambient light level, fixed time
of day, whether water evaporates, and the coordinate scale relative to the overworld.

**These do more for identity than any texture.** A ceiling and no skylight makes a place
feel like the Nether before a single block is placed. Fixed time of day removes the day
cycle as a pacing device — that is a design decision, not a setting.

### Before you add a dimension, answer this

Straight from [`AESTHETIC.md`](AESTHETIC.md), and it is the executioner:

> **Could I replace it with a different random weird thing without changing anything?**

If your dimension is "the same game with purple stone," swapping purple for green changes
nothing, and it is decorative. A dimension earns itself when its **rules** differ — when
something is possible or impossible there that is not elsewhere, and the player has to
adapt.

The rest of that document's design test applies in full:

1. Why does this exist here?
2. How have ordinary people adapted to it?
3. What institution profits from, regulates, worships, or misunderstands it?
4. What limitation prevents it from solving everything?
5. What earlier decision caused the current disaster?

**Question 4 is the one modded dimensions fail most.** A dimension full of better ore with
no limitation is a dimension that deletes the overworld from the game.

---

## Structures

The most expensive thing in this document, in both worldgen time and authoring effort.

- **Jigsaw/template structures** are assembled from saved `.nbt` pieces via pools. This is
  how vanilla builds villages, and it is the right tool for anything modular.
- **Coded structures** are for shapes that must respond to terrain or state.
- **Structure sets** control spacing and separation across the world, and this is where
  "structures are everywhere" and "I have never found one" both come from. Both are bad;
  the second is worse, because the player concludes the mod is broken.

> **A structure nobody finds is a structure you did not build.** Decide its intended
> encounter rate as a number — "a player should meet one within N minutes of normal play" —
> and then *measure it*, with a fixed seed and a locate command, rather than guessing from
> spacing values.

---

## Where this project's worldgen will live

```
src/main/resources/data/<mod_id>/
  worldgen/
    biome/  configured_feature/  placed_feature/
    noise_settings/  density_function/  structure/  structure_set/
  dimension/  dimension_type/
  neoforge/biome_modifier/          VERIFY: exact path
```

**Generate these with datagen rather than hand-writing them** — see
[`DATAGEN.md`](DATAGEN.md). Worldgen JSON is deep, repetitive and easy to typo in ways that
fail silently, which is the exact profile of a thing that should be code.

---

## Verification

- **Fixed seed, fresh world, every time.**
- **A locate-and-count probe** beats reading spacing numbers. Ask the real question — "how
  long until a player meets one" — and measure it.
- **Chunk generation timing** before shipping a new feature or structure.
- **Look at it from a distance**, not from inside. A biome is a silhouette and a colour
  before it is anything else, which puts it squarely under `ARTSTYLE.md`'s greyscale test.
