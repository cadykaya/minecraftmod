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

## One sealed letter, and it does not say who it is for

The letters existed as text nobody could hold. Now there is an item.

**One item for all four, and its name says nothing.** `Sealed Letter` — not "Letter to
Rill", not four items with four names. You are the only one left carrying a dead
stranger's mail and you do not know who any of them are; an item that told you would
spend the mid-game's best reveal in a tooltip, before the letter had even been opened.
Which letter it is rides in a data component as a **god id**, never as an addressee, for
exactly the same reason: a stack in a hotbar is a string a player can see.

That last rule needed its own guard. `letters_check.py` scans the whole shipped string
table for the names, which is the assertion that protects the reveal — and it cannot see
a data component. So the live check had to cover the other half.

Stacks to one. Four letters that stacked into a pile of four would be four copies of one
object, and these are four different objects that happen to look alike, which is also
true of them as writing.

### The guard was a silent no-op, and a caught mutation hid it

The first version of that assertion carved the server output on
`"Item has the following entity data:"` so it could scan only the item. It reported
**clean** against a build deliberately broken to put `Rill` in the component, with the
name sitting in the output.

`data get entity` names an entity by its *item*, so the line is
`Sealed Letter has the following entity data:`. The split string never appeared, the list
had one element, and the expression short-circuited to "clean" for any input, forever.
Not a wrong answer — no answer, in the shape of a right one.

