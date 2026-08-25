# INTERREGNUM — the world

> **Provenance: DOCTRINE**, workshopped with the owner across one long session. Status tags
> below are adopted from the owner's CGTF design bible and mean the same things here:
> **[LOCKED]** canon unless deliberately reopened · **[PROPOSED]** strong direction, flexible
> until prototyped · **[NEEDS PLAYTEST]** theory cannot settle it · **[WORKSHOP]** actively
> being decided with the owner · **[EXAMPLE ONLY]** illustration, not canon.
>
> Per [`AESTHETIC.md`](AESTHETIC.md): nothing here ships until it survives question 7 in
> writing. The audit is at the bottom and is part of this document, not an afterthought.

---

## The premise

**[LOCKED]** An *interregnum* is the gap between reigns.

Every world is held coherent by a god. Physical law is not a fact; it is a god's standing
attention. The overworld's god was an **isolationist**: it banned magic, sealed the ways
between worlds, hid its heart in anonymous shrines, and stopped answering its family's
letters — all as protection.

The modpack opens with the player killing it. **By accident. By looting a chest.**

Thirteen years of Minecraft trained every player to take the shiny thing. The warnings on
the shrine read as ruin-flavour until afterwards, when they read as exactly what they were.
The tutorial indicts the player's own learned behaviour; no cutscene, no prompt, no undo.

Without its god the overworld begins to unravel. The remaining gods — each holding their
own world — are reachable only by the dead god's sealed mail-ferry, and the route to them
is its unanswered correspondence. You killed them. You are also the only one carrying
their mail.

### Vanilla's rules were the dead god's policy

**[LOCKED]** — the retcon that makes this Minecraft-shaped rather than generic fantasy:

| Vanilla "gamey" rule | What it always was |
|---|---|
| mobs burn at dawn | incineration policy |
| phantoms punish sleeplessness | a citation for a sleep-code violation |
| the height limit | permitted airspace |
| bedrock | the foundation slab of the maintained zone |
| Nether and End | the god's own under-layer and far-layer (see world-systems) |

Never stated in-game. The player just keeps finding the seams. **Never state the theme.**

---

## Chapter structure

**[LOCKED]** The mod is dormant until the deicide. **Chapter 0 changes no mechanics.**

### Chapter 0 — archaeology

Vanilla systems, untouched. The mod adds only *content*: shrines, warning steles, sealed
sanctums, and **Warden statues** — inert, decorative, screenshot-bait. Players settle in,
build, and absorb the structures as scenery. The heart sits in shrine loot, warm and
golden, next to genuinely good treasure.

### The death

Server-wide, the moment the heart is taken: the sky changes for everyone, a crater forms,
**every Warden statue on the server opens its eyes**. The mod never announces who did it.
There is simply a player online who has gone quiet.

**[NEEDS PLAYTEST]** Trigger reliability — a server of pure builders might never loot a
shrine. Candidate nudges (subtle heart-ward compass pull; shrine density) to be tuned. A
config escape hatch exists but the *default* experience must be found, not scheduled.

### Chapters 1..N — the clock is your progress

