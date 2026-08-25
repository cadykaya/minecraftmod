# Log

Phase-by-phase record. Newest last. Written as it happens, not reconstructed.

Distinct from [`LESSONS.md`](LESSONS.md): that holds transferable rules, this holds *what
was done and when*. A thing that will change how future work is done goes in both.

---

## Phase 0 — foundations, before there is a mod

**Context.** The repository was empty — no commits. The owner asked for documentation, in
quantity, split across files so that a session needing one topic does not pay for all of
them. They also delegated the platform choice outright.

**The prior art.** The owner pointed at `cadykaya/mario-3` — the Godot game **DOWNTIME** —
saying its docs on textures and models were worth taking advice from. They were: ~6,900
lines across 15 files, and the art and verification docs carry a large number of lessons
with the receipts attached. That repository is the source of everything marked **PORTED**
here.

### Done

**Platform, verified rather than remembered.** Established by web search that **Minecraft
changed its versioning scheme in 2026** — `1.21.x` was followed by `26.1`, not `1.22`, on a
quarterly game-drop cadence. Targeted **26.2 + NeoForge + Java 21 + ModDevGradle**. Writing
this from memory would have made every version string in the doc set confidently wrong.
Recorded as `LESSONS.md` #3.

**Palette system — built, run, and verified.**

- `tools/colorlab.py` — sRGB ↔ CIE L\*, HSV. Sanity-checked against known values
  (`#808080` → L\* 0.536; black 0; white 1; HSV round-trip lossless).
- `tools/palette_build.py` — solves ramps from one base hue + target L\* per step.
  **10 families, 34 steps, tightest gap 0.159** against a 0.12 floor.
- `tools/palette_check.py` — five checks, exits non-zero. **All five verified by
  reintroducing the bug each catches**, including a single-hex-digit off-palette colour.
- `assets/palette.json` — generated, single source of truth.

**Texture pipeline — built, run, and looked at.**

