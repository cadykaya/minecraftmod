# Architecture

> **Provenance: REFERENCE + DOCTRINE.** Engine behaviour is reference; the layering is a
> decision made here, chosen so that a version bump touches few files — see
> [`PLATFORM.md`](PLATFORM.md), which promises exactly that.
>
> **`VERIFY:` markers flag API specifics.** Class and method names churn hard. The
> *concepts* below are stable across every version this project will plausibly target; the
> *identifiers* must be read from the sources.

---

## The one rule that prevents the most crashes

> **The dedicated server does not have a client. Touching client-only code from shared code
> crashes it, and it crashes at *class load*, not at the call.**

This is the single most common way a mod that "works fine in singleplayer" is broken on a
server, and the class-load part is what makes it vicious: a shared class that merely
*mentions* a client type in a field, a parameter, or a `catch` can blow up before your
method ever runs. Moving the call behind an `if` does not help. **The reference must not
exist in a class the server loads.**

The defence is structural — the package layout below — not vigilance.

### Two different "sides", and conflating them is its own bug class

| | What it is | How you ask |
|---|---|---|
| **Physical side** | which *jar* is running: dedicated server, or the client | a dist/loader query, at startup |
| **Logical side** | which *thread/world* you are on: server logic, or client copy | `level.isClientSide` |

A singleplayer client runs **both logical sides in one process.** So:

- **Physical** answers *"does client code exist here?"*
- **Logical** answers *"am I allowed to change the world here?"*

**Game state changes on the logical server. Always.** The client's copy is a prediction; a
change made there is cosmetic, desyncs, and is overwritten on the next sync. If you find
yourself mutating a block or an inventory inside `if (level.isClientSide)`, that is the bug.

---

## Package layout

```
com.cadykaya.<mod_id>/
  <ModId>.java              entrypoint. registration wiring only, no game logic
  registry/                 ALL DeferredRegister holders. The version-bump blast radius.
    ModBlocks, ModItems, ModEntities, ModBlockEntities,
    ModCreativeTabs, ModSounds, ModDataComponents, ModAttachments
  content/                  what the mod IS. Shared, side-agnostic.
    block/  item/  entity/  recipe/
  system/                   cross-cutting mechanics that are not one block
  net/                      packet payloads + handlers
  config/                   config definitions
  worldgen/                 codecs and feature/structure classes (see WORLDGEN.md)
  data/                     datagen providers. Dev-time only. (see DATAGEN.md)
  client/                   *** EVERYTHING CLIENT-ONLY, AND NOTHING ELSE ***
    render/ screen/ model/ ClientEvents.java
```

### Why `client/` is a hard boundary

**One package, one rule: nothing outside `client/` may import anything inside it, and
nothing outside it may import a client-only engine type.**

That makes the rule *greppable*, which makes it enforceable, which is the difference
between a convention and a guarantee. A test that greps for client imports outside
`client/` is a few lines and catches the crash class before it ships. See
[`VERIFICATION.md`](VERIFICATION.md).

### Why all registration lives in `registry/`

Registration APIs are the thing that changes most between versions. Concentrating them
means a version bump is **a handful of files**, not a repo-wide sweep. This is the
architectural promise `PLATFORM.md` makes when it says migration is cheap — it is only true
if this boundary is kept.

Content classes stay in `content/` and are referenced *from* the registry; the registry
holds the wiring, the content holds the behaviour.

---

## Registration

**VERIFIED against 26.2.0.67**, and not from memory: every shape below is one this repo
compiles and CI boots a server on. See `registry/ModBlocks.java`, `ModItems.java`,
`ModComponents.java`, `ModAttachments.java`.

Registration is **deferred**: you declare what you want during construction and the loader
creates it when the registry is ready. The pattern is one `DeferredRegister` per registry
type, each held in a class in `registry/`, each explicitly registered on the mod event bus
from the entrypoint.

**The rules that matter and do not change:**

