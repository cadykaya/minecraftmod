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

### How you come to be carrying it — **returned undelivered**

**[LOCKED — owner delegated; decided here.]** The letters are not in the shrine chests and
not in the crater. **They came back.** Every one of them was sent, none was answered, and
they have been sitting with a shrine-keeper ever since, waiting for somebody to give them
to.

The three candidates each said something different about the dead god, which is why this
was a story decision and not a plumbing one. Unsent letters in a chest would mean it never
tried — four letters written, filed, and never posted. This means **it did try.** It wrote,
it waited, and the post came back, and it went on hiding its heart in anonymous shrines
anyway.

Three things fall out of it:

- **The Quiet One's silence becomes a decision.** *"The one who never wrote back"* stops
  being an absence and becomes a choice somebody made, repeatedly, while letters kept
  arriving. That is a much crueller character and a much better one.
- **The keeper has been waiting for somebody.** They are the last person the dead god's
  correspondence passed through, and they have been holding it since. It gives them a
  reason to exist that is not the shrine, and a scene that is theirs.
- **Handing it over is a transfer of office, not a pickup.** You do not find the mail. It
  is *given* to you, by someone who has been keeping it, once the job has fallen to you —
  which is the only version where the word the ghost uses for you is already true before
  it says it.

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

#### Each god's portal is opened by the school that god teaches

**[LOCKED — owner delegated; decided here.]** The grammar above named *"that world's own
portal logic"* and never said what a portal **is**. Four sketches existed, one per god,
each read off the law that world already runs — and the answer turned out not to be four
mechanisms but **one rule with four faces**.

| God | Law | Its portal | Opened with |
|---|---|---|---|
| **The Anchorite** | weight | A shaft you do not build but **let go into** — it takes anything unanchored, which there means everything. Going *down*, into the place where down does not hold. | *Lighten* / *Loft* — you have to make yourself or your way weightless |
| **The Verdant** | growth | You **plant** it and wait. It opens when mature and closes when cut: the only portal in the mod with a lifespan. | *Bridgeroot* / *Wildgrowth* — or patience, which is worse there |
| **The Hearth-Turner** | time | Always present, open **at one hour only** — and the sky is fixed, so you cannot wait for it. You have to *make* the hour happen. | *Weather* / *Rewind* — the school is the clock |
| **The Quiet One** | silence | Opens when **nothing near it makes a sound**. The only one of the four a player can close by accident. | *Hush* — and *Held-breath*, for the last few steps |

**Why this and not four unrelated mechanisms.** It means you cannot go deeper into a god's
system until that god has taught you, which is exactly the progression already locked in
*"schools, learned in their worlds"* — the reason to cross is that the verbs are over
there, and now the reason to cross *again* is that the verbs are the doors. A portal per
god was four ideas; a portal each god's own school opens is one.

It also puts the schools' second and third spells to work. *Held-breath* stops being a
stealth trick and becomes how you finish a walk to a door that closes if you cough.

**Built: the Anchorite's, both ways.** The shaft is not a block, a frame or an entity — it
is a *Lighten* zone's **footprint taken through the height of the world**, cast in the one
place where the spell's own law is also the world's. Let go inside it for two seconds and
you go through; touch anything and the count restarts. The under-layer,
`interregnum:mass_authority_lower`, exists.

The return trip turned out to be the design, and it was a live check that found it. You
arrive **standing on the floor** down there, and a shaft that only took what had already
let go could never move you again — there is no cliff to step off and nothing to let go
*of*. So below, the shaft **lifts everything inside it**, standing or not, which is *down
does not hold* said in movement rather than in prose. One rule, one field, and the world
chooses the sign.

**Built: the Verdant's, and it has a life.** You plant a sapling in that world and the
world remembers you planted it. It is **not a door yet** — the waiting is the point. One
cast of *Wildgrowth*, the school's verb for hurrying what is already there, ripens it; so
does patience, because that world already grows at eight times the rate you know. Fell the
trunk and the door is gone in the same instant, because the door was never anything but
the tree. It is the only portal in the mod a stranger can take away from you.

