# Lessons

> **Provenance: OURS.** This file holds only things **this project** learned the hard way.
> Inherited lessons live in the doctrine docs and are marked PORTED there.
>
> **This is a living document. Add to it the moment something is learned, not at the end of
> a pass.** The value is in being read *before* a build, and a lesson written up three
> sessions later has already cost its price a second time.
>
> **Nothing goes in here that did not actually happen.** A fabricated war story is worse
> than no war story: it spends the credibility that makes the real ones worth reading.

---

## 1. The single tile is the front view

*Learned while writing the worked example in `TEXTURING.md`, by the session that had just
finished writing `ARTSTYLE.md` §4b — which is the section describing this exact failure.*

The first structure-painted stone used **7 Voronoi cells and a full-brightness highlight**.
As a single 16×16 tile it looked correct: cells, mortar, directional light, four legal
values, clean palette check.

Rendered 6×6 tiled — which is how a wall is actually seen — it produced **a regular grid of
bright dots.** One highlight cluster per tile, in roughly the same place every time, and the
eye locks onto it instantly across a wall.

**What fixed it:**

| | before | after |
|---|---|---|
| cells | 7 | 11 |
| highlight | to maximum step | **exactly one step** off the cell's own value |
| lit cells | 62% | 45% |
| breaker | none | hash *inside* the rim zone |

**The transferable part:** DOWNTIME's rule is *judge in rotation, always* — a model shipped
with a bare strip of skull because every review was a front view, and the front is the view
everything gets tuned in. **For block textures the single tile is that view.** It is the one
you author in, the one your editor shows, and the one that lies.

`tools/texview.py` therefore **defaults to 8×8 tiled**, not single. A bench whose easiest
invocation shows the misleading view is a bench that will mislead you.

*Evidence committed: `docs/img/tiled_hash.png`, `docs/img/tiled_structure.png`.*

---

## 2. A confounded test is a false instrument

*Learned minutes later, verifying the review-render skip in `palette_check.py`.*

The test was: does a file named `mossy_grey.png` get skipped as a review artifact? It ran,
printed `exit=1`, and was briefly read as **"the skip does not work."**

It did work. A *different* file in the same directory — `grey_structure.png`, misnamed
against the convention because a `--out` flag had overridden it — was failing, and the exit
code belonged to that. The run said **nothing whatsoever** about the case under test.

Re-run in an isolated directory, with byte-identical content under two different names, it
proved what it claimed: skipped as `mossy_grey.png`, caught as `mossy_cobble.png`.

**Two things to carry:**

- **A test with more than one possible cause of failure has not tested anything.** Isolate,
  every time. `exit=1` is not evidence of *which* thing failed.
- This is DOWNTIME's *"a new bench is the least trustworthy thing in the repository"* in
  miniature, committed by someone who had just written that section into
  [`VERIFICATION.md`](VERIFICATION.md). **Knowing the rule does not exempt you from it.**

*Secondary lesson: the naming convention was load-bearing and got broken by a convenience
flag. `texview` names outputs `<base>_view.png` / `<base>_grey.png` for a reason — the
checker's skip list keys off it. Do not override with `--out` when writing into a checked
tree.*

---

## 3. Verify the platform before writing about it

*Learned at the very start, and it would have poisoned the entire doc set.*

The obvious plan was to target "Minecraft 1.21.x, the current modding standard." That is
what a model trained before 2026 confidently believes, and it is **wrong**.

**Minecraft changed its versioning scheme in 2026.** `1.21.x` was followed by **`26.1`**,
then `26.2`, on a quarterly game-drop cadence. There is no `1.22`. Current stable at the
time of writing is **26.2 "Chaos Cubed"** (June 2026).

Had the docs been written from memory, every version string, every gradle property and the
whole migration section would have been confidently, uniformly wrong — and wrong in a way
that *looks* right to anyone with pre-2026 knowledge, which is the worst kind.

**The rule, and it is [`PLATFORM.md`](PLATFORM.md)'s standing policy:** never trust a
remembered API fact or version number. Not from a tutorial, not from a model, not from this
doc set. Modding churns harder than almost anything else in software.

*Corollary discovered the same way: this sandbox's egress proxy blocks `neoforged.net` and
`docs.neoforged.net` outright, so the primary source is unreachable from here. That is why
every API specific in these docs carries a `VERIFY:` marker instead of a confident
signature — and why the NeoForge sources in the Gradle cache are named as the real
authority.*

---

## 4. Measure the process you mean, not the pipeline's tail

*Learned while verifying the dialogue engine's self-test by mutation.*

Three deliberate bugs were introduced; the test printed `FAIL:` for each — and the
harness reported `exit=0` every time. The mutations were fine and the checks were fine:
the shell line was `java ... | grep -v noise; echo $?`, and **`$?` was grep's exit code,
not the JVM's.** Piping a program through a filter replaces its exit status with the
filter's.

This is DOWNTIME's *"a filter that cannot express failure will only ever report success"*
in shell form, committed within the hour of porting that exact sentence into
`VERIFICATION.md`. Knowing the rule does not exempt you from it — that is now two for two.

**Fixes:** redirect instead of piping when the exit code matters (`java ... >out.txt 2>&1;
echo $?`), or use `PIPESTATUS`. `tools/check_all.sh` runs every stage bare under `set -e`
for this reason.

---

## 5. A test that passes either way is not a test

*Learned the moment `tools/mutate_check.py` was first run against a self-test that had
already "passed" 44 checks and been hand-verified against three mutations.*

Two of eleven mutations **survived** — the engine was broken and the suite stayed green:

| Mutation | Why the test could not see it |
|---|---|
| VOTE's tie-break rule deleted | The test submitted the **initiator first**, so tally insertion order already put their pick at `top.get(0)`. Deleting the rule changed nothing. Fixed by having the other player submit first. |
| Chapter's monotonic guard deleted | Every case only ever **added** milestones, so the derived chapter never fell below the high-water mark and the guard was never load-bearing. Fixed by deserializing a save whose milestones no longer justify its chapter. |

Both assertions were *correct*. Both were *blind*. This is DOWNTIME's **"a metric must be
able to see the thing you are asking about"** — if a change you can plainly make does not
move the number, the number is the wrong number.

**Hand-checking a few mutations is not the same as checking them all**, which is why this
is now a tool (`tools/mutate_check.py`, wired into `check_all.sh`) instead of something
done from memory in a shell loop. Every new guard gets a mutation added beside it; a
surviving mutation fails the build and names itself.

