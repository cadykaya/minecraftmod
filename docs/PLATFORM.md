# Platform

> **Provenance: REFERENCE + DOCTRINE.** The version facts were gathered by web search in
> August 2026 and are cited below. The choices made from them are ours.
>
> **Read [`INDEX.md`](INDEX.md)'s standing warning about API specifics before trusting any
> exact string in this file.** Build numbers especially: they move weekly, and this
> document was written without access to the NeoForge version index.

---

## The decision

| | Choice |
|---|---|
| **Game** | Minecraft: Java Edition **26.2** ("Chaos Cubed", released June 2026) |
| **Loader** | **NeoForge** 26.2.x |
| **Java** | **JDK 25** |
| **Build** | Gradle with **ModDevGradle** (MDG) 2.0.x |

The owner's instruction was "whatever version you think is best, same with mod loader," so
this is a decision on record rather than an inherited default. The reasoning is below so
that a future session can overturn it on grounds rather than on taste.

---

## Minecraft changed how it is versioned, in 2026

**This is the single most important fact in this document, because it invalidates almost
every modding tutorial written before 2026 and most of what a model trained earlier
"knows".**

Minecraft used `1.x.y` from 2011 until 2026. It now uses **`year.release.patch`**:

- **`26`** — the year, 2026.
- **`.1`** — which content drop of that year. Mojang moved from one big annual update to
  **three to four quarterly "game drops"**, and `1.21.50`-style numbers had stopped
  carrying meaning.
- **`.0`** — patch/hotfix.

So the line runs `1.21.x` → **`26.1`** → `26.2` → `26.3` → `26.4`, and there is **no
`1.22`**. If you find yourself writing `1.22`, you have been misled by a stale source.

| Version | Name | State as of Aug 2026 |
|---|---|---|
| 26.1 | Tiny Takeover | released; first year-versioned drop |
| **26.2** | **Chaos Cubed** | **released June 2026 — current stable, our target** |
| 26.3 | *(third drop)* | snapshots since 23 June 2026, full release expected Q3 2026 |

### Why target 26.2 and not 26.3

26.3 is *imminent* — snapshots have been out since June and release is expected within the
quarter. Targeting it now would mean writing against a moving target for no benefit, since
this mod has no content yet and nothing to lose by starting on stable.

**The migration is expected, not feared.** Start on 26.2, and plan to move to 26.3 shortly
after it stabilises. That is a deliberately cheap move if `ARCHITECTURE.md`'s layering is
respected — the whole reason registration is centralised is so a version bump touches a
small number of files.

### Why not stay on 1.21.x

It is tempting: 1.21 accumulated 16,000+ mods and enormous tutorial coverage. But it is now
**two version-scheme generations behind**, its ecosystem is in the middle of moving, and
every month spent there is a month of migration debt. A new mod with no dependency graph
has no reason to start in the past.

---

## Why NeoForge

| Option | Verdict |
|---|---|
| **NeoForge** | **Chosen.** The successor to Forge and where the ecosystem went after the 1.20.5 split. Rich, batteries-included API — capabilities, events, registries, datagen, networking — which suits a content-and-systems mod. Version-matches the game, so `26.2.x` targets 26.2 and there is no mapping table to memorise. |
| Fabric | Excellent and lighter, faster to update, but leans on Fabric API + a constellation of libraries for things NeoForge has built in. Better for small/performance mods; more assembly required for a broad content mod. |
| Forge | Legacy. Recommendations in 2026 are consistently "NeoForge unless a pack explicitly needs Forge." |

**If the mod turns out to be small and mechanical rather than broad and content-heavy,
Fabric is the better call and switching early is cheap.** That is a real fork in the road
and it is worth revisiting once we know what we are building.

### The version-matching thing is genuinely useful

Old Forge required knowing that "Forge 47.x" meant "1.20.1". NeoForge's version now tracks
the game's, so **NeoForge `26.2.a.b` targets Minecraft `26.2`**. When you see a NeoForge
version whose leading number is not the Minecraft version you expect, you are on the wrong
branch. (Note that in the 1.21 era this scheme read as `21.x` — e.g. NeoForge `21.11` for
Minecraft `1.21.11` — which is the same idea one scheme earlier.)

---

## Toolchain

**Java 25.** Verified against the real toolchain, not a search result: MDG's
`createMinecraftArtifacts` requests `languageVersion=25` for Minecraft 26.2 and fails
outright without it.

