# The doc index

**Read this file, then read exactly one other thing.** That is the entire point of the
split. Every document here is written to be loaded *alone*, so a session working on
textures never pays for the worldgen doc and vice versa.

If you are a fresh session with no context: read [`HANDOFF.md`](HANDOFF.md) first. It is
the only document that tells you what is *currently true*. Everything else tells you what
is *always true*.

---

## Router

| If you are about to… | Read | Lines |
|---|---|---|
| pick up cold, with no idea what state anything is in | [`HANDOFF.md`](HANDOFF.md) | short, always |
| set up the toolchain, change MC version, or debug a build | [`PLATFORM.md`](PLATFORM.md) | ~193 |
| add a block, item, entity, or any registered thing | [`ARCHITECTURE.md`](ARCHITECTURE.md) | ~231 |
| draw or generate any texture | [`TEXTURING.md`](TEXTURING.md) | ~278 |
| choose a colour, any colour | [`PALETTE.md`](PALETTE.md) | ~202 |
| decide what a thing should *look* like | [`ARTSTYLE.md`](ARTSTYLE.md) | ~287 |
| write a block model, blockstate, or entity model | [`MODELS.md`](MODELS.md) | ~226 |
| add a dimension, biome, feature, or structure | [`WORLDGEN.md`](WORLDGEN.md) | ~207 |
| write recipes, loot tables, tags, advancements | [`DATAGEN.md`](DATAGEN.md) | ~175 |
| write a test, a probe, or any tool that measures | [`VERIFICATION.md`](VERIFICATION.md) | ~216 |
| invent any part of the setting, or judge one | [`AESTHETIC.md`](AESTHETIC.md) | ~164 |
| add a block, item or entity — or ask how a player reaches one | [`REACHABILITY.md`](REACHABILITY.md) | grows |
| find out whether we already learned something the hard way | [`LESSONS.md`](LESSONS.md) | grows |
| find out what happened and when | [`LOG.md`](LOG.md) | grows |

## Which documents you are allowed to change

| Kind | Documents | Rule |
|---|---|---|
| **Living** | `HANDOFF.md`, `LESSONS.md`, `LOG.md`, `REACHABILITY.md` | Update them *as you work*, not at the end. A lesson written three sessions late has already cost its price twice. |
| **Doctrine** | `ARTSTYLE.md`, `PALETTE.md`, `AESTHETIC.md`, `VERIFICATION.md` | Change only with a reason written into the diff. These exist to constrain decisions; a doctrine edited to permit what you wanted to do anyway is not doctrine. |
| **Reference** | `PLATFORM.md`, `ARCHITECTURE.md`, `MODELS.md`, `TEXTURING.md`, `WORLDGEN.md`, `DATAGEN.md` | Correct them the moment reality disagrees. A reference doc that is wrong is worse than one that is missing. |

---

## Provenance, and why every doc declares it

Each document opens with a provenance line. There are three kinds and they carry very
different weight:

- **PORTED** — a lesson that was actually paid for, in the DOWNTIME project
  (`cadykaya/mario-3`), by a rebuild or a rejected model. Trust these. They are the most
  valuable thing in this repository and none of them was free.
- **REFERENCE** — how Minecraft, NeoForge, or the toolchain actually works. Trust the
  *shape*; verify the *specifics* (see the standing warning below).
- **DOCTRINE** — a decision made for this project, with reasoning attached. Argue with
  these freely. They are choices, not facts.

**Nothing in this repository invents a war story.** If a document describes a failure, that
failure really happened, in a real project, and the doc says which. When this project
starts accumulating its own scars they go in `LESSONS.md`, marked as ours.

---

## The standing warning about API specifics

> **Modding APIs churn harder than almost anything else in software, and this doc set was
> written without access to the NeoForge documentation** — the sandbox that produced it
> could reach a search engine but the egress proxy blocked `neoforged.net` and
> `docs.neoforged.net` outright.

So: every document here is deliberately written about **concepts, structure, formats and
decisions**, which are stable, and it avoids quoting exact Java method signatures, which
are not. Where a signature does appear it is marked `VERIFY:` and you must check it
against the real sources before trusting it.

The rule, and it is the same rule as everywhere else in this project: **measure, do not
estimate.** The actual NeoForge sources are in the Gradle cache once the project has been
set up once, and reading them takes a minute:

```sh
find ~/.gradle/caches -name 'neoforge-*-sources.jar' 2>/dev/null
```

That is the primary source. This doc set is a map, not the territory.