*Footnote, and it is the third instance of the same pattern: the first version of that
shell loop was itself broken — bash heredoc escaping mangled the `javac` line — which is
precisely why the harness became a real program with a real exit code (see #4).*

---

## 6. Minecraft 26.2 needs Java 25, and every source says 21

*Learned the moment the first real Gradle build ran.*

`PLATFORM.md` said **Java 21**, sourced from a web search that stated NeoForge "officially
supports JDK 21." That was true for the entire 1.21 line, and the search result was not
lying — it was **a version behind**, which is the more dangerous failure because it reads
as authoritative.

The real toolchain settled it in one error:

```
Cannot find a Java installation ... matching: {languageVersion=25, ...}
```

MDG requests Java 25 for Minecraft 26.2 and will not build without it. Fixed by applying
the **foojay resolver** in `settings.gradle` so Gradle downloads the JDK itself.

This is the second time (see #3) that a confidently-remembered platform fact about this
project was one version stale. Both times the answer was in a registry that takes ten
seconds to query. **The `VERIFY:` markers exist for exactly this, and the moment a real
toolchain exists they should be cleared against it rather than left to age.**

*Other facts corrected in the same pass, all by asking the source instead of remembering:
ModDevGradle is 2.0.144 (search said 2.0.141), Parchment has no 26.2 build at all (so the
mapping line was removed rather than pinned to something that does not exist), and the
resource/data pack formats are 88 and 107 (a guessed 90 was rejected by the game).*

---

## 7. Killing the server leaves a lock, and the next run's silence looks like success

*Learned immediately after fixing `pack.mcmeta`, while trying to prove the fix worked.*

The first server boot was stopped with `pkill`, which killed the JVM but left
`run/world/session.lock` behind. The next boot died on that lock **before loading a single
mod** — and the check that was supposed to confirm the fix reported:

```
=== pack metadata warning still present? ===
0
(0 = fixed)
```

Zero, because the server never got far enough to read pack metadata. The check could not
see the thing it was asking about. **That is LESSONS #5 for the third time in one session**,
which is why the fix was to stop writing this in ad-hoc shell and build
`tools/server_smoke.sh`: it clears the lock and **fails loudly if the server never finished
loading** instead of silently finding nothing.

> **Correction, several commits later.** This entry originally said the script "shuts the
> server down cleanly through stdin (`stop`)". **It did not.** Stdin never reached the
> server under Gradle's `runServer`; the process died on `SIGTERM` and the JVM shutdown
> hook saved chunks, which made the log look exactly like a clean stop. The claim went
> unverified into this file — in an entry about unverified claims. Corrected in #10.

### The corollary: a filter widened until it is green cannot fail

The smoke test's first log filter was a line-based grep with an ignore list. It ignored the
*header* of the Mojang-auth exception and then flagged that same exception's `Caused by:`
lines, which carry no package name and so look unattributable. The tempting fix — add
`Caused by` to the ignore list — would have blinded the check to every real stack trace the
mod will ever throw.

`tools/server_log_check.py` instead groups the log into **records** (a timestamped line and
everything beneath it) and attributes each record as a unit, with a written reason per
ignore entry. Verified both directions: the real log passes, and an injected
`[interregnum/ERROR]` record fails it.

*Grouping by stack shape was tried first and was also wrong — a gson "See https://..."
advice line in the middle of a stack split one exception into two.*

---

## 8. A smoke test that inherits state from your machine is not testing the repository

*Learned when the first CI run of the `game` job failed on a check that passed locally
every time.*

`tools/server_smoke.sh` wrote `run/eula.txt` but not `run/server.properties`. `run/` is
gitignored, so on the dev machine the properties file existed -- created by hand, once,
during the first manual experiment -- and on a fresh CI checkout it did not. The server
booted either way, but on a clean checkout it logged:

```
[main/ERROR] [minecraft/Settings]: Failed to load properties from file: server.properties
```

which the log checker correctly refused to attribute to anything, and the job went red.

**The tempting fix was to add the message to the ignore list**, and it would have worked
and been wrong: the error is real, it just is not *interesting*, and the honest response is
to stop causing it. The script now writes its entire run environment -- eula, properties,
a flat world with a fixed seed and a small view distance -- so a run is deterministic and
identical everywhere.

> **The general rule: a check must create everything it depends on.** Anything it merely
> *finds* on the machine where it was written is a difference between your box and every
> other one, and it will surface as a CI failure that "cannot be reproduced."

Reproduced locally first, exactly as `docs/VERIFICATION.md` requires for a CI fix: `rm -rf
run/` reproduced the identical failure, the fix was applied, and the same clean state then
passed.

---

## 9. The gate itself printed ALL CHECKS PASSED while broken

*Learned while making the staleness check's error message accurate.*

`tools/check_all.sh` began `#!/bin/sh` with `set -e`. A `comm -13 <(...) <(...)` added to
it is a **bashism**; under dash it produced

```
./tools/check_all.sh: 42: Syntax error: "(" unexpected
```

and then the script **kept going and printed `ALL CHECKS PASSED`.** The single gate that
every commit and every heartbeat tick depends on had silently stopped running one of its
stages while reporting success.

Fixed by switching to `#!/bin/bash` with **`set -euo pipefail`**, and verified by planting
a bogus command in the middle of the script: it now exits 127 at that line instead of
sailing past it.

> **A gate that can silently stop checking is worse than no gate**, because it converts
> "nobody is checking" into "checking says it is fine" — the same conversion as the false
> commit message in #4 and the confounded test in #2.

### And the reason the edit was needed at all

`git diff --exit-code` conflates two different situations: *committed generated output is
stale* and *you have uncommitted work in those paths*. It was reporting the second as the
first, which is a check crying wolf — and a check people learn to ignore has already
failed. The staleness stage now snapshots what was dirty **before** regenerating and blames
only files that regeneration itself changed. Verified all four ways: clean tree, uncommitted
work (passes, says so), genuinely stale committed art (fails, names the file), and an
internal error (fails loudly).

### Footnote: do not verify a script by resetting the repo it lives in

The first attempt at this fix silently vanished, because the test for the stale-art case
used `git reset --hard HEAD~1` to undo a simulated commit — which reverted
`tools/check_all.sh` along with it. The next run tested the *old* file and its results were
meaningless. Destructive verification now happens in a scratch clone.

---

## 10. The shutdown that was never a shutdown

*Found while trying to run a command in the live server, and it invalidated a claim already
written into #7.*

`server_smoke.sh` held a fifo on the server's stdin and wrote `stop` to it. The server did
stop. Every log ended with chunks saved and all dimensions written. It looked clean, it was
documented as clean, and **none of it was true**: stdin does not reach the game under
Gradle's `runServer`. The process was dying on the `SIGTERM` that followed, and Minecraft's
shutdown hook saves on the way out — so the evidence of a clean stop is *also* the evidence
of a killed process.

The tell, once looked for, was one grep: **`Stopping server` appeared in zero smoke logs.**
That line is the `/stop` path. Its absence had been sitting in every log the whole time.

```
smoke_final.log: 0 'Stopping server' | 3 saves
smoke_dlg.log:   0 'Stopping server' | 3 saves
smoke_loot.log:  0 'Stopping server' | 3 saves
```

**Fixed with RCON** (`tools/rcon.py`), which is a real channel with real replies rather than
a hope: commands now come back with the server's own answer, and `Stopping server` appears
for the first time. The same channel is what makes worldgen verifiable at all without a
client — `setblock 8 66 8 interregnum:warning_stele[axis=y]` answering *"Changed the block
at 8, 66, 8"* is proof the block is registered, has a valid blockstate, and can exist in a
world.

**Two things to carry:**

- **A side effect that resembles success is not success.** Saving chunks is what a clean
  stop does *and* what a killed process does. When two very different causes produce the
  same evidence, the evidence decides nothing — find a signal only the intended cause
  produces (here, one log line).
- **Prefer a channel that answers.** Writing into a pipe and assuming it arrived is the same
  shape of mistake as a filter that cannot express failure (#4, #9). RCON returns the
  server's reply, so "the command ran" stops being an assumption.

*Footnote: the first RCON wiring split commands in the shell with `IFS=$'\n'` under
`#!/bin/sh`, where that is not a newline but the literal characters — including `n`, so
`minecraft` split into `mi` and `ecraft`. Same class as #9, second occurrence. Commands now
go to `rcon.py` through a file and no shell touches them; the script is `#!/bin/bash` with
`set -euo pipefail`.*

---

## 11. A feature that cannot be `/place`d cannot be verified

*Found by the worldgen check on its first real run.*

`ShrineFeature` located the ground with `level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, ...)`,
which is what vanilla features do and which works perfectly during world generation. Invoked
on an already-generated chunk with `/place feature`, it logged:

```
[Server thread/ERROR] [minecraft/ChunkAccess]: Unprimed heightmap: WORLD_SURFACE_WG 8 8
```

The `_WG` heightmaps exist only during the generation phase. So the feature worked in the
one situation that is hardest to observe and errored in the only one that can be checked
without a client.

**The fix was not to special-case the test.** It was to stop depending on generation-phase
state at all: the feature now scans downward for the topmost solid block, which is correct
during generation *and* on a live chunk. `/place` becomes a first-class way to use the
feature rather than a testing hack, and a future "re-seed the shrines" tool gets the same
guarantee for free.

> **Generalisation: if a thing can only run inside the one context you cannot inspect, that
> is a design constraint, not a testing inconvenience.** Removing the constraint made the
> code simpler and the behaviour identical.

### The check also caught the author

Rewriting `findSurface` changed what `floorY` meant -- it now returns the topmost *solid*
block rather than the first air above it -- and the assertion coordinates were "fixed" by
+1 on a guess. Two of four markers vanished, and the two that still passed only passed by
luck: a stele is 1 or 2 blocks tall, so both candidate heights matched. The coordinates are
now written down with the reasoning beside them, and the check was verified twice over --
once with a deliberately wrong assertion, once with a deliberately broken feature -- because
a check that passes when the world is empty is the failure this project has now hit four
times (#5, #7, #10, and nearly here).

---

## 12. A quoted heredoc expands nothing, and the probe reported 100%

*Found within a minute of writing the shrine rate probe -- which is the only reason
it did not become a number in a design document.*

The shrine's real density depends on two things multiplied: a rarity filter (known by
construction) and how much natural terrain `ShrineFeature` refuses as too uneven (never
measured). The probe was built to measure the second, which meant `server_smoke.sh` had to
be able to boot a **real** world instead of its usual flat one.

`${LEVEL_TYPE}` was added inside the `server.properties` heredoc. The heredoc is
`<<'PROPS'` — **quoted, so nothing in it is ever expanded** — and the literal text went
into the file. The server kept booting flat, and the probe answered:

```
acceptance: 100%
=> one shrine per 90 chunks
=> roughly 4 minutes of walking to the first one
```

Every one of those numbers is true of a flat world and meaningless anywhere else. The
output was clean, plausible, and precisely the kind of thing that ends up quoted in a
design doc six weeks later.

**What caught it** was not the number looking wrong — 100% looks *good*. It was checking
the instrument: `grep level-type run/server.properties` after the edit, before trusting
anything it produced. On real terrain the answer is **46%**, which changes the design
conclusion completely (and incidentally validates `MAX_RELIEF=2` as selective rather than
crippling).

> **This is DOWNTIME's "a new bench is the least trustworthy thing in the repository",
> arriving on schedule.** Every measurement tool in this session has been wrong on its
> first run. The habit that works is boring: after wiring a bench, make it report the thing
> it depends on, and read that before reading its results.

*Two related fixes went in with it. The probe now reads the rarity value out of the
generated JSON instead of keeping its own copy — a probe holding a second copy of the number
it is measuring against will report the old answer forever after that number changes. And
`MAX_RELIEF` and the rarity filter multiply, so the code says in writing that changing
either one silently moves the density and the probe must be re-run.*

---

## 13. The dirty flag that was already set

*Found by mutating `ChapterSavedData` after the persistence check had already passed.*

`ChapterSavedData.record()` calls `setDirty()`, with a comment saying that forgetting it is
the classic way saved data silently fails to persist. To check that the claim -- and the
check -- were real, `setDirty()` was deleted.

**The persistence check still passed.**

The reason is in `SavedDataStorage`:

```java
public <T extends SavedData> void set(SavedDataType<T> type, T data) {
    this.cache.put(type, Optional.of(data));
    data.setDirty();                    // <- freshly CREATED data is already dirty
}
...
this.cache.forEach((type, o) -> o.filter(SavedData::isDirty).ifPresent(...));  // saving does consult it
```

So data **created** in a session is dirty for that whole session and saves whatever the mod
does. Data **loaded from disk** is clean. The original check only ever did
create-then-mutate-then-restart, which is precisely the path where `setDirty()` does not
matter — so it could not see the bug it was written to catch.

Fixed by adding the path that matters: restart, mutate the now-LOADED data, restart again.
With that, deleting `setDirty()` fails with *"a change made to LOADED saved data was lost on
restart"*.

> **The general shape, and it is the fourth time this session:** a check that only exercises
> the easy path passes for the wrong reason. Mutation testing is the only thing that has
> reliably found these — it found blind unit tests in #5, and here it found a blind
> *integration* test. **Whatever the layer, break the thing on purpose before believing the
> check.**

*Footnote, and it is #4 arriving a third time: the shell line measuring this read
`./tools/persistence_check.sh | tail -4; echo "exit=$?"` and printed `exit=0` while the
script had plainly failed — `$?` was `tail`'s. The `FAIL:` line was the real evidence.
Knowing a rule really does not exempt you from it.*

---

## 14. setblock on an identical block is a no-op, and the probe inflated by 6x

*Found while measuring how often a shrine's offering box holds the heart.*

The probe rebuilt a chest at one position, rolled the loot table into it, and read the
contents back — sixty times. It reported **47 hearts in 60 rolls of a 12% pool**.

`setblock <pos> minecraft:chest` where a chest already stands is a **no-op**. So the same
chest survived every iteration and `loot insert` kept *adding* to it: once a heart landed
around roll 14, every later read still saw it. Clearing to air first gives the honest
answer, **8 in 60 — 13%**, which is the 12% it was configured for.

**The check still passed both times.** Its assertions are "at least one before" and "exactly
zero after", and both held. The *number* was nonsense, and the number was the interesting
part — it was one sentence away from being written into a design document as a measured
drop rate.

> **A passing test can still be reporting a lie.** Assertions and measurements are different
> things: an assertion that survives a broken instrument tells you nothing about the numbers
> that instrument printed. If a probe reports a figure anybody might act on, sanity-check
> that figure against what it should be — 47/60 is not 12% and never was.

*This is the sixth measurement tool in this session to be wrong on its first run, and the
second where the tool passed anyway (see #13). The pattern is now unmistakable enough to
state plainly: **assume a new bench is wrong until it has agreed with something you already
knew.***

---

## 15. Assert the setup, or the test proves whatever absence implies

*Found while testing that distant Warden statues wake when their chunk loads.*

The test placed a statue far from spawn, killed the god, loaded that chunk, and checked the
statue was awake. It reported **no markers at all** — not even the "still asleep" one that
was supposed to fail first.

There was no statue. `setblock 100 -60 100 ...` had answered:

```
That position is not loaded
```

`setblock` does not throw; it declines and carries on. The reply was right there in the
output and nothing read it, so every later assertion was quietly measuring an empty
coordinate. **A test built on setup that silently failed will confidently report whatever
the absence of that setup implies** — here, "the statue is not awake", which is true, means
nothing, and looks exactly like a real failure.

Fixed by asserting the setup itself: the check now proves each statue exists and is asleep
*before* the god dies, and says "the far statue was never placed" if it does not.

> **The rule: a check needs a positive assertion at every step it depends on, not only at
> the step it is about.** Absence is not evidence unless you have proved presence first —
> which is the same shape as #5 (a test that passes either way), #13 (a test that only
> exercised the easy path), and #14 (a passing test reporting a lie). Four different
> disguises for one mistake: *believing an outcome without checking the conditions that
> would make it meaningful.*

---

## 16. A check on the shape of data cannot tell you the data does anything

*Found while writing the runtime that finally reads the unraveling table.*

`tools/unraveling_check.py` is the strictest checker in this repository. It has six checks,
every one of them verified by breaking something on purpose. It passed this rule:

```json
{ "id": "leaves_brown", "from": "minecraft:oak_leaves", "to": "minecraft:dead_bush", "chance": 0.03 }
```

Every field is well-formed. Both blocks exist. The chance is a real probability under the
cap. Neither block is on the forbidden list. It reverses nothing. And it can never fire:
a dead bush needs a supporting block under it, oak leaves are a canopy, and the replacement
would pop off on the tick after it was placed. The rule was committed, it was green, and it
was inert — the worst possible failure for a system like this, because a world where one
band does nothing looks exactly like a world where that band has not reached you yet.

The checker was not wrong. It was **checking a different question**: it validated the
sentence, not whether the sentence describes something the game can do. No amount of
strictness on the first question answers the second.

Two things came out of it. The rule is now `oak_leaves -> air` ("the canopy thins"), which
the game can actually perform. And the runtime refuses to place a state whose
`canSurvive` is false at the position, so a future rule with this bug reports `UNSUPPORTED`
by name instead of quietly doing nothing — with a datapack fixture in `unravel_check.sh`
that proves that answer comes back, because a guard nothing exercises is not a guard.

The same question, asked of the check suite rather than the data, found a second hole in
the same hour. Every assertion about the unraveling reached it through `/interregnum
unravel`, so a build where the tick handler was never subscribed would have passed all of
them — the system would have been perfectly correct and never once have run. `/interregnum
status` now reports `ticks=` and `passes=`, and deleting the `@SubscribeEvent` fails the
check by name.

> **The rule: for every check, ask what could be deleted while it still passed.** Whatever
> that is, is not being checked — and "the whole feature" is a real answer more often than
> it sounds. This is #3 (green tests are not a working feature) arriving from the data side
> rather than the code side, and it is why `VERIFICATION.md` rule 2 says to assert on the
> effect: the effect is the only thing that cannot be faked by a well-formed input.

---

## 17. A field set in a constructor is not a property the object has

*Found while giving the first Warden a body.*

`WardenEntity`'s constructor called `setPersistenceRequired()`, with a comment above
it explaining why Wardens must never despawn. It compiled, it read correctly, and it
did nothing. A probe printed:

```
$ data get entity @e[type=interregnum:warden,limit=1] PersistenceRequired
  Warden has the following entity data: 0b
```

`Mob#readAdditionalSaveData` assigns `this.persistenceRequired` straight from NBT,
defaulting to false — so every load overwrites whatever the constructor decided,
and `/summon` constructs and *then* loads. The guarantee moved to
`requiresCustomPersistence()`, which no tag can undo.

The general shape is worth more than the specific bug: **for any object the engine
serialises, the constructor runs before the state does.** Anything set there that
also has a saved form is a default, not a decision, and the two are
indistinguishable in the source. The tell is that the field appears in
`readAdditionalSaveData` — if the engine reads it back, the engine owns it.

But the reason this was caught at all is the part to keep. The check that found it
did not exist yet: the run was an **exploratory probe with no assertions**, printing
raw command replies so they could be read before anything was written to match them.
Had the assertions been written first, they would have been written from the design
intent — "it is persistent, so assert `1b`" — and the check would have failed, and
the obvious next move on a failing new check is to suspect the check. Instead the
wrong value arrived with nothing invested in expecting the right one.

> **The rule: look at the instrument's raw output before you write the assertion.**
> Writing the expected value first means the first disagreement is a fight between
> two things you wrote, and the code usually wins that fight. It is the same
> mistake as #14 in the other direction: there a probe reported a number nobody
> sanity-checked; here nobody had yet decided what the number should be, which is
> exactly why the real one was believed.

---

## 18. A mutation caught for the wrong reason leaves the check unverified

*Found while mutation-testing the conversation runtime.*

Five guards, five mutations, five `[CAUGHT]`. One of them was a lie:

```
[CAUGHT]  the initiator leaving does not end the table -- FAIL: 2 unattributed problem record(s):
```

The assertion that was *supposed* to catch it never ran. Deleting that guard made the
server throw — the core engine refuses to remove an initiator, so the command hit an
unhandled exception and the smoke test's log scanner failed the run on the stack trace.
The mutation was caught by a completely different instrument than the one being tested,
and the check I actually wanted to verify was still unverified while the report said
otherwise.

The fix was a second, quieter mutation: make the guard a no-op *without* throwing, so
the only thing that can notice is the assertion under test. It failed by name —
`z2 was left sitting at a table whose initiator had gone` — and only then was that
assertion known to work.

> **The rule: a mutation test verifies an assertion only if the FAILURE MESSAGE is
> the one that assertion produces.** Reading exit codes is not enough; read what
> failed. A mutation that crashes, times out, or trips an unrelated guard has proved
> that the mutation is bad, which was never in question — the thing being measured is
> whether *this check* can see it.

Corollary, and the reason this is worth a whole entry: the more guards a project
accumulates, the more likely any given mutation trips one of the *others* first. A
mature suite makes this failure mode more common, not less.

---

## 19. If the expected value comes from an unordered collection, the test is flaky by construction

*Found in the same hour, one layer down.*

`Resolution.stances()` returned `Map.copyOf(picks)`. The picks are a `LinkedHashMap`
specifically so the table can show who spoke first — that order is content, it is most
of what an argument reads as — and `Map.copyOf` throws it away.

The tell was two probe runs of the same code printing different orders:

```
stances={p2=comply, p3=refuse, kaya=comply}
stances={kaya=comply, p2=refuse, p3=refuse}
```

Java's immutable maps iterate in a **salted** hash order; the salt is randomised per
JVM run. So the first version of the test — one table asserted against a literal —
would have passed or failed depending on which JVM it ran in, and would have looked
like a real intermittent bug for as long as anyone tolerated it.

The test that works asserts a **relationship instead of a value**: two tables, same
participants, different submission orders, and the assertion is that their stance
orders *differ*. An unordered map gives both the same order — same keys, same salt —
so the mutation is caught deterministically, on every JVM, forever.

> **The rule: when a value's order or identity is not guaranteed, assert a
> relationship between two observations instead of one observation against a
> literal.** Two runs that must differ, or must agree, are checkable even when
> neither one alone is predictable.

### The same salt, from the other side — a *mutation* that was flaky

Weeks later the ferry's manifest arrived with the same guard: a `TreeMap` in the
canonical constructor, so a bill of lading a person reads does not reorder itself
between two identical crossings. The mutation written to prove that guard was the
obvious one:

```python
"        blocks = Collections.unmodifiableMap(new TreeMap<>(blocks));",
"        blocks = Map.copyOf(blocks);"
```

It passed locally, failed on CI, passed locally again. Eight runs of the same three-key
map showed why:

```
[minecraft:oak_planks, minecraft:jukebox, minecraft:note_block]
[minecraft:oak_planks, minecraft:note_block, minecraft:jukebox]
[minecraft:jukebox, minecraft:note_block, minecraft:oak_planks]   <-- sorted, by luck
[minecraft:note_block, minecraft:oak_planks, minecraft:jukebox]
```

The third one is in sorted order. On that JVM the mutated code produced exactly what
the correct code produces, the assertion passed, and `mutate_check` reported that a
deliberate bug had escaped — a *false alarm about a real guard*, which is the most
expensive kind of noise a verification tool can make. Roughly one run in six.

Two corrections, both needed. The mutation now reverses the order deterministically
(`new TreeMap<>(Comparator.reverseOrder())`), so it fails for the reason it names on
every JVM. And the assertion, which compared only the two entries that happened to be
violations, now compares the entire key list against its own sorted copy — comparing
two entries is a coin flip against any shuffle, and half of a shuffle still looks
sorted.

> **The rule, restated for the other side: a deliberate bug must be deliberately
> wrong.** Everything demanded of an assertion — deterministic, failing for its stated
> reason, not merely *likely* to differ — is demanded of the mutation that tests it.
> "Unordered" is not a synonym for "in a different order".

---

## 20. A relative API cannot restore an absolute value once something moved the baseline

*Found by the persistence half of the regard check, on its first run.*

`RegardState.adjust(institution, delta)` is relative — it adds. Saving a record meant
storing the absolute values and the ceilings; restoring it meant applying the ceilings
first, then the values. The ordering was deliberate and I wrote a comment defending
it: `adjust` clamps to the ceiling, so restoring values before their caps would
truncate every capped institution.

That reasoning was right, and it is what caused the bug. `lowerCeiling` **also moves
the value** — anything above the new cap is pulled down to it immediately. So by the
time the values were restored, every capped institution was already sitting at its
cap, and `adjust(VERDANT, -45)` added -45 to -10 rather than setting -45.

```
saved:   VERDANT = -45, ceiling -10
reload:  ceiling applied -> value -10;  adjust(-45) -> -55
reload:  ceiling applied -> value -10;  adjust(-55) -> -65
```

Nothing threw. Nothing logged. Every number stayed inside its legal range and looked
entirely plausible at every step — a god that resented you a little more each time the
server came up, drifting by exactly the size of the ceiling per restart until it hit
the floor weeks later.

The fix is to restore the delta *from wherever the ceiling left it*:
`s.adjust(inst, val - s.value(inst))`.

Two things to take from it.

> **A correct decision in one half can be exactly what breaks the other.** The
> ceilings-first ordering was not a mistake and reversing it would have caused a
> different bug. What was missing was noticing that the first step had side effects
> the second step assumed away.

> **One round trip does not prove a round trip.** A single save-and-reload showed
> -55, which reads like an off-by-something and could have been "fixed" by
> subtracting the cap somewhere. It is the SECOND reload that shows -65 and proves
> the record is walking rather than merely wrong. The check now boots three times, and
> the third boot is the assertion that actually catches this class of bug.

---

## 21. An assertion about a moving thing is an assertion about when you looked

*Found by CI, on a check that passed here every single time.*

`worldgen_check.sh` placed a shrine and asserted two things about its keeper: that an
entity was within four blocks of the offering box, and that its yaw pointed at the
box. Both passed locally, repeatedly, including two deliberate back-to-back runs to
test for flakiness. The runner failed on the first:

```
FAIL: the shrine placed but is missing: E_KEEPER_ATTENDS
```

My first explanation was that the keeper had wandered: it has a stroll goal, RCON
commands arrive seconds apart, and the server ticks throughout. **That explanation was
wrong, and I stated it before testing it.** A probe that placed a shrine and then
queried the keeper's position 120 commands later found it at exactly its spawn
coordinate, unmoved. Whatever happened on the runner, drift was not it.

What is actually true is weaker and more useful: **the check asserted a property of a
live mob and could not say why the property was absent.** What changed is that the
check can now answer the question -- it dumps every keeper reply and the placement log
on failure -- and that the two assertions were replaced by ones that do not depend on
when you looked.

*(The cause was unknown when this was written. That reporting is what found it, one
hour later: the keeper was never missing, it was invisible. See #22.)*

Two different fixes, and the interesting part is that they are different.

**The position assertion was replaced by a tether assertion.** A shrine-keeper who
wanders off would leave a player standing at a shrine with a scene and nobody to have
it with, so they are now tethered (`setHomeTo`) to the court regardless -- and the
check asserts **the tether**, which is time-invariant, rather than the position, which
is merely a consequence of it. The tether is good design whether or not it was the
bug.

**The facing could not be asserted at all.** A mob's yaw is set once at placement and
then overwritten by whatever it looks at next, so there is no later moment at which
the evidence still exists. Widening a tolerance would only have made the check pass
without checking anything. The arithmetic moved into `core/spatial/Facing`, where it
is four assertions against the four cardinal directions and cannot move — the same
trade the whole `core/` split exists to make.

> **The rule: before asserting a property of live state, ask what else can change it
> between the write and the read.** If the answer is "the thing itself, on its own
> schedule", the property is not observable and no amount of tolerance makes it so.
> Assert the constraint that holds it (a tether, a cap, an invariant), or move the
> logic somewhere it stops moving.

Corollary, and the reason this cost a red build rather than being caught here: **a
flaky check can be perfectly reproducible on one machine.** Running it twice locally
proved nothing, because both runs had the same timing. The disagreement between two
*environments* is the signal; two runs in one environment is not.

Second corollary, paid for in this same hour: **a diagnosis stated before it is tested
is a guess wearing a conclusion's clothes.** I had a mechanism that explained the
symptom perfectly, said so, and only afterwards ran the probe that refuted it. The
useful output of the episode was not the explanation; it was noticing that a live-mob
assertion had no way to report *why* it failed, and fixing that. Along the way it also
turned up an ignored boolean: `addFreshEntity` returns false when the level declines
an entity, and a shrine with no keeper had no line anywhere saying so.

## 22. The entity was not missing. It was invisible.

*Three red builds, six local runs, and one line of log that should have existed from
the start.*

This is the answer to #21, which ended with the cause unknown. It is worth reading in
that order, because the wrong explanation was reasonable, survived a day, and was
refuted by exactly the reporting #21 added.

### The symptom

`worldgen_check.sh` forceloads a region, generates a shrine into it with `/place
feature`, and asks about the keeper the feature places. Sometimes the keeper was not
there. The block assertions all passed -- carved centre stone, paving, standing stele,
offering box -- and then every selector came up empty:

```
$ data get entity @e[type=interregnum:shrine_keeper,limit=1] Pos
  No entity was found
```

The same check passed on the same commit on a different runner. **Two runs of the
identical tree, one green and one red**, which was the first hard proof that this was
a race and not an environment difference -- and worth noting because #21's corollary
says the disagreement between two environments is the signal. That corollary needs an
amendment: the disagreement between two *runs* is just as good a signal, and I nearly
missed it by assuming a green push run and a red pull-request run must differ in the
tree they tested. They did not; `main` was an ancestor, so the merge commit was the
head commit.

### What the log said

The decisive evidence came from a line added the hour before, for a different reason:

```
The shrine at 8,8 seated its keeper at BlockPos{x=9, y=-60, z=8}.
```

That line was present in **every failing run**. Six local runs, three of them red, and
all six seated a keeper at exactly the same coordinate. `addFreshEntity` returned
true. The feature did its job perfectly, every time.

So the entity existed and nothing could see it.

### The mechanism

`forceload add` returns immediately; the chunk arrives later. `/place feature` needs
only the chunk's **blocks**, so it succeeds in that window -- and an entity added
during it goes into a section whose visibility is never established. The entity is
accepted, is in the level, and is not in what any selector iterates. Permanently: it
was still invisible at the end of the batch, many commands later.

Blocks fine. Mob gone. `addFreshEntity` true. Nothing anywhere reporting a problem.

The fix is that the check now waits after forceloading, before generating. Confirmed
by measurement rather than by argument: **3 of 6 runs failed without the wait, 0 of 6
with it.**

### The part that is not a sleep

A bare `wait 5` is a magic number tuned on this machine, which is precisely the thing
that has now failed on the runner three times. So the wait is followed by a probe: a
`minecraft:marker` summoned into the chunk and an assertion that `@e` can see it.

That is a live question rather than a hope. If the marker is visible, entities added
to this chunk are visible and generating the shrine is safe. If it is not, the check
fails **as itself**:

```
FAIL: the chunk was still loading when the shrine was generated.
  A marker summoned into it was invisible to @e, so any entity the feature
  places would be invisible too. The wait ... is too short for this machine
  -- lengthen it; do not delete the probe.
```

Verified by watching it fail: with the wait set to zero, that is the message that
comes out, not a word about keepers. A check that cannot distinguish "the thing is
broken" from "I asked too early" will eventually blame the thing.

### Two siblings had it too

`warden_check.sh` and `talk_check.sh` both summon an entity immediately after a
forceload and then interrogate it. Same race, same fix. `warden_check`'s restart pass
needed it for the mirrored reason: there the Warden is read back **off disk**, and a
chunk's entities arrive after its blocks, so asking too early answers "No entity was
found" -- which that check would have reported as a Warden that failed to survive a
restart. The loudest possible wrong answer.

> **The rule: an API that accepts your write has not promised anyone can read it.**
> `addFreshEntity` answering true means the level took the entity, not that the entity
> is live, findable, or in any structure a query walks. Where a write and a read cross
> an asynchronous boundary -- chunk loading, disk, a worker thread -- assert the
> boundary is closed before asserting anything through it.

Corollary, and the fourth time this project has paid for it: **the log line that finds
the bug is usually one added for a different reason.** "Seated its keeper at ..." was
written to catch a silent `return`. It never caught one. It answered a question nobody
had thought to ask instead, by being the only thing in the system that knew what had
actually happened.

## 23. A diagnostic dump that finds nothing kills the failure it was explaining

*Found while deliberately breaking a brand-new assertion to confirm it could fail.*

The assertion was fine. What was broken was the machinery for TELLING me it had failed.

Every check in `tools/` follows the same shape: assert, and on failure dump the
evidence before saying what went wrong.

```bash
[ "$n" = "1" ] || { grep -n 'show| KEEPER' /tmp/tc.txt | tail -8;
                    fail "the keeper's opening does not change"; }
```

Under `set -e -o pipefail` -- which every one of these scripts sets, for good
reasons -- that is a trap. When the assertion fails, the dump runs; if the dump's
`grep` **matches nothing** it exits non-zero; `pipefail` propagates that through the
pipe; and because the whole `{ ... }` is the last branch of an `||` list, `set -e` is
back in force inside it. The shell kills the script **at the dump**, before `fail`
ever runs.

The symptom is a check that exits 1 having printed **nothing**:

```
$ ./tools/talk_check.sh; echo "exit=$?"
  ... the previous assertion's success line ...
exit=1
```

That is worse than a bad error message. A silent exit 1 in a suite that prints
progress reads as "the last thing you saw is where it stopped", and the last thing
you saw was something PASSING. I read that output and briefly concluded the new
assertion could not fail -- the exact opposite of what had happened.

The fix is one `|| true` per dump, and it is load-bearing rather than defensive:

```bash
[ "$n" = "1" ] || { grep -n 'show|' /tmp/tc.txt | tail -8 || true;
                    fail "the keeper's opening does not change"; }
```

Note the second bug hiding inside the first: the pattern was `'show| KEEPER'`, and
the speaker renders as `SHRINE-KEEPER`. So the dump matched nothing *because the
pattern was wrong* -- a harmless mistake in a diagnostic, which under `pipefail`
became the thing that hid the failure. Two errors, neither serious, and together they
produced a check that appeared to be unable to fail.

> **The rule: a failure path must not contain anything that can fail.** Whatever
> gathers evidence for a failure message runs at exactly the moment the script is
> least able to cope with a surprise. Terminate every dump with `|| true`, and never
> let one sit between the test and the message it explains.

This is [#4](#4-measure-the-process-you-mean-not-the-pipelines-tail) wearing the other
face. There, a pipe *swallowed* a failure and reported success. Here, a pipe
*manufactured* a failure and swallowed the explanation. Both come from the same
place: a pipeline's exit status is not the thing you meant to measure.

---

## 24. If the expected string already appears earlier in the log, the assertion is a no-op

The ferry's most ordinary use is nudging a hull a couple of blocks along, and that is
the one move that eats it: origin and destination overlap, so a naive block-by-block
`clear this, write that` erases blocks it has already placed. `Ferry.place` runs two
passes over the whole hull for exactly this reason, and the check I wrote to guard it
was, on its face, perfect:

```bash
want /tmp/fc.txt 'ferry=manifest total=5 interregnum:ferry_keelx1 minecraft:chestx1 minecraft:oak_planksx3' \
    "the hull did not survive a two-block nudge"
```

Then I broke `place` into a single pass on purpose to watch the check fail, and it
printed `OK`.

The reason is the assertion's own premise. What it means to say is *the hull that
arrived is the same hull that left* -- so the string it looks for is, deliberately and
necessarily, character-for-character the manifest printed at the dock before the
crossing. `grep -q` cannot tell the two apart. It found the dock's line, every time,
no matter what happened to the boat. The keel was genuinely being deleted mid-move and
the check had no way to notice.

This is not the same mistake as [#15](#15-assert-the-setup-or-the-test-proves-whatever-absence-implies)
(a check whose subject never varies). Every ingredient here varies; the check was
written *because* it varies. The failure is that the log is append-only and `grep` is
position-blind, so an assertion of the form "X is still true afterwards" is
automatically satisfied by X having been true beforehand.

Two ways out, and the check now uses both:

```bash
# Count, so "still true" means twice, not once.
seen=$(grep -cF 'ferry=manifest total=5 ...' /tmp/fc.txt || true)
[ "$seen" = 2 ] || fail "... (saw $seen/2 identical manifests)"

# And assert the world, not the log: coordinates cannot be printed twice.
execute if block 22 -60 20 interregnum:ferry_keel run say NUDGE_KEEL_INTACT
```

> **The rule: before trusting an assertion about an "after" state, ask whether its
> expected text could have been emitted by the "before".** If it could, `grep -q` is
> measuring history, not outcome -- count the occurrences, or assert something the
> earlier state could not have produced.

The general shape is [#18](#18-a-mutation-caught-for-the-wrong-reason-leaves-the-check-unverified)
again from a new angle: a mutation is the only thing that tells you which of these two
kinds of check you wrote, and it is worth the compile every single time. This one had
been green, reviewed, and believed for a whole run before a deliberate break exposed
it.

---

## 25. A method that returns whether it worked was not asking rhetorically

The Warden's new patrol goal asked its navigation to walk to the next point on its beat:

```java
mob.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, speed);
```

It compiled, it ran, the goal ticked, the target was correct — and the Warden stood
perfectly still at its statue.

Two wrong theories went first, and the order matters. I suspected the new goal, then I
suspected the goal *selector*. Both were wrong, and the thing that settled it was
running the **control**: I put the old `WaterAvoidingRandomStrollGoal` back and the
Warden did not move under that either. Whatever this was, it was not a regression I had
just written — which immediately halved the search space. When a new thing does not
work, check whether the old thing did.

The probe that finished it printed one line:

```
PROBE moveTo ok=false onGround=false pos=(1.5, -60.0, 0.5) target=BlockPos{0,-60,-8} path=null
```

`GroundPathNavigation` refuses to build a path unless the mob is `onGround`. A mob that
has just been spawned is not on the ground *yet*: goals tick **before** movement inside
the same `aiStep`, so on a posted Warden's very first tick the call can only fail. One
tick later it would have succeeded.

`moveTo` returns a boolean saying exactly that, and I had thrown it away. Because the
failure was discarded, the goal marked the leg as started, then sat waiting out a
200-tick timeout before trying anything else. Ten seconds of a unit standing at its post
doing nothing, once per corner, which is indistinguishable from a patrol that does not
work — and was one.

> **The rule: when an engine method returns a status, the status is the API telling you
> about a case you have not thought of.** Discarding it does not remove the case; it
> removes your only notification of it. Treat an ignored return value as an unhandled
> branch, because that is what it is.

The fix is not "call it twice". It is to branch on the answer: keep the target only if
the path was accepted, retry shortly if it was not, and abandon the leg after a bounded
number of refusals — which also, for free, handles the case this repo will actually meet
in play, where somebody walls off one corner of a Warden's beat because they have worked
out where it goes.

---

## 26. A check that splits on a guessed string is a check that never ran

The sealed letter carries a **god id** — `verdant` — and never an addressee, because a
stack in a hotbar is a string a player can see and the whole mid-game reveal is that
`Rill` is unheard until the letter is opened. `letters_check.py` guards the lang file
and cannot see a data component, so the live check had to guard the other half.

The first version carved the server output into "before the item dump" and "after", so
it could scan only the item:

```bash
dump = text.split("Item has the following entity data:")
sys.stdout.write("leak" if len(dump) > 1 and re.search(r"\bRill\b", dump[1]) else "clean")
```

It reported **clean** against a build that had been deliberately broken to put `Rill` in
the component. The name was right there in the output.

`data get entity` names the entity by its **item**, so the line is
`Sealed Letter has the following entity data:`. The split string never appeared, `dump`
had one element, `len(dump) > 1` was false, and the expression short-circuited to
`"clean"` — every time, for any input, forever. Not a wrong answer: **no answer**, wearing
the shape of a right one.

The tell was there and I nearly missed it. The mutation *was* caught — by the assertion
on the next line, which noticed the component no longer held `verdant`. A caught mutation
is exactly what makes this easy to skip past: the check failed, the build was broken, the
system worked. It was only reading *which* assertion fired that showed the important one
had not.

The fix is to stop guessing at all. The claim is about a component, so ask about the
component:

```bash
grep -qE '"interregnum:letter": "(Rill|Ballast|Ash)"' /tmp/ml.txt
```

> **The rule: never locate the thing under test by splitting on a string you have not
> looked at.** A prefix you assumed is an unchecked assumption on the *test's* side, and
> when it is wrong the test does not fail — it stops testing, silently, while continuing
> to print OK.

This is [#15](#15-assert-the-setup-or-the-test-proves-whatever-absence-implies) reached by a new route, and it
shares a moral with [#24](#24-if-the-expected-string-already-appears-earlier-in-the-log-the-assertion-is-a-no-op):
both are assertions that could not fail, and in both cases the surrounding check was
green and looked thorough. The difference here is that a *neighbouring* assertion was
doing the work, which is the most comfortable way for a dead check to hide.

## 27. I wrote the lesson, then walked into it one increment later

`tools/verdant_check.sh` carries this, in its own comments, in as many words:

> Not "the overworld converted nothing": at eight times the rate over 25 seconds the
> overworld genuinely converts a few, and **a check demanding zero at home would be flaky
> by construction**.

Two increments later I wrote `tools/exodus_check.sh`, which measures grass spreading in
the overworld at eight shrines, and asserted:

```bash
[ "$other_grew" -eq 0 ] || fail "... grew anyway ..."
```

It failed on its first run. The two Verdant shrines greened 7 of 8; the six others
greened 0, 0, 0, 0, 1 and 2. That is not a failure — it is a **textbook pass** with a
clean boundary, rejected by a threshold that had no business being a threshold.

### Why the lesson did not transfer

Not because I had forgotten it. Because in the new check the number *looked* categorical.
`turning_check.sh`, written between the two, is legitimately absolute — **nothing in
vanilla turns stone into cobblestone**, so any conversion at home is a leak, and that
check asserts zero and is right to. So "this law's control is zero" was a live and
correct pattern in my head, sitting one file away from a law where it is false.

The distinguishing question is not about the check. It is about the **mechanism**:

> Does vanilla already do this thing on its own?
>
> * **No** (stone → cobble, a block rising, a dimension's bed rule) → the control is
>   zero, and asserting it is the strongest thing you can write.
> * **Yes** (grass spreading, crops growing, mobs pathing) → the control is *whatever
>   vanilla's rate happens to be*, you do not get to pick it, and the only honest
>   assertion is a **comparison**.

The mod's law and vanilla's law were running on the same block type, and I attributed the
whole signal to the mod.

### What it should have been, and is

Not a threshold at zero, and also not a threshold anywhere else — a slower runner ticks
less in the same wall-clock window and drags *both* numbers down together:

```bash
# the worst Verdant shrine must out-green the best non-Verdant one, by a margin
[ "$verdant_min" -gt $((other_max + 2)) ] || fail "..."
```

Sixteen targets per shrine rather than eight, for the same reason the Verdant's check
uses thirty-two: halving the relative variance is free and a coin-flip assertion is not.
Measured after the fix: **10 and 11** of 16 at the Verdant shrines, **0–3** at the other
six. The boundary was always there; the assertion just could not see it.

> **The rule: before asserting a control is zero, ask whether vanilla performs the
> mechanism under test.** "Nothing should happen here" is a claim about *vanilla's*
> behaviour, not about the mod's, and the mod's source cannot tell you whether it is true.

Related: [#2](#2-a-confounded-test-is-a-false-instrument) is the same family — a
measurement that could not separate the mod from the engine underneath it.

## 28. Three times is not a slip, it is a missing check

```bash
fail "`interregnum turning age` aged a block in the OVERWORLD -- ..."
```

Backticks inside a **double-quoted** bash string are command substitution. Bash runs
`interregnum turning age`, prints `command not found`, and delivers the failure message
with its own subject cut out of it — on the failure path, so at the exact moment somebody
needs to read it.

I wrote this bug in `turning_check.sh`, fixed it, wrote it again in `mail_check.sh`, fixed
it, and wrote it a third time in `exodus_check.sh` — where the mutation run surfaced it,
which is the only reason it did not ship. Each time I was quoting an identifier the way I
quote identifiers in markdown all day. Each time the line read as correct.

At three, the honest conclusion is that the reviewer (me) cannot see this class of defect
by looking, and the fix is not more care. It is `tools/failpath_check.py`, which flags
**any backtick on a non-comment line in any `tools/*.sh`** — blunt on purpose, because
this repository substitutes with `$(...)` everywhere and had zero legitimate backticks
outside a comment when the check was written, so the narrow rule and the blunt rule
accept exactly the same set of files. A rule that tried to parse bash quoting would be a
small bash parser, and a small bash parser is a thing that can fail on the failure path.

It carries a second assertion from the same family: a script that calls `fail` must
define it. An undefined `fail` is exit 127 under `set -e` — a red build with **no
message at all**, which is a check being right and mute.

> **The rule: the third occurrence of a defect is a request for a check, not for more
> attention.** Two is a coincidence. Three is evidence that your eyes do not catch it,
> and evidence about the reviewer is evidence about what has to be automated.

Both of its assertions were watched failing before it was trusted, per
[#18](#18-a-mutation-caught-for-the-wrong-reason-leaves-the-check-unverified).

## 29. `git checkout --` on a tracked file is not an undo, it is a discard

Mid-way through verifying `tools/doclink_check.py` I appended two deliberately dead links
to `docs/LESSONS.md`, confirmed the check caught both, and then reached for the obvious
cleanup:

```bash
git checkout -- docs/LESSONS.md
```

That file had **eighty uncommitted lines** in it — lessons 27 and 28, written minutes
earlier, plus five anchor fixes. `git checkout --` does not undo my last edit. It restores
the file from the index, and everything since is gone with no reflog entry, no stash, and
no confirmation prompt.

The specific trap is that the command is *usually* safe, because the file I am reverting
is usually one I only just touched. Here the file was doing double duty: the thing I was
editing and the thing I was testing against. Two minutes of work, and the only reason it
was recoverable is that I could still see the text.

The habit that replaces it costs nothing: **when the scratch edit is an append, undo it as
an append.**

```bash
cp docs/LESSONS.md /tmp/lessons.bak   # or:
head -n -2 docs/LESSONS.md > tmp && mv tmp docs/LESSONS.md
```

Better still, do not scratch-edit a real file at all — `tools/doclink_check.py` would have
been verified just as well against a throwaway file in the scratchpad, which is where the
next one goes.

> **The rule: never restore a tracked file from git to undo an edit you have not
> committed.** The blast radius of `git checkout --` is the whole file's uncommitted
> history, not your last change, and the two are indistinguishable from the command line.

## 30. The world stopped, and the check reported that the mod was broken

`tools/exodus_check.sh` is the first check here to wait longer than a minute. Sand placed
after that wait never fell. Sand placed five chunks from anything never fell either. The
check said, confidently and in detail, that the Anchorite's law had escaped its patch.

The mod was fine. **The server had paused itself.**

A dedicated server ships with `pause-when-empty-seconds=60`. Every check in this
repository runs with no players connected, so from sixty seconds after boot the world
simply stops: no warning, no "Can't keep up", no error in the log, no exception. Blocks
stay where they are, entities do not move, and every probe answers accurately about a
world that is no longer running.

### How it finally gave itself up

Not by reasoning. Every hypothesis I formed was wrong, in order: the gamerule (disproved
in isolation), band 3 (disproved), the leak (disproved), server load (no lag warnings),
scale (a full-size world with eight shrines worked fine).

What settled it was asking the world what time it was, on both sides of the wait:

```
time is 94     -> time is 174     # 4 seconds, 80 ticks. Sand landed.
time is 1199   -> time is 1199    # 4 seconds, 0 ticks.  Sand stuck.
```

1199 ticks is sixty seconds. The world had run normally and then stopped dead, and the
difference between the two runs was only that one of them waited long enough to cross it.

### Two things came out of it, and the second matters more

`server_smoke.sh` now writes `pause-when-empty-seconds=0`, so no future check has to know
this exists. `turning_check.sh` waits forty-three seconds and had been living just under
the edge without anyone noticing.

The second is the control I should have written first. This check had no answer to *does
sand fall in this world at all* — `anchorite_check.sh` has carried exactly that control
since the day it was written, and says why in its own header. Adding it here took one
column of sand in an empty chunk, and on its very first run it turned

> the leak is applying one god's law at every patch

into

> gravity is not working anywhere in this world, so the fault is in the harness rather
> than the mod

which is the difference between an hour of reading mod source and a one-line fix.

> **The rule: when a check reports that the system under test is broken, the first
> question is whether the world it measured was running.** A frozen world answers every
> probe correctly and every answer is about the past. Assert that time passed — literally,
> in ticks — before concluding anything from what did not happen.

This is [#2](#2-a-confounded-test-is-a-false-instrument) with the confound being the
absence of the thing that makes a game a game, and
[#15](#15-assert-the-setup-or-the-test-proves-whatever-absence-implies) with the unasserted
setup being the passage of time.

## 31. The same threshold mistake, three times in one night, from three directions

`tools/attrition_check.sh` defends one fact: tending must reach a **shorter** distance
than chunk loading does, because attrition can only act on loaded ground and if
everything loaded were also tended there would be nowhere in the world it was ever
allowed to touch. Widen one constant and band 4 becomes permanently inert — nothing
crashes, nothing logs, the world simply never forgets anything.

So the check tends the chunk under you, reads a chunk four out, and asserts it was not
tended. Ground stamped this instant reads `0` ticks ago, so:

```bash
[ "$far_tended" -gt 0 ]
```

**It escaped the mutation.** Widening tending to twelve chunks stamps both chunks in the
same instant — but the two probes are separate RCON commands and can land one tick apart,
so the far one read **1**, and 1 is greater than 0. Green. The one defect the file exists
to catch, walking straight through, on a coin flip.

Re-running caught it, which is worse than a clean miss: the check is not wrong, it is
*flaky*, and a flaky check that fails half the time reads as an infrastructure problem
rather than a real one.

The fix is a comparison, as it was the last two times. The far chunk is stamped at chunk
load and the near one at tending — about eighty ticks apart honestly, zero or one when
mutated:

```bash
gap=$((far_tended - home_tended))
[ "$gap" -ge 20 ]
```

### What is actually going on, across all three

This is the third time in one session, and the surface details were different every time:

1. **[#27](#27-i-wrote-the-lesson-then-walked-into-it-one-increment-later)** — *"nothing
   grew here"*, where vanilla grows grass on its own.
2. A ceiling set at 4 of 16 that a clean run then hit at **exactly 4**.
3. This one — *"more than zero ticks ago"*, where the clock ticks between two commands.

Each time I reached for a threshold, and each time the threshold sat on the boundary of
something that varies for reasons that have nothing to do with the property under test:
vanilla's own behaviour, Poisson noise, the server's tick schedule. The number looked
principled because I could name what it meant. What I could not name — because I never
measured it — was the *spread*.

> **The rule: a threshold is a claim about variance, so do not write one until you have
> measured the variance.** If the two populations you are separating can be measured in
> the same run, compare them and never pick a constant at all — a comparison is immune to
> every source of drift that moves both sides together, which is most of them.

The tell is specific and worth learning to feel: **if you can state what the number
means but not how far the honest value ranges, you are guessing.** "Tended ground reads
zero ticks ago" was a true sentence about an idealised world and a false one about a
server that schedules commands.

## 32. Four scenes shipped and the thing they exist to produce was never recorded

`LETTER_DELIVERED` has been in `core` since the chapter machine was written. `ChapterState`
counts it. `Chapter` gates chapters 3, 4 and 5 on the count. And a grep for it across the
entire game module returned **nothing**.

So all four delivery scenes shipped, each one the opening of a god's questline, each one
walked end to end by a live check that asserted the scene reached its ending and the god's
regard moved — and delivering all four letters advanced the world by exactly nothing.

Every check passed. They were all true. The scene *did* play to its end; the regard *did*
move; the ceiling *did* hold. Nothing any of them asserted was wrong, and the feature was
missing its entire point.

### Why the checks could not see it

Each assertion was written while building the thing it asserts, and it asked *did the
thing I just built do what I built it to do*. The scene plays: yes. The regard lands: yes.
Nobody asked the question that spans two increments — **does the rest of the system
notice?** — because at no point was that the thing being built.

That is the specific blind spot: a check written alongside a feature inherits the
feature's scope. It cannot ask about the seam between this feature and the one written
last month, because that seam was not on the author's mind, and it is exactly where things
fail to connect.

The tell was available and I walked past it twice. `HANDOFF.md` said *"`FIRST_CROSSING`
and `LETTER_DELIVERED` are still unreachable"*, and I read that line while writing three of
the four scenes, and took it as describing the state before the scenes existed rather than
as a claim to verify.

### What it cost, and what fixed it

Nothing, because it was found before anyone played it — by re-reading the roadmap
sceptically rather than by any check. That is the uncomfortable part: **the thing that
caught it was doubt, not machinery.**

The machinery now exists. A node may carry a `milestone`; arriving at it records that
milestone. It hangs on the node rather than the option because "the letter was delivered"
is a fact about the conversation having *arrived* somewhere, not about which sentence
somebody picked — three routes into one ending should record one delivery, not three. And
`delivery_check.sh` now reads the count: `letters delivered: 0 -> 4`.

> **The rule: for every state a feature is supposed to produce, assert the state — not the
> feature.** "The scene played correctly" and "the world is different afterwards" are
> different claims, and only the second one is what the feature is for. When a system
> exists to move a number that something else reads, the check belongs on the number.

Related: [#16](#16-a-check-on-the-shape-of-data-cannot-tell-you-the-data-does-anything) is
the same gap one level down — there, data that validated but did nothing; here, behaviour
that ran correctly and connected to nothing.