**[LOCKED — decided here, completing a locked design.]** `WORLD.md` never said how you
*pass through* a grown door, and a mature trunk is a solid block, so there is nothing to
walk into. **You stand still under it.** That is the exact counterpart of the Anchorite's
rule rather than a copy: one is a thing you do to yourself in mid-air, the other is a thing
you allow the world to do to you — and in a world where growth is the hazard and the path
you cut closes behind you, standing about is the one thing that is never free.

One consequence had to be paid for elsewhere. *Wildgrowth* spares blocks a player placed,
which is right for a spell that sweeps a cube — but a planted sapling **is** placed, so the
school's own verb would have stepped over the only thing the caster was aiming at. The
resolution is the existing rule read properly, not an exception to it: the ledger gates
what you did not aim at, and a position somebody deliberately registered as a door is not
that. Nothing else in the cube loses its protection.

**Built: the Hearth-Turner's, and the hour is a stage rather than a time.** That world has
the mod's one stopped sky, so there is no hour up there to wait for. What the school moves
is the age of things — the Turning's own chain, `stone_bricks → cracked → mossy` — and a
doorway is six blocks framing a gap, open only while all six are at one stage. The open
hour is the **middle** link, the only stage reachable from both directions: *Weather* ages
a fresh frame into it, *Rewind* brings an old one back. **[LOCKED — decided here.]** You
pass through by **walking in**; all the difficulty was in making the hour, and what you do
with a door is walk through it. The far side is stamped with a matching frame, because
nothing over there has ever built anything.

**Built: the Quiet One's, and it listens the way sculk listens.** `Hush`'s own note has
always said the audible half of this god's law lives on a client and is not claimed. The
way through is that the game already has a **server-side** model of a noise that is not the
sound system: the vibration a sculk sensor feels, posted with a position every time
something happens. So the door is a cast *Hush* zone — the school's verb makes the boundary
that *"nothing near **it**"* needs — and it opens once no game event has landed inside it
for five seconds. **Every** event counts, with no allow-list: an accident is precisely the
thing you did not know counted.

**[LOCKED — decided here.]** It is the only door with no second condition, and the only one
that is **shared**. The Verdant's asks you to hold still and answers about you; this asks
the world to be quiet and answers about the place — so a stranger's footstep closes your
door and yours closes theirs. Of the four gods, this is the one whose law was never about
the person standing in front of it. A noise **resets** rather than delays, which is what
makes it *"the only one of the four a player can close by accident"*; and the silence needed
is a quarter of the spell that holds it, so an accident is a setback rather than a wasted
cast. *Held-breath* — unbuilt — is still the answer to the last few steps, because walking
posts an event every time.

**All four portals exist.** Four doors, four verbs: **let go, stand still, walk in, make no
sound.** No god has a far-layer, and nothing has been decided about what one is.

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

### Casting is a spoken word

**[LOCKED — owner delegated; decided here.]** Not a focus item, not a keybind. **You say
the word, out loud, in chat, and everyone in earshot sees you say it.**

A focus item would be smoother to use and it is the wrong answer, because the interesting
property of this mod's magic is not that it is powerful — it is that it is **illegal at
home**. A ban nobody can catch you breaking is a lore note. Speech is the only affordance
where the act of casting is *itself* evidence: a Warden standing in the room has now
witnessed the offence, a bystander can repeat what you said, and doing it quietly in a
cellar becomes a real choice rather than a flavour one.

It also fits the register. This is a world whose institutions run on dockets, statements
and correspondence; a magic system where the spell is a **word you are on record as having
said** belongs to it in a way a wand never would.

Three consequences, all wanted:

- **You can be overheard.** By a Warden, or by another player who will remember.
- **You cannot cast silently** — which is what makes *Held-breath* interesting rather than
  a stealth trinket: while you hold it you have no voice, so you have no spells.
- **The Wardenate finally has something legible to cite.** See the citations below.

