# Reachability

> **Every registered thing, and how a player touches it.**
>
> Enforced by `tools/reachability_check.py`: every block, item and entity this mod
> registers must appear below, and nothing may appear that is not registered. Adding
> content without saying how somebody reaches it fails the build.

## Why this file exists

Three separate systems in this mod were **built, verified, green in CI, and unreachable in
play** — found one at a time, by hand, on three consecutive passes:

- **the Haunt.** The scene, the gate and the once-only rule all worked. Nothing but
  `/interregnum haunt dream` could reach them, so the mod's best beat could not happen.
- **the sealed letter.** Four letters written, loaded, validated, checked. The item that
  carries them was `registerSimpleItem` and did nothing when used.
- **the warning steles.** Block, texture and model since the chapter-0 art pass, and no
  text on any of them anywhere — while a shipped line of the shrine-keeper's told players
  they were readable.

Every one of them was green. That is the point: **a check that covers a path says nothing
about whether anything else reaches it.** `haunt_check.sh` drove the command seam and was
right to; `mail_check.sh` read the letters from the data and was right to. Coverage of a
mechanism is not coverage of a route to it, and no test this project knows how to write
would have told the difference.

So the route is written down instead, and the writing is checked.

## How to read the table

**Reached by** is what a player does. It is a fact about the shipped game, not an
intention — if the only way to reach something is a command, that is what the column says.

**Status** is one of:

| | |
|---|---|
| `PLAY` | a player can reach it by playing |
| `OP` | only an operator can, through a command |
| `BLOCKED: <question>` | it waits on an answer in HANDOFF's "Waiting on owner" |
| `SCENERY` | it is a block you look at or build with, and that is all it is for |

`OP` with no blocker named is the state that produced all three bugs above. It is allowed —
sometimes an affordance genuinely has not been built yet — but it must be *deliberate*, and
writing it here is what makes it so.

## Blocks

| Registered | Reached by | Status |
|---|---|---|
| `shrine_stone` | generated in shrines; mined, placed, built with | `SCENERY` |
| `shrine_stone_carved` | generated in shrines; mined and built with. **Its script cannot be read** — see below | `BLOCKED: reading is dangerous` |
| `warning_stele` | generated at shrines; **used to read its notice**, if there is light | `PLAY` |
| `warden_statue` | generated at shrines; opens its eyes at the deicide and posts a Warden | `PLAY` |
| `ferry_keel` | crafted and placed by the player. **Empty hand: inspect the hull** against all four crossings. **A letter in hand: sail that crossing** | `PLAY` |

## Items

| Registered | Reached by | Status |
|---|---|---|
| `god_heart` | found in shrine loot, once per world; **picking it up is the deicide** | `PLAY` |
| `sealed_letter` | **given by a shrine-keeper**, once the god is dead — the returned post they have been holding. Used to read the letter it carries, and **held out to a ferry keel to sail that god's crossing** | `PLAY` |
| `clast` | scattered at the crater and at shrines when the god shatters. **Held out to a shrine-keeper** it is a rite: if the villages think well enough of you, it attunes you and is consumed | `PLAY` |

## Entities

| Registered | Reached by | Status |
|---|---|---|
| `shrine_keeper` | generated at every shrine; **right-click to talk** | `PLAY` |
| `warden` | posted by a woken statue; **right-click to talk**, walks a fixed beat, takes statements | `PLAY` |

## Systems with no registered thing of their own

Not covered by the check — there is no id to key them to — but they are the other half of
the same question, and they are why "Waiting on owner" is as long as it is.

| System | Reached by | Status |
|---|---|---|
| the unraveling, the exodus, attrition | happens to the world on its own | `PLAY` |
| the deicide, the crater, the statues waking | the heart | `PLAY` |
| dialogue, regard, the villages, the Wardenate's opinion | talking to people | `PLAY` |
| the Haunt | **sleeping**, after the death, as its killer | `PLAY` |
| the clast pool | the death, and finding shrines | `PLAY` |
| the ferry's **crossing** | `interregnum ferry sail` | `BLOCKED: how the ferry is told where to go` |
| the **return** leg | `interregnum ferry home` | `BLOCKED: same` |
| eight **spells**, four schools | `interregnum cast <spell>` | `BLOCKED: how a spell is cast` |
| a Warden **citing** you | nothing | `BLOCKED: what a Warden can cite you for` |
| the god-worlds' **under- and far-layers** | nothing | `BLOCKED: each god's portal logic` |

## The shrine's carved script

`shrine_stone_carved`'s own javadoc says it carries *"a band of the dead god's script"*, and
nothing can read it. That is deliberate rather than another oversight, and it is the one
case where making it readable would be the wrong move: `WORLD.md` marks the whole reading
lane **[PROPOSED]** —

> raw god-script (letters, shrine inscriptions) read without transcription at the ferry's
> desk *marks* the reader — visions, afflictions, manifestation exposure. Knowledge-as-hazard;
> the codex desk is the safe path.

Shipping plain readable inscriptions now would quietly settle that question in the safe
direction, which is the owner's to settle. The steles were different: they are civic
notices posted for the public, nothing proposes a hazard for reading one, and a shipped
line of dialogue already promised they could be read.