- `tools/pngio.py` — dependency-free PNG read/write. Verified by byte-identical round-trip
  *and* by decoding real externally-produced PNGs (DOWNTIME's Blender exports).
- `tools/paintkit.py` — palette access, toroidal geometry, deterministic hashing, and
  `Cells`, the structure index that makes structure-aware painting possible. Contains **no
  per-asset constants**, deliberately, following DOWNTIME's `charkit` rule.
- `tools/texview.py` — the review bench. **Defaults to 8×8 tiled**, because the single tile
  is the view that lies.
- `tools/demo_structure.py` — the worked example: hash-painted vs structure-painted stone,
  same palette, same four values. Output committed to `docs/img/`.

**Doc set — 13 documents**, each written to be loaded alone. See [`INDEX.md`](INDEX.md).

### Learned

Two failures, both committed by the session that had just written the section describing
them, both recorded in `LESSONS.md`:

1. **The single tile is the front view.** The first structure-painted stone passed as one
   tile and produced a grid of bright dots as a wall.
2. **A confounded test is a false instrument.** A skip-list test reported `exit=1` because a
   *different* file was failing; it said nothing about the case under test.

### Deliberately not done

- **No Gradle project.** Creating it needs a real NeoForge build number, and this sandbox
  cannot reach `neoforged.net` to look one up.
- **No content of any kind.** Blocked on the subject question — see
  [`HANDOFF.md`](HANDOFF.md) open question #1.
- **No fabricated lessons.** `LESSONS.md` holds three real ones and nothing else.

### Open at end of phase

The subject of the mod. Everything built in this phase is subject-agnostic on purpose and
survives whatever the answer is.

---

## Phase 0.5 — the subject

**The mod is INTERREGNUM.** Decided across one workshop session with the owner, who drove
every major turn. The trail, because the rejections shaped it more than the acceptances:

1. First pitch: parameter-driven "drift" worlds (eight named constants). **Rejected by our
   own doctrine** — false specificity; a slider constrains nobody.
2. Second pitch: governors — physical law as maintained infrastructure, machines you can
   walk to. The architecture survived; the *dress* did not.
3. Owner's pivot: **more fantasy — it should be god.** Real magic, illegal in the
   overworld; the modpack opens with the player accidentally killing Earth's god. This
   fixed the thing the governor version lacked: a wound.
4. Owner locked: the name (**Interregnum**), vanilla-until-death pacing, world-systems
   (each god holds surface/under/far dimensions — vanilla's own triad generalized),
   deicide inheritance → first playable class, the Haunt (dead gods torment their
   killers), repeatable deicide, and **ensemble BioWare dialogue** (SWTOR/DOS) — which the
   owner correctly spotted as the fix for the pseudo-main-character concern.

`WORLD.md` written with the full seven-question audit (passes). Palette semantic law
written: **cool means held, warm means spent.** `mod_id=interregnum` locked in PLATFORM.
Open: class name ([WORKSHOP], shortlist in WORLD.md), god roster, playtest items.

No code yet. Phase 1 is defined in HANDOFF: "Chapter 0 and the Death," overworld only.

**Addendum:** the first class is named — **Theoclast**, the owner's own coinage, and it
beat every candidate on the shortlist by meaning both *god-breaker* and *god-fragment*
simultaneously (iconoclast vs pyroclast). Splinters renamed to **clasts**. The four-voices
naming (Theoclast / Usurper / saint / Executor) is locked in WORLD.md.

---

## Phase 1a — first verified artifacts (autonomous build-out begins)

Owner delegated full build authority; hourly heartbeat to be armed with agreed wording.

**Environment finding that shapes everything:** the egress proxy blocks
`maven.neoforged.net`, `libraries.minecraft.net`, and `piston-meta.mojang.com` — the
whole NeoForge dependency chain. Java 21, Gradle, and Maven Central are open. So the
build order flips: everything loader-independent first, the game module the moment the
owner opens the allowlist. NeoForge 26.2.0.67 recorded from search, marked VERIFY.

**Built and verified this pass:**
- `core/` dialogue engine — pure Java, zero deps: graphs validated at construction,
  four resolution rules (INITIATOR/VOTE/ROLL/UNANIMOUS), stances preserved for the
  table, UNANIMOUS dissent → REPROMPT. 15 self-test checks, verified by three engine
  mutations (each caught) — after first catching the test harness itself reporting
  grep's exit code instead of the JVM's (LESSONS #4).
- First written scene: `warden_intake` — exercises every rule; procedure-voice per the
  dread covenant. `tools/dialogue_check.py` mirrors the Java validation + lang coverage,
  verified by breaking the scene.
- Phase-1 draft textures: shrine_stone (calm uniform ashlar — two failed iterations
  recorded in the builder comments), shrine_stone_carved (ornament band, first pass read
  as ASCII), god_heart, clast. All palette-legal; block models + blockstates generated.
- `tools/client_leak_check.py` (verified by planting a leak) and `tools/check_all.sh` —
  the single gate: palette, dialogue, leak, core selftest, generated-asset staleness,
  doc links.

---

## Phase 1b — core state, mutation testing, and the heartbeat

**Owner decisions:** MIT license; `main` branch created (pinned at the Foundations commit
so PR #1 carries the whole INTERREGNUM body of work as a reviewable diff); owner is the
playtester; hourly heartbeat approved and armed.

**Network:** still denied. Diagnosed precisely rather than assumed — the gateway answers
403 to CONNECT and the proxy logs a per-host policy denial. The owner allowed the domains,
but network policy is applied at container start, so this long-running session keeps the
old set. HANDOFF carries a one-line re-probe for future sessions; the heartbeat runs it
every tick.

**Built:** chapter state machine (milestone-driven, derived-not-stored, monotonic in both
directions) and the regard model (per-institution standing, bands not numbers, permanent
ceilings that make a deicide a scar rather than a debt).

**The pass's real finding:** `tools/mutate_check.py`. Run against a self-test that had
already passed 44 checks and been hand-verified against three mutations, it found **two
surviving mutations** — both assertions correct, both blind. Hand-checking a few mutations
is not checking them all. Now a tool, wired into the gate, 11 mutations all caught.
Recorded as LESSONS #5.

**CI landed.** `.github/workflows/checks.yml` runs the gate on every push and PR — the
first thing enforcing `check_all.sh` anywhere but a local shell. PR #1 had zero check runs
before this. Verified the way everything else here is: three real regressions introduced
in a clean checkout (stale texture, missing lang key, broken doc link), each caught, tree
restored and green.

---

## Phase 1c — the toolchain, and the first thing that actually runs

Owner set network access to **Full**, and it applied to the running container
immediately -- the earlier belief that it needed a fresh session was wrong.

**The mod exists.** Builds against NeoForge 26.2.0.67, and a dedicated server boots with
it loaded (`Done (0.283s)`). Blocks, items, a creative tab, and a datapack-driven dialogue
loader that turns `data/interregnum/dialogue/*.json` into validated core `DialogueGraph`s
-- with the Codecs deliberately in the game module so `core/` keeps its zero dependencies.

**Four platform facts were wrong and are now verified rather than remembered:** Java 25
(not 21), ModDevGradle 2.0.144 (not 2.0.141), no Parchment for 26.2 at all, and pack
formats 88/107 (a guessed 90 was rejected by the running game). The registration API was
wrong too and the compiler corrected it. PLATFORM.md now carries a table of every value
with how it was checked.

**Two more lessons about blind checks (LESSONS #6, #7).** Stopping the server with `pkill`
left a stale `session.lock`, so the next boot died before loading anything -- and the check
meant to prove a fix reported "0 = fixed" while measuring nothing. Then the smoke test's
first log filter ignored an exception's header while flagging its own `Caused by:` lines,
where the obvious fix would have blinded it permanently. Both became real tools:
`server_smoke.sh` (clean stdin shutdown, fails if the server never loads) and
`server_log_check.py` (record-based attribution, a written reason per ignore entry).

**The smoke test asserts content, not just boot.** Verified by removing the only dialogue
file: the server starts perfectly, logs no errors, and the check now fails with "loaded 0
dialogue graph(s), expected at least 1". That is `VERIFICATION.md`'s "green tests are not a
working feature", caught by a test instead of by a player.

CI gained a `game` job that does all of this on every push, with the Minecraft artifacts
cached.

---

## Phase 1d — steles, and the registry gate

**Warning steles** exist as a real rotatable block: a dressed slab with a recessed panel
carrying two columns of god-script. The script is drawn from a five-glyph stroke grammar
rather than scattered pixels, so it reads as *writing* at play distance without any glyph
being readable -- which is the whole job, since Chapter 0 players walk past these for hours
reading them as ruin dressing. One column was tried first and read as a single scratch with
a half-empty panel.

**`tools/registry_check.py`** closes VERIFICATION.md's Tier 1 item 1. Every registered
block resolves a blockstate, every model it names, every texture those models reference,
and a translation key; block items resolve their block; missing loot tables are reported as
warnings (nothing generates them yet, and a known-absent thing reported as a hard failure
trains people to ignore the tool). Verified by breaking four things -- a missing lang key,
a missing texture, a blockstate pointing at a missing model, and **the registration API
being renamed so the check would go blind**. That last one matters most: six API facts have
already churned under this project in one session.

Loot tables are now the top of the queue -- three blocks currently drop nothing when mined,
which is a real player-facing bug the new check surfaced immediately.

---

## Phase 1e — datagen, and the first design decision made in code

Owner approved the art ("super minecrafty" -- which is exactly what ARTSTYLE's
vanilla-adjacency rules exist to produce) and delegated the loot question outright.

**The answer taken:** plain shrine stone drops itself, because the world not stopping you
from taking the god's things *is* the opening -- a shrine you were forbidden to touch could
never have been looted. But **carved shrine stone drops plain stone.** You may take the
stone; you may never take the word. Nothing announces it. A player mines an inscribed block,
gets an uninscribed one, and learns before any lore exists that the script is not a
decoration and not a material. It also makes carved stone found-only, which turns the
ability to inscribe into a genuine Theoclast reward much later. Steles drop themselves,
because a warning you can carry is a warning you can misplace, and on a server that is an
interesting thing for someone to do.

**Datagen works.** Two API corrections, both from the running tool rather than memory:
the MDG run type is `serverData` (the error helpfully lists the valid set), and `--mod <id>`
is required -- without it the gatherer reports `for mods []` and dies with a
`RejectedExecutionException` that names nothing relevant. Output goes to
`src/generated/resources`, committed and visibly separate from hand-authored resources.

**A third API correction:** `@EventBusSubscriber` in 26.2 has only `value()` and `modid()`
-- there is no `bus` parameter; the bus is inferred from the event type. Every pre-2026
tutorial writes `bus = Bus.MOD` and it no longer compiles. `ARCHITECTURE.md` corrected.

`registry_check.py`'s loot warning is now a hard failure, since something generates them.
CI's game job regenerates data and fails on a dirty tree.

---

## Heartbeat tick 1 — the smoke test was lying

Network probe: 200. Picked HANDOFF item 1 (the shrine structure) and got one step in before
discovering that the tool meant to verify it does not work.

`server_smoke.sh` wrote `stop` into a fifo on the server's stdin. **Stdin does not reach the
game under Gradle's `runServer`.** No command ever ran, and the "clean shutdown" documented
in LESSONS #7 was the JVM's SIGTERM hook saving chunks -- which looks identical in a log.
The tell was one grep: `Stopping server` appeared in zero smoke logs, and had been absent
all along.

Replaced with **RCON** (`tools/rcon.py`): commands come back with the server's own reply
instead of being hoped into a pipe. `Stopping server` now appears, and
`setblock 8 66 8 interregnum:warning_stele[axis=y]` answers *"Changed the block at 8, 66,
8"* -- the first proof that one of our blocks can exist in a real world with a valid
blockstate.

LESSONS #7 corrected in place (an unverified claim, in the entry about unverified claims)
and #10 added. The shrine structure moves to the next tick, now that there is a way to check
it.

---

## Heartbeat tick 2 — shrines exist

`ShrineFeature` builds a wayside shrine: a 5x5 court, a carved stone at the centre where the
heart will sit, steles at the corners (some fallen), and missing paving for age. It rejects
uneven ground rather than terracing it -- a shrine cut into a cliff looks like a bug, a
shrine that simply is not there looks like nothing. Coded rather than an NBT template,
because an NBT template has to be built by hand in a client this project does not have, and
cannot be reviewed in a diff.

Configured feature, placed feature and the biome modifier are all generated by
`runServerData` via a `DatapackBuiltinEntriesProvider`, so a wrong registry key is a compile
error rather than a shrine that never appears. The modifier targets `#minecraft:is_overworld`
and runs at SURFACE_STRUCTURES, so paving happens before grass is scattered over it.

**Two bugs the new check found, both in the author's work rather than the framework:** the
feature read a generation-phase heightmap and so errored on any live chunk (LESSONS #11),
and the smoke test had been silently reusing a world created before `server.properties`
existed -- so "flat world" tests were running on ordinary terrain, which is why identical
coordinates placed in one spot and refused in another. `server_smoke.sh` now deletes
`run/world` unless `KEEP_WORLD=1`.

`tools/worldgen_check.sh` is in CI and was verified failing on a wrong assertion and on a
broken feature.

---

## Heartbeat tick 3 -- how often is a shrine

Shrine density was a guess (1 rarity attempt per 90 chunks) and `WORLDGEN.md` is blunt that
"a structure nobody finds is a structure you did not build". The unknown was never the
rarity filter; it was how much natural terrain `ShrineFeature` refuses as too uneven, which
multiplies against it. `tools/shrine_rate_probe.sh` now measures that directly by attempting
a placement at every chunk centre of a real-terrain grid.

Its first run reported **100% acceptance** -- because `${LEVEL_TYPE}` had been added inside
a *quoted* heredoc, which expands nothing, so the server kept booting flat. Caught by
checking the instrument (grep the properties file) rather than by the number looking wrong;
100% looks fine. LESSONS #12.

Real answer: **45-46% acceptance**, which also validates `MAX_RELIEF=2` as selective rather
than crippling. Rarity retuned 90 -> 55 for a target of one shrine per ~120 chunks, about
six minutes of walking -- these are Chapter 0 furniture, and a player has to pass enough of
them to stop looking, so that the one with a heart in it reads as "another shrine" right up
until it does not. Whether six minutes is right is [NEEDS PLAYTEST].

The probe reads the rarity out of the generated JSON rather than keeping its own copy.

---

## Heartbeat tick 4 -- the world remembers

`ChapterSavedData` persists the tested core `ChapterState` on the overworld's storage,
serialised through the same single string the core self-test already round-trips.
`/interregnum status` reads it; `/interregnum record <milestone>` (level 2) advances it.
Recording DEICIDE moves a world from DORMANT to VIGIL and it is still VIGIL after a restart.

Two API corrections from the sources rather than memory: `SavedDataType` is a codec-based
record, and `CommandSourceStack.hasPermission(int)` no longer exists -- permissions moved to
a PermissionSet model, so the idiom is
`.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))`.

**The check was blind and mutation testing caught it.** Deleting `setDirty()` did not fail
the first version, because `SavedDataStorage.set()` marks freshly-created data dirty -- so
create-then-mutate-then-restart passes whatever the mod does, and only data LOADED from disk
exposes the bug. The check now restarts, mutates the loaded data, and restarts again;
deleting `setDirty()` now fails it by name. LESSONS #13.

Both the worldgen check and the persistence check are in CI's game job.

---

## Heartbeat tick 5 -- the sun stops

The deicide exists. `Deicide.commit()` is the single place the catastrophe happens: it
records the milestone, remembers who did it, and **stops the day cycle in every level**.
The day cycle was the god's, and with nobody left to turn it the light stays exactly where
it was at the moment of death. There is no message and no name, per WORLD.md -- the world
just stops moving.

It is idempotent, because a world can only lose its god once, and both the chapter and the
stopped sun survive a restart, because otherwise the catastrophe un-happens the first time
an operator reboots.

**Two callers, one implementation.** The pickup handler needs a real player and a headless
server has none, so `/interregnum record deicide` calls the same method. The command is not
a test hook bolted on; it is the second legitimate caller, and having exactly one
implementation is what lets the untestable path be three lines of adapter over a path that
is verified end to end.

Three more API facts corrected from the sources: **gamerules were renamed in 26.x**
(`doDaylightCycle` is now `advance_time`, and most of the set moved with it),
`ItemStack.is()` takes a `Predicate<Holder<Item>>`, and `ServerPlayer#getServer()` is gone
-- the server comes off the level.

`tools/deicide_check.sh` asserts the whole beat and is mutation-verified: removing the
consequence fails with "the sun did not stop", removing idempotence fails with "a second
deicide was NOT a no-op". In CI.

---

## Heartbeat tick 6 -- the heart is in the shrine

Chapter 0 is now playable end to end: walk, find a shrine, open the offering box standing on
its centre stone, and eventually take something warm and gold out of it, and the sun stops.

The offering box is deliberately obvious. The opening of this mod is a player doing the most
ordinary thing in Minecraft, and it being deicide -- a shrine you have to dig up would be a
puzzle instead.

**Uniqueness without bookkeeping.** `interregnum:god_lives` is a custom loot condition that
is true only while the overworld has a god. Loot tables roll when a container is first
OPENED, not at worldgen, so every shrine is a candidate until one pays out and none are
afterwards. No shrine is chosen in advance and nothing is tracked per-shrine: the heart is
somewhere until it is taken, and then it is nowhere.

Measured 8/60 (13%) before the death against a configured 12%, and a deterministic 0/60
after.

**The probe was wrong first, again.** It reported 47/60 because `setblock` on a position
that already holds that block is a no-op, so the same chest survived every iteration and
`loot insert` kept adding to it. The check PASSED both before and after the fix -- its
assertions were sound and only the number was nonsense, one sentence away from being quoted
as a measured drop rate. LESSONS #14, and the sixth bench in this session to be wrong on
first run.

---

## Heartbeat tick 7 -- the ground gives way

The crater exists, and it is **subsidence rather than an explosion**. Nothing detonated: a
god died and the world stopped being held up in that spot, so it sinks. No fire, no
scorching, no thrown blocks, nobody hurt -- which is deliberate, because the person standing
next to it is the one who just did it and the mod is not in the business of punishing them
for the thing it tricked them into.

**Only natural ground moves.** Minecraft does not record who placed a block, so the
guarantee is enforced by a narrow tag whitelist that errs toward sparing -- an unlisted
block is left alone. A lumpy crater is a cosmetic complaint; a deleted house is somebody
quitting the server. And the image it produces is the one the beat wanted: a house at the
shrine left hanging over a pit, untouched and resting on nothing.

Verified in a live world: diamond block, chest, planks and glass all survive; the dirt under
and around them does not; bedrock holds. Mutation-verified both ways -- removing the
whitelist fails with the list of destroyed player blocks, removing the crater fails with
"the ground did not subside".

Also cleaned a stale HANDOFF entry that still listed already-finished work.

Next is the unraveling, and it needs design before code: the crater gets away with a tag
whitelist because it fires once at one spot, but the unraveling runs forever over a whole
world and will need real placement tracking.

---

## Heartbeat tick 8 -- every statue opens its eyes

The beat this chapter was built toward. `warden_statue` is decorative furniture for the
whole of Chapter 0 -- the mod hands you a nice statue for hours and players put them in
gardens -- and the moment the god dies, every one of them wakes, including the one in your
garden.

The art carries it: the eyes are the ONLY warm pixels on an entirely cool figure, so a woken
Warden reads across a field, and the ember says what ember always says in this palette --
this is running on the corpse.

Two paths. Statues within eight chunks of the site or of a player wake instantly; everything
else wakes on chunk load, which is the better version anyway because the player gets to
notice it themselves. Only already-loaded chunks are touched: `getChunk(..., false)` never
generates terrain as a side effect of a god dying. Waking is a blockstate flip -- no block
entity, no ticking, no bookkeeping.

**The test lied first, in a new way.** It reported no markers at all, because `setblock` at
an unloaded position answers "That position is not loaded" and carries on -- so the far
statue never existed and every assertion was measuring an empty coordinate. The check now
asserts its own setup. LESSONS #15, which names the pattern the last four lessons have all
been wearing different disguises of: believing an outcome without checking the conditions
that make it meaningful.

Art notes: the statue's first pass drew its visor at a fixed width, overflowing the head
into the empty margin, and read as a box with ears. Marks are clipped to the silhouette now
-- paint that leaves the silhouette IS the silhouette being wrong.

---

## Heartbeat tick 9 -- the world remembers what people built

The prerequisite for the unraveling, designed before it was written as promised.

Minecraft does not record who placed a block. The crater gets away with a tag whitelist
because it fires once at one spot; the unraveling runs forever over a whole world and would
eventually take somebody's cobblestone wall on the grounds that cobblestone is natural.

`PlacedBlocks` is a chunk attachment of chunk-relative positions, one int each (x and z are
4 bits, y is 9 -- a BlockPos long would cost twice the memory for information the
attachment's own chunk already implies). Past 4096 placements the chunk **saturates**: the
set is dropped and the whole chunk counts as claimed. Memory is bounded, the failure
direction is safe -- it protects more, never less -- and it is the honest answer anyway,
because a chunk with four thousand placed blocks in it is somebody's.

Breaking a block forgets its position, or mining through your own wall would leave permanent
invisible holes the unraveling could never touch. But a saturated chunk stays saturated:
once a place is somebody's, digging a hole does not make it wilderness again.

`Claims` fails CLOSED -- an unloaded chunk answers "claimed", because the unraveling has no
business touching ground nobody is looking at, and the cost of being wrong that way is a
block that does not decay rather than a block a person made and cannot get back.

`/interregnum claim at|record|forget` are operator tools rather than test hooks: any world
predating the mod is full of untracked builds and an admin needs to say "this is ours".
They also make the tracker testable without a player, which is why the event handler stays
three lines.

Two more API corrections: `BlockEvent.BreakEvent` is gone (it is
`net.neoforged.neoforge.event.level.block.BreakBlockEvent`, its own package), and
`ChunkAccess.setUnsaved(boolean)` is now `markUnsaved()`.

Mutation-verified three ways: no serialisation fails the restart, no cap fails saturation,
and a forget that un-saturates fails by name. The check's own last assertion was blind
first -- it read the smoke test's epilogue instead of the command reply, and failed on
correct behaviour.

## The overworld begins to spend itself

`bands.json` has existed since the first design pass and nothing read it. Now it does.

The order of the gates is the design. Dormancy first, because Chapter 0 promising to change
nothing is the whole reason the death lands. Then the band, then the scope, then **the
claim** — deliberately before the probability roll, so a block somebody placed is never a
candidate rather than merely usually spared. A guarantee that holds on average is not one.

Every gate answers by name (`OUT_OF_SCOPE`, `CLAIMED`, `UNSUPPORTED`) rather than returning
a bare false. This system's normal output is "nothing happened", thousands of times a
minute, and the difference between working and broken is invisible from outside unless it
can be asked why.

Sampling is the surface column near players. That reads like a performance compromise and
is not one: `Claims` fails closed on an unloaded chunk, so unloaded ground was never
reachable, and the table's blocks are one layer thick — uniform sampling through the world
would have hit grass and flowers so rarely that band 1 would have appeared to do nothing.

`thin_places` is the crater (now persisted on the chapter data, where the ferry and the
ghost will both want it) plus any chunk beside one holding shrine masonry. The shrine test
reads section palettes rather than blocks, and `maybeHas` is honest about being a maybe: a
section with a global palette answers yes to everything. That over-reports, which widens a
gentle band slightly; under-reporting would have made band 1 invisible. Wrong in the safe
direction, on purpose.

Two holes found by asking what could be deleted while the checks still passed, both now
[`LESSONS.md`](LESSONS.md) #16:

* the committed table contained `oak_leaves -> dead_bush`, which is well-formed, passes all
  six data checks, and can never fire — a dead bush cannot stand in a canopy. It is now
  `-> air` ("the canopy thins"), and the runtime refuses any state whose `canSurvive` is
  false, reporting `UNSUPPORTED` by name.
* every assertion reached the system through a command, so an unsubscribed tick handler
  would have passed all of them. `/interregnum status` now reports `ticks=`/`passes=`.

`tools/unravel_check.sh` runs both halves against a live server, the second with a datapack
that replaces the table — which verifies the override path and the support guard in one
assertion, since the shipped rules would have answered differently. Seven mutations, seven
caught: no claim check, no scope, no band gate, no dormancy check, no support check, and
either handler unsubscribed.

Bands 3 and 4 stay empty. "The ways are open" and "geography frays at the edges" are not
block substitutions, and the table would happily accept a pile of them.

## The Warden takes the field

The statues have been watching since the god died and nothing ever arrived. Now
something does.

The decision that shaped the rest: **a woken statue is not consumed.** It would have
been easy to have the statue animate and walk off, and it would have deleted the best
image the mod has — every statue on the server opening its eyes at once, including the
one in your garden. So there are two objects. The eye and the officer are not the same
thing, and the statue stays exactly where its owner built it, forever.

**A Warden never attacks**, and that is enforced rather than intended: no target
selector, no melee goal, and no `ATTACK_DAMAGE` attribute at all, so a careless goal
added later has nothing to reach for. A thing that walks up, stops at conversational
distance and files a report is worse than a thing that swings, because there is no move
that resolves it. `warden_check.sh` asserts the attribute's *absence*, and the check
passes on an error reply — the command is supposed to fail.

### A bench for looking at mobs

MODELS.md has said since the first week that silhouette is the whole design, that the
front view lies, and that you judge in rotation. None of that was possible: a texture
sheet shows unwrapped nets, and the game was the only other renderer available.

So geometry now lives once, in `tools/entity_specs.py`, and three things read it -- the
texture painter, the generated `WardenGeometry.java`, and `tools/entity_view.py`, which
ray-casts the boxes orthographically and samples the real texture through Minecraft's
own unwrap. It refuses to exist if two nets overlap, which is a failure that renders
rather than crashing.

It earned its keep immediately. The robe was one box; as a net and from the front it
looked fine, and assembled in profile it was a bollard — the "judge it with the head
hidden" failure arriving exactly as advertised. Two stepped boxes fixed it, and that
was only visible because the figure could be seen. The remaining weakness (pure profile,
where the arms hide inside the torso) is written down in MODELS.md rather than left to
be rediscovered.

### The constructor that decided nothing

`setPersistenceRequired()` sat in the constructor with a comment explaining why Wardens
must never despawn. A probe printed `PersistenceRequired: 0b`. `Mob` reads that field
straight back out of NBT on every load, `/summon` loads, and the constructor's decision
was overwritten before anything could observe it. It moved to
`requiresCustomPersistence()`, which no tag can undo.

What actually caught it was running an **exploratory probe with no assertions** and
reading the raw replies before writing anything to match them. Assertions written first
would have said `1b`, the new check would have failed, and the obvious suspect on a
failing new check is the check. LESSONS #17.

Wardens exist and nothing spawns one; that is the next decision and it is the owner's,
because the good answer -- the woken statue calls them -- turns statue placement into a
strategy layer and hands players a lever on enforcement that reaches the endgame. It is
in HANDOFF as a proposal, not in the code.

## The table argues

`warden_intake` has been written since the first week and no one could reach it. It
runs now, and everything about how it runs is server-side.

The engine in `core/` already knew who wins a node. What did not exist is the half a
server has to own: who is at which table, when a node resolves, and what happens when
somebody walks off mid-sentence. Every rule in `Conversations` is a way a table wedges
in front of real people -- resolve on the last pick rather than the first, a leaver's
vote leaves with them, a departure that completes the table resolves it on the way
out, the initiator leaving ends it, and silence times out rather than deadlocking.

Participants are opaque string ids rather than players. That is what the core engine
asked for, and it turns out to be the whole reason this could be built at all here: a
container with no game client can still drive three-way votes, ties, failed unanimity
and someone rage-quitting, because none of that needs a player object.

Talking to a Warden is what records `WARDEN_CONTACT` -- being addressed, not seeing
one -- so a world can reach band 2 by playing now instead of by command.

Two core fixes fell out. `Conversation.remove` did not exist despite the class comment
describing exactly when a caller would need it. And stance ordering was being thrown
away by `Map.copyOf`, which matters because who spoke first before the others fell in
behind them is most of what an argument reads as.

The more useful half of this pass is in LESSONS #18 and #19, and neither is about
dialogue. One mutation reported `[CAUGHT]` while the assertion it was meant to verify
never ran -- deleting the guard crashed the server, and a different instrument caught
it. The check under test was still unverified while the report said otherwise. And the
stance-order test, in its first form, asserted one observation against a literal, which
cannot work when Java salts immutable-map iteration order per JVM run; the version that
works asserts that two tables with different submission orders *differ*.

Still missing: the screen. A player cannot see any of this yet.

## Playable, in chat

The conversation runtime worked and nothing in the game could open one. Two pieces
closed that: right-clicking a Warden, and an interface.

The interface is **chat**, and that is a decision rather than a stopgap. Clickable
text is vanilla, so a player on an unmodified client can play every scene in the mod
with no client code in existence — which means the dialogue system could be finished
and verified end to end in a container that has no game client. The screen, when it
comes, upgrades something already working instead of being the thing between the
writing and anyone reading it.

Right-clicking pulls in **everyone within 8 blocks with line of sight**, not just
whoever clicked. That is the SWTOR beat the owner asked for by name, and it is the
only version where the resolution rules mean anything: a VOTE node with one player at
the table is an INITIATOR node with extra steps. Standing back is how you decline.

`/interregnum reply` is the single unprivileged node in the whole command tree, which
is what makes it safe to hang off a clickable option — it can only ever speak for
whoever ran it. Everything under `talk` moves other people's conversations and stays
gamemaster-gated.

Two view bugs, both found by rendering it and reading the output rather than by
reasoning about it. The waiting line counted the reader among the people being waited
for, so one of two participants was told the table was waiting on two of them. And a
participant who had answered was not told what they had said, which matters because
re-picking before a node resolves is legal.

The second of those produced the more useful failure. The mutation that removes the
viewer exclusion was **MISSED** on the first attempt: the assertion only asked whether
a "waiting on 1 other(s)" line existed, and a view that counts the reader still emits
one from some seats. The fix was to assert an exact tally *and* an absence — three
"waiting on 1" and zero "waiting on 2" — which is LESSONS #18 arriving from the other
direction. There, a mutation was caught by the wrong instrument; here, a mutation
walked straight through a real assertion that was simply too weak to see it. Both say
the same thing: read what the mutation run actually did, never just its exit code.

## Two scenes, and the ending nobody saw

`shrine_keeper` and `dream_audience` are written.

The keeper is the dread covenant's permitted comedy and none of the forbidden kind.
The joke is the *system* -- an offering ledger that must be reconciled quarterly, for
a report, filed for a reader who is dead -- and the keeper is played completely
straight. Nobody in the scene is stupid. The keeper knows. Everyone knows. The
quarter closes anyway. It is also the mod's first consequence conversation without
saying so: the shortfall in the box is the players' own looting, which is how the god
died, and they are being asked very politely to help with the paperwork for their own
deicide. Telling the keeper there is nothing on the other end of the slot is a
UNANIMOUS node, because it is irreversible and nobody should be able to do it alone.

The dream-audience is every node INITIATOR, and that is the design rather than
laziness: every other scene is an ensemble where each player has an equal voice, this
is the one table nobody else sits at, and the contrast is the point. The god calls
them EXECUTOR and never explains it -- an executor settles an estate, and a line
explaining that would take the work away from the word. It is not seeking revenge. It
is trying to hand over a job. The roster stays uncounted ("a family", never a number)
because the pantheon size is still [WORKSHOP] and shipped dialogue would settle it.

Playing the keeper through found a bug that nothing else could have. **The terminal
node was never shown.** The table closed the instant the conversation ended, so every
scene's last line -- the payoff of every branch, the line the whole conversation walks
toward -- was resolved, recorded and thrown away without ever reaching a player. From
the inside nothing was wrong: the state machine was correct, the tests passed, the
resolution was right. It was only visible by reading what came out.

Two verification notes, both worth more than the fix.

The mutation for that fix was **MISSED**, and correctly so: the assertion reads the
command's rendering of the finished table, not the push that sends it to players, and
`push` writes only to real ServerPlayers of which a headless server has none. That
limit is now written into the check itself rather than left for someone to assume
away.

And a second mutation reported MISSED when it had never been applied at all -- a
regex that did not match, and a script that carried on past it. The result looked
exactly like a real hole in the suite. Every mutation run now proves the file changed
before it believes the outcome, which is the same rule as LESSONS #18 one level down:
check what actually happened, not what the exit code implies.

## Conversations have consequences

`RegardState` has sat in `core/` since the first week, fully tested, and nothing read
or wrote it. Dead tested code is a smell; it is now the spine of what a conversation
is for.

One rule carries the whole design: **each participant is judged on what they said, not
on what the table decided.** A vote you lost is still on your record with the party you
sided with, and going along with a group atrocity does not launder it, because you
still said the words. Without that, every player ends up with the initiator's record
and the ensemble system is decoration -- the only choice that ever mattered would be
whoever clicked first. The live check proves it the only way that means anything: two
players at the same node, opposite stances, opposite records, while the table resolves
to one of them.

Effects are data, and the data check refuses unknown institutions, non-integers,
out-of-range values, no-op zeroes, and anything bigger than 25 -- a band is 35 wide and
one sentence should not cross one. All five watched failing.

Nothing is announced. No karma bar and no "+5 Villages": you find out what an
institution thinks of you from how it treats you. The operator readout prints numbers,
which is not the meter coming back -- its audience is someone asking "did that scene do
anything", and the first version printed bands only, where a conversation that moved
five institutions read exactly like one that moved none.

Wiring the deicide surfaced a contradiction worth recording: `recordDeicide` capped
VILLAGES along with the gods, while WORLD.md's four voices has the villagers whispering
*saint* for the killer. Mechanics that contradict locked lore are a bug, not a
tradeoff, so the coupling was narrowed to gods. The people are genuinely undecided and
that is the content; it is also the only regard the killer can still play for, besides
the ghost's.

And the persistence check earned its keep on its first run. Saved records were restored
through a RELATIVE api after the ceilings had already pulled every capped value down to
its cap -- so a god saved at -45 under a -10 ceiling came back at -55, then -65, drifting
by exactly the cap every restart until it hit the floor. Nothing threw, nothing logged,
every number stayed legal. LESSONS #20, and the second half of that lesson is the one
that matters: a single reload showed -55, which reads like an off-by-something. It takes
the SECOND reload, showing -65, to prove the record is walking rather than merely wrong.
The check boots three times now.

## The Haunt begins

The dead god reaches its killer now. Sleep, and it is there.

The gate is the feature, and every clause in it is a way the beat goes wrong. Nothing
haunts anybody while the god is alive. Only the killer -- this is the ghost's private
conversation, and an admin with good intentions must not be able to hand it to
somebody else. Once, because that is what "first dream-audience" means. And a player
already at a table is deferred rather than evicted, *without spending the dream*, so a
coincidence of timing cannot cost them the only scripted delivery the scene has.

That last clause is the one worth having written a check for. A deferral that quietly
recorded the milestone would be invisible: the player sleeps, nothing happens, and
nothing ever happens again. The check proves it by counting openings -- one after a
deferral, one forced -- so a BUSY that spent the milestone leaves only one and fails
by name.

It fires on WAKING rather than on lying down. The conversation needs somebody
conscious enough to answer it, and Minecraft's sleep is a skip rather than a duration,
so what the player gets is the dream they just had.

The handler is three lines. Everything else lives where a command can reach it,
because a headless server has no sleeping players and this would otherwise be a
[LOCKED] headline beat with no way to test it at all -- the same arrangement as the
deicide, for the same reason, and `haunt dream ... force` is a real tool besides: a
player who slept through a crash has no other route back to the scene.

One small thing found by asserting it: a bystander who is offered the dream and
refused ends with NO RECORD AT ALL, not an empty one. Reading regard cannot create a
file. An institution's opinion of somebody it has never dealt with is an absence
rather than a nought, and the check now says so -- my first assertion there expected
the weaker thing and was wrong.

## The shrine-keeper, in person

The scene written two hours ago has somebody to speak it.

The keeper is the Warden's opposite in every way that shows, and the palette law does
the characterisation without a word. HELD is cool, SPENT is warm. The Warden is cold
worked metal on a tall frame under a wide mantle; the keeper is short, hooded in
cloth, warm brown -- a person still reconciling a ledger for a reader who is dead,
which is precisely what spending yourself looks like. No ember on them anywhere: that
step is the dead god's, and a living person who happens to be sad is not running on
the corpse.

The second entity cost a fraction of the first, which is the point of having built the
pipeline. Spec, paint, generate, look, fix, look. And LOOKING is what earned it --
three faults that no amount of reasoning about the numbers had surfaced:

* the hood is a box over the head's top half, so eyes painted in the upper rows were
  geometrically INSIDE it and never rendered at all. What came out was a hooded figure
  with a blank pale bandage for a face and nothing to look back with.
* the ledger -- the entire silhouette signature -- disappeared in profile, sitting at
  exactly the torso's depth.
* its page edges were painted on the top face only, which nobody ever sees on a mob
  under two blocks tall.

All three are the same mistake in different clothes: paint placed without asking where
the geometry actually is.

Killing a keeper costs VILLAGES -25, and only when a player did it. WORLD.md says
regard moves on choices AND deeds, and until now only choices moved it -- which
quietly taught the opposite lesson, that the only thing anybody is ever judged on is
dialogue. A death by no player's hand costs nobody, and the check proves that half
rather than the half it cannot reach: with no players on a headless server, the murder
charge itself is unverifiable here, and the fairness guarantee is the part that is.

## Somebody is at the shrine

Every shrine gets a keeper now, placed at worldgen, standing beside the offering box
and facing it.

The spot is chosen rather than fixed. The court has missing paving by design -- that
is what makes it read as old -- so the placement walks candidate tiles, edges before
corners, and takes the first with solid footing and two blocks of headroom. If none
qualify the shrine gets nobody, because a keeper standing inside a stele or hovering
over a cave mouth is worse than a shrine with nobody at it.

Two API notes, both read from the sources rather than remembered: `Entity#moveTo` is
`snapTo` in 26.2, and there is a `BlockPos` overload that bottom-centres for you.

And one bug that only a live run would ever have shown. The first version had the
keeper standing correctly and **facing away from the box** -- Minecraft's yaw is the
negated atan2 of the offset, the offset here already points away from the centre, and
the two negations do not cancel. Nothing about that is visible in the source, in a
compile, or in any assertion about existence. It is the sort of thing you only ever
find in a screenshot, so it now has an assertion instead: the check computes the
keeper's look vector from the reported yaw and requires it to point at the box.

The check that caught it also caught the tool that nearly ate it. Inlining the Python
parsing as a heredoc inside `worldgen_check.sh` closed the OUTER heredoc early -- the
one feeding commands to the server -- and silently corrupted the file, with the only
symptom being a Python syntax error about an unterminated string and a permission
error on a path that had become a command. Nested heredocs sharing a terminator have
no useful error. The parsing lives in `tools/keeper_pos_check.py` now.

Flagged rather than hidden: the ledger scene opens with "the offering box is short",
which presumes the box has been looted. At an untouched shrine the line still works --
offerings stopped coming -- but the "It was us. We took it." reply becomes a strange
lie. HANDOFF has it as the next content item.

## The world tells you, mildly

The ledger scene presumes the offering box has been looted -- that is the whole
consequence-comedy engine, the players asked politely to help with the paperwork for
their own deicide -- and at an untouched shrine its first line is simply false.
Diluting it so it worked in both cases would have cost the best thing about it. So
there are two scenes and the keeper picks.

`shrine_keeper_intact` is the same person before any of it. The ledger balances, the
quarter is closing, they are pleased somebody came. The warning is real and arrives
as an apology for the housekeeping: the box opens for the one it is addressed to,
nobody has come to open it in a very long time, and they keep it tidy anyway because
it would be embarrassing otherwise. Ask who it is addressed to and the register says
"the holder", and they have never needed to write anything else down, and you will
forgive them, it has not come up.

A player reads that as flavour and opens the chest. That is the design. The world told
you, in a tone so mild you did not register it as being told.

The signal is the box's own PENDING loot table, which Minecraft clears the instant a
container is unpacked: no bookkeeping of ours, nothing to keep in sync, still true if
an admin replaces the chest.

Two Minecraft gotchas, both found by probing rather than reasoning, and both already
in LESSONS in other clothes.

`@e` could not see an entity summoned into a chunk the same command block had just
forceloaded -- the chunk had not finished loading. Every earlier check happened to
summon at (8, 8), inside the always-loaded spawn chunks, so this never showed. The
new assertions use the shrine at spawn for the same reason.

And clearing the loot table by writing a plain chest over the loot chest is a NO-OP:
setblock on an identical block does nothing, and the block entity survives untouched
with its loot table intact (LESSONS #14, in my own test this time). The first version
of the check "proved" the keeper stayed on the intact scene after a looting that had
never happened. Air first, then chest -- and the check now asserts the loot table is
present before, and gone after, so the setup cannot silently fail again (#15).

## The keeper stays put

CI failed on a check that passes here every time, and it was right to.

`worldgen_check` asserted that an entity was within four blocks of the offering box
and that its yaw pointed at it. Both are reasonable. Both passed locally, including
two deliberate back-to-back runs looking for flakiness. The runner failed on the
first attempt -- because the keeper had a stroll goal, RCON commands arrive seconds
apart, the server ticks throughout, and by the time anything asked, the keeper had
walked out of range and looked away.

My first explanation was that the keeper had wandered. It was wrong, and I said it
before testing it: a probe that placed a shrine and queried the keeper 120 commands
later found it at exactly its spawn coordinate, unmoved. The cause of the CI failure
is still unknown.

What the episode actually produced is better than the explanation would have been. The
check could not say WHY the property was missing, and now it can -- it dumps every
keeper reply and the placement log on failure. And chasing it turned up an ignored
boolean: `addFreshEntity` answers false when the level declines an entity, which would
leave a shrine with no keeper and nothing in any log to say so. That return is checked
now.

The two halves still needed different treatment.

The POSITION assertion was replaced by a tether assertion. A shrine-keeper who wanders off leaves
a player standing at a shrine with a scene and nobody to have it with. They are
tethered now, and the check asserts the tether -- which is time-invariant -- rather
than the position, which is merely a consequence of it. CI did not find a bad test
here. It found a bad mob.

The FACING could not be asserted at all. A mob's yaw is set at placement and
overwritten by whatever it looks at next, so no later moment still holds the evidence,
and widening a tolerance would only have made the check pass without checking. The
arithmetic moved to core/spatial/Facing: four assertions against the four cardinal
directions, plus the exact shape the shrine uses (stand east of a thing, face west),
and two mutations. Twenty now, all caught.

LESSONS #21, and its corollary is the part I will need again: a flaky check can be
perfectly reproducible on one machine. Running it twice here proved nothing, because
both runs had the same timing. The disagreement between two ENVIRONMENTS is the
signal; two runs in one environment is not.

---

## The keeper was never missing

The answer to yesterday's unknown, and it took the reporting added for a different
reason to find it.

Three red builds said the shrine-keeper was not at the shrine. It was. In all six
local runs -- three of which failed -- the feature logged "seated its keeper at
BlockPos{x=9, y=-60, z=8}", `addFreshEntity` returned true, and every single `@e`
selector afterwards answered "No entity was found". The entity existed and nothing
could see it.

`forceload add` returns before the chunk arrives. `place feature` needs only the
chunk's BLOCKS, so it succeeds inside that window, and an entity added there lands in
a section whose visibility is never established -- permanently, not transiently. It
was still invisible at the end of the batch.

The proof that this was a race and not an environment came from two runs of the SAME
commit: the push run green, the pull-request run red, identical trees. LESSONS #21
said the disagreement between two environments is the signal; it needed amending. Two
runs are just as good a signal, and I nearly discarded it by assuming a PR run must
test a different tree. It did not.

The fix is a wait after forceloading, measured rather than argued: 3 of 6 runs failed
without it, 0 of 6 with it. But a bare wait is a magic number tuned on this machine,
which is exactly what has now failed on the runner three times -- so it is followed by
a probe. A marker is summoned into the chunk and the check asserts that `@e` can see
it. If it cannot, the check fails as ITSELF ("the chunk was still loading"), not as a
missing keeper. Verified by watching it fail with the wait set to zero.

Two sibling checks had the same race latent in them and are fixed the same way.
warden_check's restart pass needed it for the mirrored reason: there the Warden is
read back off disk, and a chunk's entities arrive after its blocks, so asking too
early would have reported a Warden that failed to survive a restart -- the loudest
possible wrong answer.

LESSONS #22: an API that accepts your write has not promised anyone can read it.

---

## Somebody changed their mind about you

Regard has been recorded and persisted for several passes and was, until now,
completely invisible: no way for a player to learn the system existed short of
reading the source. HANDOFF called this "let a player feel the regard" and it sat
behind a rule that is easy to over-read. `WORLD.md` bans the karma bar. That is a ban
on the NUMBER, not on the news.

So the event is a band CROSSING, and most changes are not one. `core/regard/Standings`
computes it against a snapshot rather than by subtracting deltas -- two effects of -20
each are one crossing, not two, and reconstruction would report the second from a
baseline that never existed. A snapshot is a fact; arithmetic on deltas is a guess.

The load-bearing assertion is the quiet one. Two players in run 1 of regard_check move
regard by -4, +5, +2 and +3 and cross nothing, so they hear nothing. If that ever
starts firing, every conversation ends in a burst of notifications and the meter is
back wearing a thesaurus.

Seventy lines, one per (institution, band, direction) a player can reach -- you cannot
rise into the bottom band or fall out of the top. Each institution speaks through its
own domain, because that is the only characterisation available without a scene: the
Verdant's regard is whether paths close behind you, the Anchorite's is whether what
you set down stays there, the Hearth-Turner's is how fast your gear ages. The Quiet
One's is that you cannot tell, and every one of its ten lines is about the
impossibility of reading a silence.

At a deicide the killer hears one line each from four gods at once, which is how the
mod says "you killed a god" without saying it. The ghost says nothing, and that is the
design: recordDeicide deliberately does not floor its own victim, because the dead
god's opinion of its killer is the one relationship still open. I wrote that assertion
backwards first -- expecting the ghost to bottom out with everyone else -- and the
check failed. The assertion was wrong, not the code. It now asserts the silence.

Three things the verification turned up that reading would not have:

A mutation that announced every movement CRASHED rather than failing an assertion,
because BandChange refuses a change that changes nothing. Caught, but for the wrong
reason (LESSONS #18), so it verified nothing about the assertion under test. Two more
mutations were also caught by the wrong assertion before one isolated it: announcing a
fabricated crossing to everybody, which moves no value and changes no printed band.

That first crash was also a real bug. It threw out of the middle of Deicide.commit,
which had already recorded the milestone and set the killer and had not yet stopped
the sun. A god half-killed because a line of chat could not be composed is far worse
than a missing line of chat, so the notice dispatch is wrapped: the change stands, the
message is lost, and the error is loud.

And two checks that look like one are not. regard_lines_check proves the lang file
covers every crossing -- against a key rule it writes out itself, in Python, a second
copy of what the Java does. regard_keys_check reads the keys a RUNNING server emitted
and requires each to resolve. Only the pair of them can catch the two copies drifting.

---

## What they will and will not say to you

Regard has been written for several passes and read by nothing. This is the first
thing that consults it.

An option can carry a floor, a ceiling, or both. The floor is the obvious half --
content you earn. The ceiling is the half worth arguing for: content you LOSE by being
liked is what makes standing read as a relationship that moved rather than a score
that went up. Both are live in warden_intake now. A party the Wardenate trusts may
answer for the absent; a party it resents, and only while it resents them, may say
"Before you ask. Yes. It's us." and be fixated on for it.

THE_GHOST needed its own rule. A non-killer's ghost regard is pinned immovably at
zero, which reads as WARY, which satisfies any floor at WARY or below -- so a naive
gate would leak the dead god's private options to everybody who never met it. A gate
naming THE_GHOST admits only its killer, and that is asserted for ceilings too, where
an absence satisfies the condition even more easily than it does a floor.

The live check is the part that took thought. The property is not "the option exists",
it is that THE SAME NODE OFFERS A DIFFERENT SET OF REPLIES to the same player at three
different standings -- which no single render can show. Counting occurrences would
pass an implementation that showed both gated options in one render and neither in the
others; the totals are identical. So tools/standing_gate_check.py parses the sequence
into three groups and checks each one, and every group must also contain an UNGATED
reply, or three empty renders would agree with each other perfectly.

Two things the running server corrected.

My first version of the assertion grepped for translation KEYS, on the belief that a
dedicated server cannot resolve them. It resolves them fine -- the mod's assets are on
the classpath -- and `talk show` prints finished English. The comment in RegardNotices
asserting otherwise has been corrected rather than quietly deleted; it was a factual
claim about the platform and it was wrong.

And the gate needed a way to be reached at all, so `interregnum regard <who> adjust
<inst> <delta>` exists, in the same shape as `record deicide` and `unravel at`.
Reaching TRUSTED through actual conversation would take a dozen scenes that do not
exist yet. It is routed through RegardNotices like every other mover, so it cannot
become a back door that changes somebody's standing without telling them.

Four new mutations, 28 now, all caught. Self-test 82 -> 99.

---

## How they greet you

"A Warden's opening line depending on your file" was the oldest item on the regard
list and it wanted much less machinery than it looked like.

The temptation is scene selection: pick a whole different conversation per standing.
That means maintaining three copies of a scene that differ by one sentence, and they
drift. So the variants hang off the NODE. Same id, same rule, same replies underneath,
and only the line changes. The node's own text_key is what everybody else reads.

A variant with no condition is refused at load. It would match everyone, silently
shadowing both the node's line and every variant after it, and an author who wants
that should edit the node. First match wins in the author's order rather than
"narrowest wins", because narrowest has no definition for two gates naming different
institutions.

The question worth recording is whose standing decides, at a table of three. The
answer is the viewer's, so two players can read different words for the same beat.
That sounds like a desync and is not: the option list has worked exactly that way
since the gates landed, so a Warden already offers you a reply it does not offer your
friend. Resolving the line against the initiator instead would make the text and the
replies disagree about whose file is open, which is worse than either rule on its own.

The Warden's census opens three ways now, and stays procedure throughout. It does not
warm up and it does not threaten; what changes is how much of the file it reads out
before asking the same question.

The finding of the pass: dialogue_check was BLIND to the feature I had just shipped.
A variant with a misspelt text_key passed clean, and would have rendered as a raw key
to precisely the players the variant was written for -- the ones with a history, who
are the least likely to be a first playtester. I only learned that by breaking a key
on purpose and watching the check say OK. It now validates variant keys, unconditional
variants, and the institution and band names in every gate, option gates included --
which had ALSO gone in unchecked one commit earlier, where a misspelt band does not
fail the load but decodes to an absent gate and shows content nobody had earned.

Three new mutations, 31 now, all caught. Self-test 99 -> 106.

---

## The keeper knows what the village thinks

The villages are the second institution to act on standing. The keeper is the place
for it because there is no separate village institution to meet -- only its people,
and the keeper is one of them.

Two ways it shows. The opening changes: a party the villages resent is told, mildly,
that they are already in the register under remarks, in another keeper's hand, and
that the keeper would rather reconcile than do the other thing. And a courtesy is
withdrawn -- writing a theft up as "a withdrawal against an authorised holder" is the
keeper being kind on the record, and somebody they resent is not offered it.

The rule that kept this honest: standing costs you the easy way out, never the
content. The admit node still has two replies for a resented party and both lead
somewhere, and the check asserts it, because a gate that empties a node is a wedged
table rather than a consequence.

An idea that died on contact with the text, which is the good kind of dying: a
trusted party skipping straight to the `truth` node. That node reads "Yes. The
quarter still closes." -- it is the keeper CONCEDING, and it only lands after the
player has said there is nothing on the other end of the slot. Reached directly it
answers a question nobody asked. Reading the destination before wiring the shortcut
is the whole of that decision.

The real finding was in the failure machinery, not the feature. Breaking the new
assertion on purpose produced a check that exited 1 having printed NOTHING -- and the
last line on screen was something passing, so it read as "this cannot fail". The
opposite was true. Under set -e -o pipefail a diagnostic dump whose grep matches
nothing exits non-zero, pipefail propagates it, and because the dump sits in the last
branch of an || the shell kills the script before the fail message runs.

Two harmless mistakes made one dangerous one: the dump's pattern was 'show| KEEPER'
while the speaker renders as SHRINE-KEEPER, so it matched nothing -- and matching
nothing is what hid the failure. LESSONS #23, which is #4 wearing its other face:
there a pipe swallowed a failure and reported success, here a pipe manufactured a
failure and swallowed the explanation.

Every dump in talk_check now ends in `|| true`. A failure path must not contain
anything that can fail.

---

## The same unit, one question changed

A Warden conducts a census of the living before the death and takes statements about it
afterwards. Same mob, same manner. What moves is what the procedure is FOR, and that
pairing is the reason the second scene works at all: a player who met a Warden in
Chapter 0 meets the identical procedure afterwards, and the only thing that has changed
is the question.

warden_interrogation is named directly in WORLD.md -- "Warden interrogations after the
death (where were you when--)" -- and the locked rule sitting beside it is what shaped
every line of it: enforcement targets SITES, never a single player, no player is the
system's butt.

That rule is what makes the scene. It cannot be an accusation, so it is a canvass. The
unit is taking the same statement from every living thing in the world, in the same
order, and says so unprompted because it is required to: "This unit asks every party
the same questions in the same order. It is not an accusation. Nothing about you has
been marked."

Which means the player who did it is asked routinely, and cleared routinely, by
somebody methodically working a list that cannot be finished. "Three hundred and eleven
statements outstanding. Four hundred and twelve were taken yesterday. The figure does
not fall."

Dramatic irony like shrine_keeper_intact, but where the keeper was content this one is
grieving, and the grief is never the punchline. The unit expresses everything it has as
procedure, because procedure is what it has instead of mourning: "This unit is required
to say that your cooperation has been noted. This unit would like to say something else
and is not authorised to."

Two things I checked rather than assumed.

HANDOFF said first Warden contact recording WARDEN_CONTACT was "reachable only by
command". Stale -- both mobs have had mobInteract for several passes and Conversations
records the milestone on being addressed. What is NOT stale is the gap behind it:
nothing creates a Warden, so band 2 is still unreachable by playing. That is now marked
as the single biggest functional gap, and it is blocked on the owner because the
mechanism (a woken statue summoning one) is new scope.

And I nearly invented lore. A line in the declined-help node ran "There are four of us.
There were nine hundred." -- which is a specific claim about the Warden population that
WORLD.md does not make. Cut. It reads flatter now and the flatness is the grief: "This
unit is authorised. This unit will continue."

Scene selection reads the world's chapter data rather than a flag on the mob, so a
Warden standing in a field since before the deicide answers the same as one that walked
up afterwards. openingScene is the shared pattern now, and `talk scene <entity>` works
for any mob that has one -- a headless server can never reach mobInteract, so without
it an NPC's choice of opening is observable only by playing.

Watched failing: with the unit never noticing the god died, the check says exactly that.

---

## Six items that would have been purple cubes

The tick's task was clearing the `VERIFY:` markers in WORLDGEN.md, DATAGEN.md and
MODELS.md -- documentation debt, written before this container could reach the NeoForge
docs, and flagged since as things not to trust. The mod builds and runs now, so most of
them can be settled against artifacts on disk rather than against a website.

Most were confirmations. The biome modifier shape, the placement modifier names and the
`neoforge/biome_modifier/` path all check out against the JSON this repo's own datagen
writes. `/perf start` and `/perf stop` are the profiler. The item model system had been
guessed at from memory -- "a separate items/ layer, model types, condition/select/range
dispatch" -- and the guess was right, which is worth recording precisely because it now
rests on 1538 counted vanilla files instead of on recall.

Two were real corrections. The datagen task is `runServerData`; `runData` does not
exist. And `dimension_type` is where 26.2 moved the furniture: `ultra_warm`, `natural`,
`bed_works`, `respawn_anchor_works`, `piglin_safe` and `has_raids` are gone as top-level
fields and now live in a namespaced `attributes` map -- `minecraft:gameplay/water_evaporates`,
`minecraft:gameplay/bed_rule`, and so on -- alongside a new `visual/` and `audio/`
namespace. `fixed_time` is gone too, replaced by `default_clock` and a `timelines` tag.
A pre-2026 answer here would have been confidently, uniformly wrong, which is exactly
what LESSONS #3 said would happen.

That one is not just a correction, it is a gift to the design. WORLD.md has each god-world
teaching its own rule before arrival; the Quiet One's silence is `minecraft:audio/*` and
`minecraft:gameplay/sky_light_level` entries, not code.

Then the finding. Verifying MODELS.md's item-model section meant looking at what this repo
actually ships, and it ships SIX registered items and ZERO item definitions. Every check
green. A dedicated server never loads assets/, so nothing in CI could see it -- and
registry_check.py was asserting translation keys while its own summary line said items
"resolve models". All six would have been the missing-model cube in front of a player.

Both halves fixed: the definitions are written (block items point straight at the block
model, flat items get a definition plus a generated model), and registry_check now asserts
every registered item has a definition and that the model it names resolves. Watched
failing three ways: no definition, a definition naming a model that does not exist, and a
block model renamed out from under a block item.

The transferable part is not "write your item models". It is that **the one area this
container cannot look at directly is the one where a check's summary line has to be read
twice.** The line said "all resolve models". It had never been true.

---

## Where enforcement reaches

The owner said yes to the statue proposal, so it is built.

A woken statue posts a Warden. This was the last thing standing between the mod and
being playable end to end: the entity had two scenes, a tether, a renderer, a regard
cost for killing one -- and nothing whatsoever created one, so WARDEN_CONTACT and
therefore band 2 could only be reached by typing a command.

The statues were handed out as scenery for a hundred hours and all opened their eyes at
once when the god died. Making the woken one the thing that CALLS turns that scenery
into a map: where the statues are is where enforcement reaches. It also retroactively
explains why the mod gave everybody free decorative statues for so long, which is the
part that makes it feel designed rather than bolted on.

Three decisions worth recording.

THE STATUE IS PERMANENT, THE WARDEN IS NOT. A posted Warden is not
persistence-required; it stands down when nobody is there and the statue posts another
when somebody returns. One immortal Warden per statue forever is not an institution, it
is a leak, and on a server where people have built with these blocks for a hundred hours
it is a very large one.

TEARING ONE DOWN COSTS WARDENATE -8, and only when a PLAYER did it. An unwoken statue
costs nothing -- before the death these are garden ornaments and the Wardenate has no
opinion about anybody's landscaping. The lever the design wanted (pull the statues out
of your valley, go dark) now has a price, so it is a decision rather than just the
correct move.

THE CAP IS LOGGED. MAX_POSTED_PER_SWEEP bounds one pass and names what it deferred,
because a silent truncation reads as "everything was handled".

The bug the check caught is the good part, and I would not have found it by reading.

I overrode `requiresCustomPersistence()` to return `!posted`, wrote the comment
explaining why, and believed it. `Mob.checkDespawn` gates on
`isPersistenceRequired() || requiresCustomPersistence()` -- BOTH -- and `Mob` offers no
way to clear the first: the field is private, the constructor sets it, and
`setPersistenceRequired()` only ever sets it true. So a posted Warden still reported
`PersistenceRequired: 1b` and would never have stood down. Exactly the leak the design
exists to avoid, sitting underneath a comment saying it could not happen.

The check found it because it asserted the NBT rather than the intent. Fixed by
overriding `isPersistenceRequired()` too, and by re-stating the corrected value in
`addAdditionalSaveData` -- an NBT flag that disagrees with the behaviour is worse than
no flag, because it is what somebody debugging this at two in the morning will read and
believe.

Second, smaller: the first version of the check asserted
`data get entity <sel> interregnum:posted`. The key is namespaced, so that path does not
parse -- the command fails, which reads exactly like a missing flag. It reads off the
whole-entity dump now.

Both assertions watched failing: statues breeding (two Wardens across two sweeps), and
an unwoken statue posting before the god was dead.

---

## The advancement that must not speak

The status survey turned up that the mod has no advancements at all -- including the
one WORLD.md marks [LOCKED]: "The advancement at the moment of death: Deicide." So it
exists now, and it turned out to be a better increment than its size suggests, because
of what it collides with.

WORLD.md locks a second thing about that moment: the mod never announces who did it.
There is simply a player online who has gone quiet.

Minecraft broadcasts advancements to chat by default. Shipped with the default flag,
the mod would have printed "<player> has made the advancement [Deicide]" to every
person on the server at the exact instant the design says nobody is told. The loudest
possible violation of the mod's central beat, delivered by a boolean nobody looked at,
in a feature that otherwise looks like housekeeping.

So announce_to_chat is false and hidden is true -- the killer gets a toast, alone, and
the tree does not advertise to everyone else that killing the god is a thing that can
be done. That flag IS the feature, and advancement_check.py fails the build if it flips.

Three other things the same file can drift on, all now checked: the criterion name
(Java awards by string, JSON declares by string -- two copies of one name, and if they
disagree the award silently does nothing and returns false), the advancement id, and
the title and description keys, which render raw if absent.

What is NOT checked, stated plainly: the award itself. It needs a real player and a
headless server has none -- the same wall mobInteract sits behind. What is checkable is
that everything the award depends on is correct and agrees with itself, and that is
what the check does.

Watched failing four ways: it announces to chat, the criterion names drift apart, the
advancement is not generated at all, and it stops being hidden.

Generated rather than hand-written, so the staleness check covers it -- and because a
hand-edited JSON is exactly where that flag would quietly come back.

## The boat that must not eat the world

WORLD.md gives the mail-ferry one sentence: *built and furnished from real blocks; a
keel block captures the structure, validates it against the destination's law, and
re-places it at the far pad.* Every hard problem is in the first clause.

A flood-fill from the keel through connected solid blocks eats the seabed, then the
mountain the seabed is attached to, then the world. The usual answers are all bad. A
fixed bounding box makes the ferry a template rather than something you built. A
"boat blocks only" whitelist makes you furnish it from a catalogue. A size cap alone
does not help: it just decides how much of the planet leaves with you.

The answer was already in the repo. `Claims` has recorded every player-placed block
since the unraveling needed to know what not to eat. **The ferry takes only what a
player placed.** The walk stops dead at natural terrain, so a hull resting on the
seabed lifts off it, and a hull carved *out of* the seabed does not float -- which is
correct, because this is a thing you build, not a thing you dig. One system, three
jobs now: it stops the apocalypse eating your house, it will decide what attrition
may generalise, and it is the hull of the ferry.

That claim is the thing `tools/ferry_check.sh` exists to test, and it was watched
failing: with the claim test removed, the capture ran off the four-block hull into
the flat world's ground and hit `MAX_HULL` -- `ferry=refused reason=TOO_LARGE`,
repeatedly. The cap did its second job there. It is not a performance guard; it is
what stands between a bug in the walk and somebody's island.

### The keel had to be a block

`ferry sail <x> <y> <z>` without a keel requirement is a command that teleports any
structure anybody has ever built. So `capture` refuses `NOT_A_KEEL` unless the block
at that position really is one, and the keel is also the single block admitted to the
hull without a claim -- it is the origin of the walk, and demanding a claim on it
would make an unclaimable ferry unbuildable.

It is cheap wood on purpose. It is the one piece of infrastructure a player builds for
themselves rather than finds, and a keel that resisted a hand would read as furniture
somebody else placed. It drops itself for the same reason: losing a keel to a
mis-swing would turn an hour's hull into scenery.

The top face carries a brass ring, because the block's position is load-bearing and it
has to be findable at a glance. Getting that ring to read took two rewrites. The first
carried the side face's brass strapping through and speckled tarnish round the rim; in
the contact sheet the strapping ran straight through the ring and the speckle broke its
top-left, so the whole tile read as a repeating `@`. A ring only becomes a ring when it
is the only circle-adjacent thing on the face. The side face needed the opposite
correction: bare brass columns over a busy grain dissolved entirely, because the brass
and wood ramps are both warm browns a step apart in L*. Metal on wood reads by the dark
seam where it is seated, not by its own colour, so each strap is now a wood-0 shadow
followed by a brass-2 highlight.

### The checklist is the tutorial

Four destinations, four laws, in `data/interregnum/ferry/laws.json`: the Quiet One
refuses anything that can make a sound, the Anchorite anything loose, the Verdant
anything stripped or dead, the Hearth-Turner anything waxed. A held hull gets every
violation named -- block, count and reason -- not just the first, because a checklist
that reports one problem at a time is a checklist you fight rather than read.

The check asserts the same hull is *cleared* by a different law. Without that it would
pass against an implementation that simply refuses everything (LESSONS #15).

### The nudge

The most ordinary thing anybody will do with a ferry is move it two blocks along, and
that is the move that eats it: origin and destination overlap, so a block-by-block
clear-then-write erases blocks it has already placed. `Ferry.place` runs two full
passes for exactly this reason.

The check I wrote to guard that was green against a deliberately one-pass version. It
asserted the arriving hull's manifest, and the arriving hull's manifest is
character-for-character the manifest printed at the dock -- that is the point of the
assertion -- so `grep -q` found the *earlier* line every time. The keel was genuinely
being deleted mid-move and the check had no way to notice. It now counts the
occurrences and asserts block-by-block at the destination coordinates, and it has been
watched failing. Written up as LESSONS #24, because the shape generalises: an
assertion of the form "X is still true afterwards" is automatically satisfied by X
having been true beforehand.

## The world that will not answer you

The ferry sailed and there was nowhere to sail to. `interregnum:unresponsive` is the
first god-world surface layer, and the whole question was what makes a dimension worth
the trouble of having.

AESTHETIC.md's executioner, quoted in WORLDGEN.md: *could I replace it with a different
random weird thing without changing anything?* "The same game with purple stone" fails
it — swap purple for green and nothing moves. A dimension earns itself when its **rules**
differ, when something is possible or impossible there that is not elsewhere.

So the Quiet One's law could not be *quiet*. A muted soundtrack is purple stone. What
the law had to be was already printed on every Warden docket that mentions this god:
`SUBJECT: UNRESPONSIVE`. **Every affordance in Minecraft that consists of asking the
world for something is dead here.** A bed. A respawn anchor. A raid. All three are
attributes in 26.2's `dimension_type`, so the law is data, not code.

The bed is the whole character in one record. `BedRule(NEVER, NEVER, explodes=false,
message=absent)`. The Nether refuses you loudly. The End refuses you loudly. Both
explode. This place declines to react — and the entire difference between "hostile" and
"unresponsive" is one absent boolean and one absent string. It is the cheapest piece of
characterisation in the mod and it required writing nothing.

The audio attributes needed the opposite care: declared **empty**, not omitted. An
omitted attribute inherits, and what would be inherited is the overworld's cave moaning
— the one sound this world must not make. Present-and-empty is a decision; absent is an
accident that sounds exactly like home.

### Two checks, because they prove different things

A dimension that merely *loads* proves almost nothing. A stem mis-wired to
`minecraft:overworld`, a dimension_type that failed to resolve, a datapack that never
loaded — all of them still let `execute in interregnum:unresponsive` succeed. So every
assertion in `crossing_check.sh` is a **relationship between two worlds** rather than a
fact about one (LESSONS #19): y=-10 is legal at home and illegal there, y=250 is legal
in both, and a block written there is not at the same coordinates at home. Watched
failing against a stem pointed at the overworld's dimension type, which is precisely the
bug a "does it load?" check waves through.

The law itself is not testable that way. A dedicated server exposes no command that
reads a dimension's attributes back, so `dimension_check.py` asserts it as data and is
labelled as the weaker proof it is: *the data we ship declares this*, not *the game
behaves like this*. What it catches is the failure that would actually happen — somebody
edits `ModDimensions` to fix a colour, regenerates, and quietly hands the bed back its
explosion. Nothing else would fail. Nobody would notice until a player slept.

Terrain is a placeholder and the file says so in its own javadoc: vanilla noise, one
biome, ground you can stand on. Under-layer, far-layer and portal logic do not exist.
Writing that down is cheaper than a later reader inferring that a placeholder was a
decision.

### The check written an hour earlier earned itself immediately

Adding `crossing_check.sh` to the workflow made the live-check count 16 while HANDOFF
still said 15. `tools/ci_claims_check.py` — committed earlier the same session, for
exactly this — failed the gate before the commit. First real use, and it caught its
author.

## The world where nothing holds still

Second god-world, and unlike the first one it was not a design problem at all. The law
was already written, in the mod's own voice, on a boarding notice that has been shipping
for hours:

> Refused for the crossing to the Mass Authority. Nothing that pours. Where you are
> going, unanchored things go up, and they do not stop.

A player reads that before they arrive. So the implementation had no freedom: it had to
be the thing that line describes, because the line shipped first and a world that
contradicts its own boarding notice is worse than one with no notice at all.

**"And they do not stop" turned out to be load-bearing.** A rising `FallingBlockEntity`
never satisfies vanilla's ground test, so it never places itself — it climbs past the
build height and vanilla's own timeout discards it. Nothing had to be written to get
that. The tempting version, where sand sticks to ceilings, is a nicer toy and a broken
promise.

### Weight cost code where silence cost data

Every one of the Quiet One's rules turned out to be a 26.2 `dimension_type` attribute:
beds, raids, respawn anchors, ambience, all of it declarative. Weight is not there. The
full attribute list was read out of `EnvironmentAttributes` rather than guessed at —
there is no gravity attribute, no fall-damage attribute, and nothing that inverts
anything. So this law is a tick handler where the last one was a JSON field.

Worth recording rather than hiding: the platform made one god cheap and the next one
not, and any estimate of "three more worlds to go" that assumed the first one's cost
would have been wrong by an order of magnitude.

### The check had to distinguish rising from three kinds of vanishing

"The sand did not land" is satisfied by sand that was deleted, sand that fell through
the void, sand that never became an entity because gravity was switched *off* rather
than reversed, and sand that never spawned because the chunk was not loaded. Every one
of those is a bug that would ship green against a naive check.

So the check measures four things: the control at home (sand lands), no landing there,
the block is no longer sitting where it was placed, and a falling-block entity exists
*above* its start position.

The first draft waited six seconds and failed — correctly, and for a reason I had not
predicted. In six seconds the sand had legitimately risen out of the world and been
discarded, so "no falling block exists" was true *because the law worked*. That is
indistinguishable from "the law never ran" unless you also ask whether the sand is still
sitting at its original coordinates, which is why that assertion exists. Watched failing
on the law never applying, and on gravity being zeroed rather than inverted — the second
of which a check that only asked "did it land?" would have called a pass.

### Two gods, one boolean apart

Both worlds refuse a bed. The Quiet One declines to react at all: never/never, no
explosion, no error message. The Anchorite detonates. That single boolean is most of the
difference between two characters, it costs nothing to lose in a refactor, and no test
looking at one dimension alone would notice — so `dimension_check.py` now asserts them
against each other rather than each in isolation.

### What was left out, and why it is the owner's

Only `FallingBlockEntity` rises: sand, red sand, gravel, anvils — exactly the set the
ferry's notice names. "Unanchored things" plainly also covers a dropped item, and the
more complete reading would lift those too. It would also mean every death in that world
costs the whole inventory, unrecoverably, with the items visibly leaving. That is either
the best scene in the mod or the reason nobody goes back, it is nowhere in WORLD.md, and
inventing it here would be new scope arriving disguised as a detail. Flagged for the
owner; the restrained version ships.

## A beat, not a walk

The Warden had been using `WaterAvoidingRandomStrollGoal` since it first took the field.
It is the goal a sheep uses. It looks fine for ten seconds, and it is wrong for the
reason the whole mod exists.

A random walk is an **animal foraging**. A Warden is a **unit on a post**. What separates
those two is not speed, or path shape, or animation. It is that the unit does the same
thing in the same order — and once that is true, everything else about the character
follows for free.

`WardenPatrolGoal` walks four points on a ring of 8 around its statue, in a fixed order,
starting from the same leg, standing still for two seconds at each. Four points rather
than eight because at that radius eight legs keep it moving nearly all the time, and a
unit that never stops reads as agitated; the standing still is most of what makes it look
like it is *checking* something rather than travelling.

### Predictability is the feature

WORLD.md locks the thesis that violence does nothing and the exploit a player finds is
administrative. An enforcement system you can plan around is a *prerequisite* for that.
A Warden that wandered unpredictably makes the only viable answer "wait and hope"; a
Warden with a beat makes the answer "it is at the north point for four seconds every
circuit", which is a thing a person can use.

And the same fact does double duty tonally, because of the order it arrives in. First it
reads as *this thing is not improvising, it is executing.* Only later does it read as
*and therefore I can time it.*

### The check tests the property that is hard to fake

"Does it move" is worthless — a strolling mob moves. The assertion that actually pins
down *deliberate* is a relationship between two observations (LESSONS #19): **two Wardens
posted in the same tick on identical flat ground stay in step**, because both execute the
same fixed route from the same leg. Two strolling mobs draw from the level's shared
random and diverge within seconds. Backed by a weaker but independent one: the Warden is
out on its ring rather than in the middle of its tether, counted across samples rather
than caught once, because one sample at the edge is a strolling mob having a good minute.

Watched failing on the leg order being randomised — which turns the beat back into a
wander with waypoints — and on the sheep goal being restored.

### And it found a real bug on its first run

The Warden did not move at all. Two wrong theories went first, and the thing that
settled it was running the **control**: I put the old stroll goal back and it did not
move under that either, which immediately said "not a regression you just wrote" and
halved the search space.

The probe that finished it printed one line: `moveTo ok=false onGround=false ... path=null`.
`GroundPathNavigation` refuses to build a path unless the mob is `onGround`, and a mob
that has just been spawned is not — goals tick **before** movement inside the same
`aiStep`, so the first `moveTo` of every posting could only fail. One tick later it would
have worked.

`moveTo` returns a boolean saying exactly that, and I had thrown it away. The goal marked
the leg started and then waited out a 200-tick timeout before trying anything else: ten
seconds of a unit standing at its post doing nothing, once per corner. Indistinguishable
from a patrol that does not work, and it was one. Written up as LESSONS #25.

The fix branches on the answer — keep the target only if the path was accepted, retry
shortly if not, abandon the leg after a bounded number of refusals. That last clause
handles, for free, the case this repo will actually meet in play: somebody walls off one
corner of a Warden's beat because they have worked out where it goes. The round does not
stop. Neither does anything else this institution does.

## It counts, it files, it does not conclude

WORLD.md gives the Wardens four verbs — *inspect, cite, confiscate, escalate* — and the
tempting move after building the patrol was to do all four. It cannot be done honestly
yet, and finding out why was most of the value of this pass.

**A citation needs an offence, and the mod does not have one it can find.** The locked
countermeasures — shielded casting rooms, forged dispensations — say plainly that what
the Wardenate polices is *casting*, and magic does not exist. Of the two locked offences
that need no magic, one is the sleep code and the other is **permitted airspace**
(`the height limit | permitted airspace`) — and the second is only findable once the
unraveling has loosened the limit enough for anybody to break it. Choosing a lower
licensed ceiling to make it findable today would be inventing a rule the world never had.
So `cite` is blocked on a design decision rather than on effort, and that is now written
down where the next person will look instead of being rediscovered.

What the unit can do without inventing anything turns out to be the thing it was already
doing in the shipped dialogue: *"This unit is conducting a census of the living.
Attendance was nine hundred and forty-one. Attendance is now nine hundred and forty."*

So at each corner of its beat it files a return: where it stood, and how many blocks a
player placed are standing there. It counts. It files. It does not conclude.

The count comes from the same claim ledger the unraveling reads to know what not to eat —
deliberate reuse, because a Warden whose idea of "somebody's work" differed from the
apocalypse's would be a second definition of the same word. And a return of zero is filed
exactly like any other: an institution that only reported when it found something would
be one you could learn to read for signal, and the point of these returns is that they
are indifferent.

### The assertion that matters is not that it filed

A survey that always returned zero would file returns forever and look exactly like a
working one, because an empty site and a broken counter are indistinguishable from a
single reading (LESSONS #15). So the check gives site A's north corner three claimed
blocks and site B's none, and asserts both numbers appear. Watched failing with the
survey stubbed to zero.

An unplanned corroboration fell out of it. The two units filed at *identical relative
offsets* from their own statues — `site=1 -60 -6` and `site=65 -60 -6` — which is the
keep-step property showing up again in a completely different measurement that was not
designed to test it.

### A mutation that survived, and why that is not a check failing

Moving the filing to fire on abandoned legs as well as arrivals — which would mean the
institution filing returns on places it never stood — left the check green. That is
correct rather than embarrassing: on flat open ground no leg is ever abandoned, so the
mutation is behaviourally inert in this scenario. Pinning it needs a site with a corner
walled off, and the cheap versions of that fight the keep-step assertion.

The rule is in the code and it is not yet checked, and both the check header and HANDOFF
now say so. A mutation surviving is information; the failure would have been quietly
deleting the mutation and claiming the coverage.

## Everything grows, and that is the hazard

Third god-world. `WORLD.md` had already done the design work, in one clause of the
Verdancy school entry: *"and in the Verdant's own world, accelerating growth is a
**hazard**."*

That word is the brief. Fast growth as a convenience is a farming mod. Fast growth as a
hazard is a place where you cannot keep ground clear, where the path you cut closes
behind you, and where standing still is a decision. The mechanism for both is identical;
what makes it a hazard is that it applies to everything, everywhere, and not to the
things you wanted.

Implemented as **more random ticks** rather than a list of growable blocks. Vanilla
already grows things that way, so every crop, sapling, vine, moss and mushroom is covered
without naming one, and so is whatever Mojang ships next. A hand-written list would be
out of date the first time they added a plant.

Two gods now cost code and one cost data. Silence was entirely 26.2 attributes; weight
and growth have no attribute at all. That is worth recording because it is the second
time an estimate based on the Quiet One would have been wrong.

### The promise had to be narrowed before it was shipped

The first version of the claim check carried a comment saying player-placed blocks are
not touched. That is false, and the falseness was in the same commit that added the
protection.

Random ticking grows things by ticking a **source** which then reaches to a neighbour:
grass spreads by ticking the *grass*, not the dirt. So skipping claimed positions stops
the mod accelerating a block you placed — and does nothing about an unclaimed grass block
turning claimed dirt beside it. No check on the ticked position can.

The honest promise is the narrow one: **everything the mod itself accelerates, it
accelerates only on the world's own blocks.** What vanilla's ordinary spread reaches is
what it reaches at home, and a dirt block turning to grass beside grass is Minecraft
rather than the apocalypse. Writing the wide version into a javadoc and shipping the
narrow one is how a guarantee becomes a lie without anybody lying.

### Three things the check taught, in order

**A single target block is unobservable.** The first draft planted one crop in each world
and failed — while the law underneath it was working perfectly. Random ticking picks
uniformly out of a 16³ section, so one specific block is hit roughly once every eight
seconds even at eight times the rate. The probe that settled it showed the handler
cheerfully ticking grass at the terrain surface while the two test blocks sat at y=100
being missed. The fix is a *row* of targets, counted, rather than one block asked a
yes/no question.

**The margin is measured, not chosen.** Four runs at sixteen targets gave the Verdant
8–12 and the overworld 1–4. A real gap, but close enough at the extremes to be
uncomfortable, so the sample was doubled to halve the relative variance: 18–24 versus
0–4. The assertion is a comparison rather than a threshold on purpose, because a slower
CI runner ticks less in the same wall-clock window and lowers both counts together.

**And a mutation survived, which forced a second assertion.** Removing the dimension
check — so the Verdant's law fires in *every* level — gave 25 there and 21 at home. That
still satisfies "more here than there", and it is a catastrophe: one god's law applied to
the entire game, including the overworld a hundred hours of somebody's building sits in.
The overworld count was sitting right there in the output being ignored. It is now
asserted directly, with a ceiling set at double the worst measured run.

That is the second time this session a mutation surviving has been worth more than a
mutation dying. The failure would have been deleting it and claiming the coverage.

## Nothing is allowed to be over

The fourth and last god-world, and the one with the largest risk attached: a fourth
dimension is where a pantheon becomes wallpaper. `AESTHETIC.md`'s executioner —
*could I replace it with a different random weird thing without changing anything?* — is
hardest to survive on the last one, because by then you have three mechanisms and the
cheapest move is to reuse one with a different table.

Which would have been especially tempting because `WORLD.md` **tells** you to reuse
something: *"the block-aging registry powering the Turning is the same system that runs
the unraveling. One mechanism; a school and an apocalypse."*

So this reuses the registry and not the mechanism. The ageing table is the unraveling's
own `ConversionDef` — same record, same codec, same JSON shape, imported rather than
copied. What is deliberately *not* shared is the band and the scope, and their absence is
the character: the unraveling escalates and has a frontier because it is an apocalypse on
a clock. Ageing has neither. It is what time does, everywhere, always, at the rate it did
yesterday. The overworld is coming apart because nobody is holding it. This world is not
coming apart at all — it is accumulating, and it will not let any of it go.

That is the same god that *"has never let a grievance become past tense"*, expressed in
masonry.

### It also had to not be the Verdant

The Verdant asks vanilla for **more of what it already does**: extra random ticks, so its
world grows the way any world grows, faster. Nothing new happens there; it happens sooner.

Vanilla has no notion of stone acquiring moss with age and never will. So the Turning
applies an explicit table, and the chains run through every state rather than jumping:
stone, then cobble, then mossy cobble, because each rule's `to` is another rule's `from`.
Walk back through somewhere you built and you can read how long ago you were there off
the walls. That is what *keeping every past* means when it is a block rather than a
grievance.

The difference shows up in the checks, which is how you know it is real: the Verdant's
can only compare **rates**, because grass spreads at home too. The Turning's is
**categorical** — any ageing in the overworld at all is a leak.

### The bed is the fourth answer and the only permissive one

Four worlds, four opinions about a bed, no two alike. The Quiet One declines to react.
The Anchorite detonates. The Verdant lets you sleep and will not hold your spawn. The
Hearth-Turner allows **everything** — sleep, spawn, anchor — and `has_fixed_time` is on,
so the night does not pass and none of it achieves anything.

It refuses you nothing. It simply will not let go of the time you were trying to skip.
This is the one world allowed a fixed sky, and the other three were denied one precisely
so that it would mean something when this one had it.

### Three things the check taught, and two are about failure paths

**The first draft waited forty seconds and got nothing**, while the law worked perfectly.
One sample per section per tick against 4096 positions is not observable inside any
window CI will tolerate. The rate is now vanilla's own budget, spent on memory instead of
growth — and the chain, which needs two separate landings on one block, got a
deterministic seam (`interregnum turning age`) rather than a longer wait. Waiting for two
independent rolls would have turned a categorical fact into a statistical one for nothing.

**The setup probes ran after the commands that could alter them.** So a mutation that let
the command age the overworld made the check report *"no stone was placed in the
overworld"* — which is false, and points at the wrong file entirely. The probes now run
first, and the leak is asked as its own direct question. A diagnostic that misattributes
is not much better than one that does not fire.

**And a failure message used backticks inside a double-quoted string.** Bash ran
`interregnum turning age` as a shell command, printed "command not found", and delivered
the failure message with its own subject missing — on the failure path only, so precisely
when somebody most needed to read it. That is LESSONS #23 (*a failure path must not
contain anything that can fail*) arriving by a route the original entry did not imagine,
so it is recorded where it happened rather than as a new lesson.

## The crossing crosses

`WORLD.md` locks one sentence about how anybody reaches a god: *travel between systems is
only by ferry.* The ferry had existed for hours and could not leave its own dimension, so
that sentence was half true — a hull cleared for the Quiet One's crossing was picked up,
checked against the Quiet One's law, and set down again in the overworld.

That is the smaller half of the "portal logic" gap and it was worth doing first, because
the other half is genuinely undesigned: each god's *own* portals between its surface,
under-layer and far-layer are locked in principle and specified nowhere.

**The destination is a property of the law, not a parameter.** That was the one design
decision here and it decided the shape of everything else. `ferry sail <keel> <law> <pad>`
reads where the crossing goes from the law the hull was cleared against — so a hull
cleared for the Quiet One arrives in the Quiet One's world and *cannot* be sailed
anywhere else. A destination argument would have made the checklist advisory: you could
be refused for carrying a note block and then sail to a world that has no opinion about
sound.

It also closes a loop that has been open since the ferry shipped. The boarding notice a
player reads on the dock says *"Refused for the crossing to the Unresponsive"*, and until
today that name pointed at nothing.

### Where the destination is validated, and where it deliberately is not

Each law carries a `destination` dimension id, decoded to an `Identifier` at load so a
typo is a loud failure rather than a law that clears you for nowhere. It is **not**
checked against the loaded dimensions there: datapacks load before levels do, and a law
naming a dimension another datapack supplies is legitimate. So a missing destination is
refused at sail time, where the refusal can name what is missing instead of taking down
every law in the file.

That is the same reasoning as the ferry's other refusals, and the same reasoning the
crossing laws already used for one broken file taking down all of them — the difference
being *when* the information to decide exists.

### The two-pass move still earns its keep

`Ferry.place` clears the origin in a complete pass before writing anything, which across
two levels looks like leftover caution: nothing in another dimension can overwrite
anything here. It stays because the commonest crossing of all is the one that does not
change dimension — somebody nudging a ferry three blocks sideways while still building —
and one rule that is right in both cases beats a fast path that is wrong in the ordinary
one.

### What the check had to become

Every arrival marker is now asserted **inside** `interregnum:unresponsive`, and a new
`NEVER_LEFT_HOME` marker fails the run if the hull is sitting at the destination
coordinates back in the overworld. That second marker is the one that matters: without
it, a ferry that ignored the destination entirely would still put a keel at
`20 100 20` and satisfy every positional assertion, because the check would have been
looking in the wrong world.

Watched failing two ways: the destination ignored (the hull moves sideways, and the
notice naming a destination is decoration), and every law pointed at the same world.

## The mail

`WORLD.md` builds the whole mid-game on four letters and one reveal:

> You spend a hundred hours calling it The Verdant. Then you open the mail you are
> carrying and it says **"Rill —"**, and you understand for the first time that you are
> holding a stranger's correspondence about people you have never met.

The letters now exist. The interesting part was working out what could break that reveal,
because it is not the letters.

**It can be destroyed from anywhere in the mod.** A villager mentioning Rill. A Warden
docket carrying Ballast. A scene where somebody says Ash. Any one of those spends the name
early, the letter lands as recognition instead of as a stranger's mail, and nothing
anywhere fails — no test, no load error, nothing. The reveal simply stops being one, for
readers who will never know it was supposed to work.

So the load-bearing assertion is a **negative one about the whole shipped string table**:
the three names appear in their own letters and absolutely nowhere else. It is the first
check in this repo whose subject is the *absence* of something across every file, and it
was watched failing on a planted villager line.

### A rule about a set

*Three letters open with a name; the fourth opens `To —`.* No individual letter can be
checked against that: an unaddressed letter is legal — exactly one must be — and an
addressed one is legal too. The invariant only exists once you have all four, which makes
it precisely the kind of thing that rots quietly. Somebody decides the Quiet One should
have a name after all, or drafts a fifth letter and leaves the addressee out because they
have not written it yet, and nothing fails.

It lives in `core/letters/Post`, with mutations, and is re-checked in the fast gate and
again on a live server — three places, because the cost of it going is invisible.

Absence is `Optional.empty()` and a blank string is refused. `To —` is a decision; `To `
is a typo; in a JSON file they are one keystroke apart and they mean opposite things.

### Voice

The last letter's design says where the Wardens got their register: *they speak in
procedure because it did.* So these are not laments. They are correspondence between
colleagues who have stopped speaking, filed by somebody who has one register and is using
it to say something else — and the check enforces the `SUBJECT:` line, because that prefix
is the tell that this is filed mail rather than a farewell.

None of them explains the plot. Same constraint the last letter is built on: a letter that
told you what happened would make the search retroactively pointless.

### Two false positives, both naming the wrong culprit

`grep -q 'interregnum.letter.'` — **unescaped dots** — matched the echoed command line
`$ interregnum letter read verdant`, because `.` matches a space. The check reported a raw
translation key while the letter had rendered perfectly.

And a broken post makes the loader log an ERROR and degrade to no mail, which is correct
and which `server_smoke.sh` then fails the run for, because it fails on any ERROR.
Reporting "the run did not complete" is true and useless — the run did not complete
*because the mail is broken*. The failure path now looks for the loader's own message
first and quotes it.

Both are the same shape as the backtick bug from the Turning: a failure path that fires
for the right reason and says the wrong thing costs as much as one that does not fire.