**[NEEDS PLAYTEST], and more than most:** typing to cast may simply feel bad. The
affordance is therefore built as one seam over the existing spells — every spell is
reachable by command today and none of them knows how it was triggered — so swapping this
for a focus item later costs the seam and nothing else.

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

#### The six that were only names

**[LOCKED — owner delegated; decided here.]** The lists above describe six of their twelve
verbs and leave six bare. All six described ones are built; here is what the rest are. Each
is read off its school rather than invented beside it, and two were decided rather than
sketched because they interlock with decisions elsewhere on this page.

| Spell | School | What it does |
|---|---|---|
| *Hedge* | Verdancy | A living wall that grows where you draw it and **thickens where it is struck**. The only defence in the mod improved by being attacked. |
| *Graft* | Verdancy | Join two growing things, or a growing thing to a block, so one feeds the other — and a plant lives somewhere it could not. |
| *Moor* | Weight | The exact opposite of *Lighten*: fix a thing where it is, against any push. Not water, not pistons, not the Anchorite's own law. |
| *Held-breath* | Silence | Your own sound, taken away. Nothing tracks you while you hold it — **and you cannot cast, because casting is a spoken word.** Power for silence, exactly. |
| *Ripen* | The Turning | Age a living thing forward: crop, sapling, animal. The kind half of the school. |
| *Rot* | The Turning | Age a thing forward **past its end**: compost, spoil, collapse. **Never aimed at a player or a mob.** |

**Built: *Held-breath*.** It refuses every spoken word while it holds — **including its
own**, so a breath cannot be put down early; you wait it out. Fifteen seconds, the shortest
of the four Silence spells, because the others cost a cast and this one costs every cast.
It suppresses the caster's own game events, which is the same server-side vibration the
Quiet One's door listens for — so a breath-holder walks into that door without closing it,
which is what *"for the last few steps"* has always meant.

**Built: *Moor*.** The three forces it resists turned out to be one rule: water pushes
entities, a piston pushes what stands in front of it, and the Anchorite's law lifts a
falling block — which is an entity too. So a moored thing is an entity fixed in place, and
*its position does not change* refuses all three without knowing what any of them is. It is
aimed at one thing, which is what makes it the opposite of *Lighten* rather than a second
one, and it holds for a minute — the longest duration in the mod, on the least dramatic
spell in it, because an anchor that lapses while you are relying on it is a delay.

**Built: *Ripen*.** The first thing in the mod that ages something alive — the school had
only ever touched stone, which was a strange gap for a god whose law is keeping every past.
A crop goes forward one growth step and a calf becomes a cow. **An adult is nothing at
all**, and that is the line between this spell and its own twin: forward from grown is
toward the end, which is *Rot*'s country. It does not refuse an adult by naming one; it asks
for something with growing left to do, and a player has no such state either.

One cast is worth exactly one step, and the implementation had to work for that. Vanilla
growth is a **dice roll** — wheat on unhydrated farmland advances on about one random tick
in twenty-six — so a fixed number of pushes is a spell that works sometimes, which is not a
verb. The spell keeps asking until the block moves, up to a bound.