> **This document previously said Java 21, and was wrong.** Every pre-2026 source says 21,
> because 21 was correct for the entire 1.21 line. The version-scheme change came with a
> Java bump. See `docs/LESSONS.md` #6 — it is the second time a confidently-remembered
> platform fact about this project turned out to be a version behind.

This image ships JDK 21, so `settings.gradle` applies the **foojay resolver** and Gradle
downloads a JDK 25 toolchain on demand. Without it the build fails with
*"Cannot find a Java installation ... matching {languageVersion=25}"*.

`core/` still compiles under 21 (`tools/check_all.sh` uses the system `javac` directly) —
it is loader-independent and deliberately stays on the lower baseline so the fast offline
checks need no toolchain download.

**ModDevGradle (MDG), not NeoGradle.** MDG 2.0.x is the modern plugin and the one to use
for a new project; NeoGradle (7.1.x) is the older path, still maintained, mostly of
interest to projects migrating. Both were current as of mid-2026.

### `gradle.properties` shape

`VERIFY:` — the *names* below are stable, the *values* are the part to check against the
NeoForge version index at setup time. Do not paste a build number out of this document and
assume it exists.

All values below are **verified against the real registries**, not remembered:

```properties
minecraft_version=26.2
neo_version=26.2.0.67       # confirmed <latest> AND <release> on maven.neoforged.net
mod_id=interregnum
mod_name=INTERREGNUM
mod_version=0.1.0
mod_license=MIT
mod_group_id=com.cadykaya.interregnum
mod_authors=cadykaya
```

| Thing | Value | How it was verified |
|---|---|---|
| Minecraft | `26.2` | boots; `Done (0.28s)` on a dedicated server |
| NeoForge | `26.2.0.67` | `<latest>`/`<release>` in maven-metadata; pom returns 200 |
| ModDevGradle | `2.0.144` | `<latest>` on the NeoForged maven (a search said 2.0.141) |
| Java | `25` | MDG demands `languageVersion=25`; build fails without it |
| Gradle | `8.14.3` | shipped in this image; works |
| Parchment | **none** | `parchment-26.2` does not exist on maven.parchmentmc.org yet |
| resource pack format | `88` | `SharedConstants.RESOURCE_PACK_FORMAT_MAJOR` |
| data pack format | `107` | `SharedConstants.DATA_PACK_FORMAT_MAJOR` |

**`mod_id` is effectively permanent, and it is decided: `interregnum`.** It is the
namespace on every resource location, every texture path, every registry key, every
save-game reference. Renaming it after a world has been saved orphans every block placed
in it. Locked by the owner alongside the subject — see `WORLD.md`.

Constraints: lowercase, `[a-z0-9_]`, no leading digit, and short — it prefixes every asset
path you will ever type.

---

## Where the truth actually lives

When this document and the machine disagree, the machine is right. In order of authority:

1. **The NeoForge sources in the Gradle cache.** After one successful setup:
   ```sh
   find ~/.gradle/caches -name 'neoforge-*-sources.jar'
   ```
   This is the primary source for any API question. Reading it beats asking anyone.
2. **The generated run configs and the `runData`/`runClient` output.**
3. `docs.neoforged.net` and the NeoForge Discord — when the network allows.
4. This document.

### The network in the dev sandbox

The container that produced these docs reaches a search engine but **the egress proxy
blocks `neoforged.net` and `docs.neoforged.net`**. Gradle's own dependency fetching may or
may not be permitted depending on the environment's network policy — Maven Central and the
NeoForge maven are both required for a build to resolve.

**If a Gradle build fails to resolve dependencies, check the proxy before debugging the
build script:**

```sh
curl -sS "$HTTPS_PROXY/__agentproxy/status"
cat /root/.ccr/README.md
```

Never disable TLS verification and never unset `HTTPS_PROXY` to work around it. A build
that only succeeds with security off is not a build.

---

## Things 26.x renamed, and where each one bit

This table exists because the facts in it were already known to this repository and got
re-broken anyway. Each row was learned once, written into a commit message or a comment in
one file, and then not found again by the next person who needed it — which for a solo
autonomous build means me, an hour later.