- **Nothing is usable during construction.** A registry object is a *handle*; resolving it
  while classes are still loading gives you null or throws. Store the handle, resolve late.
- **Registration order between registries is not yours to choose.** If A needs B at
  creation time, that is a design error, not an ordering puzzle. Pass a supplier.
- **Every registry name is `<mod_id>:<path>`, lowercase `[a-z0-9_/.-]`.** These strings end
  up in save files. A rename after a world is saved orphans everything placed in it — see
  `PLATFORM.md` on why `mod_id` is effectively permanent.
- **Static initialisers are a trap.** A `static final` referencing a registry object binds
  at class load, which may be before the registry exists.

---

## Events

NeoForge is event-driven, and there are **two buses**, which is a real and load-bearing
distinction:

| Bus | Fires | For |
|---|---|---|
| **Mod bus** | during startup, per-mod, ordered | registration, datagen, client setup, entity attributes |
| **Game bus** | during play | ticks, player actions, damage, world load |

Putting a handler on the wrong bus means it silently never fires. **This is the second most
common "my code doesn't run" cause after the client/server split**, and it produces no
error at all — the game just behaves as though the handler is not there.

> **Corrected against 26.2.0.67.** `@EventBusSubscriber` no longer takes a `bus` parameter
> — its only members are `value()` (a `Dist[]`) and `modid()`. **Which bus a handler lands
> on is inferred from the event type**, so `GatherDataEvent.Server` routes to the mod bus
> on its own and nothing needs to say so. Every pre-2026 tutorial writes
> `bus = Bus.MOD`, and that no longer compiles. The distinction between the buses is still
> real and still matters; only the way you *declare* it changed.

**Handler discipline:**

- Game-bus handlers run **very often**. A hot handler doing allocation or a lookup per tick
  per entity is a framerate bug that will be blamed on something else.
- Client-only handlers live in `client/`, on a client-only registration path. No exceptions.
- Prefer a **capability/attachment** or a block entity over a global tick handler that
  scans the world. Scanning is how mods get a reputation.

---

## State

**VERIFIED against 26.2.0.67.** This was the area flagged as most reworked, and the
rework is real: **capabilities are gone as the place you hang arbitrary data.** Data
attachments replaced them, and both halves of the modern shape are exercised here.

**Attachments** (data on an entity, chunk or level) are a registry like any other:

```java
DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MOD_ID);
// read back with chunk.hasData(TYPE) / chunk.getData(TYPE)
```

`registry/ModAttachments.java` holds the mod's one attachment — the per-chunk record of
player-placed blocks that the unraveling, the ferry and three of the four god-worlds all
consult. It is the single most load-bearing piece of state in the mod.

**Data components** (data on an item stack) are a separate registry, and the two are
easy to confuse because both are "arbitrary data on a thing":

```java
DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MOD_ID);

DataComponentType.<String>builder()
        .persistent(Codec.STRING)                    // survives save/load
        .networkSynchronized(ByteBufCodecs.STRING_UTF8)  // reaches the client
        .build();
```

`registry/ModComponents.java` holds the mod's one component: which of the dead god's
letters a sealed letter is. **A component that is `persistent` but not
`networkSynchronized` exists on the server and is invisible to the client**, which for a
tooltip is a bug that looks like a rendering problem.

**Where state belongs, in order of preference:**

1. **On the object** — a block state property, an item's data component. Free serialisation,
   free sync, no lifecycle to manage.
2. **A block entity**, when a block needs more than block-state properties can hold.
3. **An attachment/capability**, for data hung on an entity, chunk, or level.
4. **Saved level data**, for genuinely global state.
5. **A static field** — **essentially never.**

### On static state, specifically

> A `static` field holding a `Level`, a player, a block entity, or anything derived from
> them is a memory leak and a bug. It survives world unload, it is shared between the
> integrated server and the client in singleplayer, and it is shared between *simultaneous
> worlds* on a server.

