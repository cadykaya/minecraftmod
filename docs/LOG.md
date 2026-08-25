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
