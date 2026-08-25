# Handoff

**Living document. Read this first if you are a fresh session.** It is the only file that
says what is *currently true*; everything else says what is *always true*.

Updated at every phase boundary, so that if a session ends — for any reason — the next one
picks up cold without archaeology.

**Last updated:** autonomous build-out, hourly heartbeat running. The toolchain is
**unblocked and building** (NeoForge 26.2.0.67, Java 25) — the old network-policy
blocker is resolved and the probe returns 200. Chapter 0's content, the deicide and
its consequences, placement tracking, the unraveling, and the first Warden entity all
exist and are verified against a live server.

---

## Where things stand

There is a mod. It builds, it boots a dedicated server, and every system below is
asserted against a running world rather than against its own source.

| | State |
|---|---|
| Gradle project | **building**, NeoForge 26.2.0.67 / Java 25 |
| Java source | `core/` (pure, self-tested) + the game module |
| Textures / models / data | Phase-1 set, all resolving in a live server |
| Palette system | **working and verified** |
| Texture pipeline | **working and verified** (paint kit + review bench) |
| Doc set | **complete** — 15 documents, see [`INDEX.md`](INDEX.md) |
| Live-world checks | **17**, every one mutation-verified, all in CI |
| Regard | recorded, persisted, **audible**, and **read** — bands, never numbers |
| Entities | Warden, Shrine-Keeper — both spec-driven, both judged in rotation |

### The ferry lifts what was built

`interregnum ferry manifest|check|sail`, four destination laws in
`data/interregnum/ferry/laws.json`, and a `ferry_keel` block that decides where a
crossing starts.

The hard problem is the capture, and the answer was already in the repo: **the ferry
takes only what a player placed.** `Claims` has recorded that since the unraveling
needed to know what not to eat, so the walk stops dead at natural terrain — a hull
resting on the seabed lifts off it, a hull carved *out of* the seabed does not float.
`MAX_HULL` (4096) refuses rather than sailing half a boat, and when the claim test was
removed to watch the check fail, that cap is what stood between a bug in the walk and
the whole flat world.

`Ferry.capture` refuses `NOT_A_KEEL` off a bare coordinate, because without it `ferry
sail` is a command that teleports any structure anybody ever built. It refuses
`NOTHING_BUILT` on a keel with nothing attached. `Ferry.place` runs two full passes —
clear everything, then write everything — so a two-block nudge, where destination
overlaps origin, does not erase blocks it has already placed.

A held hull is told **every** violation, with block, count and reason, and the same
hull is asserted *cleared* by a different law so the check cannot pass against an
implementation that simply refuses everything.

`tools/ferry_check.sh` was watched failing three ways: the claim test removed (the
capture eats the world), the keel test removed (bare ground answers to the ferry), and
`place` collapsed to one pass (the nudge deletes the keel). That third mutation
initially passed — the assertion was matching the manifest printed *before* the
crossing, since the two are identical by design. See [`LESSONS.md`](LESSONS.md) #24.

One of the core mutations for the manifest's stable order was itself flaky, and CI
found it: `Map.copyOf` randomises iteration order with a per-JVM salt, so about one
run in six a three-key manifest came out sorted by luck, the mutation escaped, and
`mutate_check` reported a deliberate bug surviving against a guard that was fine. The
mutation now reverses the order deterministically and the assertion compares the whole
key list rather than two entries. [`LESSONS.md`](LESSONS.md) #19 has the detail —
everything demanded of an assertion is demanded of the mutation that tests it.

### The advancement that must not speak

`WORLD.md` locks two things that collide: the advancement at the moment of death is
called **Deicide**, and *the mod never announces who did it*.

Minecraft broadcasts advancements to chat by default. Shipped with that flag, the mod
would print **"&lt;player&gt; has made the advancement [Deicide]"** to everybody on the
server at the exact instant the design says nobody is told — the loudest possible
violation of its central beat, delivered by a boolean nobody looked at.

So `announce_to_chat` is false and `hidden` is true: the killer gets a toast, alone,
and the tree does not show anyone else that killing the god is a thing that can be
done. **That flag is the whole feature**, and `tools/advancement_check.py` fails the
build if it ever flips.

The criterion is `minecraft:impossible` and `Deicide.commit` awards it directly, so the
condition lives with the rest of the death's consequences. The check also compares the
criterion name in Java against the one in the JSON — two copies of one string, and if
they disagree the award silently does nothing.

The description does the same job as everything else at that moment: *"The box was
addressed to the holder. It did not say which one."*

### Where enforcement reaches

**A woken statue posts a Warden.** This was the last thing standing between the mod and
being playable end to end: the entity had two scenes, a tether and a renderer, and
*nothing created one*, so `WARDEN_CONTACT` — and therefore band 2 — could only be reached
by command.

The statues were handed out as scenery for a hundred hours and all opened their eyes at
once when the god died. Making the woken one the thing that *calls* turns that scenery
into a **map**: where the statues are is where enforcement reaches, players can read it
off the landscape, and it retroactively explains why the mod gave everybody free
decorative statues for so long.

**The statue is permanent; the Warden is not.** A posted Warden is explicitly not
persistence-required — it stands down when nobody is there and the statue posts another
when somebody returns. One immortal Warden per statue, forever, is not an institution;
on a server where people have built with these blocks for a hundred hours it is a very
large leak.