A rename is the worst kind of churn to carry in your head, because the old name is not an
error you can see. In Java it is a compile failure and you find out immediately. In a
**command string handed to a server** it is a runtime rejection: one line in a log, the
command silently does nothing, and every assertion downstream keeps running against a
world that is not the one you asked for.

| Was | Is, in 26.x | Where it bit |
|---|---|---|
| `doDaylightCycle` | `advance_time` | `deicide_check.sh` — the sun would not stop |
| `randomTickSpeed` | `random_tick_speed` | `exodus_check.sh` — vanilla's growth never switched off, and the failure message described a world state that had been rejected two minutes earlier |
| *(whole gamerule set)* | snake_case, behind a registry | assume every rule you remember is renamed |
| `ChunkPos#asLong` | `ChunkPos.pack(x, z)` | `LeakEvents`, and it is a record now — `x()`/`z()` |
| `ServerPlayer#getServer()` | gone — take it off the level | the deicide handler |
| `ItemStack.is(Item)` | takes a `Predicate<Holder<Item>>` | the heart pickup |
| `LivingEntity#displayClientMessage(Component, boolean)` | gone — `ServerPlayer#sendSystemMessage(Component)` | `FerryKeelBlock`. It compiles as a missing symbol rather than a wrong one, so this is the cheap kind: `javac` finds it. Worth the row anyway because the replacement is on a *different type* — you have to narrow to `ServerPlayer`, which a block's `useWithoutItem` hands you as a plain `Player` |
| `net.minecraft.world.entity.projectile.*` for the concrete projectiles | **split into sub-packages** — `projectile.arrow.AbstractArrow`, `projectile.hurtingprojectile.{Fireball,SmallFireball,LargeFireball,WitherSkull,DragonFireball}`, `projectile.throwableitemprojectile.*`. The base `projectile.Projectile` did **not** move | `QuellEvents`, which only needed the base type and so was never bitten. The row is here for whoever reaches for `SmallFireball` next and imports the 1.21 path from memory |
| `BiomeSpecialEffects` fog/sky/water-fog/ambient-sound fields | **moved out of the biome** into `EnvironmentAttributes` — `SKY_COLOR`, `FOG_COLOR`, `WATER_FOG_COLOR`, `AMBIENT_SOUNDS`, `BACKGROUND_MUSIC`, set with `Biome.BiomeBuilder#setAttribute` | `ModBiomes`. The record still exists and still compiles, carrying **water and vegetation colours only** — so a biome ported from any pre-26 guide builds cleanly and has no sky |

`tools/renames_check.py` enforces the shell half of this table on every push, because a
dead gamerule name in a `COMMANDS` string is invisible until a check goes red for the
wrong reason. It cannot enforce the Java half and does not try: `javac` already does.

---

## Standing version-churn policy

Because this is the thing that will break, repeatedly:

- **Never trust a remembered API signature.** Not from a tutorial, not from a model, not
  from this doc set. Read the sources.
- **A tutorial dated before 2026 is describing a different versioning era** and probably a
  different registration API. Its *concepts* may still hold; its code will not.
- **Pin exact versions in `gradle.properties`, never ranges, for the game and loader.** A
  range means a build that succeeded yesterday can fail today with no diff.
- **When bumping versions, bump alone.** One commit that does nothing but move the version
  and fix what broke. Mixed with feature work, a migration becomes impossible to bisect.

---

## Sources

- [Minecraft new version numbering system](https://www.minecraft.net/en-us/article/minecraft-new-version-numbering-system) — the official announcement of `year.release.patch`
- [Game drop – Minecraft Wiki](https://minecraft.wiki/w/Game_drop) — quarterly drop cadence
- [Java Edition version history – Minecraft Wiki](https://minecraft.wiki/w/Java_Edition_version_history)
- [Third Drop 2026 – Minecraft Wiki](https://minecraft.wiki/w/Third_Drop_2026) — 26.3 snapshot timeline
- [NeoForge for Minecraft 26.1](https://neoforged.net/news/26.1release/) — version-matching scheme
- [Versioning | NeoForged docs](https://docs.neoforged.net/docs/gettingstarted/versioning/)
- [Getting Started with NeoForge | NeoForged docs](https://docs.neoforged.net/docs/gettingstarted/) — JDK 21
- [neoforged/ModDevGradle](https://github.com/neoforged/ModDevGradle)
- [Forge vs NeoForge in 2026](https://space-node.net/blog/forge-vs-neoforge-modloader-comparison-2026)
