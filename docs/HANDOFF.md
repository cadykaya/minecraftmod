# Handoff

**Living document. Read this first if you are a fresh session.** It is the only file that
says what is *currently true*; everything else says what is *always true*.

Updated at every phase boundary, so that if a session ends — for any reason — the next one
picks up cold without archaeology.

**Last updated:** autonomous build-out, hourly heartbeat running. The toolchain is
**unblocked and building** (NeoForge 26.2.0.67, Java 25) — the old network-policy
blocker is resolved and the probe returns 200. Chapter 0's content, the deicide and
its consequences, placement tracking, the unraveling, and the first Warden entity all
exist and are verified against a live server.

---

## Where things stand

There is a mod. It builds, it boots a dedicated server, and every system below is
asserted against a running world rather than against its own source.

| | State |
|---|---|
| Gradle project | **building**, NeoForge 26.2.0.67 / Java 25 |
| Java source | `core/` (pure, self-tested) + the game module |
| Textures / models / data | Phase-1 set, all resolving in a live server |
| Palette system | **working and verified** |
| Texture pipeline | **working and verified** (paint kit + review bench) |
| Doc set | **complete** — 15 documents, see [`INDEX.md`](INDEX.md) |
| Live-world checks | **13**, every one mutation-verified, all in CI |

### The Warden speaks, and the table argues

`warden_intake` was written in the first week, validated ever since, and unreachable.
It runs now.

The decisions -- who wins a node, what dissent does -- were already in `core/` and
tested with no game running. What was missing is the half that only exists on a
server: **who is at which table, when a node resolves, and what happens when somebody
walks off mid-sentence.** Every rule in `Conversations` is a way a table can wedge in
front of real people:

- a node resolves on the **last** pick, never the first and never a timer;
- one player leaving must not deadlock the rest -- and a departure that completes the
  table resolves it **on the way out**;
- a leaver's vote leaves with them, or someone who quit could still swing a VOTE;
- the initiator leaving ends it (there is no INITIATOR node without one);
- silence times out after a minute: the initiator going quiet ends the table, anyone
  else going quiet is taken off it so the rest can carry on.

**Participants are opaque string ids, not players.** The core engine asked for that,
and it is what lets the entire multiplayer state machine -- votes, ties, unanimity,
walking away -- be asserted on a headless server with no client in existence.
`/interregnum talk start|say|status|leave` drives it; six mutations, six caught.

**Talking to a Warden is what records `WARDEN_CONTACT`** -- being *addressed*, not
seeing one -- so a world can now reach band 2 by playing rather than by command.

Two core fixes fell out of it: `Conversation.remove` (the engine described removing
absentees and had no way to), and stance ordering, which `Map.copyOf` was silently
discarding. See [`LESSONS.md`](LESSONS.md) #18 and #19 -- both are about verification
rather than about dialogue, and both are the more useful half of this pass.

**Still missing: the screen.** Everything above is server-side and proven; a player
cannot yet see any of it. That, and right-clicking a Warden to begin, is what remains.

### The Warden takes the field

The statues wake and watch; **these arrive**. Two objects, two jobs — waking a statue
does not consume it, so the one your neighbour built into their garden wall stays
there watching forever. The eye and the officer are not the same thing.

**A Warden never attacks.** No target selector, no melee goal, and — the part that is
actually enforced — *no `ATTACK_DAMAGE` attribute at all*, so there is nothing for a
future careless goal to reach for. 100 health, full knockback resistance, unpushable:
a player's first instinct is to hit one, and that has to fail in the most
uninformative way available. Nothing happens, and it is still looking at you.
**[NEEDS PLAYTEST]** whether they should be killable at all.

The model is a squat robed figure under a wide flat mantle, ember visor slots as the
only warm pixels on the whole sheet — the statue's rule, kept. Geometry lives once in
`tools/entity_specs.py` and feeds three consumers: the texture painter, the generated
`WardenGeometry.java`, and **`tools/entity_view.py`**, a new ray-cast bench that draws
the assembled figure front / three-quarter / side / rear. See [`MODELS.md`](MODELS.md);
it caught the robe reading as a bollard in profile on its first use.

