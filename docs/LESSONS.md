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