**Built: *Rot*, and the Turning is the first complete school.** It is a **second conversion
table** in the shape the ageing chain and the unraveling already share — the locked reuse
note's third caller — and it picks up exactly where the ageing table stops. `stone →
cobblestone → mossy cobblestone` is what a wall does over a long time; collapsing to gravel
is what it does after that, and the two are one sentence finished rather than two ideas.

**The locked constraint needs no enforcing, which is the strongest form it could take.**
*Rot* is a block conversion, and a table of blocks has no way to name a cow. The bolded
clause is not checked anywhere: there is nothing to check. That is the same shape *Ripen*
reaches from the other side — it asks for something with growing left to do, and a player
has no such state. **Neither spell contains the word *player*.**

**And it is one-way.** *Rewind* reads the **ageing** table backwards, so anything in that
table can be undone — which is what *keeping every past* means when it is a block. Nothing
Rot does is in that table. Past a thing's end there is no past left to keep, and the god
that remembers everything remembers what a thing *was*, not what is left of it.

*Held-breath* and *Rot* are the two that were decided rather than sketched. The first
because the casting affordance makes silence cost something real. The second because the
obvious reading of *"age it past its end"* is an instant-kill, and **every spell is a
world-verb** rules that out — so it ages the things that *have* an end and leaves creatures
alone. A school that broke the doctrine would take the doctrine with it.

**Reuse note [LOCKED]:** the block-aging registry powering the Turning **is the same
system that runs the unraveling.** One mechanism; a school and an apocalypse.

### Reading is dangerous

**[LOCKED — owner delegated; decided here.]** The Thaumcraft lane is in. Raw god-script
(letters, shrine inscriptions) read without transcription at the ferry's desk *marks* the
reader. Knowledge-as-hazard; the codex desk is the safe path.

**And "marks" means one specific thing: the ghost gets louder.** Reading raw script raises
your manifestation rate — the server-real one, the door that moves while somebody else is
standing there. Nothing else changes. No affliction bar, no debuff, no visions system to
build.

That is the whole hazard and it is enough, because of what it makes the hazard *be*: you
read the dead god's own handwriting without going through the desk, and the dead god
notices you have been reading its mail. The punishment for knowing too much is being
**known**. It also means the risk lands on the one axis the mod already treats as a
credibility problem rather than a health bar — the more you read, the harder your account
of your own world is to defend.

The safe path costs time and a trip to the desk. The unsafe path costs nothing at all,
which is exactly why people will take it.

**Built: the hazard. Not built: the desk.** Both sources of raw script are readable now —
the sealed letter always was, and `shrine_stone_carved` has been advertising *"a band of the
dead god's script"* since the chapter-0 art pass with nothing able to look at it. Each
distinct piece read takes ten off the manifestation odds, floored well short of a
metronome, and it is **the reader's knowledge** that counts rather than the number of
right-clicks: the same stone twice is the same knowledge.

Nothing is announced. A player is never told their rate has moved — a message saying so
would be an affliction bar made of text, which the locked line rules out — so it is found
out the way the fiction says: by noticing, later, that things have been happening near you.

**And it only bites the killer**, because the Haunt reaches the killer and nobody else. That
is not a gap to close: the god has a line to the person who killed it, and everybody else
can read all they like with nothing listening. It makes the hazard specific to the one
person who already has the god's attention, which is a better shape than a penalty everyone
shares.

**Built: the safe path, and it is a lectern.** *"The codex desk"* never said what one looks
like, and the mod already argues this case in blocks — `FerryPad`: *"an institution does not
redesign its dock for each god. It has a standard dock."* The Post used what was to hand. A
lectern does nothing unusual unless somebody offers it one of four letters.

Leave a letter and the clerk takes **thirty seconds**, during which you cannot carry it,
deliver it or read it. That is the cost, and it is the whole reason anybody takes the other
path: the safe one takes the thing you wanted to know out of your hands and gives you back
exactly as much as you had.

**[LOCKED — decided here.]** A transcription belongs to the **world**, not to whoever paid
for it. Once a letter has been through a desk, nobody who reads it is reading the god's hand
any more — which is the only social mechanic in this lane: the first person to be patient
pays for everyone after them, and the impatient ones read it raw long before that.

It also fixes the shape of the hazard. There are four letters and they can all be made safe;
**a carved stone cannot be brought to a desk** and never stops being the god's own hand. So
the correspondence is finite and the inscriptions are not, which is what *"most people don't
bother"* has always described.

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
- **How you attune one** **[LOCKED — owner delegated; decided here.]** — **a rite at a
  shrine, and the keeper has to agree to witness it.** Not a right-click, and not the
  crater. Two things fall out of that and both are the reason:

  **The shrines keep mattering after they are looted.** Every other use of a shrine is
  extractive — take the heart, take the clast, leave. This is the one thing you can only
  do *there*, and it means a stripped shrine is still somewhere you have to go back to.

  **Becoming a Theoclast is a public act with a gatekeeper who has an opinion of you.**
  The keeper is the villages, and the villages keep a file. Standing that was previously
  a matter of prices and greetings now decides whether you can hold the class at all —
  which is the first time regard gates something a player actually wants, and it makes
  *"the class is a server negotiation"* literally true: the negotiation is with a person.
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

### "Warden" is not its name — it is its rank

**[LOCKED — owner delegated; decided here.] The name stays.** Minecraft already ships a
mob called the Warden, and in a modpack the word now means two unrelated things. That is
only a problem if *Warden* is what our thing **is**. It is not, and the mod has never
once said it was.

**Check the shipped lines. A Warden never calls itself a Warden.** Across every scene
written so far — the census, the statements, the refusals — it says **"this unit"**,
every time, without exception:

> *This unit is conducting a census of the living.*
> *This unit has amended the record.*
> *This unit is required to say that your cooperation has been noted. This unit would
> like to say something else and is not authorised to.*

The word `Warden` appears in exactly three places in the whole mod: the entity's display
name, the statue block's name, and a docket header. All three are **the Wardenate's own
paperwork**. So *Warden* is a rank an office grants to its units — like *Sergeant*, not
like *wolf* — and the vanilla mob's claim on the word is a collision between two
institutions' vocabularies, which is the exact thing this mod is about.

Which means the unit was the one thing in the world that had not been given the
four-voices treatment, and it should have been first.

| | What it is called | Why that is the true name from there |
|---|---|---|
| **It calls itself** | **this unit** | It will not claim the rank in its own mouth. Already true in every line shipped. |
| **The Wardenate** | **Warden** | A rank on a docket, granted by an office that still meets. |
| **Villagers say** | **a docket** | They name the person after the paperwork, because the paperwork is the part that happens to them. *"There is a docket standing in the road."* |
| **Theoclasts say** | **a posting** | They know what actually ends one. *"That is not a Warden in your garden. That is a posting."* |

Two of those names teach mechanics for free. A player who only ever hears Theoclasts say
**posting** has been handed the answer to *"how do I get rid of it"* without a tooltip —
and it is the same answer as ["Can a Warden be killed?"](#can-a-warden-be-killed) below,
arrived at through vocabulary instead of through combat.

**[PROPOSED]** If the rank is granted by an office, then a creature calling itself a
Warden without having been granted it is an irregularity, and there is exactly one
institution in the world that would respond to that by filing about it and changing
nothing. A notice somewhere in the deep dark — `SUBJECT: UNLICENSED WARDEN. The office
notes that this is not a matter it can resolve, and has filed accordingly.` Not built,
and it should never be findable by anyone hunting for a joke.

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

### It sails where the letter in your hand belongs

**[LOCKED — owner delegated; decided here.]** No menu on the keel, no destination written
by hand. **You hold a letter, and the ferry reads it.** No letter, no voyage.

This is *"the route to them is its unanswered correspondence"* and *"you are the only one
carrying their mail"* stopping being flavour and becoming the navigation. You cannot reach
a god you are not carrying mail for. The letters **are** the map.

**Routed by the letter's id, and NOT by its addressee.** This paragraph replaces one that
said the opposite, and the correction is kept in view because the wrong version is the more
appealing one.

The wrong version: the ferry sails where the letter is *addressed*, so the one that opens
`To —` cannot be routed, the boat does not move, and *who was it for* becomes a question
the mechanism asks rather than a line of text. It reads beautifully. **The unaddressed
letter is the Quiet One's** — see "The Quiet One has no name in the letters" above, which
is the whole character — so that rule would have made a god's world permanently
unreachable, silently, by the only affordance there is for reaching one.

The blank envelope is a fact about a god, not a defect in a document, and the ferry treats
it like any other letter. `letters_check.py` enforces the fact the correct version rests
on: every letter names a crossing and every crossing has a letter.

**[OPEN — not a decision, an idea without a mechanism.]** The endgame beat that version
was reaching for — *finding out who the last letter was for* — is still wanted and no
longer has a mechanism. Nothing is built for it and nothing should be until there is one
that does not strand a world.

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
