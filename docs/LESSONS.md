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
`tools/server_smoke.sh`: it shuts the server down cleanly through stdin (`stop`), clears the
lock, and **fails loudly if the server never finished loading** instead of silently finding
nothing.

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
