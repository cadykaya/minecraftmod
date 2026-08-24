# Verification

> **Provenance: PORTED, almost entirely.** This is the most valuable document in the
> repository and none of it was free. Every rule below was paid for in DOWNTIME
> (`cadykaya/mario-3`) with a rebuild, a wrong render, a false commit message, or in one
> case an entire discarded character.
>
> It is about **software**, not art, which is why it transfers to a Minecraft mod almost
> without translation.

---

## The five rules

### 1. A check that has never failed is unverified

Every guard in this project was tested by **reintroducing the bug it catches** and
confirming it fires. Do that before trusting one.

This is not pedantry. In DOWNTIME, *both* hair guards and the paint-bounds guard failed the
already-approved model on their first version — a check that fails the thing it was written
to protect is measuring the wrong edge.

**Worked example in this repo.** `tools/palette_check.py` has five checks. All five were
verified by breaking something on purpose:

| Check | How it was broken | Result |
|---|---|---|
| Staleness | one hex digit hand-edited in `palette.json` | caught |
| Separation | a ramp flattened to 0.06 L\* gaps | caught |
| Arithmetic | a recorded L\* changed to a lie | caught |
| Off-palette | `#6E502D` against a legal `#6D4F2D` | caught |
| Breadth | a texture using six families | caught |

The off-palette case is the one that matters: a single wrong hex digit is the *realistic*
bug, and no human will ever spot it in a diff.

### 2. Assert on the effect, never on the input

> *"The bone's rotation changed"* passes happily through a rotation about the wrong axis.

DOWNTIME's `PoseProbe` measures how far the **hand** moves through the actual skinning
transform, and it failed at 0.0019 m against a 0.10 m floor on exactly the bug that had
shipped.

Translated: do not assert that you *registered* a block. Assert that the block **resolves a
model, has a name, and drops something**. Do not assert a recipe file exists; assert the
recipe **produces the item from the inputs**.

### 3. Green tests are not a working feature

> `TailSpring` did not compile at all — an untyped `Array` breaks `:=` inference — while 43
> unit checks, the smoke test and six boot checks all passed, because `attach()` failing was
> a runtime error in a call nobody checked.

> **If a feature has no probe that would notice its absence, it has no test.**

For a mod, the loudest version of this: **a mod can load with zero errors and add nothing.**
Registration silently on the wrong bus, a datapack path off by one directory, a model that
fails to resolve — the game starts fine. Green CI proves the build compiles. It does not
prove the mod does anything.

### 4. Generated assets go stale in silence

Covered at length in [`DATAGEN.md`](DATAGEN.md) because that is where it will bite. The
short form:

> Committed output older than its source produces **no symptom at all**, survives commits,
> and gets misattributed to an unrelated change much later.

The gate: regenerate everything in CI, then `git diff --exit-code`.

### 5. Do not narrate a result you have not read

> The staleness was on screen, in output directly above the sentence calling it clean, and
> went into a commit message as *"all 20 variants rebuild with no diff."*

**Verifying a subset and reporting it as the whole is worse than not checking**, because it
converts an open question into a false certainty somebody later has to spend real work
undoing.

---

## A new bench is the least trustworthy thing in the repository

> **The most expensive habit in DOWNTIME's whole record.** It produced a false instrument
> three separate times in a single session, and every artefact *looked like evidence* and
> was believed on sight.

| The artefact | What it actually measured |
|---|---|
| a silhouette row at ~500 px, in profile | that it worked at a size and angle nobody plays at — and it was sent to the owner as proof |
| a bounding box from `local_aabb()` | a skinned mesh's **rest** bounds. Five phases all reported an identical 109 px: the pose was measured by measuring the one thing that cannot move |
| a correctly measured bounding box | not the thing being asked about — it was actively pushing the design toward a worse machine to move a number that could not see the problem |
| a GIF at a guessed frame delay | 22.4 s of game time in 90 frames: playback 5× too fast, sampled at 8 Hz, which cannot show the artefact it was built to show |

> Everything else in that project got byte-compared, round-tripped, or measured in pixels —
> **and then the instrument doing the measuring was waved through, because writing it felt
> like doing the verification. It is not.**

### The three habits

- **Establish what a bench measures before you believe what it says.** DOWNTIME's GIF writer
  was checked by decoding its own output with an independently written LZW decoder and
  comparing all 921,600 indices of a real render. Ten minutes, and every GIF afterwards was
  trustworthy.
- **A metric must be able to see the thing you are asking about.** If a change you can
  plainly see does not move the number, **the number is the wrong number** — do not tune the
  design until the metric agrees with your eyes.