The symptom is always the same and always confusing: works in the first world, misbehaves
after you leave and rejoin. If you need per-world state, it goes in the world.

### Block state properties are cheap but finite

Every combination of properties is a **distinct state object created at load**, and it
multiplies. Four boolean properties is 16 states; add a 16-value enum and it is 256. This
is fine until it is not. Anything with many values or that changes rapidly belongs in a
block entity, not in the blockstate.

### Ticking is a budget

A ticking block entity costs every tick, forever, for every instance a player has ever
placed. **Do not tick when you can react.** If a machine only needs to act when its
inventory changes, react to the change. A thousand idle ticking machines in a base is a
real and common mod-caused framerate collapse.

---

## Networking

**VERIFIED against the 26.2.0.67 sources**, and flagged honestly: **this mod does not
send a packet yet.** Everything below was read out of `PayloadRegistrar`,
`RegisterPayloadHandlersEvent`, `IPayloadHandler` and `CustomPacketPayload` rather than
compiled here, so it is a weaker claim than the two sections above — where the shapes are
ones CI boots a server on.

A payload is a record carrying a `Type` (an id) and a `StreamCodec`, registered on the
mod bus during startup:

```java
@SubscribeEvent
static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
    PayloadRegistrar registrar = event.registrar("1");    // a version string
    registrar.playToServer(MyPayload.TYPE, MyPayload.CODEC, MyPayload::handle);
}
```

The directions are named rather than inferred — `playToServer`, `playToClient`,
`playBidirectional`, and `commonToServer` / `commonToClient` for the configuration
phase. Play payloads take a `StreamCodec<? super RegistryFriendlyByteBuf, T>`; common
ones take a plain `FriendlyByteBuf`, because registries are not available that early.

A handler is `void handle(T payload, IPayloadContext context)`. **The context's
`player()` is who to trust and the packet is not**, and `enqueueWork` is how you get back
onto the main thread — a handler runs on the network thread, so touching the world
directly from one is a race, not a shortcut.

Packets are typed payloads with an id, a codec, and a handler, registered during startup.
Two rules, both security-shaped:

> **Never trust a client packet.** The client is user-controlled. Re-validate everything on
> the server: that the player is who they claim, is in range, has the item, has the
> permission, and that every number is in bounds. A packet that says "give me 64 diamonds"
> must be treated as an assertion by an adversary, because on a public server it is one.

> **Handle on the main thread.** Payload handlers arrive on a network thread. Touching the
> world from there is a race that will corrupt a save eventually. Enqueue the work.

Keep payloads small and send them rarely. A packet per tick per player is a server bill.

---

## Config

Three scopes, and choosing wrong causes desyncs:

| Scope | Lives | For |
|---|---|---|
| **Startup** | resolved before registration | things that change what exists |
| **Server/common** | with the world, **synced to clients** | gameplay balance |
| **Client** | on the player's machine | rendering, sounds, UI |

**Anything affecting gameplay is server config.** If a client can change a value the server
uses, that is a cheat vector and a desync source at the same time.

---

## Layering, and what it buys

```
        registry/        <- version churn concentrates here
            |
  content/  system/  worldgen/     <- side-agnostic game logic; the actual mod
            |
     net/     config/              <- boundaries
            |
        client/                    <- one-way: imports inward, never imported
```

**One-way dependencies.** `content/` must never import `client/`. `client/` may import
whatever it likes. The mod entrypoint wires things together and holds no game logic.

Kept honestly, this means:

- a **version bump** touches `registry/` and whatever the release notes broke, not the mod
- **server crashes from client leakage** become a grep, not a bug report
- **datagen** can regenerate resources without dragging rendering into the build
- a future **loader switch** (NeoForge → Fabric, per `PLATFORM.md`'s open fork) rewrites
  `registry/`, `net/`, `config/` and the event wiring — while `content/` and the entire art
  pipeline survive untouched

That last one is the real payoff, and it is worth keeping the boundary clean for even if we
never use it.