`tools/warden_check.sh` proves summoning, attributes, the missing attack damage, and
survival across a restart. Three mutations, three caught. `registry_check.py` now also
refuses an entity missing a renderer, a layer definition, a texture, or a name — five
more, all verified.

**Not done yet, and deliberately:** nothing spawns a Warden. They exist and can be
summoned; who places them, where, and when is the next decision, and it is bound up
with the proposal under "Open questions".

### The overworld spends itself

`bands.json` is finally read. `UnravelingLoader` turns it into a validated table (rejecting
duplicate bands, self-conversions, a band mislabelled with the wrong chapter, and any rule
that reverses another — the unraveling runs one way only), and `Unraveling` applies it.

Five gates, in order, and each one is a named answer rather than a silent `false`, because
"nothing happened" is the one thing this system says constantly and it has to be possible
to ask why: `DORMANT` · `BAND_TOO_LOW` · `OUT_OF_SCOPE` · **`CLAIMED`** · `UNSUPPORTED`.

- **It samples the surface column near players.** Not a shortcut. `Claims` answers
  "claimed" for an unloaded chunk, so unloaded ground was never reachable anyway; and the
  table's blocks (grass, flowers, leaves) are one layer thick, so uniform sampling would
  essentially never hit them and band 1 would be invisible.
- **`thin_places`** is "within 48 blocks of the crater, or in a chunk next to one holding
  shrine masonry". The shrine test reads section *palettes*, not blocks, so the common
  answer costs a few reference comparisons. The crater is now persisted on the chapter data
  (`ChapterSavedData#site`) — the world's one fixed landmark, which the ferry and the ghost
  will both want too.
- **It never places a state that cannot stand there.** See [`LESSONS.md`](LESSONS.md) #16:
  the shipped table had a rule that was well-formed, passed every data check, and could
  never once have fired.
- `/interregnum unravel at|sweep` answer for one block and measure a burst. `status` now
  reports `ticks=`/`passes=` — the only witness that the level tick is connected at all.

`tools/unravel_check.sh` proves all of it against a live server, including a datapack that
replaces the table. Seven mutations, seven caught.

### The world remembers what people built

Minecraft does not record who placed a block, and the unraveling needs to know: the crater
can get away with a tag whitelist because it fires once at one spot, but the unraveling
runs forever over a whole world and would eventually eat somebody's cobblestone wall on the
grounds that cobblestone is natural.

`PlacedBlocks` is a chunk attachment holding chunk-relative positions packed one per int
(x and z are 4 bits, y is 9). Past **4096 placements the chunk saturates**: the set is
dropped and the whole chunk counts as claimed. That bounds memory and degrades in the safe
direction — it protects more, never less — and it is the right answer anyway, because a
chunk somebody has put four thousand blocks into is theirs.

`Claims` is the single place anything asks, and it **fails closed**: an unloaded chunk
answers "claimed". Breaking a block forgets its position, or mining through your own wall
would leave permanent invisible holes the unraveling could never touch.

`/interregnum claim at|record|forget` are operator tools, not test hooks: a world that
existed before this mod was installed is full of builds the tracker never saw, and an
admin needs a way to say "this is ours". They are also what makes the tracker testable
without a player.

`tools/claim_check.sh` proves per-position claims, survival across a restart, and
saturation; mutation-verified all three.

### The statues open their eyes

`warden_statue` is a decorative block for the whole of Chapter 0 -- players build around
them, put them in gardens -- and the moment the god dies **every one of them wakes**. The
eyes are the only warm pixels on an entirely cool figure, so a woken Warden is visible
across a field, and per the palette law that ember means the same thing it always means:
this is running on the corpse.

Two paths, both verified. Statues within eight chunks of the site (or of any player) wake at
the instant of death. Everything else wakes **on chunk load**, which is the better beat
anyway: a player who was underground climbs out and finds the one in their garden already
watching.