- **State the space and the units.** Four of `BossProbe`'s first assertions measured a
  crouch in model space, where the chassis never moves and the feet always do. Every result
  inverted, and a working machine failed its own test.

### And about reading results

> **A filter that cannot express failure will only ever report success.**

*"34 PASS, zero FAIL"* came off a grep matching `FAIL` followed by a space. The real lines
read `FAIL:`. Separately, a 147-second hang was read as "slow" rather than looked at; it was
a script error aborting before the probe could quit.

**Probes carry a watchdog**, because a probe that can hang is a probe whose silence you
cannot interpret.

> **This document's own worked example.** While verifying `palette_check.py`'s review-render
> skip, a test reported `exit=1` and was briefly read as "the skip does not work" — but a
> *different* file in the same directory was failing, so the run said nothing about the case
> under test. Re-run in an isolated directory with identical bytes under two names, it
> proved what it claimed. **A confounded test is a false instrument**, and this one was built
> by someone who had just finished writing this section.

---

## Knobs multiply

> Changing a default broke a variant that had been safe: `one_sided` at sweep 1.45 against a
> new 0.86 length came out at an effective 0.736 and opened bare skin.

**Re-run every variant after any default changes.** In a mod this is: change a shared block
property, a base texture rule, or a recipe helper, and *every* consumer needs re-checking —
not the one you were thinking about.

---

## Passing the checks is not the same as the design being right

> **The lesson that cost a whole character**, and it outranks everything above.

Walker v1 met every ratio its spec asked for. Two build-time guards were written for it and
both were verified by reintroducing the bugs they catch. The seven-shot review gate passed
by the letter of its own condition. The owner rejected it on sight as a *"clickbait-thick
Among Us character,"* and they were right.

**The rules were the problem.** Read together — pelvis widest, torso deeper than wide, head
under the back, highest point formed from the torso, short hanging arms, wide post legs —
there was exactly **one** silhouette satisfying all six, and no amount of tuning inside those
constraints reaches a maintenance worker. Every check was measuring conformance to a
specification that had already decided the answer.

> **A check can only tell you the thing matches its description. It cannot tell you the
> description was worth matching.**

So: when a spec is a list of constraints, **write down what the thing should read as
first**, and check the constraints against that. And when a build passes everything and
still looks wrong, **the spec is the suspect** — not the execution, and certainly not the eye
that flagged it.

---

## What to actually build here

Ordered by value per line. The first three are cheap and catch the three commonest silent
failures in modded content.

### Tier 1 — write these before there is much content

1. **Registry completeness.** Walk every registered block and item; assert each resolves a
   model, resolves every texture that model names, has a translation key present in
   `en_us.json`, and (blocks) has a loot table. *Catches the purple-and-black cube, the raw
   `block.modid.thing` string, and the block that drops nothing — all silent, all common.*
2. **Client-leak grep.** No import of a client-only type outside `client/`. A few lines; it
   makes [`ARCHITECTURE.md`](ARCHITECTURE.md)'s hard boundary a guarantee rather than a
   convention, and it catches the dedicated-server crash class before a server ever sees it.
3. **Datagen staleness.** Regenerate, then `git diff --exit-code`.

### Tier 2

4. **Palette check** (`tools/palette_check.py`) pointed at the real asset tree.
5. **Tag round-trip** — tags you claim to produce contain what you think.
6. **Recipe reachability** — every custom recipe's output is obtainable from its inputs.
7. **Server-side boot** — the dedicated server starts, loads the mod, generates a chunk,
   shuts down. Nothing else catches physical-side mistakes.

### Tier 3

8. **In-game gametests** for block/machine behaviour. `VERIFY:` the current framework.
9. **Worldgen probes** — fixed seed, generation timing, structure encounter rate.
10. **Render probes** — the texture bench in [`TEXTURING.md`](TEXTURING.md).

---

## The real gate is still a screenshot

Ported verbatim, because it stayed true through DOWNTIME's entire development:

> **The greyscale test is the quickest honest one:** desaturate a capture, and if the
> composition falls apart, the palette is not working.

No amount of automated checking substitutes for looking at the thing, in the game, at the
distance a player sees it. Everything in this document exists to make the looking *cheaper
and rarer* — to catch the mechanical failures automatically so that human attention is spent
on the questions only a human can answer.

**And the owner is the last gate.** They rejected Walker v1 on sight when every check passed.
That judgement is the most reliable instrument in the project, and it should be spent on
things that have already survived everything above.
