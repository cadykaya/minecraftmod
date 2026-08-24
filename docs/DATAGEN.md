# Datagen

> **Provenance: REFERENCE + DOCTRINE.** The providers are reference; "generate everything"
> and the staleness rule are decisions, and the staleness rule is **PORTED** from a real
> DOWNTIME incident that cost a three-worktree forensic audit.
>
> **`VERIFY:` markers flag provider APIs**, which change between versions.

---

## The rule

> **If it is JSON in `src/main/resources/data` or `assets`, it is generated. Not typed.**

Recipes, loot tables, tags, advancements, block models, blockstates, item models, lang
files, worldgen. All of it.

This is the same argument as [`TEXTURING.md`](TEXTURING.md)'s "art is code," and it holds
for the same reasons:

- **Reviewable.** A changed rule is a changed line; 300 changed JSON files are not a review.
- **Consistent.** Every block in a family gets the same treatment because one loop made
  them, not because someone remembered.
- **Refactorable.** Rename a block and the recipes, loot, tags and models follow. Hand-typed
  JSON referencing a renamed block fails **silently at runtime**, and often only in the one
  situation nobody tested.
- **Correct by construction.** A provider that will not compile beats a JSON file that
  parses fine and is wrong.

**Ten similar blocks is the threshold where hand-typing stops being merely tedious and
starts being a source of bugs.** A content mod passes ten almost immediately.

---

## The generated output is committed

Both the providers **and** their output live in git.

- The output is what actually ships; a build that must run datagen to be correct is a build
  that can be wrong.
- The diff of the *output* is where you see what a provider change really did — which is
  frequently not what you intended.

Which creates the failure mode below, and it is the reason this document exists.

---

## Generated assets go stale in silence

> **PORTED, and this one is nasty.** In DOWNTIME a character was built by two commands. A
> pass that ran only the first left every variant describing a body that no longer existed.
> **Nothing failed:** the build was deterministic, the tests were green, and the assets were
> simply older than their source. It survived two commits, and when a later rebuild finally
> collected the change it was misread as a regression in an unrelated refactor — costing a
> three-worktree forensic audit to establish that nothing was wrong except staleness.

Every generator in this project has the same shape and therefore the same failure:
`palette_build.py`, the texture builders, and datagen. **Committed output that is older than
its source is invisible, silent, and blames the wrong change later.**

### The defence

> **CI regenerates everything and fails if the working tree is dirty.**

```sh
./gradlew runData
python3 tools/palette_build.py
# ... texture builders ...
git diff --exit-code            # non-zero => committed output is stale
```

This is the direct equivalent of DOWNTIME's `tools/check_assets_current.sh`, and it is the
highest-value CI check in the repository, because it is the only one that catches a class of
bug that produces **no symptom at all** until much later.

**Run it locally before every commit that touches a generator.** And note the companion
lesson, which is about people rather than tooling:

> **Do not narrate a result you have not read.** In that same incident the staleness was on
> screen, in output directly above a sentence calling the run clean, and went into a commit
> message as *"all 20 variants rebuild with no diff."* Verifying a subset and reporting it as
> the whole is worse than not checking, because it converts an open question into a false
> certainty someone later has to spend real work undoing.

---

## The providers

`VERIFY:` all names. The *set* is stable; the identifiers are not.

| Provider | Emits | Notes |
|---|---|---|
| Recipe | `data/<ns>/recipe/` | shaped, shapeless, smelting, and custom types |
| Loot table | `data/<ns>/loot_table/` | block drops, chests, entity drops |
| Tag | `data/<ns>/tags/` | **the most important one — see below** |
| Advancement | `data/<ns>/advancement/` | also the progression/trigger system |
| Block state + model | `assets/<ns>/blockstates`, `models/block` | mostly one-liners off vanilla parents |
| Item model | `assets/<ns>/models/item` | `VERIFY:` — highest-churn area, see `MODELS.md` |
| Language | `assets/<ns>/lang/en_us.json` | generate it; see below |
| Worldgen | `data/<ns>/worldgen/` | via a registry-dump provider |

Providers declare dependencies on each other (tags before recipes that reference them);
respect that or you get nondeterministic output.

### Tags matter more than they look

Tags are **how your mod cooperates with every other mod**. A pickaxe that works on
`#mineable/pickaxe` works on everything anyone ever adds. A recipe that takes `#c:ingots/tin`
accepts tin from any mod that ships it.

> **Consume tags, produce tags.** Never reference another mod's item directly when a
> convention tag exists. Direct references are how packs break.

- **`minecraft:` tags** — vanilla behaviour hooks (`mineable/*`, `needs_*_tool`).
- **`c:` convention tags** — the cross-mod vocabulary. Use the community convention; a
  correct-looking tag under the wrong namespace is invisible to everyone.
- **Your own tags** — for your own logic. Prefer a tag over a hardcoded list *inside* your
  code too; it makes behaviour datapack-tunable for free.

### Language files: generate, and never leave a hole

An untranslated key renders in-game as the raw key (`block.modid.thing`), which looks
exactly like a bug because it is one.

Generating `en_us.json` means **a block cannot exist without a name** — the provider walks
the registry, so a missing entry is a build failure rather than a screenshot. `VERIFY:` the
provider name, but insist on the property: **it must fail on a missing key, not skip it.**

---

## Recipes

- **Use tags for inputs.** `#c:ingots/copper`, not `minecraft:copper_ingot`.
- **Recipe advancements come along for free** in the vanilla providers — do not disable them
  casually; that is how recipes show up in the recipe book.
- **A recipe that cannot be discovered does not exist.** Unlock conditions are part of the
  recipe, not an afterthought.
- **Custom recipe types are a real cost** — a type, a serialiser, a book category, and JEI-
  style integration for anyone to see it. Worth it for a genuine machine, never worth it for
  a variant of crafting.

## Loot tables

- **A block with no loot table drops nothing**, silently. This is the most common "my block
  vanished" report and it is always this.
- Silk touch, fortune, and explosion-survival are conditions on the table, and each of them
  is a decision. Ore that ignores fortune reads as broken.
- **Generate the boilerplate.** "Drops itself" for eighty blocks is one loop.

---

## Running it

```sh
./gradlew runData          # VERIFY: task name
git status                 # inspect what moved -- ALWAYS
git diff --stat
```

**Read the diff every time.** Datagen's whole risk is that it will happily rewrite three
hundred files because of a one-character change in a provider, and a commit that says
"add copper block" containing 300 unrelated changes is a commit nobody can review or revert.

If the diff is bigger than the change, **stop and find out why before committing.**

---

## Verification

- **`git diff --exit-code` after a full regenerate** — the staleness gate, in CI.
- **Every registered thing resolves a model, a name, and a loot table.** A registry-walking
  test that fails on a missing one. Cheap, and it catches the three most common silent
  content bugs in modding.
- **Tag round-trip** — the tags you claim to produce actually contain what you think. A tag
  is a promise to other mods; an empty one is a broken promise that nothing reports.