Only already-loaded chunks are touched -- `getChunk(..., false)` never generates terrain as
a side effect of the god dying -- and waking is a blockstate flip, so there is no block
entity, no ticking, and no per-statue bookkeeping.

`tools/statue_check.sh` proves both paths across two server runs and is mutation-verified.

### The ground gives way

The crater is **subsidence, not an explosion** -- nothing detonated; a god died and the
world stopped being held up there. No fire, no scorching, no thrown blocks, and nobody is
hurt, which matters because the person standing next to it is the one who just did it and
the mod is not punishing them.

**Only natural ground moves.** Minecraft does not record who placed a block, so a narrow tag
whitelist decides, and it errs toward sparing: an unlisted block is left alone. A slightly
lumpy crater is cosmetic; a deleted house is somebody quitting. The image this produces is
the one the beat wanted anyway -- a house at the shrine left hanging over a pit, untouched
and no longer resting on anything.

`tools/crater_check.sh` proves both halves and is mutation-verified: removing the whitelist
fails with the list of destroyed player blocks; removing the crater fails with "the ground
did not subside".

### Chapter 0 is playable end to end

Shrines generate with an **offering box** standing on the carved centre stone --
deliberately obvious, because the opening of this mod is a player doing the most ordinary
thing in Minecraft and it being deicide. The box holds mundane offerings (bread, a candle,
a little copper) and, on a **12% roll while the god still lives**, the heart.

The uniqueness falls out of *when* loot tables roll: they roll on first open, not at
worldgen, so every shrine in the world is a candidate until one pays out and none are
afterwards. No shrine is chosen in advance, nothing is tracked per-shrine. The heart is
somewhere until it is taken, and then it is nowhere.

Measured: 8 hearts in 60 rolls before the death (13%, matching the 12% configured), and a
deterministic **0 in 60 after**. `tools/heart_check.sh` asserts both and is
mutation-verified -- deleting the `god_lives` condition makes hearts appear after the god
is already dead, which it catches by name.

### The god can die

`Deicide.commit()` is the one place the catastrophe happens, and it is idempotent -- a
world can lose its god exactly once. Its consequence today: **the sun stops.** The day
cycle was the god's, and with nobody left to turn it the light stays where it was. Per
`WORLD.md` there is no announcement and no name; the world simply stops moving.

Two callers, one implementation: the pickup handler (needs a real player) and
`/interregnum record deicide` (does not). That is deliberate -- it is what lets the
untestable path be three lines of adapter over a path that is verified end to end.

`tools/deicide_check.sh` asserts the whole beat and was mutation-verified: removing the
consequence and removing idempotence both fail it by name.

### Chapter state persists

`/interregnum status` reports the world's chapter; `/interregnum record <milestone>` (level
2) advances it. State lives on the overworld's saved-data storage -- the interregnum is a
fact about the world, not about a place in it -- and serialises through the same single
string `ChapterState` already round-trips in the core self-test.

