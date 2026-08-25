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