**Tearing one down costs WARDENATE −8**, and only when a player did it (a creeper is not
defiance — the same rule the shrine-keeper's death follows). An *unwoken* statue costs
nothing: before the death these are garden ornaments and the Wardenate has no opinion
about your landscaping. That is the lever the design wanted — pull the statues out of
your valley and enforcement stops reaching it — with a price on it, so it is a decision
rather than just the correct move.

`MAX_POSTED_PER_SWEEP` bounds the work per pass and **says so in the log when it bites**,
because a silent cap reads as "everything was handled".

### Six items that would have been purple cubes

Clearing the `VERIFY:` markers turned up a shipping bug, which is the argument for
clearing them.

**26.2 splits item models in two** (verified against the vanilla jar):
`assets/<ns>/items/<name>.json` is the *definition* — it names a model and is where
`condition` / `select` / `range_dispatch` live — and `assets/<ns>/models/item/<name>.json`
is the model itself. A **block item needs only the definition**, pointing straight at the
block model, exactly as vanilla ships `stone`.

This mod had **six registered items and zero definitions**, and every check was green.
A dedicated server never loads `assets/`, so nothing in CI could see it — and
`registry_check.py` was checking translation keys while its summary line claimed items
"resolve models". All six would have been the missing-model cube in front of a player.

Both halves are fixed: the definitions are written, and `registry_check.py` now asserts
every registered item has one and that the model it names resolves. Watched failing three
ways. **The lesson is not "write item models" — it is that the one area this container
cannot look at directly is the one where a check has to be read twice.**

### The same unit, one question changed

A Warden conducts a **census of the living** before the death and **takes statements
about it** afterwards. Same mob, same manner; what moves is what the procedure is for,
and the pairing is the reason the second scene works at all — a player who met a
Warden in Chapter 0 meets the identical procedure afterwards.

`warden_interrogation` is named directly in [`WORLD.md`](WORLD.md) ("Warden
interrogations after the death"), and the locked rule beside it is what shapes every
line: **enforcement targets sites, never a single player.** So it is a canvass, not an
accusation, and the unit says so unprompted because it is required to. Which means the
player who did it is asked routinely, and cleared routinely, by somebody working
through a list that cannot be finished — *"Three hundred and eleven statements
outstanding. Four hundred and twelve were taken yesterday. The figure does not fall."*

Dramatic irony like `shrine_keeper_intact`, but where the keeper was content this one
is grieving, and per the dread covenant the grief is never the punchline and the unit
never quips. It says everything it has as procedure, because procedure is what it has
instead of mourning: *"This unit is required to say that your cooperation has been
noted. This unit would like to say something else and is not authorised to."*

Scene selection reads the world's chapter data, not a flag on the mob, so a Warden
that has stood in a field since before the deicide answers the same as one that walked
up afterwards. `openingScene` is now the shared pattern for "an NPC whose opening
depends on what has happened" — the shrine-keeper already used it, and
`interregnum talk scene <entity>` asks any of them the question a right-click asks,
because a headless server can never reach `mobInteract`.

### The keeper knows what the village thinks

The villages are the second institution to act on standing, and the keeper is the
natural place for it: there is no separate village institution to meet, only its
people, and the keeper is one of them.

Two ways it shows, deliberately. The **opening** changes -- a party the villages
resent is told, mildly, that they are already in the register under remarks *in
another keeper's hand*, and the keeper adds that they would rather reconcile than do
the other thing. The **courtesy** is withdrawn: writing a theft up as "a withdrawal
against an authorised holder" is the keeper being kind on the record, and somebody
they resent is not offered it.

**Standing costs you the easy way out, never the content.** The `admit` node still has
two replies for a resented party and both go somewhere; the check asserts that, because
a gate that empties a node is a wedged table rather than a consequence.

### How they greet you

The other half of the same idea, and less machinery than it looks like. A node can
carry alternative wordings, chosen by standing:

```json
"text_variants": [
  { "text_key": "...open.filed", "standing_at_most":  { "WARDENATE": "RESENTED" } },
  { "text_key": "...open.known", "standing_at_least": { "WARDENATE": "TRUSTED" } }
]
```

**A node, not a scene.** Same id, same rule, same replies underneath -- only the
sentence changes. Three copies of a conversation differing by one line would drift.
The node's own `text_key` is what everybody else reads; there is no "default variant",
and a variant with no condition is **refused at load**, because it would silently
shadow the node's line and every variant after it.

**Whose standing, at a table of three: the viewer's.** Two players can read different
words for the same beat. That is not a desync -- the option list has worked that way
since the gates landed, so a Warden already offers you a reply it does not offer your
friend. Resolving the line against the initiator instead would make the text and the
replies disagree about whose file is open, which is worse than either rule alone.

The Warden's census now opens three ways: *"Your designation appears in three prior
returns"* for a party it has filed, *"Your returns have been accepted without
amendment"* for one it trusts, and the plain line for everybody else. Procedure
throughout — the unit never warms up and never threatens; what changes is how much of
the file it reads out before asking the same question.

`dialogue_check.py` was **blind to all of this when it landed** — a variant with a
misspelt key passed, and would have rendered as a raw key to exactly the players the
variant was written for. It now validates variant keys, unconditional variants, and
the institution/band names in every gate, option gates included.

### What they will and will not say to you

The first thing in the mod that **reads** regard rather than writing it. Until this,
standing was recorded, persisted, announced -- and consulted by nothing, which is a
feature finished everywhere except where it matters.

`StandingGate` gives an option a floor, a ceiling, or both:

```json
{ "id": "attest_for_absent", "standing_at_least": { "WARDENATE": "TRUSTED" } }
{ "id": "already_filed",     "standing_at_most":  { "WARDENATE": "RESENTED" } }
```

**The ceiling is not decoration.** Content you *lose* by being liked is what makes
standing read as a relationship that moved rather than a score that went up. Both are
live in `warden_intake`: a party the Wardenate trusts may answer for the absent, and
a party it resents -- and only while it resents them -- may say "Before you ask. Yes.
It's us.", which sends the unit to `fixate`.

**THE_GHOST is an absence, not a nought.** A non-killer's ghost regard is pinned at
zero, which reads as WARY, which would satisfy any floor at WARY or below. A gate
naming THE_GHOST admits only its killer -- otherwise the dead god's private options
leak to everyone who never met it.

Proved on a live server, not just in core: one player, one node, three standings,
three different sets of replies (`tools/standing_gate_check.py`). Counting alone would
not do it -- an implementation that showed both gated options in one render and
neither in the others has the same totals. `interregnum regard <who> adjust <inst>
<delta>` is the gamemaster affordance that makes it reachable, and it is routed
through `RegardNotices` so it cannot become a back door that moves standing silently.

### Somebody changed their mind about you

Regard was recorded, persisted, and completely invisible: no way for a player to
learn the system existed short of reading the source. It now speaks, and the rule it
keeps is narrower than "no feedback".

**The ban is on the NUMBER, not on the news.** What surfaces is a band *crossing* --
`core/regard/Standings` computes it, and most changes are not one. A conversation
that moves four institutions a little says nothing at all, which is correct: nobody's
opinion of you changed, it just moved. `regard_check.sh` asserts both halves, and the
quiet one is the load-bearing assertion: two players who moved regard without
crossing anything must hear nothing, or every conversation ends in a burst of
notifications and the meter is back with a thesaurus on.

**Seventy lines**, one per (institution, band, direction) a player can actually reach
-- you cannot rise into the bottom band or fall out of the top one. Each institution
speaks through its own domain, because that is the only characterisation available
without a scene: the Verdant's regard is whether paths close behind you, the
Anchorite's is whether what you set down stays there, the Hearth-Turner's is how fast
your gear ages, and **the Quiet One's is that you cannot tell** -- every one of its
lines is about the impossibility of reading a silence. `tools/regard_lines_check.py`
enforces coverage from the Java enums (add an `Institution` and it fails until the
lines exist) and **fails on any digit in any regard line**.

At a deicide the killer hears one line each from four gods at once. That is how the
mod says *you killed a god* without saying it. **The ghost stays silent**, which is
the design and not an omission: `recordDeicide` deliberately does not floor its own
victim, because the dead god's opinion of its killer is the one relationship still
open and the back half of the mod is spent on it. The check asserts that silence.

Two guards worth keeping: notices are wrapped in a `try`/`catch` because a line of
chat must never be able to half-finish a deicide (found by mutation -- a throw in the
crossing logic escaped through `Deicide.commit` after it had set the killer), and
`regard_keys_check.py` proves the keys a **running server** emits resolve, so the
static check and `RegardNotices.key()` cannot silently drift apart.

### The shrine-keeper, in person

The written scene has somebody to speak it. `shrine_keeper` is reachable in game.

The keeper is the Warden's opposite in every way that shows, and the palette does the
characterisation with no words at all: HELD is cool, SPENT is warm. The Warden is cold
worked metal on a tall frame under a wide mantle. The keeper is short, hooded in
cloth, warm brown — a person still reconciling a ledger for a reader who is dead,
which is exactly what "spent" means. **No ember anywhere on them**; that step belongs
to the dead god, and a living person who happens to be sad is not running on the
corpse.

The silhouette signature is the **ledger** under one arm, doing the job the Warden's
brim does. It is the only thing they carry and the only reason they are in the mod,
so it goes in the outline rather than into paint nobody will be close enough to read.

**The second entity cost a fraction of the first.** `entity_specs.py` +
`gen_resources.py` + `entity_view.py` meant spec, paint, generate, look, fix, look —
and looking caught three things reasoning had not: the hood was geometrically covering
the eyes (a hooded figure with a blank pale bandage for a face), the ledger vanished
in profile, and its page edges were painted only on the top face, which nobody ever
sees on a mob under two blocks tall.

Killing one costs **VILLAGES −25**, and only when a *player* did it. `WORLD.md` says
regard moves on choices **and deeds**, and until now only choices moved it — which
quietly taught the opposite. Charging somebody for a creeper's work is the kind of
unfairness that teaches people never to walk near an NPC again, so the check proves
a death by no player's hand costs nobody.

**Every shrine has one**, placed at worldgen, standing beside the offering box,
facing it, and **tethered to the court** (`setHomeTo`, radius 5). The tether is not a
nicety: without it they stroll away from the shrine they exist to attend and, being
persistent, never come back. It was added while chasing a red build that turned out to
have a different cause entirely — the tether is good design regardless, and the
episode is worth reading as [`LESSONS.md`](LESSONS.md) #21 followed by #22, which are
between them the sharpest verification lessons in the file: an assertion about a
moving thing is an assertion about when you looked, and an API that accepts your write
has not promised anyone can read it. The spot is chosen rather than fixed -- the court has missing paving by
design, so the first candidate tile with solid footing and two blocks of headroom
wins, and the shrine gets nobody if none of them do. A keeper standing in a wall is
worse than a shrine with no keeper.

**The keeper picks its scene.** The ledger scene is about a shortfall the players
caused -- that is the mod's whole consequence-comedy engine -- so instead of diluting
its opening to also work at an untouched shrine, there are two scenes.
`shrine_keeper_intact` is the same person before any of it: content, the ledger
balancing, pleased somebody came, and apologising for keeping tidy a box that
"opens for the one it is addressed to" and which nobody has come to open in a very
long time. A player reads that as flavour and opens the chest anyway. **The world
told you, in a tone so mild you did not register it as being told.**

The signal is the offering box's own **pending loot table**, which Minecraft clears
the instant anybody opens a container -- no bookkeeping of ours, nothing to keep in
sync, and it stays true if an admin replaces the chest. `/interregnum talk scene`
asks the same question the right-click asks, because a headless server can never
reach `mobInteract`.

### The Haunt begins

The dead god now reaches its killer. Sleep, and it is there.

`TheHaunt.offer` is the whole feature and every clause is a way the beat goes wrong:
nothing haunts anybody while the god is alive; **only the killer**, because this is
the ghost's private conversation and an admin with good intentions must not be able
to hand it to somebody else; once, because that is what "first dream-audience" means;
and a player already at a table is deferred rather than evicted -- **without spending
the one dream**, so a coincidence of timing cannot cost them the scene.

Delivered by `PlayerWakeUpEvent`, on waking rather than on lying down: the
conversation needs somebody conscious enough to answer, and Minecraft's sleep is a
skip rather than a duration. What the player gets is the dream they just had.

The handler is three lines because a headless server has no sleeping players.
`/interregnum haunt dream <uuid> [force]` is the second legitimate caller -- and a
real tool: a player who slept through a crash has lost the only scripted delivery
this scene has. `tools/haunt_check.sh`, four mutations, four caught.

`Milestone.HAUNT_OPENED` is deliberately **not** a chapter prerequisite: the Haunt is
a thread, not a gate, and a killer who never sleeps must not stall the whole world.

### Conversations have consequences

Every scene used to resolve and change nothing. `RegardState` sat in `core/`, fully
tested, and no code read or wrote it. It does now.

**One rule, and it is the whole design: each participant is judged on what THEY said,
not on what the table decided.** A vote you lost is still on your record with the
party you sided with; going along with a group atrocity does not launder it, because
you still said the words. Without this, everybody ends up with the initiator's record
and the ensemble system is decoration -- the only choice that ever mattered would be
whoever clicked first. It lives in `RegardEffects` in `core/`, tested without a game,
and `tools/regard_check.sh` proves it against a live server: two players at the same
node, opposite stances, opposite records.

Effects are **data** -- `"regard": {"VILLAGES": 5, "WARDENATE": -4}` on any option.
`dialogue_check.py` rejects unknown institutions, non-integers, out-of-range values,
no-op zeroes, and anything above 25 (the band scale is 35 wide; one sentence should
not cross a band). All five verified by breaking the data on purpose.

**Nothing is announced to the player.** No karma bar, no "+5 Villages" -- you find out
what an institution thinks of you from how it treats you. `/interregnum regard <uuid>`
is a gamemaster readout and prints band AND number, which is *not* the meter coming
back: its audience is somebody asking "did that scene do anything", and bands cannot
answer that because most changes do not cross one.

The deicide now leaves its scar: gods hit and permanently capped, the Wardenate filed.
**VILLAGES is deliberately spared**, because `WORLD.md`'s four voices has the
villagers whispering *saint* -- capping them would put mechanics in contradiction with
locked lore and flatten every village scene into a formality nobody can move. THE_GHOST
is spared for a different reason: you destroyed it, and its regard is the one
relationship still open to the killer.

That persistence found the nastiest bug of the project so far -- see
[`LESSONS.md`](LESSONS.md) #20. Saved records were restored through a *relative* API
after the ceilings had already moved the baseline, so every capped god drifted down by
the size of its cap on **every single restart**, silently, plausibly, forever.

### The Warden speaks, and the table argues

`warden_intake` was written in the first week, validated ever since, and unreachable.
It runs now.

The decisions -- who wins a node, what dissent does -- were already in `core/` and
tested with no game running. What was missing is the half that only exists on a
server: **who is at which table, when a node resolves, and what happens when somebody
walks off mid-sentence.** Every rule in `Conversations` is a way a table can wedge in
front of real people:

- a node resolves on the **last** pick, never the first and never a timer;
- one player leaving must not deadlock the rest -- and a departure that completes the
  table resolves it **on the way out**;
- a leaver's vote leaves with them, or someone who quit could still swing a VOTE;
- the initiator leaving ends it (there is no INITIATOR node without one);
- silence times out after a minute: the initiator going quiet ends the table, anyone
  else going quiet is taken off it so the rest can carry on.

**Participants are opaque string ids, not players.** The core engine asked for that,
and it is what lets the entire multiplayer state machine -- votes, ties, unanimity,
walking away -- be asserted on a headless server with no client in existence.
`/interregnum talk start|say|status|leave` drives it; six mutations, six caught.

**Talking to a Warden is what records `WARDEN_CONTACT`** -- being *addressed*, not
seeing one -- so a world can now reach band 2 by playing rather than by command.

Two core fixes fell out of it: `Conversation.remove` (the engine described removing
absentees and had no way to), and stance ordering, which `Map.copyOf` was silently
discarding. See [`LESSONS.md`](LESSONS.md) #18 and #19 -- both are about verification
rather than about dialogue, and both are the more useful half of this pass.

**It is playable now, in chat.** Right-clicking a Warden opens a table; the scene
renders as chat with **clickable options**, which is vanilla, so a player on an
unmodified client can play every scene in the mod with no client code in existence.
That was the point of building it this way: the dialogue system could be finished and
verified end to end in a container with no game client, and the eventual screen is an
upgrade to something already working rather than the thing standing between the
writing and anyone reading it.

**Everyone within 8 blocks with line of sight is pulled in**, not just whoever
clicked — the SWTOR beat, and the only version where the resolution rules mean
anything, because a VOTE node with one player is an INITIATOR node with extra steps.
**[NEEDS PLAYTEST]** the radius, and the AFK case: someone pulled in who then does
nothing makes the rest wait out the timeout.

`/interregnum reply <option>` is the one **unprivileged** node in the command, and is
what the clickable options run — it can only ever speak for whoever ran it.
`/interregnum talk show <who> [tags]` renders a participant's exact view, which is
the only way to answer "why is that option not there for them"; it takes tags so an
operator can ask what a Theoclast would see without granting anything.

`PlayerTags` returns nothing today and that is correct, not unfinished: the only tag
any scene uses is `class/theoclast` and no clast can be attuned yet. When attunement
lands it lands there, and every scene already written starts offering its gated lines
with no edit.

**Three scenes exist**: `warden_intake`, `shrine_keeper` (the offering ledger that
must still be reconciled, quarterly, for a reader who is dead) and `dream_audience`
(the god handing over an estate to the person holding its power). The last is the one
table nobody else sits at -- every node INITIATOR, against every other scene's
ensemble, and that contrast is the design. Only `warden_intake` is reachable in game.

**Still missing: the screen**, which is deliberately last — it is the one part this
container cannot verify, and everything it will render is already proven.

### The Warden takes the field

The statues wake and watch; **these arrive**. Two objects, two jobs — waking a statue
does not consume it, so the one your neighbour built into their garden wall stays
there watching forever. The eye and the officer are not the same thing.

**A Warden never attacks.** No target selector, no melee goal, and — the part that is
actually enforced — *no `ATTACK_DAMAGE` attribute at all*, so there is nothing for a
future careless goal to reach for. 100 health, full knockback resistance, unpushable:
a player's first instinct is to hit one, and that has to fail in the most
uninformative way available. Nothing happens, and it is still looking at you.
**[NEEDS PLAYTEST]** whether they should be killable at all.

The model is a squat robed figure under a wide flat mantle, ember visor slots as the
only warm pixels on the whole sheet — the statue's rule, kept. Geometry lives once in
`tools/entity_specs.py` and feeds three consumers: the texture painter, the generated
`WardenGeometry.java`, and **`tools/entity_view.py`**, a new ray-cast bench that draws
the assembled figure front / three-quarter / side / rear. See [`MODELS.md`](MODELS.md);
it caught the robe reading as a bollard in profile on its first use.

`tools/warden_check.sh` proves summoning, attributes, the missing attack damage, and
survival across a restart. Three mutations, three caught. `registry_check.py` now also
refuses an entity missing a renderer, a layer definition, a texture, or a name — five
more, all verified.

**Not done yet, and deliberately:** nothing spawns a Warden. They exist and can be
summoned; who places them, where, and when is the next decision, and it is bound up
with the proposal under "Open questions".

### The overworld spends itself

`bands.json` is finally read. `UnravelingLoader` turns it into a validated table (rejecting
duplicate bands, self-conversions, a band mislabelled with the wrong chapter, and any rule
that reverses another — the unraveling runs one way only), and `Unraveling` applies it.

Five gates, in order, and each one is a named answer rather than a silent `false`, because
"nothing happened" is the one thing this system says constantly and it has to be possible
to ask why: `DORMANT` · `BAND_TOO_LOW` · `OUT_OF_SCOPE` · **`CLAIMED`** · `UNSUPPORTED`.

- **It samples the surface column near players.** Not a shortcut. `Claims` answers
  "claimed" for an unloaded chunk, so unloaded ground was never reachable anyway; and the
  table's blocks (grass, flowers, leaves) are one layer thick, so uniform sampling would
  essentially never hit them and band 1 would be invisible.
- **`thin_places`** is "within 48 blocks of the crater, or in a chunk next to one holding
  shrine masonry". The shrine test reads section *palettes*, not blocks, so the common
  answer costs a few reference comparisons. The crater is now persisted on the chapter data
  (`ChapterSavedData#site`) — the world's one fixed landmark, which the ferry and the ghost
  will both want too.
- **It never places a state that cannot stand there.** See [`LESSONS.md`](LESSONS.md) #16:
  the shipped table had a rule that was well-formed, passed every data check, and could
  never once have fired.
- `/interregnum unravel at|sweep` answer for one block and measure a burst. `status` now
  reports `ticks=`/`passes=` — the only witness that the level tick is connected at all.

`tools/unravel_check.sh` proves all of it against a live server, including a datapack that
replaces the table. Seven mutations, seven caught.

### The world remembers what people built

Minecraft does not record who placed a block, and the unraveling needs to know: the crater
can get away with a tag whitelist because it fires once at one spot, but the unraveling
runs forever over a whole world and would eventually eat somebody's cobblestone wall on the
grounds that cobblestone is natural.

`PlacedBlocks` is a chunk attachment holding chunk-relative positions packed one per int
(x and z are 4 bits, y is 9). Past **4096 placements the chunk saturates**: the set is
dropped and the whole chunk counts as claimed. That bounds memory and degrades in the safe
direction — it protects more, never less — and it is the right answer anyway, because a
chunk somebody has put four thousand blocks into is theirs.

`Claims` is the single place anything asks, and it **fails closed**: an unloaded chunk
answers "claimed". Breaking a block forgets its position, or mining through your own wall
would leave permanent invisible holes the unraveling could never touch.

`/interregnum claim at|record|forget` are operator tools, not test hooks: a world that
existed before this mod was installed is full of builds the tracker never saw, and an
admin needs a way to say "this is ours". They are also what makes the tracker testable
without a player.

`tools/claim_check.sh` proves per-position claims, survival across a restart, and
saturation; mutation-verified all three.

### The statues open their eyes

`warden_statue` is a decorative block for the whole of Chapter 0 -- players build around
them, put them in gardens -- and the moment the god dies **every one of them wakes**. The
eyes are the only warm pixels on an entirely cool figure, so a woken Warden is visible
across a field, and per the palette law that ember means the same thing it always means:
this is running on the corpse.

Two paths, both verified. Statues within eight chunks of the site (or of any player) wake at
the instant of death. Everything else wakes **on chunk load**, which is the better beat
anyway: a player who was underground climbs out and finds the one in their garden already
watching.

Only already-loaded chunks are touched -- `getChunk(..., false)` never generates terrain as
a side effect of the god dying -- and waking is a blockstate flip, so there is no block
entity, no ticking, and no per-statue bookkeeping.

`tools/statue_check.sh` proves both paths across two server runs and is mutation-verified.

### The ground gives way

The crater is **subsidence, not an explosion** -- nothing detonated; a god died and the
world stopped being held up there. No fire, no scorching, no thrown blocks, and nobody is
hurt, which matters because the person standing next to it is the one who just did it and
the mod is not punishing them.

**Only natural ground moves.** Minecraft does not record who placed a block, so a narrow tag
whitelist decides, and it errs toward sparing: an unlisted block is left alone. A slightly
lumpy crater is cosmetic; a deleted house is somebody quitting. The image this produces is
the one the beat wanted anyway -- a house at the shrine left hanging over a pit, untouched
and no longer resting on anything.

`tools/crater_check.sh` proves both halves and is mutation-verified: removing the whitelist
fails with the list of destroyed player blocks; removing the crater fails with "the ground
did not subside".

### Chapter 0 is playable end to end

Shrines generate with an **offering box** standing on the carved centre stone --
deliberately obvious, because the opening of this mod is a player doing the most ordinary
thing in Minecraft and it being deicide. The box holds mundane offerings (bread, a candle,
a little copper) and, on a **12% roll while the god still lives**, the heart.

The uniqueness falls out of *when* loot tables roll: they roll on first open, not at
worldgen, so every shrine in the world is a candidate until one pays out and none are
afterwards. No shrine is chosen in advance, nothing is tracked per-shrine. The heart is
somewhere until it is taken, and then it is nowhere.

Measured: 8 hearts in 60 rolls before the death (13%, matching the 12% configured), and a
deterministic **0 in 60 after**. `tools/heart_check.sh` asserts both and is
mutation-verified -- deleting the `god_lives` condition makes hearts appear after the god
is already dead, which it catches by name.

### The god can die

`Deicide.commit()` is the one place the catastrophe happens, and it is idempotent -- a
world can lose its god exactly once. Its consequence today: **the sun stops.** The day
cycle was the god's, and with nobody left to turn it the light stays where it was. Per
`WORLD.md` there is no announcement and no name; the world simply stops moving.

Two callers, one implementation: the pickup handler (needs a real player) and
`/interregnum record deicide` (does not). That is deliberate -- it is what lets the
untestable path be three lines of adapter over a path that is verified end to end.

`tools/deicide_check.sh` asserts the whole beat and was mutation-verified: removing the
consequence and removing idempotence both fail it by name.

### Chapter state persists

`/interregnum status` reports the world's chapter; `/interregnum record <milestone>` (level
2) advances it. State lives on the overworld's saved-data storage -- the interregnum is a
fact about the world, not about a place in it -- and serialises through the same single
string `ChapterState` already round-trips in the core self-test.

`tools/persistence_check.sh` proves it across five server boots, including a mutation of
**already-loaded** saved data, which is the only path where `setDirty()` matters
(LESSONS #13). It has a fresh-world control, without which the other runs prove nothing.

### Worldgen works

Shrines generate. `ShrineFeature` builds a 5x5 court with a carved centre stone, corner
steles, and missing paving for age; it refuses uneven ground rather than terracing it, and
it scans for the surface rather than reading a worldgen heightmap so it can also be run with
`/place` (LESSONS #11). Configured feature, placed feature and the biome modifier are all
**generated** by `runServerData`; the modifier targets the `#minecraft:is_overworld` **tag**,
never a biome list.

`tools/worldgen_check.sh` places a shrine in a live flat world and asserts what it built.
Verified failing three ways -- a wrong assertion, a deliberately broken feature, and a
chunk that had not finished loading.

**It waits after forceloading, and then proves the wait was long enough.** Chunks load
asynchronously; `place feature` needs only the blocks, so it can generate into a chunk
whose entity storage has not arrived, and any mob it places is then accepted and
invisible to every selector for the rest of the run. This cost three red builds. After
the wait, a `minecraft:marker` is summoned and asserted visible: if it is not, the
check fails as *"the chunk was still loading"* rather than blaming the keeper.
**Lengthen the wait if that fires; do not delete the probe.** `warden_check.sh` and
`talk_check.sh` carry the same wait for the same reason. LESSONS #22.

`tools/shrine_rate_probe.sh` measures density on **real** terrain (`GRID=8` takes ~2 min).
Measured: **45-46% of natural sites are level enough**, so with the rarity filter at 55 the
real density is **one shrine per ~120 chunks, roughly six minutes of walking**. Re-run it
after any change to `MAX_RELIEF` or the rarity filter -- they multiply. Whether six minutes
reads as furniture or as litter is **[NEEDS PLAYTEST]**; only a person can say.

### Verifying against a live server

`tools/server_smoke.sh` boots a dedicated server, and `COMMANDS` (newline-separated) runs
them over **RCON**, printing the server's own replies:

```sh
COMMANDS='forceload add 0 0 15 15
setblock 8 66 8 interregnum:warning_stele[axis=y]' ./tools/server_smoke.sh
```

This is the only way worldgen and block behaviour get verified without a client, and it is
how the next tasks (shrine structure, deicide event) will be checked. Note that **stdin does
not reach the server under Gradle's runServer** — RCON is not a preference, it is the only
channel that works. See LESSONS #10.

### Datagen works

`gradle runServerData` regenerates every JSON this mod ships into `src/generated/resources`
(committed, and separate from hand-authored resources so a diff shows at a glance which is
which). The run type is **`serverData`**, not `data`, and `--mod <id>` is **required** —
without it the gatherer logs `Initializing Data Gatherer for mods []` and dies with an
unrelated-looking `RejectedExecutionException`.

**Owner's creative call, delegated and taken:** carved shrine stone drops *plain* shrine
stone. You may take the stone; you may never take the word. It is unannounced, it teaches
that the god-script is not a material before any lore exists, and it makes carved stone
found-only — which turns "learning to inscribe" into a real Theoclast reward later.

### The toolchain works

`gradle build` produces `build/libs/interregnum-0.1.0.jar` against real NeoForge 26.2.0.67,
and `tools/server_smoke.sh` boots a dedicated server, waits for load, shuts it down
cleanly through stdin, and fails if the mod logged anything or if content did not load.

Registered so far: `shrine_stone`, `shrine_stone_carved`, `god_heart`, `clast`, a creative
tab, and the datapack-driven dialogue loader. The loader is the seam between `core/` and
the game: Codecs live in the game module so `core` stays dependency-free.

**Every `VERIFY:` marker in PLATFORM.md is now cleared** — see the verified-values table
there. Markers elsewhere (MODELS.md's item-model system, DATAGEN.md's provider names,
WORLDGEN.md's schemas) are still open and should be cleared the same way: read the sources
in `~/.gradle/caches/.../neoforge-26.2.0.67-sources.jar`, never remember.

### CI

`.github/workflows/checks.yml` runs `tools/check_all.sh` on every push and PR. The
workflow deliberately calls that one script rather than re-listing the checks, so CI and a
local run cannot disagree. No Minecraft toolchain is needed for it yet — everything checked
so far is dependency-free Python and Java 21. Verified by breaking three things in a clean
checkout (stale generated texture, missing translation key, broken doc link); each failed
the gate, and the restored tree passed.

### What actually runs today

```sh
python3 tools/palette_build.py                   # solve ramps -> assets/palette.json
python3 tools/palette_check.py                   # enforce the palette law (exit != 0 on fail)
python3 tools/demo_structure.py docs/img         # the worked example
python3 tools/texview.py <png> --tile 8 --scale 6 # the review bench
```

No dependencies. No pip install. Python 3 only.

- **10 palette families, 34 steps**, tightest separation **0.159 L\*** against a 0.12 floor.
- **All five palette checks verified by reintroducing the bug they catch** — see
  [`VERIFICATION.md`](VERIFICATION.md).
- **`pngio.py` round-trips byte-identically** and decodes real externally-produced PNGs
  (checked against DOWNTIME's Blender exports).

---

## Decisions on record

| Decision | Value | Where |
|---|---|---|
| Minecraft version | **26.2** ("Chaos Cubed", June 2026) | [`PLATFORM.md`](PLATFORM.md) |
| Loader | **NeoForge** | [`PLATFORM.md`](PLATFORM.md) |
| Java | **21** | [`PLATFORM.md`](PLATFORM.md) |
| Build | Gradle + **ModDevGradle** 2.0.x | [`PLATFORM.md`](PLATFORM.md) |
| Texture resolution | **16×16, no exceptions for blocks/items** | [`ARTSTYLE.md`](ARTSTYLE.md) |
| Art produced by | **generator scripts, not an image editor** | [`TEXTURING.md`](TEXTURING.md) |
| Colour | **`assets/palette.json` or it is a bug** | [`PALETTE.md`](PALETTE.md) |
| Resources | **generated by datagen, not hand-typed** | [`DATAGEN.md`](DATAGEN.md) |

The owner delegated version and loader explicitly: *"Ill use whatever version you think is
best, same with mod loader."* Both are recorded with reasoning so a future session can
overturn them on grounds rather than taste.

---

## The heartbeat

An hourly Routine (`INTERREGNUM heartbeat`, `trig_01KE2aMo3eAqVz72AtPoJZNW`) fires into
this same session, so it keeps full context. Its prompt is the agreed working contract:
one focused increment per tick, single repo, single branch, never force-push, new scope is
the owner's call, `tools/check_all.sh` green before every commit, and a two-tick circuit
breaker — if all work is blocked or the same failure repeats twice, it stops and says so
rather than improvising.

**Known limitation:** the Routine carries no MCP connectors, so heartbeat ticks have **no
GitHub API tools**. Git over HTTPS still works, so commit and push are fine and PR #1
updates automatically with each push. A tick **cannot** open a new PR, post a comment, or
read PR reviews — if a tick needs any of those, it should record the need in this file and
the owner (or an interactive session) does it.

To pause it: ask, or disable the Routine from the claude.ai Routines UI.

## Waiting on owner

1. **"Warden" collides with vanilla's Warden.** Minecraft already ships a mob called the
   Warden — the deep-dark one. Ours is a bureaucratic enforcement officer and shares
   nothing with it but the word, and in a modpack "a warden" now means two unrelated
   things. Every design doc here says Warden, so that is what is built; the id is
   `interregnum:warden` and the display name is "Warden".

   This is a lore call, not an engineering one, so it is the owner's. If it should
   change, changing it is cheap now and expensive once players learn it. Candidates
   that keep the institutional register: **Assessor** (they assess and file),
   **Invigilator** (one who watches an examination — and the census scene already reads
   as one), **Proctor**, **Registrar**. "Wardenate" as the institution's name could
   survive any of them, or become e.g. the Assessorate. *No action needed unless the
   owner wants a change.*
2. **Playtesting, and looking at the Warden.** This container has no game client, so two
   things about the model are unverifiable here and are not claimed: how it looks
   **animated**, and how it looks **lit**. `tools/entity_view.py` covers shape and paint;
   it cannot cover those. The render has been sent for review.

**Answered:** license is **MIT** (`LICENSE`, `gradle.properties`). `main` branch exists;
work flows to `claude/minecraft-mod-dev-rp0x8j` and PRs into `main`.

## Open questions

### Proposed, needs the owner's yes

*(Nothing currently waiting. The statue proposal was answered — see below.)*

### Answered this session

- **The god roster**: **four, and everyone calls them something different.** The
  pantheon inherits the Theoclast's four-voices doctrine — the dead god's letters use pet
  names (Rill, Ballast, Ash) that no other voice uses, and **the Quiet One has none**.
- **The dead god's last letter**: **it is not a letter, it is a citation it issued
  against itself** — and it is where the Wardens' procedural voice comes from.
- **Bands 3 and 4**: decided; see item 1 above and `WORLD.md`.
- **Can a Warden be killed**: **no, and the question is wrong.** You dissolve the
  authority by breaking the statue — which the mod already implements. Violence does
  nothing; paperwork works.
- **The statue summons the Warden**: **yes, owner-approved and built.** A woken statue
  posts a Warden while somebody is there; the statue is permanent, the Warden stands
  down. Statue density is now a readable map of where enforcement reaches, and tearing
  one down costs the breaker with the Wardenate. See "Where enforcement reaches" above.
- **The class name**: **Theoclast** — owner's coinage, locked. Breaker and fragment in
  one word; the pieces themselves are **clasts**. Four-voices naming table in `WORLD.md`.
- **The subject**: INTERREGNUM. You accidentally kill the overworld's god by looting a
  shrine chest; vanilla's rules were its policy; the world unravels chapter by chapter
  while you carry the dead god's unanswered letters to its estranged family, looking for a
  successor. Full design + seven-question audit in [`WORLD.md`](WORLD.md).
- **`mod_id`**: `interregnum`, locked, recorded in `PLATFORM.md`.
- **Loader check-in**: NeoForge stands. The mod is broad content+systems (dimensions,
  dialogue, classes, enforcement AI) — the Fabric fork in `PLATFORM.md` was for a small
  mechanical mod, which this is not.

### Still open — [WORKSHOP] with the owner

*(Nothing. The owner delegated the four remaining calls — the roster, the last letter,
bands 3 and 4, and whether Wardens can be killed — and all four are decided and written
into [`WORLD.md`](WORLD.md). Anything genuinely new still comes back here first.)*

### [NEEDS PLAYTEST] (cannot be settled by argument)

Deicide trigger reliability · clast scarcity · unraveling band pacing.

## Working agreement

Inherited from DOWNTIME, where the owner said: *"this whole project is yours partner, ask me
if you need help with stuff but if not, go ahead and make whatever."* The instruction here
was the same in spirit — *"You know this stuff better than i do"* — so:

- **Decide, document the reasoning, and proceed.** Do not stall on questions that have a
  defensible default; do stall on #1 above, which has none.
- **Push back.** The most valuable thing in DOWNTIME's record is the owner rejecting a
  character that passed every check. That judgement is the best instrument in the project.
- **Never fabricate a lesson.** [`LESSONS.md`](LESSONS.md) holds only things that actually
  happened here; inherited ones are marked PORTED in the doctrine docs. A fabricated war
  story spends the credibility that makes the real ones worth reading.

---

## What to do next

Done this pass: core dialogue engine (+15 verified checks), first scene + validator,
client-leak guard, `tools/check_all.sh` gate, Phase-1 draft textures (shrine stone,
carved, heart, clast) + block models/blockstates, Gradle scaffold for `core`.

In order, all unblocked unless marked:

1. **Bands 3 and 4 — designed, now buildable.** The owner delegated the decision and
   [`WORLD.md`](WORLD.md) now carries it. Neither is a conversion table. **Band 3
   (EXODUS)** is the overworld leaking other gods' *law* in patches — reconnaissance for
   the ferry. **Band 4 (ATTRITION)** is the world losing its *distinction*: biome detail
   generalising where nobody tends, never touching player-placed blocks. Band 3 wants the
   26.2 `attributes` map; band 4 wants a "when was this last tended" signal, for which
   the chunk attachment used by placement tracking is the obvious home.
2. **Wardens are in the world.** Done — a woken statue posts one, so `WARDEN_CONTACT`
   and band 2 are reachable by playing for the first time. What is left is behaviour:
   a posted Warden currently stands where it was put and can be talked to. It does not
   yet **patrol**, **inspect a site**, or **cite** anything, which is what WORLD.md's
   Wardens actually do. That is the natural next block of work and none of it is new
   scope — inspect/cite/confiscate/escalate is all locked design.
3. **More scenes.** Five exist now (`warden_intake`, `warden_interrogation`,
   `shrine_keeper`, `shrine_keeper_intact`, `dream_audience`) and the machinery has
   the range it was missing, so the shortage from here is content rather than
   plumbing. The four gods have regard lines but no scenes; `dream_audience` is the
   only place any of them speaks, and reaching one properly needs **the ferry**, which
   is locked design with no code behind it yet. **Bands 3 and 4 and the
   statue-summons-Warden proposal remain the owner's call.**
4. **The dialogue screen.** A real GUI over `Conversations.Table`, replacing the chat
   rendering. Deliberately last: it is the one part this container cannot verify, and
   everything it would render is already proven server-side. Chat works meanwhile.
5. **Clear remaining `VERIFY:` markers.** The specific ones in WORLDGEN.md, DATAGEN.md
   and MODELS.md are now **cleared and marked VERIFIED against 26.2.0.67** (see below).
   What is left is the standing header caveat on each doc, which should stay: it is the
   policy, not a debt. ARCHITECTURE.md's remaining markers (capabilities → data
   attachments, payload/handler registration) are the next real batch.

## Standing warning

> **Every API specific in this doc set is marked `VERIFY:` and unverified.**

The docs were written in a sandbox whose egress proxy blocks `neoforged.net` and
`docs.neoforged.net`. Concepts, structure and formats are sound; **exact identifiers are
not** and must be read from the NeoForge sources:

```sh
find ~/.gradle/caches -name 'neoforge-*-sources.jar' 2>/dev/null
```

Modding APIs churn harder than almost anything else in software. A tutorial dated before
2026 is describing a different versioning era *and* a different registration API.