`tools/persistence_check.sh` proves it across five server boots, including a mutation of
**already-loaded** saved data, which is the only path where `setDirty()` matters
(LESSONS #13). It has a fresh-world control, without which the other runs prove nothing.

### Worldgen works

Shrines generate. `ShrineFeature` builds a 5x5 court with a carved centre stone, corner
steles, and missing paving for age; it refuses uneven ground rather than terracing it, and
it scans for the surface rather than reading a worldgen heightmap so it can also be run with
`/place` (LESSONS #11). Configured feature, placed feature and the biome modifier are all
**generated** by `runServerData`; the modifier targets the `#minecraft:is_overworld` **tag**,
never a biome list.

`tools/worldgen_check.sh` places a shrine in a live flat world and asserts what it built.
Verified failing two ways -- a wrong assertion, and a deliberately broken feature.

`tools/shrine_rate_probe.sh` measures density on **real** terrain (`GRID=8` takes ~2 min).
Measured: **45-46% of natural sites are level enough**, so with the rarity filter at 55 the
real density is **one shrine per ~120 chunks, roughly six minutes of walking**. Re-run it
after any change to `MAX_RELIEF` or the rarity filter -- they multiply. Whether six minutes
reads as furniture or as litter is **[NEEDS PLAYTEST]**; only a person can say.

### Verifying against a live server

`tools/server_smoke.sh` boots a dedicated server, and `COMMANDS` (newline-separated) runs
them over **RCON**, printing the server's own replies:

```sh
COMMANDS='forceload add 0 0 15 15
setblock 8 66 8 interregnum:warning_stele[axis=y]' ./tools/server_smoke.sh
```

This is the only way worldgen and block behaviour get verified without a client, and it is
how the next tasks (shrine structure, deicide event) will be checked. Note that **stdin does
not reach the server under Gradle's runServer** — RCON is not a preference, it is the only
channel that works. See LESSONS #10.

### Datagen works

`gradle runServerData` regenerates every JSON this mod ships into `src/generated/resources`
(committed, and separate from hand-authored resources so a diff shows at a glance which is
which). The run type is **`serverData`**, not `data`, and `--mod <id>` is **required** —
without it the gatherer logs `Initializing Data Gatherer for mods []` and dies with an
unrelated-looking `RejectedExecutionException`.

**Owner's creative call, delegated and taken:** carved shrine stone drops *plain* shrine
stone. You may take the stone; you may never take the word. It is unannounced, it teaches
that the god-script is not a material before any lore exists, and it makes carved stone
found-only — which turns "learning to inscribe" into a real Theoclast reward later.

### The toolchain works

`gradle build` produces `build/libs/interregnum-0.1.0.jar` against real NeoForge 26.2.0.67,
and `tools/server_smoke.sh` boots a dedicated server, waits for load, shuts it down
cleanly through stdin, and fails if the mod logged anything or if content did not load.

Registered so far: `shrine_stone`, `shrine_stone_carved`, `god_heart`, `clast`, a creative
tab, and the datapack-driven dialogue loader. The loader is the seam between `core/` and
the game: Codecs live in the game module so `core` stays dependency-free.

**Every `VERIFY:` marker in PLATFORM.md is now cleared** — see the verified-values table
there. Markers elsewhere (MODELS.md's item-model system, DATAGEN.md's provider names,
WORLDGEN.md's schemas) are still open and should be cleared the same way: read the sources
in `~/.gradle/caches/.../neoforge-26.2.0.67-sources.jar`, never remember.

### CI

`.github/workflows/checks.yml` runs `tools/check_all.sh` on every push and PR. The
workflow deliberately calls that one script rather than re-listing the checks, so CI and a
local run cannot disagree. No Minecraft toolchain is needed for it yet — everything checked
so far is dependency-free Python and Java 21. Verified by breaking three things in a clean
checkout (stale generated texture, missing translation key, broken doc link); each failed
the gate, and the restored tree passed.

### What actually runs today

```sh
python3 tools/palette_build.py                   # solve ramps -> assets/palette.json
python3 tools/palette_check.py                   # enforce the palette law (exit != 0 on fail)
python3 tools/demo_structure.py docs/img         # the worked example
python3 tools/texview.py <png> --tile 8 --scale 6 # the review bench
```

No dependencies. No pip install. Python 3 only.

- **10 palette families, 34 steps**, tightest separation **0.159 L\*** against a 0.12 floor.
- **All five palette checks verified by reintroducing the bug they catch** — see
  [`VERIFICATION.md`](VERIFICATION.md).
- **`pngio.py` round-trips byte-identically** and decodes real externally-produced PNGs
  (checked against DOWNTIME's Blender exports).

---

## Decisions on record

| Decision | Value | Where |
|---|---|---|
| Minecraft version | **26.2** ("Chaos Cubed", June 2026) | [`PLATFORM.md`](PLATFORM.md) |
| Loader | **NeoForge** | [`PLATFORM.md`](PLATFORM.md) |
| Java | **21** | [`PLATFORM.md`](PLATFORM.md) |
| Build | Gradle + **ModDevGradle** 2.0.x | [`PLATFORM.md`](PLATFORM.md) |
| Texture resolution | **16×16, no exceptions for blocks/items** | [`ARTSTYLE.md`](ARTSTYLE.md) |
| Art produced by | **generator scripts, not an image editor** | [`TEXTURING.md`](TEXTURING.md) |
| Colour | **`assets/palette.json` or it is a bug** | [`PALETTE.md`](PALETTE.md) |
| Resources | **generated by datagen, not hand-typed** | [`DATAGEN.md`](DATAGEN.md) |

The owner delegated version and loader explicitly: *"Ill use whatever version you think is
best, same with mod loader."* Both are recorded with reasoning so a future session can
overturn them on grounds rather than taste.

---

## The heartbeat

An hourly Routine (`INTERREGNUM heartbeat`, `trig_01KE2aMo3eAqVz72AtPoJZNW`) fires into
this same session, so it keeps full context. Its prompt is the agreed working contract:
one focused increment per tick, single repo, single branch, never force-push, new scope is
the owner's call, `tools/check_all.sh` green before every commit, and a two-tick circuit
breaker — if all work is blocked or the same failure repeats twice, it stops and says so
rather than improvising.

**Known limitation:** the Routine carries no MCP connectors, so heartbeat ticks have **no
GitHub API tools**. Git over HTTPS still works, so commit and push are fine and PR #1
updates automatically with each push. A tick **cannot** open a new PR, post a comment, or
read PR reviews — if a tick needs any of those, it should record the need in this file and
the owner (or an interactive session) does it.

To pause it: ask, or disable the Routine from the claude.ai Routines UI.

## Waiting on owner

1. **"Warden" collides with vanilla's Warden.** Minecraft already ships a mob called the
   Warden — the deep-dark one. Ours is a bureaucratic enforcement officer and shares
   nothing with it but the word, and in a modpack "a warden" now means two unrelated
   things. Every design doc here says Warden, so that is what is built; the id is
   `interregnum:warden` and the display name is "Warden".

   This is a lore call, not an engineering one, so it is the owner's. If it should
   change, changing it is cheap now and expensive once players learn it. Candidates
   that keep the institutional register: **Assessor** (they assess and file),
   **Invigilator** (one who watches an examination — and the census scene already reads
   as one), **Proctor**, **Registrar**. "Wardenate" as the institution's name could
   survive any of them, or become e.g. the Assessorate. *No action needed unless the
   owner wants a change.*
2. **Playtesting, and looking at the Warden.** This container has no game client, so two
   things about the model are unverifiable here and are not claimed: how it looks
   **animated**, and how it looks **lit**. `tools/entity_view.py` covers shape and paint;
   it cannot cover those. The render has been sent for review.

**Answered:** license is **MIT** (`LICENSE`, `gradle.properties`). `main` branch exists;
work flows to `claude/minecraft-mod-dev-rp0x8j` and PRs into `main`.

## Open questions

### Proposed, needs the owner's yes

- **[PROPOSED] The statue summons the Warden.** Woken statues are already scattered
  across every server by Chapter 0, they already went into people's gardens, and they
  already all opened their eyes at once. Making a woken statue the thing that *calls*
  a Warden turns that scenery into a map of where enforcement reaches — and turns
  breaking one into a real decision with a real cost. It also explains why the mod
  handed everybody free decorative statues for a hundred hours.

  Why this is the owner's call and not mine: it makes statue placement into a
  strategic layer nobody has agreed to, and it gives players a lever on the Wardens
  (tear down the statues, go dark) that reaches into the endgame. It is exactly the
  "absurd system you find out how it ticks and use to your advantage" the brief asked
  for, which is why it should be chosen deliberately rather than arrived at.

### Answered this session

- **The class name**: **Theoclast** — owner's coinage, locked. Breaker and fragment in
  one word; the pieces themselves are **clasts**. Four-voices naming table in `WORLD.md`.
- **The subject**: INTERREGNUM. You accidentally kill the overworld's god by looting a
  shrine chest; vanilla's rules were its policy; the world unravels chapter by chapter
  while you carry the dead god's unanswered letters to its estranged family, looking for a
  successor. Full design + seven-question audit in [`WORLD.md`](WORLD.md).
- **`mod_id`**: `interregnum`, locked, recorded in `PLATFORM.md`.
- **Loader check-in**: NeoForge stands. The mod is broad content+systems (dimensions,
  dialogue, classes, enforcement AI) — the Fabric fork in `PLATFORM.md` was for a small
  mechanical mod, which this is not.

### Still open — [WORKSHOP] with the owner

1. **God roster** — working set is four named gods + the dead one; final count and names.
2. **The dead god's last letter** (the ending document) — co-write with owner, later.

### [NEEDS PLAYTEST] (cannot be settled by argument)

Deicide trigger reliability · clast scarcity · unraveling band pacing.

## Working agreement

Inherited from DOWNTIME, where the owner said: *"this whole project is yours partner, ask me
if you need help with stuff but if not, go ahead and make whatever."* The instruction here
was the same in spirit — *"You know this stuff better than i do"* — so:

- **Decide, document the reasoning, and proceed.** Do not stall on questions that have a
  defensible default; do stall on #1 above, which has none.
- **Push back.** The most valuable thing in DOWNTIME's record is the owner rejecting a
  character that passed every check. That judgement is the best instrument in the project.
- **Never fabricate a lesson.** [`LESSONS.md`](LESSONS.md) holds only things that actually
  happened here; inherited ones are marked PORTED in the doctrine docs. A fabricated war
  story spends the credibility that makes the real ones worth reading.

---

## What to do next

Done this pass: core dialogue engine (+15 verified checks), first scene + validator,
client-leak guard, `tools/check_all.sh` gate, Phase-1 draft textures (shrine stone,
carved, heart, clast) + block models/blockstates, Gradle scaffold for `core`.

In order, all unblocked unless marked:

1. **Bands 3 and 4.** Bands 1 (VIGIL) and 2 (ENFORCEMENT) exist and run. EXODUS and
   ATTRITION are named in [`WORLD.md`](WORLD.md) and empty in `bands.json`, and they are
   the ones that need design rather than typing: block-for-block conversion is the wrong
   grammar for "the ways are open" and "geography frays at the edges". Band 3 probably
   is not a conversion table at all. **Do not fill them in just because the format fits.**
2. **Put Wardens in the world.** The entity exists and nothing creates one. The obvious
   move — and the reason the statues were never consumed — is that a **woken statue is
   what summons them**: the statue is the sensor, the Warden is the response, and
   statue density becomes a thing players can read and act on. That is new scope, so
   it is a proposal under "Open questions" rather than something to build.
   Unambiguously in scope meanwhile: **first Warden contact records
   `Milestone.WARDEN_CONTACT`**, which is what moves the world to band 2 and is
   currently reachable only by command.
3. **The dialogue screen, and the right-click.** The conversation runtime is done and
   verified; what is missing is any way for a player to see or reach it. Two pieces:
   `WardenEntity#mobInteract` opening a table, and a client screen rendering
   `Conversations.Table` (node text, options, and — the whole multiplayer point —
   everyone else's stance as they pick). The screen cannot be verified in this
   container, so build it last and keep every decision on the server, where it is
   already proven.
5. **More dialogue scenes** (shrine-keeper, first dream-audience) and the client screen.
   The engine, the loader and the first scene are done and verified end to end.
6. **Clear remaining `VERIFY:` markers** in MODELS.md, DATAGEN.md, WORLDGEN.md against the
   sources jar, exactly as PLATFORM.md's were.

## Standing warning

> **Every API specific in this doc set is marked `VERIFY:` and unverified.**

The docs were written in a sandbox whose egress proxy blocks `neoforged.net` and
`docs.neoforged.net`. Concepts, structure and formats are sound; **exact identifiers are
not** and must be read from the NeoForge sources:

```sh
find ~/.gradle/caches -name 'neoforge-*-sources.jar' 2>/dev/null
```

Modding APIs churn harder than almost anything else in software. A tutorial dated before
2026 is describing a different versioning era *and* a different registration API.