What makes this worth an entry (LESSONS #26) is how it nearly got past: **the mutation was
caught.** The assertion on the next line noticed the component no longer held `verdant`,
the check went red, the build was broken, and by every visible signal the system worked.
Only reading *which* assertion fired showed the important one had not. A dead check hiding
behind a live neighbour is the most comfortable place for it to hide.

The fix is to stop guessing: the claim is about a component, so ask about the component.

### The icon, and a thing 16 pixels will not let you do

Bone for the paper — the only family in the palette that reads as a written thing rather
than a material, and the god's script elsewhere is void on bone, so a sheet the player has
never seen written on still belongs to that world. A plain wax seal with no crest, because
any mark at all invites reading it as an identity and the item must not have one.

The seal came out a **square**. A radius test over five pixels has nothing to round off,
which is why vanilla's item icons are drawn rather than generated; the mask is now six
rows set by hand.

### What is deliberately still missing

Nothing in the world produces a letter. `WORLD.md` says you are the only one left carrying
the mail and never says how you came to be carrying it, and the options are not
interchangeable — shrine chests implies it never sent them, the crater implies you find
them standing in the hole you made, and *returned undelivered* implies it did send them
and they came back, which is a different and sadder story. That choice is the story rather
than the plumbing, so it went to the owner instead of being decided here.

## Unverified and unverifiable are not the same word

The last real batch of `VERIFY:` markers — the ones ARCHITECTURE.md carried for
registration, for capabilities, and for networking. Clearing them turned out to be less
about looking things up than about noticing that the markers were being used for two
different situations that deserve different answers.

**Registration and state cleared on evidence this repo generates.** Every shape is one
CI compiles and boots a server on: `DeferredRegister.Blocks`, `registerSimpleBlock` with
a `UnaryOperator`, and — the one the marker was really about — the capability rework.
Capabilities are genuinely gone as the place you hang arbitrary data. Attachments
replaced them, and this mod's single most load-bearing piece of state is one: the
per-chunk record of player-placed blocks that the unraveling, the ferry and three of the
four god-worlds all consult.

The section now separates attachments from **data components**, because both are
"arbitrary data on a thing" and they are different registries. Writing them side by side
surfaced the trap worth recording: a component that is `persistent` but not
`networkSynchronized` exists on the server and is invisible to the client, which for a
tooltip is a bug that presents as a rendering problem.

**Networking cleared on weaker evidence, and says so.** This mod does not send a packet.
The shapes came out of `PayloadRegistrar`, `RegisterPayloadHandlersEvent`,
`IPayloadHandler` and `CustomPacketPayload` in the sources jar, so the section is marked
as read rather than compiled. That is a real distinction and the doc makes it, because a
reader who cannot tell which kind of claim they are looking at will trust both equally
and be wrong about one.

### The four that remain are not debt

The interesting half. `MODELS.md`'s render types cannot be cleared by reading the jar at
all — the question is whether a cutout model *renders correctly*, and answering it needs
a client this container does not have. The tint marker is a note to whoever adds the
first tinted texture. `PLATFORM.md`'s is a standing caveat that build numbers must be
checked at setup, and should never be cleared. And `VERIFICATION.md`'s gametest marker
has been **overtaken**: block and entity behaviour here is tested by booting a real
server and driving it over RCON, which covers what gametests would and additionally
proves the mod loads.

Each of those now states what evidence would clear it. That was the actual work. A
marker that says only "unverified" invites someone to clear it by looking harder, and
three of these four cannot be cleared that way — one needs a client, one needs a feature
nobody has written, one is permanent policy. Saying so is the difference between a
to-do list and a list of things that will sit there forever making the to-do list look
longer than it is.

## The overworld starts leaking somebody else's law

Band 3. `WORLD.md`: *"Not their blocks. Their **rules**. The dead god's policy was what
held the systems apart — the Isolation was a policy, not a wall — and with nobody
enforcing it, patches of the overworld begin obeying somebody else's law."*

Three decisions, and each of them was the interesting part of an otherwise ordinary
feature.

**The patches sit on the shrines.** The obvious implementation is a per-chunk roll, and it
is wrong for a reason that has nothing to do with performance: it scatters single chunks
of foreign law across the map, and *"a hollow where nothing makes a sound"* is a place you
stand in and walk out of, not confetti. Anchoring on the shrines makes the patches
contiguous by construction — and says the thing the band is about, because the shrines are
already the mod's map of where the dead god's attention was. **The places its authority
was strongest are where its absence shows first.**

**The god at a shrine never changes.** `Exodus.lawAt(chunkX, chunkZ)` is a pure function
of the coordinates: nothing stored, nothing rolled, no state to migrate. Walk away from a
silent hollow and come back next week and it is the same silent hollow. That is not tidy
engineering, it is the feature — `WORLD.md` makes band 3 reconnaissance, *"the apocalypse
is teaching you the curriculum"*, and a patch that changed god between visits would teach
nothing. It would be weather.

The hash is a finalising integer mix rather than `(x + z) % 4`, which draws diagonal
stripes: a player walking one direction would meet the same god over and over and three
quarters of the curriculum would never appear. The self-test asserts the decorrelation by
sampling a 41×41 grid and walking an anti-diagonal — and getting *that* assertion right
took two attempts, because the first one walked `x == z`, where `(x + z) % 4` evaluates
`2x mod 4` and alternates, so the check I had written to catch the striping bug agreed
with it.

**The laws are not reimplemented.** A leak calls `Verdant.grow` and `Hearth.age` — the
same methods the gods' own dimensions call. A curriculum that taught a slightly different
lesson than the exam would be worse than no curriculum, and this way there is exactly one
method to change. Two of four leak; the Anchorite's law is per-entity and the Quiet One's
is per-dimension, and both are named as gaps in `HANDOFF.md` rather than half-built.

### The check was wrong, and the mod was right

`tools/exodus_check.sh` failed on its first complete run, and the failure was mine.

It asserted that a shrine leaking a *non*-block-level law greened **zero** of its targets.
The run came back with the two Verdant shrines at 7 of 8 and the six others at 0, 0, 0, 0,
1 and 2 — which is a clean pass with an unambiguous boundary, rejected by a threshold that
had no business being one. Grass spreads in the overworld. `verdant_check.sh`, two
increments earlier, measures exactly that and says in its own comments that *"a check
demanding zero at home would be flaky by construction."*

I had written the lesson and then walked into it, and the reason is worth more than the
fix: `turning_check.sh` sits between the two and is *legitimately* absolute, because
nothing in vanilla turns stone into cobblestone. "This law's control is zero" was a live
and correct pattern one file away from a law where it is false. The distinguishing
question is not about the check at all — it is whether **vanilla already performs the
mechanism under test**. Recorded as [`LESSONS.md`](LESSONS.md) #27.

The assertion is now a comparison: the worst Verdant shrine must out-green the best
non-Verdant one by a margin, with sixteen targets per shrine instead of eight to halve the
relative variance. Measured green: **10 and 11** of 16 at the Verdant shrines, **0–3** at
the other six. Watched failing first, against a build where `Leaks.apply` called
`Verdant.grow` for every law.

### Two checks that came out of getting this wrong

That mutation run also surfaced, for the **third** time, a backtick inside a
double-quoted `fail` message — which bash runs as a command, so the diagnostic arrives
with its own subject cut out of it, on the failure path, at the moment somebody needs to
read it. Twice is a coincidence. Three times is evidence that I cannot see this class of
defect by looking, and evidence about the reviewer is evidence about what has to be
automated. `tools/failpath_check.py` now flags any backtick outside a comment in any
`tools/*.sh`, plus a script that calls `fail` without defining it — which under `set -e`
is a red build carrying no message at all.

And while writing the cross-references for those lessons I found that `docs/LESSONS.md`
already contained **five** dead ones. Headings had been rewritten over the months; the
`#anchor` links pointing at them had not. They still rendered, still looked like
citations, and landed the reader at the top of a two-thousand-line file. The doc-link
check validated only the file half of a link and was green throughout. It is now
`tools/doclink_check.py` and checks the fragment too — verified by being written against
the rot it then found, which is the best kind of first run a check can have.

### And one entry in the ledger of my own mistakes

Cleaning up after testing that new check, I ran `git checkout -- docs/LESSONS.md` to drop
two deliberately-dead links I had appended — discarding eighty uncommitted lines of
lessons written minutes earlier. `git checkout --` is not an undo; it is a restore from
the index, and its blast radius is the file's entire uncommitted history. Recoverable only
because the text was still on screen. [`LESSONS.md`](LESSONS.md) #29, written twice.

## Weight leaks too, and the world had been stopping without telling anyone

The Anchorite's law now leaks into the overworld, closing the gap named in `HANDOFF.md`
an hour earlier. Three of the four gods' laws show up in band-3 patches; only the Quiet
One's is still missing, and it is missing for a reason this container cannot fix.

**It arrives by a different door on purpose.** The Verdant's growth and the Hearth-Turner's
ageing are per-chunk operations, so they come through `Leaks.apply`, which the level tick
calls once per leaking chunk. Weight is not a chunk operation — a falling block rises —
and there is no honest way to write it as one. So it comes through `Leaks.leaks`, which
`AnchoriteEvents` asks about the block's own position on the same `EntityTickEvent.Pre` it
already used for the Mass Authority.

That matters for the reason the whole band exists: **the patch has to run the same law,
and it can only be the same law if it is the same method.** Sand in an overworld patch
calls `Anchorite.lift` — the same call the Anchorite's own world makes. A second handler
that merely behaved similarly is what `Leaks` exists to prevent, and it was the easier
thing to write.

One ordering changed with it. `AnchoriteEvents` tested the dimension first, on the
reasoning that a reference comparison is the cheapest instruction available. True, and
beside the point: what matters is how many entity ticks a test throws away. `unanchored`
is an instanceof against a class almost nothing in a running world is, so it rejects
nearly every tick in the game, and the leak lookup behind it is paid for only by the
occasional falling block.

### The feature took twenty minutes. The check took the rest of the night.

**The Verdant's control was wrong twice more.** The absolute ceiling I had set two hours
earlier at 4 of 16 was hit by a clean run at exactly 4, on the nose — a ceiling a passing
run touches is flaky by construction, the same mistake as the zero it replaced. Then the
margin itself failed: a run produced a Verdant shrine at 5 and a control at 4, which would
have been a red build with nothing wrong. Grass spread here is roughly Poisson with a mean
near nine; no margin over sixteen samples was ever going to be safe.

So stop tolerating the confound and remove it. `Verdant.grow` performs its own explicit
random ticks and never consults `random_tick_speed`; vanilla's spread does nothing else.
One gamerule at zero switches vanilla off and leaves the mod untouched, and the control
stops being statistical: **the only thing left that can turn dirt into grass is the leak.**
Measured green at 16 and 14 of 16 against 0 at all six controls. `LESSONS.md` #27 was the
right rule and got me to an honest check; `LESSONS.md` #2 is the better one, and I reached
for the second-best tool because I had just finished writing it down.

**Then the gamerule name was wrong.** 26.x renamed the whole set to snake_case behind a
registry — `random_tick_speed`, not `randomTickSpeed`. The server answered "Incorrect
argument for command", nothing switched off, and the check went red with a message reading
"in a world with randomTickSpeed at zero", describing a world that did not exist.
`deicide_check.sh` has carried a comment about this exact rename since the day it was
written. The rule is now read *back* from the server rather than assumed to have taken,
`docs/PLATFORM.md` has a rename table with what each one broke, and
`tools/renames_check.py` enforces the shell half on every push.

### And underneath all of it, the world had stopped

Sand still would not fall. Every hypothesis was wrong in turn — the gamerule (disproved in
isolation), band 3, the leak, server load, scale. What settled it was asking the world what
time it was, on both sides of the wait:

```
time is 94     -> time is 174     # 4 seconds, 80 ticks. Sand landed.
time is 1199   -> time is 1199    # 4 seconds, 0 ticks.  Sand stuck.
```

A dedicated server ships with `pause-when-empty-seconds=60`. Every check here runs with no
players, so from a minute after boot the world simply stops — no warning, no lag message,
no exception — and every probe keeps answering accurately about a world that is no longer
running. `exodus_check.sh` is the first check that ever waited longer than a minute.

`server_smoke.sh` now writes `pause-when-empty-seconds=0` so no future check has to know.
Nothing that shipped was wrong because of it: every other check waits well under a minute
and their results stand. `turning_check.sh`, at forty-three seconds, had been living just
under the edge.

**The control I should have written first** is what made the difference. This check had no
answer to *does sand fall in this world at all*; `anchorite_check.sh` has carried exactly
that control since the day it was written and says why in its own header. Adding it took
one column of sand in an empty chunk, and on its first run it turned "the leak is applying
one god's law at every patch" into "gravity is not working anywhere in this world, so the
fault is in the harness rather than the mod". `LESSONS.md` #30.

### A mutation caught by the wrong assertion proved nothing

Worth its own note, because it is the failure mode that hides best. The mutation written to
verify the new Anchorite assertions made `Leaks.leaks` answer true for every law rather
than the one asked about. The check went red — on the *Verdant* comparison, which that
mutation cannot affect, purely because the same run drew an unlucky 5. The run ended before
the sand table printed, so the assertions under test never executed. Reading only the exit
code would have recorded them as verified. With the Verdant half now deterministic, the
mutation reaches what it was aimed at.

## The first god's letter can be delivered

`WORLD.md`: *"Each world's questline opens by delivering that god's letter. Their reaction
to the news is their characterization."* The Verdant's reaction is locked — *"the one who
covered … delivered its letter it is immediately **defensive**: it assumes it is being
blamed, before anyone has said anything"* — and the scene takes that literally. It opens
mid-argument. Nobody has accused it of anything; nobody has spoken. The first line is a
god defending an arrangement to people who have not mentioned it.

The permitted comedy is the **system**, per the dread covenant: a god's answer to a
bereavement is a coverage dispute. An arrangement, agreed by both parties, temporary. A
handover that never happened because it was never asked for — *"that is what an unreturned
duty means. It means the other party is content."* Played completely straight. The Verdant
does not know it is being funny, and the grief in the letter is never the punchline.

**The name stays out of it.** The reveal `WORLD.md` builds on the letters is that the mail
uses names the player has never heard, and `letters_check.py` asserts categorically that
the three appear in their own letters and nowhere else in the shipped string table. A
delivery scene is the most tempting place in the entire mod to break that — the god is
literally being handed a letter addressed to it. So the Verdant reacts to being addressed
that way without ever repeating the word, which is the better beat regardless: *"Nobody has
addressed me like that in a very long time."*

The scene's one UNANIMOUS node is telling it the letter never mentions the arrangement at
all — taking away the defence it has been holding for an age. Nobody should be able to do
that alone, which is the same reasoning as the shrine-keeper's one cruelty.

### The interesting part is the ceiling

`RegardState.recordDeicide` drops every surviving god by 45 and locks a permanent cap at
−10. The most generous path through this scene is worth +29, so it lands at **−16**: still
hostile, still capped. There is no branch that makes the Verdant comfortable, and that is
not restraint I exercised while writing lines — it is arithmetic the regard model imposes
on any god scene written after a deicide. `tools/delivery_check.sh` measures it live:
`VERDANT: -45 -> -16 (permanent cap -10)`.

**Then I checked whether that assertion could actually fail, and it could not.** I had
written it believing it would catch a later scene with one over-generous branch. Raising
this scene's most generous option to +25 should have produced −1. It produced exactly
−10: the engine clamps, so a scene physically cannot lift a god past its ceiling and the
assertion cannot fail from the dialogue side. The header now says so. A check described as
defending something it cannot defend is worse than no check, because the next person
writing a god scene would trust it. What it does prove — that the clamp survives JSON,
conversation runtime and saved data rather than only living inside `RegardState` — is
real, and is what it now claims.

The experiment turned up the opposite hazard, which is recorded in `HANDOFF.md` rather
than fixed: an author writes `+25` into an option, a capped player silently receives `+0`,
and the choice reads as consequential while doing nothing. `dialogue_check.py` already
rejects `regard: 0` for that exact reason — *"omit it rather than implying a
consequence"* — and the clamped case is the same defect wearing a number. It cannot be
caught statically, because whether a player is capped is runtime state.

### Two things the check caught about itself first

It failed twice before it was right, both times loudly, both times for reasons worth
keeping.

**A guessed marker string.** The first version asserted `talk=started`. The engine prints
`talk=open`. `LESSONS.md` #26 is exactly this, and the only reason it was not a silent
no-op is that the assertion was positive — a negative one would have passed forever.

**An untested precondition.** Bare `interregnum record deicide` records the milestone with
no killer, so nobody's regard is touched. The first run came out at `KNOWN(29)` with no
ceiling at all: the cap assertion — the one the file exists for — had nothing to test. It
failed on the "could not read the cap" branch rather than passing green, which is the
difference between a check that is wrong and a check that lies. Naming the killer is what
puts the ceiling in play.

## The world can be held together by living in it

Band 4's first half, and the half with the idea in it. `WORLD.md`: *"It frays where nobody
tends. Regions people visit and keep hold their definition. This makes the 'take the job'
ending literal rather than thematic — holding the world together shrine by shrine is
exactly the counter-move. The apocalypse becomes a thing you can argue with."*

Tending is simply **being somewhere**. No item, no ritual, no button. A chunk carries the
game time anybody was last near it, and a player who lives in their base keeps their base
without ever learning that the system exists — which is the right way for a counter-move
to a slow apocalypse to work.

### The contradiction that had to be resolved before anything could be built

Band 4 cancels itself out if you take it at face value. Attrition must act where **nobody
tends**. But like the unraveling it can only act on **loaded** ground, because placement
tracking answers "claimed" for an unloaded chunk and so protects it absolutely — and
loaded, in practice, means near a player. Those two leave nowhere in the world where
anything can ever happen.

The resolution is that the two distances are **different sizes**. Tending is intimate: two
chunks, the ground you are standing on and can see the detail of. Loading reaches several
times further. So the ring between them is ground that is present but unattended, and that
ring is the only place attrition can act.

That is not a workaround, it is the mechanic. A base you actually live in keeps its
forest. The forest eight chunks out — loaded every day, walked through never — goes
quietly generic, and one day you notice you cannot remember it being like that.

**First sight counts as tending**, which is the other decision worth recording. Ground
with no stamp is not ancient ground, it is ground nobody has looked at yet. If unstamped
meant stale, a player exploring at band 4 would find fresh land that was already plain —
which reads as broken worldgen, not as a world forgetting. Attrition has to be something
you can watch happen to a place you knew.

### A check that missed the one thing it was for

`attrition_check.sh` exists to defend a single fact: tending must reach *less* far than
loading. Widen one constant and band 4 is permanently inert, with nothing crashing and
nothing logged.

So it tends the chunk under you, reads a chunk four out, and asserts that one was not
tended. Ground stamped this instant reads `0` ticks ago, so the assertion was
`far_tended -gt 0`.

**It let the mutation through.** With tending widened to twelve chunks both chunks are
stamped in the same instant — but the two probes are separate RCON commands and can land
one tick apart, so the far one read **1**. Green. Re-running caught it, which is worse
than a clean miss: the check was not wrong, it was *flaky*, and a check that fails half
the time gets read as an infrastructure problem rather than a real one.

The fix is a comparison. The far chunk is stamped at chunk load and the near one at
tending — about eighty ticks apart honestly, zero or one when mutated — so the assertion
is that the gap is at least twenty. Verified against two consecutive mutation runs rather
than one, because the defect being fixed was a coin flip.

**This is the third time in one session**, and the surface details differed every time:
"nothing grew here" where vanilla grows grass; a ceiling of 4 that a clean run hit at
exactly 4; and now "more than zero ticks ago" where the clock ticks between two commands.
Every one was a threshold sitting on the boundary of something that varies for reasons
unrelated to the property under test. `LESSONS.md` #31 states the rule that covers all
three: a threshold is a claim about variance, so do not write one until you have measured
the variance — and if both populations can be measured in the same run, compare them and
never pick a constant at all.

### What is left of band 4

The conversion table: what *generalised* actually means, block by block. Ores to stone,
biome foliage to grass and then to nothing in particular. It is a third use of the
`ConversionDef` machinery that the unraveling and the Turning already share, which makes
it the cheapest large thing still on the list.

## Band 4: the world forgets what it was

The other half of attrition — what *generalised* actually means, block by block.

Every rule answers one question: what is the plainest thing this could be? Not the most
broken thing, the most **generic** one. Nothing in the table makes rubble. Ores return to
stone, deepslate ores to deepslate (their plainest equivalent is the stone they are
actually embedded in — a vein of stone at y=−40 would read as somebody having been there).
Birch, spruce, acacia and jungle go to oak. Podzol and mycelium go to coarse dirt and then
to dirt. Flowers go to grass and then to nothing in particular. A birch forest does not
become a ruin; it becomes an oak wood, then trees, then somewhere with some trees on it.

Loss by generalisation reads sadder than loss by destruction, and it is the whole reason
band 4 is not band 2 with bigger numbers.

### Three uses, and by now one type

`WORLD.md` locked the first reuse — *"the block-aging registry powering the Turning is the
same system that runs the unraveling"* — and attrition is the third caller. By then the
shape had been written twice identically: a map from block to rule, a refusal to accept
two rules claiming the same `from`, and chains formed by one rule's `to` being another's
`from`.

So `StepTable` was extracted from the Turning's copy rather than written a third time, and
the Turning migrated onto it. `turning_check.sh` in CI is what makes that migration safe
to do: the Hearth-Turner's world still ages stone through cobble to moss, so the shared
type behaves exactly as the copy did. What differs between the three systems is only what
their tables contain — loosening, weathering, generalising — which is the content.

### The gates were in the wrong place, and the check would not have noticed

`Generalise.step` originally tested only the claim ledger. The command that calls it
tested the dimension, the band and the staleness, so it could report a precise reason for
refusing.

That is the shape of a check that tests its own harness. `attrition_check.sh` asserts that
tended ground is spared — but with the staleness gate living in the command, deleting it
from the sweep would have left the check green and the law gone. The assertion would have
been about my command's argument handling.

All four gates now live in the law, the command reports what it returns, and the sweep and
the command are the same rule with different callers. Both refusals are mutation-verified
against the law itself: removing the staleness gate fails with *"a birch log in TENDED
ground was generalised anyway"*, and removing the claim check fails with *"a block a
player had placed was generalised"*.

### One operator verb, and why it is not a bypass

Ground frays after twenty minutes untended, and no CI run waits that out — `/time add`
cannot help, because it moves dayTime while the stamp is compared against gameTime. So
`interregnum attrition abandon` stamps a chunk as last tended long ago, the way an
operator marking a region abandoned would.

It writes the same field tending writes, with a different value. Every gate downstream
still applies to ground marked that way, exactly as it would to ground that got there by
being ignored for twenty minutes — which is what the two refusal assertions demonstrate,
since both of them operate on abandoned ground and still refuse. The threshold itself is
arithmetic and is proven in core's self-test, on both sides of the boundary.

This half of the check is categorical throughout, and gets to be for a reason the
Verdant's growth did not: nothing in vanilla turns a birch log into an oak log, or podzol
into dirt, or diamond ore into stone. There is no background process to separate the mod's
effect from, so a single conversion where none was permitted is the law having escaped its
gates rather than a rate to compare.

## The second god's letter, and a hazard I had written off

The Anchorite's delivery scene, and the pair with the Verdant's is the point: **two
openings, one machinery.** The Verdant opens mid-argument, defending a coverage
arrangement nobody mentioned. The Anchorite does not defend anything — it has been
holding something for an age, and asks *what the load is* before it asks who you are.

Its locked source is its own letter, which shipped long before this scene existed:

> `SUBJECT: MASS AUTHORITY — no matter arising`
>
> *There is no matter arising. I have checked the register twice and there is nothing I am
> required to raise with you, which is why this has taken the form it has. The undersigned
> has no procedure for the other kind.*
>
> *You held the corner of it while I set the rest. I do not think either of us has said so
> since. Filed for the record, in case there is one.*

So the relationship is not an estrangement at all. It is a shared piece of work neither of
them ever acknowledged, and the dead god wanted to say thank you and could only do it by
filing the form that exists for saying nothing.

The scene follows the consequence rather than restating the letter. Nobody ever came back
to say the setting was finished, so the Anchorite never let go — and its whole world, the
one where unanchored things rise and do not stop, is a place where the only thing still
being held down is that one corner. The UNANIMOUS node is telling it the setter is dead
and it can put the corner down: a mercy, and the end of the only job it has had for an
age, which is why nobody gets to do it alone.

Neither scene spends its god's name. `letters_check.py` still passes, which is the whole
reason it exists — a delivery scene is the most tempting place in the mod to break that
reveal, and nothing else would fail if it did.

### The hazard I recorded as uncheckable, checked

One increment ago I found that the regard engine **clamps** to the post-deicide ceiling,
so an author can write `+25` into an option, a capped player silently receives `+0`, and
the choice reads as consequential while doing nothing. I wrote it into `HANDOFF.md` as
something that could not be caught statically, on the grounds that whether a player is
capped is runtime state.

That was true and beside the point. **The worst case is arithmetic.** Walk every path
through a scene, take the most generous route per god, and compare the post-deicide floor
plus that route against the ceiling. If it passes the ceiling, then for the players the
scene is actually about — the ones who killed a god — some of its choices are decoration.

`dialogue_check.py` now does that on every push. The two constants are read out of
`RegardState.java` with a regex rather than copied: if `recordDeicide` is reshaped the
check fails loudly instead of quietly validating against numbers that no longer exist.
Both failure modes were watched — the over-generous scene, and the constants moving.

I found it by walking into it. The Anchorite's scene was written at **+40** against a
floor of −45 and a ceiling of −10, so its final choice — accepting the errand, the beat
the whole scene builds to — was worth nothing to the only audience it is for. Retuned to
+30, which lands at −15.

The live check now measures both: `VERDANT: -45 -> -16` and `ANCHORITE: -45 -> -15`,
matching the static computation of +29 and +30 exactly. That agreement is worth more than
either number — it means the fast gate's path arithmetic and the running game have the
same idea of what the best route through a scene is worth.

### And one thing nothing can check

`ci_claims_check.py` guards `HANDOFF.md`'s counts on every push by counting the workflow
rather than trusting the table. The pull request description carries the same numbers and
**nothing checks it**, because it does not live in the repository.

It has now gone stale twice — once at 48 commits, again at 67 — both times in the
flattering direction, and the second time it told the owner that nothing was waiting on
them while two decisions were. Rewritten against the tree, with every number counted, and
`HANDOFF.md` now carries a note saying the description must be updated by hand whenever
those counts move. That is a weaker mitigation than a check and it is labelled as one.

## The third letter, and an exposition scene you can turn off

The Hearth-Turner's delivery. Its letter is the sharpest of the four:

> `SUBJECT: TEMPORAL AUTHORITY — request for a copy`
>
> *You will have kept it. You keep all of them. I am asking for the version where I was
> wrong, and I am asking because I no longer have it and I have looked.*
>
> *Send it as it was filed. Do not correct it on my account. I am aware that asking you
> not to correct a record is its own kind of request.*

`WORLD.md` locks this god as *"the one who kept every version of the argument… the
exposition god, but earned: it is not telling you because the plot needs it told, it is
telling you because it has never been able to stop."*

**That constraint is the whole design of the file.** A god who cannot stop talking is only
characterisation if the player is the one who has to stop it — otherwise it is a lore dump
wearing dialogue options. So the exposition branches are optional at every step: letting
it run is generous and earns regard, cutting it off is reasonable and costs nothing you
can see, and neither is punished. The falling-out is available and never compulsory.

The beat is that **the copy is already held out when you arrive.** It has been ready for
an age. Nobody ever asked. The one who finally asked is dead — so a request this god can
fulfil perfectly has nowhere to go, and it has been treating that as a clerical problem
because the other way of putting it is not available to it.

The three UNANIMOUS nodes across the three scenes are deliberately different
transgressions: take away the Verdant's defence, tell the Anchorite the job is over, ask
the Hearth-Turner to let a record *leave*. For a god whose entire law is that nothing is
ever finished and nothing is ever lost, handing something over is the unthinkable one.
None of the three is "be sad about your relative", which is the version of this scene that
writes itself and is worth nothing.

Live: `VERDANT -45 → -16`, `ANCHORITE -45 → -15`, `HEARTH_TURNER -45 → -15`, each matching
the static path arithmetic exactly. The Hearth-Turner's is the longest generous route in
the mod — it runs through every optional beat rather than skipping any — which makes its
scene the one most likely to wedge, and therefore the one worth walking end to end.

## The fourth letter, delivered to something that will not say it arrived

The Quiet One's scene, and the last of the four. It could not be written like the others.

Its letter opens `To —` with no name at all, and `WORLD.md` locks the reason: whether the
dead god never got close enough to have one for this god, or had one and struck it out,
*"is never answered; the letter itself is the only evidence and it is ambiguous on
purpose."* Every other delivery scene gets its god's side of the story. **This one cannot
have one** — the moment the Quiet One explains itself, *"the one who never wrote back"*
stops being a question about the dead god and becomes a fact about this one, and the
mid-game's best ambiguity is spent for a line of dialogue.

**So the god never speaks.** Not once. Every `speaker` line in the file is a description
of what does not happen: the letter held out and neither taken nor refused, no gesture of
declining, no turning away. That is the same joke the whole world is built on — the Nether
and the End both refuse a bed *loudly*, and this world declines to react at all — and it
makes the scene the players, in a silence, deciding what to do about it. A different shape
from the other three rather than a fourth variation on them.

The fourth transgression is **signing for the letter yourself**: recording a delivery as
received when nothing acknowledged it, filing a receipt on behalf of somebody who will not
speak. It is exactly the mod's procedural register, it is a real intrusion, and nobody
should be able to decide alone that a silence counted as consent — so, UNANIMOUS. The
alternative leaves the line blank, which is also a record: *"someone will read the gap one
day and have to decide what it meant."*

### The one assertion that sees something the player cannot

Regard with this god moves through the scene exactly as it does in the other three, and
**nothing in the text ever acknowledges it.** You are treated differently afterwards by
something that never told you it noticed. That is the intended experience.

It is also indistinguishable from the consequences being silently broken. Every other
scene in the mod tells you when you have moved something — a line changes, a speaker opens
differently. Here, a scene whose regard did nothing at all would read as the scene working
perfectly, forever, to every player.

So `delivery_check.sh` reads the number. Watched failing by stripping the positive regard
out of the file: `QUIET_ONE: -45 -> -45`, where a working scene reads `-45 -> -15`. It is
the only place in this repository where a check exists specifically to see something the
player is deliberately not shown, and that is worth naming as a category rather than
leaving as a detail.

All four now measure live: `VERDANT -16`, `ANCHORITE -15`, `HEARTH_TURNER -15`,
`QUIET_ONE -15`, each from a floor of −45, each matching its static path arithmetic, and
none of them anywhere near the −10 ceiling that killing a god leaves behind.

## Delivering a letter now moves the world

`LETTER_DELIVERED` has been in `core` since the chapter machine was written. `ChapterState`
counts it. `Chapter` gates chapters 3, 4 and 5 on the count. A grep for it across the
entire game module returned nothing.

So all four delivery scenes shipped — each the opening of a god's questline, each walked
end to end by a live check asserting the scene reached its ending and the god's regard
moved — and delivering all four letters advanced the world by exactly nothing.

Every check passed, and every one of them was true. The scene did play to its end. The
regard did move. The ceiling did hold. Nothing any assertion claimed was wrong, and the
feature was missing its entire point.

**The blind spot is worth naming.** Each check was written while building the thing it
asserts, so it asked *did the thing I just built do what I built it to do*. Nobody asked
the question that spans two increments — does the rest of the system notice? — because at
no point was that the thing being built. A check written alongside a feature inherits the
feature's scope, and the seam between this feature and one written a month ago is exactly
where things fail to connect.

The tell was there and I walked past it twice: `HANDOFF.md` said *"`FIRST_CROSSING` and
`LETTER_DELIVERED` are still unreachable"*, and I read that line while writing three of the
four scenes and took it as history rather than as a claim to check. It was found by
re-reading the roadmap sceptically, which is to say by doubt rather than by machinery.
`LESSONS.md` #32.

### The mechanism

A node may carry a `milestone`; arriving at it records that milestone in the world's
chapter state. It hangs on the **node** rather than on the option that leads there, and
that is not stylistic: "the letter was delivered" is a fact about the conversation having
*arrived* somewhere, not about which sentence somebody picked to get there. Three routes
into one accepting ending should record one delivery, not three, and the route where the
players refuse the errand should record none at all. On the option, the milestone would be
a property of a choice, and every new branch into the same ending would be a fresh chance
to forget it.

`dialogue_check.py` refuses a milestone on a non-terminal node for the same reason, and
**derives the valid names from core's enum** rather than keeping a list. The two lists
above it — institutions and standings — are hand-kept with a comment asking the next
person to remember, which is the older convention here and not the better one: this
session has spent most of its length finding out what silently-rotted copies cost.

Core gained the guard and the mutation that matters, which is not "a scene forgot to mark"
— a live check catches that — but **every node marking**, which would advance the chapter
on every line anybody speaks and end the progression before the first scene did.

Live: `letters delivered: 0 -> 4`. Watched failing by stripping the marks back out, which
is the state the repository was actually in: `0 -> 0`, with every other assertion in the
file still passing.

## The first spell, and two ways it nearly shipped as a coin flip

*Weather* — the Turning's, and the mod's first. `WORLD.md` names it: *"age blocks: instant
mossy/cracked/oxidized — magic as a builder's palette"*, under the locked doctrine that
**every spell is a world-verb**: no damage buttons with particle effects.

It is the cleanest possible first case for that rule because it has no combat use at all.
The first thing this mod teaches you to do with magic is decorate.

And it is not new machinery. `WORLD.md`'s locked reuse note says the ageing registry
powering the Turning **is** the system that runs the unraveling, so the spell calls
`Hearth.step` — the same method the Hearth-Turner's world runs on its clock. `ageOnce` had
the dimension gate baked into it, which would have made Weather castable only in the god's
own world, so the gate moved out to its callers and the law became one method. Every
promise the table already makes now holds for the spell for free, including the oldest one
here: you cannot Weather somebody's wall.

**The economics are the interesting half.** Locked: *"With the god dead, all overworld
casting draws on the corpse… Heavy casting visibly frays its surroundings. The Wardens'
law is right, and the player can discover it is right."* So a cast at home frays the
ground around the caster, through the unraveling, spending the same residue in the same
currency the player has been reading since chapter one — and the cost rises with the band
with nobody tuning it.

The enforcement agency is not wrong, and nothing in the mod ever says so. You cast at
home, watch the ground go, cast off-world, watch it not, and work it out.

### Twice it was a coin flip wearing a categorical assertion

The check failed its first run reporting `frayed 0` and the failure was mine both times.

**The fraying sampled a cube** around the cast. A thirteen-block cube is almost entirely
air, so under a tenth of samples landed on anything at all and a cast usually cost nothing.
Fixed by sampling the surface column — which is also what the sentence means, since
*"frays its surroundings"* is about ground a caster can see.

**Then it sampled a random depth below that surface**, and only the top layer had a band-1
rule, so the cost came out at 2 of 12 and one cast in fourteen still spent nothing. That
would have shipped `casting costs the overworld something` as a 93%-true assertion dressed
as a categorical one. Depth sampling belongs to the passive unraveling, which is eating a
whole world and should reach under it; a cast's cost is local, immediate, and has to be
reliable. Now every sample lands on the surface: **12 of 12, twice running.**

Both are `LESSONS.md` #31 again — a threshold on something that varies — except the
randomness was not in a roll this time, it was in *where the samples landed*, one level
below where I was looking. That is the fourth time this session, and the first where the
variance was hidden in the sampling geometry rather than in a probability.

### What is not built

How a player learns it. The command is the seam, as it is for `unravel at` and
`turning age`. `WORLD.md`'s *"schools, one per god, learned in their worlds"* is the next
increment — and it is exactly what the questline middles have been short of, so the two
gaps close together.

## Magic is learned in its god's world, and only there

`WORLD.md` locks schools as *"learned in their worlds"*, and the last three words are the
whole progression. They are now enforced: **nothing is known by default**, and an untaught
caster is refused outright. The reason to recommission the ferry is not a stat bonus — the
verbs themselves are over there.

A per-player `Grimoire` persists with the world, following `RegardSavedData` exactly and
stored on the overworld for the same reason regard is: what somebody knows is a fact about
them, not about where they happen to be standing. A player who learns the Turning in the
Hearth-Turner's world still knows it at home, which is the entire premise of the overworld
ban being a *choice* rather than a wall.

It only ever grows, and there is no method to unlearn. A school is something you
understand about how the world works; the Wardenate can make casting a citable offence and
a god can refuse to teach you the rest, but neither reaches into your head. The
consequences of casting are enforced where casting happens.

**A scene does the teaching**, through a `teaches` field on a node — the same shape as
last increment's `milestone`, and on the node for the same reason: being taught is a fact
about where the conversation *arrived*, not about which sentence somebody picked. The
Hearth-Turner's accepting ending teaches the Turning, so delivering its letter now opens
onto something rather than promising and stopping.

**Everyone at the table learns.** Not only the initiator, and that is design rather than
convenience: the table is this mod's answer to the pseudo-main-character, and a god that
taught one of four people standing in front of it would hand the group a protagonist.

### The before-and-after that makes it evidence

`delivery_check.sh` asks the same caster to cast the same spell on the same block twice —
once before any scene has run, once after all four. Before: `unlearned`. After:
cobblestone. The only thing between the two attempts is the conversation, which is what
makes the pair evidence rather than two separate observations.

`casting_check.sh` gained the matching negative: it casts once before teaching anybody and
expects the refusal, because without it every successful cast further down would be
measuring a default rather than a rule.

Two small decisions worth keeping. The refusal reports **`unlearned`** rather than falling
through to "that block had no rule" — a caster who has never been taught should be told
that, since the two answers point at different things to do next. And the cast command
takes a **caster** rather than firing anonymously from the console: a seam that could cast
with nobody behind it would be a seam that skipped the prerequisite, and the check driving
it would prove nothing about the rule.

Core: 157 self-test checks, 50 mutations. The three new ones are the directions that would
be silent — everybody can cast untaught, one lesson opens all four schools, and casting at
home is free.

## A second spell, and the school system stops being one special case

***Lighten*** — the Anchorite's. `WORLD.md`: *"shared low-gravity zone, **mobs float
too**."* Those last three words are the spell. It is not a buff you put on yourself; it is
a piece of the world briefly obeying the Anchorite's law, and everything inside it is
subject — you, the skeleton chasing you, the gravel over your head.

That is what satisfies the locked doctrine that a spell's *"combat use falls out of its
world use, never the reverse"*, and it is the clearest case in the kit: **you cannot aim
Lighten at anybody.** You can only change the rules where they happen to be standing.

**It is the god's own law, borrowed.** The zone does not implement floating — it makes
`Anchorite.lift` apply where it otherwise would not. There are now three callers and one
law:

- the Mass Authority, where it is simply how things are;
- a band-3 patch of overworld that has forgotten whose it is;
- a person who has learned how to ask.

That progression is the school system's entire argument. You meet the law as a **place**,
meet it again as a **wrongness** leaking into your own world, and the third time **you are
the one doing it**. It is only the same law because it is the same method, which is why
all three are clauses on one handler rather than three handlers that would drift.

### Two spells, two shapes, on purpose

*Weather* changes a block and is finished. *Lighten* opens a **zone** — a cube with an
edge and a lifetime — and having both is what makes the school system a system rather than
one hardcoded case. The parts they share are exactly the two rules every spell has: you
must have been taught it, and at home it costs. Everything else about them is different.

The zone is a cube by Chebyshev distance, matching every other region in this mod, because
a player has to be able to find where it **ends** — stepping out is how you learn it has an
edge, and therefore that it is a rule rather than the world breaking. A sphere's edge is
not findable by walking.

Zones are held in memory and do not survive a restart, which is a decision rather than an
omission: a spell whose effect outlived the server could strand somebody inside a
low-gravity field cast by a player who has since left, with no way to know what it is or
when it ends. Everything permanent here is persisted; half a minute of altered physics is
deliberately not. They are also cleared on shutdown, because a static map outlives a world
in a development run and a leftover zone would apply to the next world at coordinates that
mean something else.

### The check that had to look like another check

`lighten_check.sh` is deliberately shaped like `anchorite_check.sh` — same sand, same
floor, same probes, including the two that tell *rising* apart from *deleted* and from
*gravity switched off rather than reversed*. The claim being made is that the outcome is
**identical** to the god's own world, so the check that proves it should be too.

Its control is twenty blocks away in the same world on the same tick: outside the zone,
sand still falls. Without that, "the sand inside did not land" is also satisfied by sand
that never spawned and by a chunk that never loaded — and the mutation that gives the zone
no edge at all is caught by exactly that control, reporting that gravity had stopped
working everywhere.

One self-inflicted bug worth keeping. The untaught assertion looked for
`opened=false refused=unlearned` as one adjacent string, which the output never contains —
`frayed=0` sits between them. It failed loudly against behaviour that was correct, which is
`LESSONS.md` #26 in its cheapest possible form: a positive assertion about text that was
right there in the output, guessed at instead of read.

164 self-test checks, 52 mutations. The two new core guards are the ones that fail
silently: a zone with no edge looks like the spell working from inside it, and one that
never lapses looks like it working for longer than you were watching.

## Three spells, three shapes, and what you grow is yours

***Bridgeroot*** — the Verdant's. `WORLD.md`: *"grow a living span toward your gaze, **real
persistent blocks**."*

Those last three words are the design brief and they are unusual enough to dwell on. Most
games' bridge spells are temporary platforms that evaporate, which is a *movement ability*
wearing a spell's clothes. This one leaves actual world behind. You can build a house out
of it. Somebody can walk across it a year later.

**Which makes the load-bearing decision the claim ledger.** Every block a span leaves is
recorded exactly as if you had placed it by hand, so the unraveling, the Turning and band
4's attrition all refuse it — all three consult that ledger. It is the only reading that
makes "real persistent blocks" mean anything: a bridge the world dissolves next chapter is
a temporary platform with extra steps, and a player who lost one that way would, correctly,
never trust the spell again. Growing something and having it be **yours** is what lets
Verdancy be a building school rather than a traversal one.

It never replaces anything: a span grows into air and stops at the first block it meets.
Stopping short is legible. Boring through terrain — and worse, through whatever somebody
built in the way — is not.

### The geometry is in core because it is arithmetic

Which blocks a span occupies has no game in it, so it is decided in `core` and tested
without one. A Bresenham-style walk along the dominant axis, rounded rather than truncated
so the line stays centred instead of drifting toward the origin on the minor axes.

**Gaps are the failure that matters**, and both the self-test and the live check assert
continuity as *adjacency* rather than as a count — a count of twelve is equally satisfied
by twelve blocks with a hole in the middle, and the hole is the entire problem, because
you find out about it while standing over it. The mutation that skips every other block
reports exactly that: `holes at 2 4 6 8 10 12`.

### The three shapes are the point

*Weather* changes a block. *Lighten* encloses a region. *Bridgeroot* creates. Having all
three is what shows the school system carries genuinely different **kinds** of verb rather
than one mechanism with three names — and all three share exactly two rules: you must have
been taught it, and at home it costs. Everything else about them differs.

Three of the four questlines now open onto their school. Only the Quiet One's has no spell:
*Hush* is named and locked, and its region form is the same thing band 3 cannot leak, for
the same reason — client-side audio suppression this container cannot verify.

170 self-test checks, 54 mutations. The two new core guards are an uncapped span, which
makes the overworld's casting cost a rounding error, and one that starts under the
caster's feet, which suffocates whoever cast it the first time they use it correctly.

## Four schools, and the spell whose combat use most obviously falls out of its world use

***Hush*** — the Quiet One's, and the last of the four. `WORLD.md`: *"true no-sound zone:
sculk blind, mobs cannot alert, **a creeper that cannot hiss cannot detonate**."*

That bolded clause is the whole spell and it is not a joke. A creeper's fuse **is** a
sound. Take the sound away and the mechanism it belongs to has nothing to complete. The
mod's doctrine — *"every spell is a world-verb… its combat use falls out of its world use,
never the reverse"* — arrives here at its most literal: Hush is not a defensive ability, it
is silence, and silence is fatal to a thing that kills by announcing itself. A player will
work that out about two seconds after being told what it does to sound.

**Two clauses enforced, one not claimed.** Nothing inside acquires a target, and a
creeper's fuse is wound back each tick so it chases, looms, and never arrives — which
feels far better than a creeper standing inert. The *audible* silence and sculk going
blind are client-side and this container has no client, so they are stated as unbuilt
rather than implemented on the strength of a comment. It is the same wall band 3 met: the
Quiet One's law is the one law whose most characteristic form lives on a client.

The deliberate case forced a decision worth recording. A creeper struck with flint and
steel carries an `ignited` flag with a public setter and **no public way to clear it** —
verified by reading the decompiled source rather than assuming. So for that one case the
tick is cancelled outright, which freezes it: more than silence would do, and the honest
trade, because the alternative is a hole in a locked promise. A lit creeper standing
perfectly still in a silent field is also exactly what this god should look like.

### The second zone made the first one wrong

`Zones` held one list per world. That was correct while one spell opened zones and became
wrong the moment a second did: standing in a Lighten field would have silenced creepers,
and standing in a Hush would have made the gravel float.

**And it would have looked like both spells working**, from inside either one. The failure
has no symptom except in the case nobody thinks to try — a creeper inside the *wrong*
zone — which is why `hush_check.sh` puts one there and asserts it detonates normally. Zones
are now keyed by school, and the mutation that pools them again is caught by that
assertion alone.

### The control that had to come first

The check asserts three creepers: inside a Hush, outside everything, and inside a Lighten
field. The **outside** one is asserted first and deliberately so — without it, "nothing
exploded inside the silence" is equally satisfied by creepers that never spawned, a chunk
that never loaded, or a mod that quietly broke creepers everywhere. That last one would
pass every other assertion in the file while being the worst possible outcome.

173 self-test checks, 55 mutations. The new core guard is four spells belonging to four
different schools: two sharing one would make a single lesson hand out another god's verbs,
and there would be no reason left for four journeys.

## Six increments in, the docs described a mod without magic in it

A consolidation pass rather than a feature, and it is worth one because the pace of the
last stretch outran the thing a fresh session reads first.

`HANDOFF.md`'s roadmap still listed the questline middles as the gap scenes were short of
— which was true when it was written and stopped being true two increments later, when
each god's delivery scene started teaching its school. It also had no row for magic in the
state table at all, and told a reader that two things waited on the owner when there were
about to be three.

**One question changed rather than closed, which is the interesting one.** "What should a
Warden be able to cite you for, **before magic exists**?" was written when WORLD.md's
locked countermeasures were all about casting and casting did not exist. It does now: four
schools, and an overworld ban with a real cost behind it. So the original locked answer is
available, and the two magic-free offences — the sleep code, permitted airspace — went from
being the only options to being alternatives. The question is still the owner's; it just
got wider, and a question whose *shape* has changed is worse left standing than one that is
simply unanswered, because the reader assumes it still means what it says.

**And one new question, which I walked past four times.** Four spells exist and none of
them can be cast in play — the command is the only way in. `WORLD.md` locks what a spell
*is* and says nothing about the affordance that triggers one, which makes it a mechanic
under the standing rule and therefore the owner's. Three candidates are written up: a focus
item per school given when the god teaches you, a spoken word in chat, or nothing held at
all. Nothing is blocked behind the answer — the spells are built and verified either way —
but shipping four spells and never mentioning that they are unreachable would have been the
same defect as the letters that recorded no milestone, caught earlier this session: a
feature complete in every respect except the one that connects it to a player.

## The ageing table runs backwards, and one check was proving the wrong thing

***Rewind*** — the Turning's second spell, *"repair by un-aging"*. One table, two
directions: *Weather* reads it forwards and this reads it back, which is the same doctrine
that made the Turning and the unraveling share a registry, and why the school's second
spell cost almost no new machinery.

Two decisions, both the kind that look fine while being wrong.

**It may touch what a player built.** Every other system here consults the claim ledger and
refuses anything somebody placed — the unraveling, attrition, the Turning's own clock,
*Weather*. Rewind does not, because the ledger exists to stop **the world** eating your
work, not to stop you working on it. Refusing would make the spell useless at its whole
purpose: you do not un-crack a cave, you mend your own wall. The check asserts both sides
in the same run — the Turning's clock still refuses that same block — so it is a
distinction between the two, not a hole in the guarantee.

**And some blocks have no single past**, where it refuses rather than choosing. That
refusal is the most characterful thing in the school: the god whose entire law is keeping
every version of everything is precisely the one that will not invent one.

### The assertion about that was passing for a reason that did not exist

The check aimed at a dead bush, because a dandelion and a poppy both become one. True — in
the **unraveling's** table, which Rewind does not read. It reads the **Turning's**, where
nothing becomes a dead bush at all. So the refusal fired, the assertion passed, and it
proved *"nothing ages into a dead bush"* wearing the label *"two things do"*. The
convergence logic had no live coverage at all and the header describing it was false.

It was invisible because both readings produce the same outcome, and the outcome is all a
check can see — `no-single-past` covers "nothing ages into this" and "two things do"
deliberately, because the table genuinely cannot tell them apart. That is the right design,
and it means the evidence cannot come from the assertion. It has to come from the setup.

Fixed by giving the Turning's table a genuine converging pair rather than by writing a
cleverer assertion: deepslate tiles crumble to cobbled deepslate, which plain deepslate
also wears into. That is a rule worth having on its own terms, which is the test of whether
test-driven data is honest — if the rule would embarrass you in the shipped file, the check
was asking for the wrong thing. `LESSONS.md` #33.

Two markers were added naming *which* past a guess restored, since "it guessed" and "it
guessed toward tiles" are different bugs. The mutation that keeps ambiguous reverses now
prints `GUESSED_TILES` before it fails.

174 self-test checks, 29 live checks. `turning_check.sh` passes unchanged with the new
rule, which is what made the table edit safe to make.

## A key that was unique only because one thing used it

***Still*** — the Quiet One's second spell. `WORLD.md`: *"freeze primed TNT / falling block
**mid-state**."* The last word is the whole thing: it is already happening, and it stops,
holding the state it was in. Nothing is deleted and nothing is defused — when the zone
lapses the sand falls and the TNT goes off, which is what keeps it a reprieve rather than a
damage button with extra steps.

It is a genuinely different verb from its own school-mate. **Hush is about information**:
nothing hears, nothing notices, no fuse completes because a fuse is a sound. **Still is
about motion**: what is already in flight stops. A creeper walking at you is unaffected by
Still, and a falling anvil is unaffected by Hush. They overlap on exactly one object,
primed TNT, and treat it differently — one denies it the sound it needs, the other the
moment. That is what two spells in one school should look like.

### The bug it surfaced before it happened

Spell zones were keyed by `School`. Correct while each school had at most one zone spell —
and Hush and Still are both Silence, so keyed that way **they become each other**: a
silence would stop falling blocks, a stillness would mute creepers, and nothing anywhere
would fail.

That is the second time this exact shape has come up. The first was one list of zones per
world, which broke the moment a second spell of *any* school opened one. Both are the same
signature: **a key that is unique today because only one thing uses it.** `LESSONS.md` #34.

Zones are now keyed by `Spell`, an enum that was a fact about the mod already — `WORLD.md`
lists four verbs per god — rather than an abstraction invented in case it was needed. That
distinction is the difference between this and speculative generality.

`still_check.sh` is the only place the difference has a symptom: it drops sand into a
*Hush*, the wrong zone entirely, and asserts it lands. Meaningless in every world except
the broken one, which is exactly what a guard against an invisible bug has to look like.
The mutation that keys by school again fails on precisely that line.

### And a core guard that did not guard

The first version asserted "some school teaches more than one spell", which sounds like the
right property and is not. Moving Still to another school entirely left Weather and Rewind
still sharing the Turning, so the assertion passed and the mutation escaped. Fixed by
naming the pair that actually motivated the key — Hush and Still are both the Quiet One's —
rather than counting. An assertion about "some pair exists" cannot defend a specific pair.

177 self-test checks, 56 mutations, 30 live checks.

---

## The ledger gates the world, not the caster

A bug that had already shipped, found while designing the spell *next door* — and the
check that should have caught it was the reason it lasted.

***Weather*** ages a block one step. It went through `Hearth.step`, which refuses anything
in the claim ledger. So the one spell a builder would most obviously point at their own
wall was the one thing it could not touch — and it did nothing *silently*: cast, no
change, no message. `WORLD.md` sells magic as a builder's palette; that is the opposite.

The ledger exists so the **world** may warp whatever it likes and never take a block
somebody placed. That is a promise about the world acting on its own. It was never a
promise that a player may not point a spell at their own cobblestone. `Hearth.step(level,
pos)` still spares placed blocks and is what the unraveling, attrition and the Turning's
clock call; `Hearth.step(level, pos, false)` is what a spell calls.

### The check had asserted the bug

`casting_check.sh` contained `mark CLAIMED_SPARED`, with a failure message invoking the
mod's oldest guarantee. Confident, thematically fluent, mutation-verified, and wrong — it
had been written by reading `Hearth.step` and describing what the code did. A check that
cannot disagree with its implementation is not evidence, it is a lock, and this one had
locked an accident in as a requirement. `LESSONS.md` #35.

What found it was writing *Rewind*'s neighbour rule: refusing to **repair** your own wall
is absurd enough that the question got asked aloud, at which point the same absurdity was
plainly sitting one spell over, where it read as merely odd rather than broken.

The two directions are now asserted in two different files on purpose: `turning_check.sh`
proves the world still spares a build, `casting_check.sh` proves a caster may aim at one.
Either alone is satisfiable by getting it uniformly wrong.

177 self-test checks, 56 mutations, 30 live checks.

---

## A spell that crushes nothing

***Drop-forge*** — the Anchorite's second. `WORLD.md`: *"crafting by crushing."*

The spell crushes nothing. It makes a few metres of ground somewhere an impact *means*
something, and an impact is not something it provides: you go and get the weight, get it
above the thing, and let it go. Cast into an empty room it does nothing at all for a
minute and then lapses. Nothing here is aimable, which is the strongest reading available
of the locked doctrine that a spell's combat use falls out of its world use.

It completes its own school rather than adding to it. *Lighten* takes weight away so a
thing can be moved; *Drop-forge* is what that thing is for once it is above where you want
it. And the two cannot overlap — inside a Lighten nothing falls, so a forge under a
low-gravity field is a forge with the hammer floating over it.

### The fourth table, and the first that no world runs

The unraveling loosens, the Turning weathers, attrition generalises, all on a clock. This
one waits. It answers one question — what does this do under force? — and the world has
two answers: rock **shatters** (stone, cobble, gravel, sand) and loose or soft matter
**packs** (snow, ice, packed ice, blue ice). A block does one or the other and never both.
`chance` is 1.0 throughout, which is the whole difference between an act and weather: a
crush that failed 30% of the time would not read as a rate, it would read as broken.

It bites down. Cobblestone crushes to gravel and gravel falls, so a weight dropped on a
cobble floor makes the floor under itself fall, follows it, and crushes again until the
chain or the zone runs out. Left in: bounded by the radius rather than by a special case,
and the clearest possible demonstration of what the cast actually did.

### The event, and the one place Pre will not do

Every other falling-block handler here uses `EntityTickEvent.Pre` — the Anchorite's lift
must set the delta before the entity moves, and Still must cancel the tick before it
happens. This one cannot. `FallingBlockEntity.tick` calls `move`, which is what sets
`onGround`, and then lands and discards itself, all in one tick — so `Pre` of the landing
tick still sees a block in the air and `Pre` of the tick after never comes. `Post` is
fired from `ServerLevel.tickNonPassenger` immediately after `tick()` returns,
unconditionally, including for an entity that removed itself during it. Read out of the
decompiled source rather than assumed, because the assumption was wrong.

### Verification

`dropforge_check.sh`, watched failing twice. Keying zones by school again leaves the
Lighten column green and kills `FORGE_CRUSHED`: the forge hovers its own hammer, with no
error anywhere. Adding a claim check to `Crush` kills only `CLAIMED_CRUSHED`.

The control column matters as much as either. Twenty blocks from any spell, the same drop
must leave the ground alone — without it, "the stone became cobblestone" is equally
satisfied by the unraveling, the Turning's clock, or attrition, all three of which are
converting blocks in that world while the check runs.

`crushing_check.py` is the same lesson in a second place. The table's `_comment` said the
only thing between a misfired drop and somebody's wall was that walls are not in this file
— true, and enforced by nothing. It is enforced now: no block a player builds with may
appear on the LEFT of an arrow, no ore either, and `chance` must be exactly 1.0. Both the
worked-`from` rule and the chance rule were watched failing. What a crush PRODUCES is
unconstrained on purpose; producing it is what the spell is for.

And `CLAIMED_CRUSHED` is the LESSONS #35 principle applied one increment after it was
learned: the ledger gates the world, not the caster, so a drop-forge crushes a wall its
caster built. Written from what the spell is for, not from what `Crush` does.

181 self-test checks, 59 mutations, 31 live checks.

---

## The ferry did not eat the planet

A red CI run on `391c1ba` reported *"the ground under the dock is gone — the ferry took the
world with it."* It had not. The keel sits directly on the seabed block `ferry_check.sh`
watches, and an opaque block over a grass block is how **vanilla** kills grass: on a random
tick it becomes dirt, on its own. Over the few seconds the hull stands there that is around
a one-in-thirty chance, and the file had been living on it since it was written.

Reproduced rather than assumed: with `random_tick_speed` cranked to 400 the same run fails
the same way every time, and the ferry is not involved.

Two fixes, and only one of them is about the flake.

`gamerule random_tick_speed 0` removes the confound rather than tolerating it — the third
file to need it, after the exodus and attrition checks — and the gamerule is read back from
the server, because a rejected one is silent apart from one log line.

The larger half: **the probe could fail two ways and the message asserted which.** A
capture leaves AIR; the check already knew that, ten lines above, where `ORIGIN_CLEARED`
asserts it about the hull's old position. Grass becoming dirt is not air. Split into two
probes: air names the ferry, anything else says look at the block tables and names no
culprit, because the check cannot tell which one. `LESSONS.md` #36.

Both new guards watched failing — a `setblock ... air` at the seabed produces the ferry
message, and setting the gamerule to 3 produces the readback message.

181 self-test checks, 59 mutations, 31 live checks.

---

## Everything here, at once

***Wildgrowth*** — the Verdant's second, and the fourth caller of one law. It runs
`Verdant.quicken`: the same acceleration the Verdant's world applies to every chunk it
holds and band 3's leaks apply to a patch of overworld that has forgotten whose it is. You
meet a god's law as a place, meet it again as a wrongness leaking into your own world, and
then you are the one doing it — four times now.

It accelerates and does not choose. `Verdant` refuses to keep a list of growable blocks;
so does this. A cast asks the world to tick, hard, in a small volume, so every crop,
sapling, vine, moss and mushroom is covered without being named. Which is why the locked
word for this school is *hazard*: a surge you cannot aim at the wheat and away from the
jungle closes the path behind you. In the Verdant's own world it is nearly pointless —
everything already grows at eight times the rate you know — and also free, because a
living god replenishes what casting spends. It costs most exactly where it does most.

No probability anywhere: every position gets exactly `PUSHES` ticks in a fixed order.
Twenty-four, calibrated against sugar cane, which advances one segment on exactly sixteen
random ticks with no light check and no randomness. A cast worth less than one segment of
cane is not worth a journey to another world.

### The ledger gates what you did not aim at

The sharper form of #35, forced by the first spell in the kit that sweeps a volume. "The
ledger gates the world, not the caster" is right for a spell that names one block; said
that flatly it hands an area spell a licence over other people's greenhouses.

Not a new rule — what the code has always done, now written down. Weather and Rewind name
one block and take it, your own wall included. Drop-forge changes the block your weight
lands on, and you chose where to drop it. A cast's fraying sweeps a volume nobody pointed
at and has spared placed blocks since the day it was written. Wildgrowth sweeps a volume,
so: same answer.

### The check failed on its own scenery first

The cane stood on a single sand block in mid-air with a loose water source beside it. Sand
falls. The column collapsed the tick after it was placed, all three canes broke, and the
setup probe passed anyway because it ran before gravity did — so the file reported "the
control cane is gone" about scenery it had built wrong. Dirt on a stone floor now, with
the water sunk into the bed so the source cannot flow away from the block it has to be
adjacent to.

Both halves watched failing after that: deleting the ledger gate fires `CLAIMED_GREW`,
and dropping `PUSHES` below one cane segment kills `CAST_GREW`.

184 self-test checks, 61 mutations, 32 live checks.

---

## Four worlds that no longer look alike

Three god-worlds generated `minecraft:the_void` and the Verdant's generated
`minecraft:plains`, so the register's four distinct places were, on the ground, three
identical grey rooms and one meadow carrying vanilla's whole mob spawn list. Each has a
biome of its own now.

The names use a split the mod already had. `WORLD.md` gives each world a SUBJECT line
from the dead god's letters and a name people actually use; the dimension ids were the
first set, so the biomes are the second — *the Long Green*, *Old Heavy*, *the Turning*.
The Quiet One's cannot take that name because it does not have one, so its biome is
`unanswered`: named for the silence rather than for whoever is being silent, which is the
move the fourth letter makes when it opens `To —`.

Every colour is a literal step off `assets/palette.json` and `biome_check.py` fails the
build on any that is not. The palette system existed to stop art direction being decided
one file at a time, and stopping it at textures was arbitrary — a sky is the one colour a
player cannot look away from. The Quiet One's water is the colour of its stone; the
Anchorite's world is the `metal` family, glossed in the palette as "cold, manufactured;
holds its shape"; the Hearth-Turner's is `brass` in a late afternoon never allowed to
become evening; the Verdant's is the brightest foliage step on the grass, the leaves and
the sky at once, because a place with one colour left is a place something has gone wrong
in.

Nothing spawns in any of them, and only the Verdant's generates features. The shape of
these worlds is still vanilla noise and `ModDimensions` still says so.

**A 26.2 fact worth the row it got in PLATFORM.md:** `BiomeSpecialEffects` no longer
carries fog, sky, water-fog or the ambient sound loops. Those moved into the same
environment-attribute map the dimension types here already use, set with
`Biome.BiomeBuilder#setAttribute`. The record still exists and still compiles with water
and vegetation colours only — so a biome ported from a pre-26 guide builds cleanly and has
no sky.

`worlds_check.sh` asserts relationships, not facts: each world reports its own biome AND
none of the other three (four passing probes would also pass if all four stems resolved to
one biome), the overworld is none of them, and vegetation is counted over 1024 columns
rather than probed at one. 244 plants in the Long Green, 0 in the Quiet One's. Watched
failing by pointing two stems at the same biome.

### The gate that regenerated nothing

Found by accident: two generated biomes were hand-edited to watch a new check fail, and
re-running datagen did not put them back.

CI's rule for generated files is *regenerate everything, then `git diff --exit-code`*.
That catches a source that changed without its output being regenerated, and it does not
catch a generated file somebody edited by hand and committed — `HashCache` skips writing
a file whose newly generated hash matches the cached one, without looking at the file on
disk, and the cache was committed alongside the output.

Proven rather than reasoned about. A `_hand_edited` key added to a committed loot table
survived a green `runServerData` and the diff came back clean; with the cache deleted
first, the same experiment came back dirty. The cache is gitignored now and CI removes it
before regenerating. `LESSONS.md` #37.

### And the check killed itself silently

`worlds_check.sh` reads its plant counts out of the server's own `fill` reply. The first
version looked for "Changed N blocks"; the server says "Successfully filled N block(s)".
Under `set -o pipefail` the grep that matched nothing killed the script before any message
it exists to print, so the check went red with **no output at all** — the third time
LESSONS #23 has been paid for, and the first time it cost only a minute.

184 self-test checks, 61 mutations, 33 live checks.

---

## The first thing in the mod a player can touch

`WORLD.md`, locked: *"a keel block captures the structure, validates it against the
destination's law, and re-places it at the far pad. **The validation checklist teaches
each world's rule before arrival** — the Quiet One's crossing: no note blocks, no jukebox,
muffle your animals."*

The middle clause shipped a long time ago and was reachable only from a command. Which
means the beat that sentence is about — a player learning what a god is like by being
refused by its paperwork — had never once happened in play. Touching the keel runs the
capture and hands back the docket now.

All four crossings, every time. A player told about one destination learns one rule; a
player handed the whole page learns that the four gods refuse *different* things, which is
the reconnaissance band 3 exists to begin and the reason the letters are worth reading. It
costs four map lookups over a census, so there was no case for the narrower version.

It does not sail. Nothing in `WORLD.md` says how a player names the destination and the
options are not interchangeable — a keel that cycles four worlds is a menu, while a ferry
that goes where the letter in your hand is addressed is *"the route to them is its
unanswered correspondence"* made mechanical. Recorded under "Waiting on owner"; this ships
the half that is locked.

### The seam, again

`FerryDocket.of` builds the page; the block and `interregnum ferry inspect` both call it
and do nothing else. A right-click cannot be driven from a headless server, so a docket
only the block could produce would be a docket no check could read — the same arrangement
`interregnum learn` has with the dialogue node that teaches a school.

`inspection_check.sh` asserts what the page is FOR, not what it prints: all four crossings
named, **the four disagreeing about one hull**, the block and the count on every refusal,
the god's reason line beneath it, every violation rather than the first, one line for a
bare keel, and the same page twice for an unchanged hull. Watched failing twice —
validating every destination against one law (4 cleared, 0 refused: a page with one law
behind it teaches nothing), and truncating the violation list.

Two of its own assertions were wrong before they were right, both about reading rather
than about the mod: the count regex used a single `.` for the `×` between the number and
the block id, which is multi-byte and does not match in byte mode; and the two-inspections
comparison included the `say` markers that bound each range, so it compared marker names
and called a stable page unstable.

**26.2 removed `LivingEntity#displayClientMessage`.** The replacement is
`ServerPlayer#sendSystemMessage`, on a different type — which matters because a block's
`useWithoutItem` hands you a plain `Player`. Row added to PLATFORM.md.

184 self-test checks, 61 mutations, 34 live checks.

---

## The far pad

`WORLD.md`: *"a keel block captures the structure, validates it against the destination's
law, and re-places it at **the far pad**."* There was no pad. The arrival position was a
command argument an operator typed, so the ferry did not go anywhere in particular — it
went wherever you said, and a mail service whose destination is a parameter is not one.

The same dock, four times, in whatever was to hand: an identical seven-by-seven apron, an
identical three-by-three landing, four corner posts, and only the material different. The
Post does not redesign its dock per god — that is the joke the rest of the bureaucracy
runs on, said in blocks — and the standard dock is the only navigational aid any of these
worlds has. The Hearth-Turner's arrives already cracked, because in that world a new
object would be the one thing without a past.

Not claimed: the ledger records what a *player* placed and nobody placed this, so the
Verdant grows over its dock and the Turning ages it. `FerryPad.ensure` rebuilds a dock
whose landing has gone, which reads as the last of the Post still doing its job.

The berth is not a queue. A second crossing to a world whose dock already carries a ferry
is refused; without it the second hull lands on the first and silently replaces whatever
shared a coordinate. One dock per world is the design, a queue would be a mechanic, and
inventing one is not mine to do.

### A position derived from a world it had already changed

The first version asked the surface heightmap where the ground was. That works exactly
once: building the dock RAISES the surface, so the second crossing measured a different
height, found no landing there, and built a second dock a block above the first. Three
crossings, three docks, and every one of them "working".

`ensure` now scans the column for its own landing material before computing anything,
which is immune to that by construction — and also survives a player building on the
apron or the Verdant growing over it.

The check found it on its first real run, which is the only reason it is a paragraph here
rather than a bug in a released mod. `pad_check.sh` counts landing blocks with `fill ...
replace`: exactly 13 per world sailed to, and **zero in a world no ferry has visited**,
which is what stops a pad built eagerly everywhere from satisfying the first two counts.
Watched failing on the heightmap version and again with the berth guard removed.

Two of its own assertions needed fixing first, both about reading rather than the mod: the
`fill` volume asked for 58089 blocks against a 32768 ceiling and was refused outright,
which read as "no dock" rather than "no answer".

184 self-test checks, 61 mutations, 35 live checks.

---

## The way home

`WORLD.md`: *"Travel between systems is only by ferry."* That was a one-way sentence —
four crossing laws, four destinations, and no law whose destination was the overworld. A
player who sailed to a god's world could hop between gods forever and never get back.

The return is not a fifth law. `Law` refuses a law with no rules and says why: *"a crossing
that refuses nothing is not a law"*. So a home law would need something to refuse, and the
overworld has nobody left to refuse it — every other checklist is a god's policy about its
own world, and inventing one for the world whose god this player killed would be inventing
an authority the fiction has spent the whole game removing. `WORLD.md` also says what a
checklist is for: *"teaches each world's rule before arrival"*, and the overworld's rule is
the one the player already lives under.

So `interregnum ferry home <keel>` is a mail service returning a vessel to the depot it
left. `Voyages` files the leg on departure, keyed by the keel's arrival position, and
spends it on use. A keel that never sailed is told *no return leg on file*.

### The check went green and then two mutations walked through it

The first version sailed one ferry out and home and asserted it landed where it started.
Keying the record by *world* instead of by keel passed that. So did never deleting a spent
leg. Both are properties of the relationship between instances, and one instance makes
them unobservable.

It now sails two ferries to one world from two origins before either returns, and asserts
the returning one lands on its own coordinates and not the other's; and for the deletion it
plants a fresh keel by hand on a landing a ferry has already left from — the actual hazard,
a stale record attached to a position teleporting whatever stands there to a stranger's
dock. Both mutations die against that, with the messages that name them. `LESSONS.md` #38.

184 self-test checks, 61 mutations, 36 live checks.

---

## The god shatters

`WORLD.md`, locked: *"The overflow detonates outward, scattering **splinters** at shrines
and the crater"*, and *"the shattered god-pieces are **clasts** (item). Anyone may attune
one; **clasts are finite** — the class is a server negotiation."*

The item had existed since the first registry pass and nothing produced one. `PlayerTags`
said so in its own javadoc: *"the Theoclast class does not exist yet — no clast can be
attuned, so no player can truthfully hold it."*

Finite is the mechanic, so the count is the mechanic. Everything else this mod produces
falls out of a rule applied to whatever is there; this does not. `Clasts.TOTAL = 7`
— **[NEEDS PLAYTEST]**, as `WORLD.md` marks it: small enough that a server of any size has
to decide who gets one, odd so two factions cannot split it, more than the four gods so a
full set is not the obvious goal. The pool is per world rather than per shrine, because a
world with forty shrines would otherwise hand out forty classes and the negotiation would
never happen.

Three in the crater at the moment of death, one at each shrine as it is *found* — the
deicide only reaches loaded chunks, which is the statues' constraint and the statues'
solution, and the better beat for the same reason. They do not despawn: seven in a world,
ever, and a finite thing that can be lost to a timer is not finite, it is random.

A shrine is marked whether or not it paid. Waking a statue is self-marking because a woken
statue is a different block; scattering is not, so a chunk that loaded twice would pay
twice and a player walking back and forth could drain the world at one shrine.

### Two things the check got wrong before the mod did

The despawn assertion was written against `Lifespan`. `ItemEntity.setUnlimitedLifetime`
writes the `-32768` sentinel into **`Age`** and leaves `Lifespan` at 6000, because the tick
loop stops counting rather than raising the cap — so a check reading `Lifespan` reports a
despawning item that is not despawning. Read out of the decompiled source after the probe
disagreed with the code.

And the "found twice" leg used a forceload remove/add cycle. Removing a ticket does not
unload a chunk promptly, so the reload fires a Load event only sometimes: it passed once
and failed on a clean tree. Three server runs with `KEEP_WORLD=1` now — a shrine being
*found* is the thing under test, and a restart is the only way to guarantee the finding.

Both mutations watched failing: dropping the per-chunk mark takes the count from 5 to 7 on
a second visit, and removing `setUnlimitedLifetime` puts a clast on a 40-tick-and-counting
timer.

193 self-test checks, 63 mutations, 37 live checks.

---

## More of the world to lose

Nineteen conversions where there were nine.

Bands 1 and 2 are the only part of this mod a player can reach today — everything past the
deicide is behind one of the five questions in "Waiting on owner" — so the unraveling is
where content is worth adding, and it was thin.

Band 1 finishes its own chain. Cornflowers and daisies wilt like the poppies and dandelions
already did, and a dead bush eventually crumbles to nothing. That last rule is the point:
until now a thin place converged on a field of dead bushes and stopped, which reads as a
state rather than a process. It goes to bare ground now, so a player who keeps coming back
can see how far along it is.

Band 2 loosens more rules. Birch and spruce canopies thin as oak did; andesite, diorite and
granite crack to cobblestone and join the cobble-to-gravel chain; sandstone slumps to sand,
which then falls — the first conversion in the table whose consequence is not the block it
names. And dirt paths forget they were walked on, which is the one addition that takes away
a human mark rather than a natural one, and is what band 2's locked "rule loosening" is
really about.

Every new id was confirmed by a live load — "Unraveling: 2 band(s), 19 conversion(s) in
force" — rather than by reading the registry, and the oscillation guard was watched failing
on a deliberate `sand -> sandstone`.

### A stale number in a check, removed rather than updated

`unravel_check.sh` asserted `"2 band(s), 9 conversion(s) in force"` with the nine typed into
the shell script. Adding a conversion failed a correct build, which is the way round that
gets a guard deleted rather than fixed. It counts the rules out of `bands.json` now — which
is what the assertion always meant: everything in the file loaded, not "nine things loaded".

193 self-test checks, 63 mutations, 37 live checks.

---

## The ghost could not reach anybody

`WORLD.md`, locked, on the Haunt: *"dream-audiences: **sleep** sometimes routes the killer
to a small dimension where the ghost…"*

The scene, the gate and the once-only rule had all existed for a long time. The only thing
that could reach them was `/interregnum haunt dream`. The mod's best beat could not happen
in play, and nothing said so — `haunt_check.sh` was green throughout, because it drives the
command seam, which is exactly what it is for. **A check that covers a path does not tell
you anything about whether anything else reaches it.**

The same shape as the questions in "Waiting on owner" — a system built, verified and
unreachable — with one difference that makes it mine rather than the owner's: the
affordance is locked. `WORLD.md` names sleep. There was nothing to decide.

### The two locked beats that would have cancelled each other

The death stops the daylight cycle. That is locked and it is the entire announcement of the
death. So if the god dies in the afternoon, night never comes — and "the dream arrives when
you successfully sleep" would have made the Haunt unreachable in precisely the worlds it is
about.

`HauntSleepEvents` never looks at `CanPlayerSleepEvent.getVanillaProblem`. You lie down in a
world where the sun has not moved since you did it, and the thing you cannot stop thinking
about takes you anyway. The refusal is `OTHER_PROBLEM`, the one that carries no message,
because the world does not narrate what it is doing to this player.

Every other outcome leaves the event exactly as vanilla decided it. `TheHaunt.offer`
already refuses a non-killer, a living god, a second dream and a player mid-conversation, so
the handler adds no rule of its own.

### What is and is not verified

Not the trigger. A headless server has no player to right-click a bed, so that branch is not
exercised here and is not claimed — it is registered as a fifth `VERIFY:` marker with what
would clear it. Everything it calls is verified: `haunt_check.sh` drives `TheHaunt.offer`
and asserts the ghost reaches its killer once and nobody else ever. The handler is three
lines of adapter over that, which is the arrangement `Deicide` documents and the reason it
is worth keeping exactly one implementation of a gate.

193 self-test checks, 63 mutations, 37 live checks.

---

## A letter that can be opened

`sealed_letter` had been `registerSimpleItem` since the first registry pass: no behaviour
at all. The letters themselves were fine — written, loaded, validated, readable through
`interregnum letter read` — but the thing a player would be holding did nothing when they
used it, so the mid-game's best reveal was reachable only by an operator. Unlike casting or
attuning, the affordance was never an open question: you read a letter by opening it.

One renderer, two callers. `LetterPage.of` builds the page and both the item and the
command go through it. The command used to render it inline, which was fine while nothing
else could open a letter — and the moment the item could, it would have been two renderers
to keep in step with only one of them reachable by a check. `mail_check.sh` now covers the
item's page as a side effect of covering the command's, and was re-run green.

The page adds no salutation. Every letter's first body line already IS its salutation —
"Ballast —", "Rill —", and for the fourth, "To —" — so a rendered `To <addressee>` would
print the name twice above the line that is the point of the whole set. The `addressee`
field is the machine-readable half of the same fact and earns its place in `Post`, which
enforces that exactly one letter in the set is unaddressed: an invariant about a SET, which
cannot be read off any single letter's text.

### I overwrote a file I thought I was creating

The component the item needed already existed, in a `ModComponents.java` I wrote from
scratch and thereby replaced.

Every usual defence missed it. The build stayed green — same component, same registration.
The checks stayed green — they read letters through the command, which had not changed.
And `git status` said `M` rather than `??`, which I did not look at, because I believed I
had just created the file. What surfaced it was an unrelated symptom: I had also added a
second `ModComponents.register(modBus)` beside the one already there, and the mod refused
to load with "Cannot register DeferredRegister to more than one event bus". Chasing that
duplicate led back to the file.

What was lost was a rule with no check behind it — *the component must never carry the
addressee, because a stack in a hotbar is a string a player can see, and the names are
meant to be unheard until the letter is opened* — written in the one place a future author
would be standing when they broke it. Restored from git. `LESSONS.md` #39.

193 self-test checks, 63 mutations, 37 live checks.

---

## The steles say something

The block, the texture and the model have existed since the chapter-0 art pass. There was
no text on any of them anywhere — and the shrine-keeper has been telling players for just
as long that *"the steles are readable if you have the light for it; most people don't
bother, and I have never held it against anybody."* A shipped line of dialogue describing a
rule nothing implements is worse than a missing feature: it is the mod lying in its own
voice.

Five notices, each the Wardenate explaining a rule that is about to stop being true: the
four locked vanilla-rules-as-policy entries, and one saying what to do if any of them ever
fails. That fifth is what `WORLD.md` means by *"after the death, the only instruction anyone
left behind"* — written by somebody who did not believe they were writing it.

Not one word changes at the deicide. `WORLD.md`'s locked comedy list names *"steles that
re-read differently"*, and text that swapped at the death would throw the joke away: what
re-reads differently is the reader.

Which notice stands where is a pure function of the coordinates — band 3's idiom — so a
stele reads the same tomorrow, two steles in a ruin can differ, and none of it costs a
block state. `floorMod`, not `%`, because a negative hash under `%` indexes backwards off
the end of the list and works perfectly in every world anybody tests near spawn.

The light rule exists because the keeper says it does. Seven: outdoors in daylight is
always enough, a buried stele is not. It gets sharper after the death, with nobody left to
turn the sun.

### The bug no amount of reading would have found

The first version asked for the light AT the stele. A stele is an opaque block, and the
inside of an opaque block is dark in every world there has ever been — so a stele in open
daylight reported itself unreadable and one buried in stone reported itself fine. Wrong
everywhere, and wrong in a way that reads as a rule rather than a bug. Found by probing a
live server before writing a single assertion; it takes the brightest of the six
neighbours now.

And one flake found and removed: the buried stele read out perfectly on one run, because
the check asked before the engine had finished propagating light into the new stone. It
waits now — three seconds for a five-block cube is a bounded computation with enormous
margin, not a threshold on a random variable.

Watched failing on both mutations that matter: the light read at the stele (0 of 6 lit
steles readable) and the position hash collapsed to a constant (six steles, one notice).

200 self-test checks, 66 mutations, 38 live checks.

---

## The audit, made permanent

Three consecutive passes each found a system built, verified, green in CI, and unreachable
in play: the Haunt's dream, the sealed letter, the warning steles. Finding the fourth by
hand was not a plan.

Every check was green throughout, and rightly so. `haunt_check.sh` drove the command seam;
`mail_check.sh` read the letters out of the data. Both were correct and complete about what
they cover. **A check that covers a path says nothing about whether anything else reaches
it** — and no test this project knows how to write would have told the difference, because
"is there a right-click handler that calls this" is not a property of the thing being
called.

So the route is written down and the writing is checked. `docs/REACHABILITY.md` lists every
registered block, item and entity with how a player touches it and a status from a fixed
set — `PLAY`, `OP`, `SCENERY`, `BLOCKED: <question>`. `reachability_check.py` enforces that
every registered id appears, that nothing appears which is not registered (the direction a
table goes stale: content deleted, row left behind promising something gone), and that
every status is from the set.

It does not check whether a status is TRUE, and says so in its own docstring. Nothing
static can — `PLAY` is a claim about a handler somewhere, and a wrong one is precisely the
bug this is about. What it enforces is that somebody had to write the claim down, which is
the whole of what was missing all three times.

Watched failing three ways: an item registered and undocumented, a row for something that
does not exist, and a free-text status.

### One finding recorded rather than fixed

`shrine_stone_carved`'s javadoc says it carries *"a band of the dead god's script"* and
nothing can read it. Unlike the steles, making it readable would be the wrong move:
`WORLD.md` marks the whole reading lane **[PROPOSED]** — raw god-script read without
transcription *marks* the reader, and the codex desk is the safe path. Shipping plain
readable inscriptions would settle that in the safe direction by default. Sixth question in
"Waiting on owner".

200 self-test checks, 66 mutations, 38 live checks, 20 fast-gate stages.