**[PROPOSED]** (owner's sentence completed; veto openly): **the overworld degrades one band
per chapter, and chapters advance on the server's own milestones** — the death, the first
ferry voyage, each god's letter delivered. Not wall-clock. Exploring costs Earth; every
homecoming, home is worse, *because you went*. The Deltarune/KH rhythm: the hub decays
between worlds.

Unraveling bands, escalating: sky and day-length wrongness → rule loosening (crops
uncertain, gravity hiccups at thin places) → geography fraying at the edges.

**[LOCKED] The unraveling never destroys player-placed blocks.** It warps the world, never
the player's work. Visual weathering of builds at most, always reversible. The moment the
apocalypse eats a player's house, the design has turned hateful and lost them.

**[BUILT]** Bands 1 and 2. Band 1 is grass and flowers going over, and **only within sight
of the crater or a shrine** — for one chapter the wrongness has an edge you can stand
outside of, which is what makes crossing it later mean anything. Band 2 drops that boundary
and turns structural: grass to coarse dirt, stone to cobble, cobble to gravel, canopies
thinning. The guarantee above is enforced by placement tracking, not by a block whitelist.

**[LOCKED — owner delegated; decided here.]** Bands 3 and 4 were empty because
"the ways are open" and "geography frays at the edges" are not block-for-block
substitutions. They are not. **Neither of them is a conversion table at all**, and that
was the thing blocking them.

**Band 3 — EXODUS: the overworld starts leaking other gods' LAW.**

Not their blocks. Their *rules*. The dead god's policy was what held the systems apart —
the Isolation was a policy, not a wall — and with nobody enforcing it, patches of the
overworld begin obeying somebody else's law. A hollow where nothing makes a sound. A
slope where dropped things fall too slowly. A meadow where crops visibly move. A ruin
where your tools age while you stand in it.

Each patch is shaped like exactly one god, and it is **the same law you will meet in
their world**. So band 3 is reconnaissance: the apocalypse is teaching you the
curriculum. By the time the ferry's checklist tells you the Quiet One's crossing forbids
note blocks, you have already stood in a silent hollow and worked out why.

This is also why band 3 had to wait for a platform fact: 26.2 moved dimension rules into
a namespaced `attributes` map (see [`WORLDGEN.md`](WORLDGEN.md)), so "this region obeys
the Quiet One" is a set of declared attributes rather than a pile of special cases.

**Band 4 — ATTRITION: the world forgets what it was.**

Attrition is loss by repeated small subtraction, and what gets subtracted is
**distinction**. Biome-specific detail reverts to its plainest equivalent. Your forest
stops being a forest — not destroyed, *generalised*. Ores return to stone. Foliage goes
to grass and then to nothing in particular. The world becomes a place rather than a
particular place.

That is what a dead god's world would actually do, because **biomes were its taxonomy**.
Nobody is maintaining the categories any more.

Two consequences fall straight out of it, which is how you know it is the right grammar:

- **It frays where nobody tends.** Regions people visit and keep hold their definition.
  This makes the "take the job" ending literal rather than thematic — holding the world
  together shrine by shrine is exactly the counter-move, and anchor-rites are how you do
  it. The apocalypse becomes a thing you can argue with.
- **The Hearth-Turner is the obvious ally**, and it falls out of the fiction rather than
  being assigned. A god whose law is keeping every past is the natural answer to a world
  losing its own. Ash can tell you what a place *was*.

**The LOCKED guarantee still holds and is enforced the same way.** Attrition never
touches a player-placed block; the placement tracking that protects bands 1 and 2
protects this one unchanged. It generalises the world's own terrain, never anyone's
work.

---

## World-systems

**[LOCKED]** Each god holds a **system of connected dimensions**, not a single dimension.
The grammar, proven by vanilla itself (Overworld + Nether + End was always a three-layer
system under one god):

> **surface · under-layer · far-layer**, joined by that world's own portal logic.

Each god's system expresses its law at every layer. Travel between systems is only by
ferry; travel within a system is by its native portals.

### The pantheon — **four, and everyone calls them something different**

**[LOCKED — owner delegated the roster; decided here.]** The count stays at **four**.
Adding a fifth god adds a world, a school, a letter and an ending branch, and buys
nothing the fourth does not already buy. What the roster needed was not more gods.

**The decision: the pantheon inherits the four-voices doctrine.** The Theoclast already
has one true name and three institutional ones (`Theoclast` / `Usurper` / *saint* /
`Executor`), and that table does more worldbuilding than any lore book could. The gods
get the same treatment — and the payoff is that **the dead god's letters do not use the
names you have been using all game.**

| God | Law | In the letters | On Warden dockets | Villagers say |
|---|---|---|---|---|
| **The Verdant** | growth | **Rill** | `SUBJECT: GREEN AUTHORITY` | the Long Green |
| **The Anchorite** | weight | **Ballast** | `SUBJECT: MASS AUTHORITY` | Old Heavy |
| **The Hearth-Turner** | time | **Ash** | `SUBJECT: TEMPORAL AUTHORITY` | the Turning |
| **The Quiet One** | silence | *(none)* | `SUBJECT: UNRESPONSIVE` | *they will not say it* |

You spend a hundred hours calling it The Verdant. Then you open the mail you are
carrying and it says **"Rill —"**, and you understand for the first time that you are
holding a stranger's correspondence about people you have never met.

**The Quiet One has no name in the letters, and that is the whole character.** Three
letters open with a name. The fourth opens `To —`. Whether the dead god never got close
enough to have one, or had one and struck it out, is never answered; the letter itself
is the only evidence and it is ambiguous on purpose. "The one who never wrote back"
stops being a fact about the Quiet One and becomes a question about the dead god.

**The dead god's own name is nowhere.** No voice has one. The throne is vacant and so is
the word for it — which is what an *interregnum* is.

Each world's questline opens by **delivering that god's letter**. Their reaction to the
news is their characterization. Identity AND relation come from the same structure: the
pantheon is a family that stopped speaking.

**The two undefined relationships, settled** (the Anchorite and the Quiet One were
already locked):

- **The Verdant / Rill — the one who covered.** During an older crisis it took over the
  overworld's duties and never quite handed them back; the estrangement is professional
  rather than personal, which is worse. Delivered its letter it is immediately
  *defensive*: it assumes it is being blamed, before anyone has said anything.
- **The Hearth-Turner / Ash — the one who kept every version of the argument.** A god
  whose law is memory has never let a grievance become past tense. It holds the family's
  entire falling-out, in order, and will show you. That makes it the exposition god, but
  earned: it is not telling you because the plot needs it told, it is telling you because
  it has never been able to stop.

---

## Magic

**[LOCKED] Every spell is a world-verb.** No damage buttons with particle effects. A spell
changes the world's state — blocks, physics, capabilities — and its combat use falls out of
its world use, never the reverse.

**[LOCKED] The overworld ban is *correct*.** With the god dead, all overworld casting draws
on the corpse — the residue still holding the world together. Heavy casting visibly frays
its surroundings. The Wardens' law is right, and the player can *discover* it is right.
Off-world, living gods replenish what casting spends: legal, sustainable. The ban forces
travel by law *and* economics.

### Schools, one per god, learned in their worlds

Samples — all implementable on verified hooks (entity attributes, spawn/ability events,
block-conversion registries). Full kits are design-phase work.

- **Weight** (Anchorite): *Lighten* — shared low-gravity zone, mobs float too · *Drop-forge*
  — crafting by crushing · *Loft* — make a small structure weightless and carry it · *Moor*.
- **Verdancy** (Verdant): *Bridgeroot* — grow a living span toward your gaze, real
  persistent blocks · *Hedge* · *Graft* · *Wildgrowth* — and in the Verdant's own world,
  accelerating growth is a *hazard*.
- **Silence** (Quiet One): *Hush* — true no-sound zone: sculk blind, mobs cannot alert,
  **a creeper that cannot hiss cannot detonate** · *Still* — freeze primed TNT / falling
  block mid-state · *Quell* — strip one ability (a blaze that cannot ignite) · *Held-breath*.
- **The Turning** (Hearth-Turner): *Weather* — age blocks: instant mossy/cracked/oxidized —
  **magic as a builder's palette** · *Rewind* — repair by un-aging · *Ripen* · *Rot*.

**Reuse note [LOCKED]:** the block-aging registry powering the Turning **is the same
system that runs the unraveling.** One mechanism; a school and an apocalypse.

### Reading is dangerous

**[PROPOSED]** The Thaumcraft lane: raw god-script (letters, shrine inscriptions) read
without transcription at the ferry's desk *marks* the reader — visions, afflictions,
manifestation exposure. Knowledge-as-hazard; the codex desk is the safe path.

---

## The deicide inheritance

**[LOCKED]** The god's power enters its killer. An ordinary Minecraft body cannot hold it.
The overflow detonates outward, scattering **splinters** at shrines and the crater.

- **The first class: the THEOCLAST.** **[LOCKED — owner's coinage.]** Greek `-clast`
  compounds split two ways: *iconoclast* is the breaker (agent), *pyroclast* is the broken
  piece (patient). "Theoclast" therefore means **god-breaker AND fragment of god at once**
  — and both are true of every member: complicit in the breaking, carrying the pieces.
  (Third echo, free: an *osteoclast* breaks down old bone so new bone can form — breaking
  as the first act of renewal, which is the mod's whole question.) Web-checked: no game or
  mod owns the word.
- The shattered god-pieces are **clasts** (item). Anyone may attune one; **clasts are
  finite** — the class is a server negotiation. **[NEEDS PLAYTEST]** count.
- **The killer**: the First Theoclast. Unique, involuntary, permanent. Carries the full
  weight and the Haunt. The server's pseudo-main-character, by design — owner has
  explicitly blessed asymmetric multiplayer here.
- The advancement at the moment of death: **"Deicide."**
- **The four voices** **[LOCKED]** — one UI name, four in-world names, because
  institutions disagreeing about a word is free worldbuilding:

  | Voice | Calls them | Because |
  |---|---|---|
  | the game's UI, the truth | **Theoclast** | breaker and fragment, both |
  | the Wardens' citations | **Usurper** | the legal theory: seized power belonging to a throne |
  | the villagers' whisper | **saint** | channeling a dead god through relics was always the job description |
  | the ghost, to its killer only | **Executor** | you killed me; now you administer my estate. Deliver my mail. |

**[LOCKED] The class's identity is the collision:** residue rites are the only thing that
can locally slow the unraveling (*anchor-rites*, fed with gathered residue) — and every
rite also spends the corpse. The criminal is the caretaker. The Wardens outlaw the practice
keeping regions alive, sincerely, and both sides are right.

Sample kit: *Anchor-rite* · *Lastword* — speak at a recent death site · *God's-eye* — see
through the dead god's old wards (prospecting) · *Borrowed-hand* — animate one tool to
work alone.

### Repeatable deicide

**[LOCKED — the gun stays.]** Every living god can, in principle, die the same way. The mod
never suggests it; the possibility simply exists, because it happened once. Each killed
god: their world begins unraveling, their school curdles into residue-form, their killer
gains an Executor-role and **their own Haunt**, and the pantheon learns what the players
are. New classes can be born this way — the dark path has real rewards. The peaceful path
(apprenticeship under a living god) yields that school's class without a body.

---

## The Haunt

**[LOCKED]** Each dead god binds to its killer. Personal, permanent, escalating with
residue use and chapter. Most manifestations are **client-side, rendered only for the
haunted player**:

- a figure at the tree line no one else sees
- a mob wearing the god's face, one frame
- **item marginalia** — the ghost annotates the killer's tooltips in handwriting
- dream-audiences: sleep sometimes routes the killer to a small dimension where the ghost
  holds court. This is the relationship channel — rage, then bargaining, then the discovery
  that the ghost *needs* its killer (its only remaining anchor) and the killer needs it
  (the only one who knows how anything worked).

**Rarely, a manifestation is server-real** — a bystander sees the door move too. Not a
sanity bar: a *credibility problem*. Each god's ghost manifests in its own register (the
Quiet One's killer hears less and less).

[EXAMPLE ONLY] marginalia: *"you keep your tools in a barrel. a barrel."*

---

## The Wardens

**[LOCKED]** The dead god's enforcement, still working. **Mourners, not cops** — enforcing
the law is how they grieve. They inspect, cite, confiscate, escalate; they never quip, and
they never destroy player builds. Citations read like parking tickets ([EXAMPLE ONLY]:
*UNLICENSED MIRACLE — third notice*). Countermeasures: shielded casting rooms, **forged
dispensations** (craftable from researched paperwork), captured-and-reconsecrated Wardens.
Enforcement targets *sites*, never a single player — no player is the system's butt.

### Can a Warden be killed?

**[LOCKED — owner delegated; decided here.] No. Not ever, by anyone, by any means.**
And the interesting part is that this costs the player nothing, because **the question
is wrong.**

A Warden is not a monster with a health bar the design forgot to balance. It is a *unit
on a post*. You do not kill it; you **dissolve the authority that put it there**. Break
the woken statue and the posting ends — which is already how the mod works, so the
answer was sitting in the codebase before the question was asked.

That makes the whole enforcement layer legible in one gesture: **violence does nothing;
paperwork works.** A player who swings at a Warden learns nothing happens and it is
still looking at them. A player who works out that the statue in their garden is the
thing *authorising* the Warden has understood the mod. It is the thesis in miniature,
and it is the "absurd system you find out how it ticks and use to your advantage" the
brief asked for — the exploit is administrative.

**One beat, and it is the whole reason to write it this way.** A Warden whose statue is
broken *while it is mid-statement* does not vanish. It finishes. It completes the
statement it was taking, files it, and only then stands down — because there is no
procedure for stopping in the middle of one. You cannot even interrupt them by removing
their reason to exist.

**[NEEDS PLAYTEST]** only whether "nothing happens" needs a stronger tell than nothing —
a filed notice, a line of procedure — for players who read no reaction as a bug.

---

## The last letter

**[LOCKED — owner delegated; decided here.]** The ending document, and it is **not a
letter.**

The dead god wrote four times and got no reply. The last thing it wrote, it could not
address to any of them — so it addressed it to itself, in the only register it had:
**a citation, issued against itself, in the exact format its own Wardens still use.**

> `UNLICENSED MIRACLE — first notice.`
> `Issued to: the holder.`
> `Particulars: the undersigned has continued to turn the sun in the absence of`
> `instruction, request, or acknowledgement, for a period the register declines to`
> `state. The undersigned is aware this is not what the sun is for.`

**This is where the Wardens come from.** They speak in procedure because *it* did. Every
flat, filed, authorised line a unit has said to the player all game — *"this unit would
like to say something else and is not authorised to"* — is inherited. They are not
bureaucrats guarding a god; they are its children doing the only thing they ever saw it
do with a feeling.

Three things it has to be, and this shape is the only one that is all three:

- **It cannot explain the plot.** A farewell that tells you what happened would make the
  whole search retroactively pointless. A self-issued citation explains *nothing* and
  reveals *everything*.
- **It cannot be sentimental.** The dread covenant forbids the deicide moment being
  comic and forbids the grief being a punchline. Procedure-as-grief is the register the
  mod has used since the first Warden line; the last letter is that register's floor.
- **It has to make all three endings honest.** You went looking for a successor and
  found the previous holder's admission that it was never sure it was doing the job
  right either. Install someone else, take the job yourself, or burn it — the letter
  argues for none of them, and makes all three defensible. That is what an ending
  document is for.

**Where it is found:** not at a god. It is what you get *instead of* a fifth
destination — filed, in the ferry's own desk, where the transcription clerk would have
logged an outgoing item. It was never sent. It was never *sendable*. Someone filed it
anyway, because there is a column for it.

---

## The ferry

**[LOCKED]** The dead god's mail-ferry, sealed since the isolation began. Built and
furnished from real blocks; a keel block captures the structure, validates it against the
destination's law, and re-places it at the far pad. **The validation checklist teaches each
world's rule before arrival** — the Quiet One's crossing: no note blocks, no jukebox,
muffle your animals.

---

## Dialogue

**[LOCKED — owner request]** Branching, ensemble, BioWare-shaped. SWTOR and Divinity:
Original Sin are the references, and the owner's key insight is on record: **ensemble
dialogue is what redeems the pseudo-main-character** — the Executor is mechanically unique,
but in conversation every player at the table has an equal voice.

### The shape

Dialogue trees are **data** (JSON graphs, datagen-emitted, per `DATAGEN.md`'s law), played
through one of the mod's few permitted custom screens. A flagged NPC conversation pulls
every nearby player in as a **participant**, not an audience:

- Every participant sees the NPC's line and picks their own response.
- **Every pick is shown to the whole table as that player's spoken stance** — so two
  players choosing opposed lines are *literally arguing in front of the NPC*, no extra
  system needed. The NPC acknowledges dissent ("Your friend does not agree.").
- Each node declares its **resolution rule**, chosen by the author, not global:
  **initiator decides** (small talk) · **vote** (group decisions) · **roll among stances**
  (the SWTOR dice, for flavour beats) · **unanimity required** (the irreversible ones).

### No karma bar

**[LOCKED]** Good/evil/neutral exists as *outcomes*, never as a meter. What is tracked is
**regard, per institution** — each god, the Wardenate, the villages — moved by choices and
deeds. A morality number is false specificity; a god remembering what you said to them is
a relationship.

### Tags — your record speaks

DOS-style tagged options, unlocked by what you *are* and what you *did*: **[Executor]**,
**[Theoclast]**, **[Hush-touched]**, and — the fun one — **[Cited: 3 offenses]**. The
Wardens' paperwork becomes dialogue ammunition; an NPC can read your rap sheet, and you can
lean on it.

### Where it lives

God audiences (each letter delivery is a scene), dream-audiences with the ghost
(killer-only — the one private channel, deliberately), Warden interrogations after the
death ("where were you when—"), and shrine-keepers. Wardens speak in procedure per the
infrastructure-voice rules; gods speak like family at a funeral.

### The endings are a conversation

**[LOCKED]** The final choice — which god, or no god, or the ash — is made **inside the
dialogue system, as a group scene with a unanimity-or-vote rule.** The server's argument
about the ending *is* the endgame content, held in the same UI they have argued in all
game.

**[PROPOSED] v1 scope** (owner: "even a super simple version is ok"): initiator + visible
stances + interjections, three resolution rules, no portraits, no camera work. The tech is
a bounded state machine + one screen; the real cost is the writing, which is data and can
grow forever.

---

## Endings

**[PROPOSED]** All mechanical, no cutscenes:

1. **Install a willing god** — Earth is saved and permanently changed to the new god's law.
   The server argues about which god. The argument is the endgame.
2. **Take the job** — refuse them all; hold the world together manually, shrine by shrine,
   forever. The thesis ending: the difference was never which god — it was whether anyone
   was tending it.
3. **God of ash** — kill them all, harvest everything, be the last law in a burnt cosmos.
   Fully supported. Never endorsed.

---

## Dread covenant (ported from the owner's CGTF bible)

**[LOCKED]** The deicide moment is never comic. The ghost's grief is never the punchline.
Villager wakes are sincere. Wardens never banter. The Quiet One's silence is never cute.
Comedy lives in the *systems*: obsolete civic ritual (the shrine donation box, still
counting), Warden procedure, the ghost's petty domestic complaints, steles that re-read
differently — and every beat must pass the bible's Bad Comedy Detector.

---

## The seven-question audit (required by AESTHETIC.md)

1. **Why does this exist here?** Worlds are held by gods; law is attention. Vanilla's
   arbitrary rules are the evidence, reframed — the setting explains the game it lives in.
2. **How have ordinary people adapted?** Villagers keep curfew (always did — now you know
   why), tend shrines, hold wakes after the death; Wardens patrol a law with no author.
3. **What institution profits, regulates, worships, misunderstands?** The Wardenate —
   enforcing a dead legislator's law as grief. The class system splits *saint* / *usurper*.
4. **What limitation prevents it solving everything?** Gods cannot leave their worlds — a
   god *is* its world's coherence, so none can simply cover Earth. Magic cannot mend what
   it spends. The ghost can speak only to its killer.
5. **What earlier decision caused the disaster?** The Isolation. Banned magic, sealed ways,
   hidden heart, unanswered mail — protection made the death both possible and unfixable.
   One character flaw; the entire causal chain falls out of it.
6. **Will anyone remember afterward?** Chapters scar the overworld permanently; citations
   accumulate; the ghost never leaves; the ending rewrites the world's laws forever.
7. **Could a different random weird thing replace it?** Swap the dead god for a dead
   machine: lose the grief, the mail, sainthood-as-necromancy, the haunting, the mourning
   Wardens. Swap for a generic cataclysm: lose the quest structure (letters), the class
   origin (spill), the ban's correctness, and all three endings. **It does not survive the
   swap. Passes.**

---

## Open items

| Item | Status |
|---|---|
| God roster size and final names | **[WORKSHOP]** |
| Deicide trigger reliability | **[NEEDS PLAYTEST]** |
| Splinter count / class scarcity | **[NEEDS PLAYTEST]** |
| Unraveling band contents per chapter | design phase |
| Dialogue v1 scope + first written scene | design phase |
| The ending memo… wrong mod now — the dead god's *last letter*, unsent, found at the end | co-write with owner |
