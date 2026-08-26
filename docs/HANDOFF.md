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
| Live-world checks | **23**, every one mutation-verified, all in CI |
| Regard | recorded, persisted, **audible**, and **read** — bands, never numbers |
| Entities | Warden, Shrine-Keeper — both spec-driven, both judged in rotation |

### The first god's letter can be delivered

`WORLD.md`: *"Each world's questline opens by delivering that god's letter. Their reaction
to the news is their characterization."* The Verdant's is locked — *"the one who covered
… delivered its letter it is immediately **defensive**: it assumes it is being blamed,
before anyone has said anything"* — so the scene opens with it already mid-argument.
Nobody has accused it of anything. Nobody has spoken. It has been rehearsing for an age
and the players walked into the middle of it.

The permitted comedy is the **system**: a god's answer to a bereavement is a coverage
dispute — an arrangement, a handover that never happened, a duty nobody asked for back.
Played entirely straight; the Verdant does not know it is being funny, and the grief in
the letter is never the joke.

**The name is not in the scene, and that is load-bearing.** The reveal `WORLD.md` builds
on the letters is that the mail uses names the player has never heard, and
`letters_check.py` asserts categorically that the three appear in their own letters and
nowhere else in the shipped string table. A delivery scene is the single most tempting
place to break that — the god is being handed a letter addressed to it — so the Verdant
reacts to being addressed that way without repeating the word. *"Nobody has addressed me
like that in a very long time"* does more than the name would.

**No branch makes it comfortable, and that is arithmetic rather than restraint.** Killing
a god drops every survivor by 45 and locks a permanent ceiling at −10. The most generous
path here is worth +29 and lands at **−16**: measured live, not asserted in prose.

**A hazard found while testing that, worth fixing before more god scenes exist.** The
regard engine *clamps* to the ceiling, so an author can write `+25` into an option, a
capped player silently receives `+0`, and the choice reads as consequential while doing
nothing. `dialogue_check.py` already rejects `regard: 0` for exactly this reason —
*"omit it rather than implying a consequence"* — and the clamped case is the same defect
wearing a number. It cannot be caught statically, because whether a player is capped is
runtime state. Not yet solved; recorded so the next god scene does not discover it the
hard way.

### The overworld starts leaking somebody else's law

Band 3. `WORLD.md`, locked: *"Not their blocks. Their **rules**. The dead god's policy was
what held the systems apart — the Isolation was a policy, not a wall — and with nobody
enforcing it, patches of the overworld begin obeying somebody else's law."*

The patches sit **on the shrines**, because the shrines are already the mod's map of
where the dead god's attention was. The places its authority was strongest are where its
absence shows first — and it makes the patches contiguous by construction, which a
per-chunk roll would not. *"A hollow where nothing makes a sound"* is somewhere you stand
in and walk out of, not confetti.

Which god leaks at a given shrine is `Exodus.lawAt(chunkX, chunkZ)`: a pure function of
where the shrine is, nothing stored, nothing rolled. Walk away from a silent hollow and
come back next week and it is the same silent hollow. **A patch that changed god between
visits would be weather**, and `WORLD.md` is explicit that band 3 is reconnaissance —
*"the apocalypse is teaching you the curriculum"* — so it has to hold still long enough
to be learned.

It is a finalising integer hash rather than `(x + z) % 4`, and that is not fastidiousness:
`(x + z) % 4` draws diagonal stripes, so a player walking one direction meets the same
god over and over and three quarters of the curriculum never appears. The self-test
asserts the decorrelation by **sampling a 41×41 grid and walking an anti-diagonal**
rather than by trusting the comment above it.

**The laws are not reimplemented.** A leak calls the same method the god's own dimension
calls — `Verdant.grow` for the Verdant, `Hearth.age` for the Hearth-Turner. Not a copy,
not a table tuned to feel similar. A curriculum that taught a slightly different lesson
than the exam would be worse than no curriculum, and this way there is only one method to
change.

**Three of four leak, and they do not all arrive by the same door.** Growth and ageing
are per-chunk operations on blocks, so they come through `Leaks.apply`, which the level
tick calls once per leaking chunk. **The Anchorite's is per-entity** — a falling block
rises — and there is no honest way to write that as a chunk operation, so it comes
through `Leaks.leaks`, which `AnchoriteEvents` asks about the block's own position on the
same `EntityTickEvent.Pre` it already uses for the Mass Authority. Sand in an overworld
patch rises for *literally the same reason* it rises in the Anchorite's world.

**The Quiet One's is the one still missing**, and it is the one `WORLD.md` actually
promises by name. Its law is per-*dimension* — bed rules, respawn anchors, ambient sound
— and none of that has a region-shaped form. *"A hollow where nothing makes a sound"*
needs client-side audio suppression over a region, which this container has no way to
verify, so it would ship on the strength of a comment. Not blocked on the owner; simply
not built, and named here rather than quietly omitted.

`tools/exodus_check.sh` builds eight shrines two chunks apart, asks the **server** which
god each one leaks (never recomputing the hash — that would be a restatement of the
implementation, and it would agree with a broken one), and then measures what actually
happens at each. Both laws are asserted **categorically**, and getting the Verdant's
there took three attempts. Grass spreads in the overworld on its own, so the first version
(demand zero) failed on a clean run and the second (compare with a margin) came within one
unlucky draw of a red build for nothing. The third stops tolerating the confound and
removes it: `Verdant.grow` does its own explicit random ticks and never consults
`random_tick_speed`, so setting that gamerule to zero switches vanilla's growth off and
leaves the mod's untouched. Measured: **16 and 14 of 16** at the Verdant shrines, **0 of
16 at all six controls**. The Anchorite's needed no such help — nothing in vanilla makes
sand rise — and sand lands at all seven non-Anchorite shrines and rises only at the one.

The check also asserts, before concluding anything, that **the world was running**: it
reads gametime on both sides of the sand window. That is there because the world once was
not — see the entry below. It also proves nothing leaks at band 0,
that one position reports one god twice running, and that a loaded position five chunks
from any shrine leaks nothing at all — which is what makes it a *patch* rather than a
switch.

### The world had been quietly stopping after sixty seconds

A dedicated server ships with `pause-when-empty-seconds=60`. Every check here runs with
no players connected, so from a minute after boot the world **stops**: no warning, no lag
message, no exception. Probes keep answering, accurately, about a world that is no longer
running.

`exodus_check.sh` is the first check that ever waited longer than a minute, and it spent
several runs reporting in confident detail that the Anchorite's law had escaped its patch.
It had not. `tools/server_smoke.sh` now writes `pause-when-empty-seconds=0`, so no future
check has to know this exists. `turning_check.sh` waits forty-three seconds and had been
living just under the edge.

Nothing that shipped was wrong because of it — every other check waits well under a
minute, and their results stand. Recorded in `LESSONS.md` #30, with the control that
caught it: a check must be able to answer *did anything happen in this world at all*
before it concludes anything from what did not happen.

### A Warden walks a beat

The Warden used `WaterAvoidingRandomStrollGoal` — the goal a sheep uses. It looks fine
for ten seconds and it is wrong for the reason the mod exists: a random walk is an
**animal foraging**, and a Warden is a **unit on a post**. What separates them is not
speed or path or animation. It is that the unit does the same thing in the same order.

`WardenPatrolGoal` walks four points on a ring of 8 around its statue, always in the
same order, always starting from the same leg, pausing two seconds at each. A player who
watches for two minutes can say where it will be next.

**That predictability is the feature, not a shortcut.** WORLD.md locks the thesis that
violence does nothing and the exploit a player finds is *administrative* — and an
enforcement system you can plan around is a prerequisite for that. A Warden that
wandered unpredictably makes the only answer "wait and hope". A Warden with a beat makes
the answer *"it is at the north point for four seconds every circuit"*, which is
something a person can use. It arrives first as **this thing is not improvising, it is
executing**, and only later as *and therefore I can time it*.

An unposted Warden has no home and so no beat, and simply stands — which is also
correct: nothing has told it where to be.

`tools/patrol_check.sh` asserts the property that is actually hard to fake. **Two
Wardens posted in the same tick on identical flat ground stay in step**, because both
execute the same fixed route from the same leg; two strolling mobs draw from the level's
shared random and diverge within seconds. Plus: the Warden is out on its ring rather
than sitting in the middle of its tether, counted across samples rather than caught once.
Watched failing on the leg order being randomised, and on the sheep goal being put back.

**It found a real bug on its first run** — see [`LESSONS.md`](LESSONS.md) #25. `moveTo`
returns whether a path was accepted, and I discarded it. `GroundPathNavigation` refuses
to path unless the mob is `onGround`, and a freshly-spawned mob is not, because goals
tick *before* movement in the same `aiStep`. The first call of every posting could only
fail, and the discarded failure cost a ten-second timeout per corner.

**At a corner it inspects: it files a return on the site it is standing in.** A census,
not an accusation — see `SiteReturn`. The check asserts the part that can be wrong
invisibly: not *that* a return was filed but that **the count answers to what is there**,
because an always-zero counter and an empty world file identical returns. Site A's north
corner has three claimed blocks beside it and files `built=3`; site B's has none and
files `built=0`. Watched failing on the survey being stubbed to zero.

The two units also filed at *identical relative offsets* from their own statues, which
independently corroborates the keep-step assertion above.

**Known gap, stated rather than papered over:** the goal files only on ARRIVAL, never on
a leg it abandoned. Moving the filing to fire on abandonment too was tried as a mutation
and `patrol_check.sh` stayed green — correctly, since on flat open ground no leg is ever
abandoned, so the mutation is inert there. Pinning it needs a site with a walled-off
corner, and the cheap versions of that fight the keep-step assertion. **The rule is in
the code and it is not yet checked.**

**Not built: cite, confiscate, escalate — and the blocker is design, not effort.** A
citation needs an offence, and the mod does not have one it can find. The locked
countermeasures (shielded casting rooms, forged dispensations) say what the Wardenate
polices is *casting*, and magic does not exist yet. Of the two locked offences needing
no magic, one is the sleep code and the other is **permitted airspace** (`WORLD.md`:
*the height limit | permitted airspace*) — and the second is only findable once the
unraveling has loosened the limit enough for anybody to break it. Picking a lower ceiling
to make it findable today would be inventing a rule the world does not have. **Owner's
call — see "Waiting on owner".**

### The dead god's mail

Four letters, one per god, in `data/interregnum/letters/post.json` with their prose in
the lang file. `WORLD.md`: *the route to a successor is the dead god's unanswered
correspondence to its estranged family, and you are the only one left carrying their
mail.*

**The reveal these exist for can be destroyed from anywhere in the mod.** Not by touching
the letters — by a villager mentioning Rill, a Warden docket carrying Ballast, a scene
that has somebody say Ash. Any of those spend the name early, the letter lands as
recognition rather than as a stranger's correspondence, and *nothing anywhere fails*.
So `tools/letters_check.py`'s load-bearing assertion is a **negative one about the whole
shipped string table**: the three names appear in their own letters and absolutely
nowhere else. Watched failing on a planted villager line.

**Three open with a name; the fourth opens `To —`.** That is a rule about a *set*, so no
individual letter can be checked against it — an unaddressed letter is legal (exactly one
must be) and an addressed one is too. It lives in `core/letters/Post`, with mutations, and
is re-checked in the fast gate and again on a live server.

**Absence is `Optional.empty()`, never `""`.** `To —` is a decision and `To ` is a typo,
and in a JSON file they are one keystroke apart. The core `Letter` refuses a blank
addressee for exactly that reason, so the codec uses `optionalFieldOf` with no default —
a default would turn a caught mistake into an uncaught one.

**Voice:** the dead god speaks in procedure, because that is where the Wardens got it.
These are not laments — they are correspondence between colleagues who have stopped
speaking, filed by somebody who has one register and is using it to say something else.
Every letter carries a `SUBJECT:` line, and the check enforces it: that prefix is the tell
that this is filed correspondence rather than a farewell. None of them explains the plot,
the same constraint the last letter is built on.

`interregnum letter read <god>` is the seam CI reads through, since a letter is a thing a
player reads and a headless server has nobody to read it.

Two check bugs worth recording, both false positives that named the wrong culprit:

* `grep -q 'interregnum.letter.'` with **unescaped dots** matched the echoed command line
  `$ interregnum letter read verdant` — the dots matched the spaces — so the check
  reported a raw translation key while the letter had rendered perfectly.
* A broken post makes the loader log an ERROR and degrade to no mail, which is correct,
  and which `server_smoke.sh` then fails the whole run for. Reporting *"the run did not
  complete"* there is true and useless; the failure path now looks for the loader's own
  message first and quotes it.

**You can carry one.** `interregnum:sealed_letter` — **one** item for all four, named
`Sealed Letter`, and its name says nothing. Not "Letter to Rill", not four items with
four names: you are the only one left carrying this mail and you do not know who any of
them are. The item telling you would spend the reveal in a tooltip before the letter was
even opened. Which letter it is rides in a data component as a **god id**, never as an
addressee, for the same reason — and `mail_check.sh` asserts that, because
`letters_check.py` guards the lang file and cannot see a component.

Stacks to one. Four letters that stacked into a pile of four would be four copies of one
object, and these are four different objects that happen to look alike — which is also
true of them as writing.

**That check was a silent no-op on its first version**, and it is worth reading
[`LESSONS.md`](LESSONS.md) #26 for: it split the server output on `"Item has the
following entity data:"` to isolate the item, and `data get entity` names the entity by
its *item*, so the real line is `Sealed Letter has the following entity data:`. The split
never happened, the check reported clean for any input, and the mutation it was written
for was caught by the assertion on the next line — which is the most comfortable way for
a dead check to hide.

**Not built, and it is the owner's call:** *where a player finds the mail.*
`WORLD.md` says you are the only one left carrying it and never says how you came to be.
Shrine chests, the crater where the god died, the shrine-keeper handing them over — each
implies something different about whether the dead god sent them and they came back, or
never sent them at all. `interregnum letter seal <god> <pos>` is the seam CI uses; there
is no in-world source yet. **See "Waiting on owner".**

**Also not built:** delivery. `LETTER_DELIVERED` needs a scene per god — their reaction to
the news is their characterization — and scenes are the next content gate.

### The crossing crosses

`WORLD.md`: *travel between systems is only by ferry.* The ferry existed and could not
leave its own dimension, so that sentence was half true — a hull cleared for the Quiet
One's crossing was moved sideways within the overworld.

**The destination is a property of the LAW, not a parameter.** `interregnum ferry sail
<keel> <law> <pad>` reads where the crossing goes from the law the hull was cleared
against, so a hull cleared for the Quiet One arrives in the Quiet One's world and cannot
be sailed anywhere else. The boarding notice a player reads on the dock names the
destination; this is what makes that true rather than flavour.

Each law in `data/interregnum/ferry/laws.json` now carries a `destination` dimension id,
decoded to an `Identifier` at load so a typo is a loud failure. It is deliberately **not**
checked against the loaded dimensions there — datapacks load before levels do, and a law
naming a dimension another datapack supplies is legitimate — so a missing destination is
refused at sail time, where it can name what is missing instead of taking down every law
in the file.

`Ferry.place` still clears the origin in a full pass before writing anything, which looks
unnecessary across two levels and is not: the commonest crossing of all is the one that
does not change dimension, a player nudging a ferry three blocks sideways while still
building.

`tools/ferry_check.sh` now asserts every arrival marker **inside**
`interregnum:unresponsive`, plus a `NEVER_LEFT_HOME` marker that fails the run if the
hull is sitting at the destination coordinates back in the overworld. Watched failing on
the destination being ignored, and on every law being pointed at the same world.

**Still missing, and it is now the whole gate:** the ferry reaches a god's *surface*.
`WORLD.md` locks each god as **surface · under-layer · far-layer, joined by that world's
own portal logic** — and none of the under-layers, far-layers or native portals exist.
`FIRST_CROSSING` is reachable; `LETTER_DELIVERED` needs letters, which need scenes.

### The Hearth-Turner's world: nothing is allowed to be over

`interregnum:temporal_authority` — the **fourth and last** god-world surface. All four
destinations now exist.

**The reuse note is locked and this is it.** `WORLD.md`: *"the block-aging registry
powering the Turning **is the same system that runs the unraveling.** One mechanism; a
school and an apocalypse."* So the ageing table uses the unraveling's own
`ConversionDef` — the same record, the same codec, the same JSON shape, imported rather
than copied.

**What is *not* shared is the character.** The unraveling's rules carry a band and a
scope because it escalates and has a frontier. Ageing has neither: it is what time does,
everywhere, always, at the rate it did yesterday. The overworld is coming apart because
nobody is holding it. This world is not coming apart at all — it is *accumulating*, and
will not let any of it go. Same god who *"has never let a grievance become past tense"*,
expressed in masonry.

**Chains, not jumps.** Stone does not arrive mossy: it is stone, then cobble, then mossy
cobble, because each rule's `to` is another rule's `from`. Walk back through somewhere
you built and you can read how long ago you were there off the walls.

**Not the Verdant with different blocks**, and that mattered more than anything else
here. The Verdant asks vanilla for *more of what it already does* — nothing new happens,
it happens sooner. Vanilla has **no notion** of stone acquiring moss with age and never
will. Different mechanism, different feel, and it makes this check *categorical* where
the Verdant's is statistical: any ageing at all in the overworld is a leak, asserted
directly rather than as a ratio.

**The bed is the fourth answer and the only permissive one — which is the joke.** Sleep
works, spawn-setting works, the anchor works, and `has_fixed_time` is on, so the night
does not pass and none of it achieves anything. The Hearth-Turner refuses you nothing; it
just will not let go of the time you were trying to skip. This is the one world allowed a
fixed sky, and the other three were denied one precisely so it would mean something here.

**The claim promise is categorical here**, unlike the Verdant's: ageing applies to the
block being aged, not to a source reaching a neighbour, so there is no indirect path to a
claimed block.

`tools/turning_check.sh` has two halves that prove different things. **Deterministic**,
through a new `interregnum turning age <pos>` seam (same shape as `unravel at` and
`warden post`): the chain, the claim refusal and the dimension gate — because waiting for
two independent rolls to land on one block would turn a categorical fact into a
statistical one for nothing. **Passive**, by waiting: that it happens at all unasked.

Three things it taught:

* The first draft waited 40 seconds for one block at 1 sample/section/tick and got
  nothing, while the law worked perfectly. The rate is now vanilla's own budget spent on
  memory instead of growth.
* **The setup probes must run before the commands that could alter them.** They didn't,
  so a mutation letting the command age the overworld reported *"no stone was placed in
  the overworld"* — false, and pointing at the wrong file. A diagnostic that misattributes
  is not much better than one that doesn't fire.
* A failure message used **backticks inside a double-quoted string**, so bash ran
  `interregnum turning age` as a shell command and delivered the failure with its own
  subject missing — on the failure path only, so exactly when somebody most needed to read
  it. [`LESSONS.md`](LESSONS.md) #23 with a new way to break the same rule.

### The Verdant's world: everything grows, and that is the hazard

`interregnum:green_authority` — the third god-world surface.

**Locked, and it says hazard.** `WORLD.md` on the Verdancy school: *"and in the
Verdant's own world, accelerating growth is a **hazard**."* Fast growth as a convenience
is a farming mod; as a hazard it is a place where you cannot keep ground clear and the
path you cut closes behind you. The mechanism is identical — what makes it a hazard is
that it applies to everything, everywhere, and not to the things you wanted.

Implemented as **more random ticks**, not a list of growable blocks: vanilla already
grows things this way, so every crop, sapling, vine, moss and mushroom is covered
without naming any, and so is anything a future version adds. Eight times the overworld
rate, stated as a multiple on purpose so it can be described honestly against a player's
hundred hours of intuition.

**Score so far: two gods cost code, one cost data.** Silence was entirely 26.2
attributes. Weight and growth have no attribute at all.

**The bed is the third different answer**, and `dimension_check.py` now holds all three
apart: the Quiet One declines to react, the Anchorite detonates, the Verdant lets you
sleep and will not hold your spawn — *"the one who covered"*, which took over the
overworld's duties in an older crisis and never quite handed them back. It will cover
you for a night. It will not take responsibility for you.

**The claim promise here is narrow, and the wide version would be a lie.** `Verdant.grow`
never applies its extra ticks to a block somebody placed. It is *not* "a block you placed
can never change here" — vanilla grows things by ticking a **source** which reaches to a
neighbour (grass spreads by ticking the grass, not the dirt), so an unclaimed grass block
can still turn claimed dirt beside it, and no check on the ticked position prevents that.
What vanilla's ordinary spread reaches is what it reaches at home. **Not covered by a
live check**, because over any CI-length window the difference is statistical rather than
categorical.

`tools/verdant_check.sh` compares **thirty-two targets in each world over the same
window**. Two findings shaped it:

* A single target block is unobservable. Random ticking picks uniformly from a 16³
  section, so one block is hit about once per eight seconds even at 8×; the first draft
  failed while the law underneath worked perfectly, and a probe showed the handler
  happily ticking grass at the terrain surface while the two test blocks sat at y=100
  being missed.
* The margin is measured, not chosen. Four runs at sixteen targets gave 8–12 versus 1–4
  — real but uncomfortably close at the extremes — so the sample was doubled.
* **A mutation survived and forced a second assertion.** Removing the dimension check so
  the law fired in *every* level gave 25 there and 21 at home: still "more here than
  there", and a catastrophe. The overworld count is the diagnostic, so it is now asserted
  directly with a ceiling.

### The Anchorite's world: unanchored things rise

`interregnum:mass_authority` — the second god-world surface layer.

**This law was already promised in the mod's own words.** The ferry has printed it as a
boarding refusal since before the world existed: *"Nothing that pours. Where you are
going, unanchored things go up, and they do not stop."* A player reads that before they
arrive, so the world has to mean it — and that line is the constraint on the
implementation, not just its flavour.

**"And they do not stop" is load-bearing.** A rising `FallingBlockEntity` never satisfies
vanilla's ground test, so it never places itself; it climbs past the build height and
vanilla's own timeout discards it. Nothing had to be written for that. A version that
stuck sand to ceilings would be a nicer toy and a broken promise.

**Weight costs code where silence cost data.** Every one of the Quiet One's rules turned
out to be a 26.2 `dimension_type` attribute. Weight is not — there is no gravity
attribute, no fall-damage attribute, nothing that inverts anything; the full list was
read out of `EnvironmentAttributes`, not guessed. So this law is a tick handler. The
asymmetry is worth recording rather than hiding: the platform made one god cheap and the
next one not.

**The two worlds refuse a bed differently, and that is deliberate.** The Quiet One
declines to react at all. The Anchorite detonates. One boolean is most of the difference
between two gods, it costs nothing to get wrong in a refactor, and no test looking at one
dimension alone would notice — so `dimension_check.py` now holds them apart explicitly.

`tools/anchorite_check.sh` measures both halves, because either alone is worthless: at
home the sand lands (the control), and in the Anchorite's it does not land, is no longer
where it was put, and a falling-block entity is found *above* its start. Those last two
clauses are what make it an assertion about **rising** rather than about vanishing —
"did not land" is equally satisfied by sand that was deleted, sand that fell through the
void, and sand whose gravity was switched *off* rather than reversed. The first draft
waited six seconds, by which time the sand had legitimately left the world, and could
not tell "rose out of the world" from "never moved". Watched failing on the law never
applying, and on gravity being zeroed instead of inverted.

**Deliberately not lifted: dropped items, players, mobs.** Only `FallingBlockEntity` —
exactly the class the ferry's law names. Lifting items away would mean every death in
this world costs the whole inventory with no way to chase it. That is a large design
consequence, it is nowhere in WORLD.md, and inventing it here would be new scope arriving
disguised as a detail. **Owner's call — see "Waiting on owner".**

### The Quiet One's world answers nobody

`interregnum:unresponsive` — the first god-world surface layer, generated as data by
`ModDimensions`.

**The law is not "quiet".** Silence-as-decoration would be a muted soundtrack, and
would fail [`AESTHETIC.md`](AESTHETIC.md)'s executioner: *could I replace it with a
different random weird thing without changing anything?* The law that earns the world
is the one already printed on every Warden docket that mentions it — `SUBJECT:
UNRESPONSIVE`. So every affordance in Minecraft that consists of **asking the world for
something** is dead here, and each one is an attribute rather than code:

* **A bed does nothing at all.** Not a refusal — *nothing*. `can_sleep: never`,
  `can_set_spawn: never`, `explodes: false`, and **no error message**. The Nether
  refuses you loudly and the End refuses you loudly; both explode. This place declines
  to react, and the entire difference is one absent boolean and one absent string.
* **A respawn anchor does nothing.** Same refusal, same silence.
* **No raid can ever start.** Nothing is summoned here, by anybody.
* **The world makes no sound of its own** — and the audio attributes are declared
  *empty* rather than omitted, because omitted means inherit and what would be
  inherited is the overworld's cave moaning.

There is no way to shorten a night here, no way to make this place a home, and no way
to be answered. That is a rule a player has to adapt to, not a palette.

**Terrain is a placeholder and says so in the file.** Vanilla noise, one biome
(`the_void`, chosen for the ambience it does *not* carry), ground you can stand on. The
under-layer and far-layer do not exist and no letter can be delivered here yet.

Verified in two halves, deliberately, because they prove different things:

* `tools/crossing_check.sh` (live) proves it is a **separate place with our floor**.
  Every assertion is a relationship between two worlds rather than a fact about one:
  y=-10 is legal at home and illegal there, y=250 is legal in both, and a block written
  there is not at the same coordinates at home. A check that only proved "the dimension
  loads" would pass against a stem mis-wired to `minecraft:overworld` — watched failing
  against exactly that.
* `tools/dimension_check.py` (static) proves **the law is still the law**, because a
  headless server exposes no command that reads a dimension's attributes back. Weaker,
  and labelled as weaker: it says *the data we ship declares this*, not *the game
  behaves like this*. It catches the failure that would actually happen — somebody
  edits `ModDimensions`, regenerates, and quietly hands the bed back its explosion.
  Watched failing on exactly that, and on the audio attributes being omitted.

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

`tools/ci_claims_check.py` counts the live-world checks in the workflow on every push
and fails if the table above disagrees. It was written because the table *did*
disagree — it said seventeen and the workflow ran fifteen, drift accumulated over
several sessions of adding a check and bumping a number by hand. Nobody catches that
by reading, because the only way to catch it is to count a workflow, and nobody counts
a workflow. The count in that row is now the workflow's, not a claim about it.

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

1. ~~**"Warden" collides with vanilla's Warden."**~~ **Decided — the name stays.** The
   owner delegated this one. See [`WORLD.md`](WORLD.md) *"Warden is not its name — it is
   its rank"*: the collision only bites if *Warden* is what the thing **is**, and it
   never was. A Warden never calls itself a Warden in any line shipped — it says *"this
   unit"*, every time — and the word appears in exactly three places, all of them the
   Wardenate's own paperwork. So it is a rank an office grants, not a species, and the
   unit now has its own four-voices row like the Theoclast and the four gods: *this
   unit* / **Warden** / *a docket* (villagers) / *a posting* (Theoclasts). No code
   change, no id change; the lore was already correct and had not been read.

2. ~~**Should the Anchorite's law lift dropped items too?**~~ **Answered: no.** The
   owner's words: *"I dont want death to cost the whole inventory."* Only
   `FallingBlockEntity` rises — sand, red sand, gravel, anvils, exactly the set the
   ferry's boarding notice names. Dropped items, players and mobs are untouched, which
   is what already ships, so this needed no code change.

   **[PROPOSED — not built, owner's call]** There is a version that keeps the law
   whole without the punishment, and it is worth recording before it is forgotten.
   *"They do not stop"* leaves a question the mod never answers: **where do they go?**
   If everything that rises out of that world is *received* somewhere — a counter, a
   ledger, a clerk who will return your property once you have described it correctly
   — then items could rise, the law would be literally true, and losing your inventory
   would be the start of an errand rather than the end of a session. The Anchorite's
   Warden dockets already read `SUBJECT: MASS AUTHORITY`; an authority that catches
   everything the world lets go of is the same joke as the rest of the mod, and the
   retrieval scene would be one of the few places a player *wants* to talk to
   bureaucracy. Costs a room, an inventory, and a conversation. Not built.

3. **Where does a player find the dead god's mail?** The letters exist, and one item
   carries them, and nothing in the world produces one. `WORLD.md` says *you are the only
   one left carrying their mail* and never says how you came to be carrying it — and the
   options are not interchangeable, because each says something different about the dead
   god:

   * **In the shrine chests**, beside the heart. Implies it never sent them: four letters
     written and filed and never posted, which makes the Quiet One's *"the one who never
     wrote back"* retroactively crueller — nobody ever had the chance.
   * **At the crater**, in what is left of its desk. Same implication, but the player has
     to have killed it first, so the mail is something you find while standing in the
     hole you made.
   * **Returned undelivered**, handed over by the shrine-keeper. Implies it *did* send
     them and they came back, which is a different and sadder story, and gives the
     keeper a reason to have been waiting for somebody.

   I have not picked one because the choice is the story, not the plumbing. *No action
   needed until then; the letters and the item ship without it.*
4. **What should a Warden be able to cite you for, before magic exists?** Patrol and
   inspect are built; `cite` has nothing to accuse anybody of. WORLD.md's locked
   countermeasures are all about *casting*, which does not exist yet. Two locked offences
   need no magic:

   * **the sleep code** — *phantoms punish sleeplessness | a citation for a sleep-code
     violation*. Buildable today, and it has the right absurdity: the Wardenate is still
     enforcing bedtime for a god that is not watching.
   * **permitted airspace** — *the height limit | permitted airspace*. The better joke,
     because the enforcement mechanism (the hard limit) died with the god while the
     policy did not. But nothing currently raises the limit, so nobody can break it, and
     choosing a lower licensed ceiling would be inventing a rule the world never had.

   Either is a small build once chosen. Choosing is the owner's, because it decides what
   the Wardenate is *for* in the stretch before magic. *No action needed until then;
   patrol and inspect ship without it.*
5. **Playtesting, and looking at the Warden.** This container has no game client, so two
   things about the model are unverifiable here and are not claimed: how it looks
   **animated**, and how it looks **lit**. `tools/entity_view.py` covers shape and paint;
   it cannot cover those. The render has been sent for review.

**Answered:** license is **MIT** (`LICENSE`, `gradle.properties`). `main` branch exists;
work flows to `claude/minecraft-mod-dev-rp0x8j` and PRs into `main`.

## Open questions

### Proposed, needs the owner's yes

**What is each god's portal logic?** This is the gate on the whole of item 1 in "What to
do next", and it is the largest thing still unbuilt.

`WORLD.md` locks the grammar — *"**surface · under-layer · far-layer**, joined by that
world's own portal logic"*, and *"travel between systems is only by ferry; travel within a
system is by its native portals"*. What it does not say is **what each god's portal is**,
and that is a mechanic per god rather than a detail, so it is not mine to pick.

Four candidates, one per god, each derived from the law that world already runs rather
than invented alongside it. They are sketches to react to, not a recommendation:

| God | Law | A portal shaped like that law |
|---|---|---|
| **The Anchorite** | weight | You do not build it, you **let go into it** — a shaft that takes anything unanchored, which in that world means everything. Going *down* into the place where down does not hold. |
| **The Verdant** | growth | You **plant** it and wait. It opens when it is mature and closes when it is cut — the only portal in the mod with a lifespan. |
| **The Hearth-Turner** | time | It is always there and only **open at one hour**, which in a world with a fixed sky means you cannot wait for it — you have to make the hour happen. |
| **The Quiet One** | silence | It opens when **nothing near it makes a sound**, which is the only one of the four a player can close by accident. |

Each is buildable on hooks this repo already has, and each would make its world's law
something you use rather than something you observe. **Nothing is blocked behind this
question** — items 2 and 4 below are both unblocked — but item 1 cannot start without it.

*(The statue proposal was answered — see below.)*

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

Done this pass: the ferry (capture, four destination laws, the `ferry_keel` block, and
`tools/ferry_check.sh` in CI), and the four decisions the owner delegated back — bands
3 and 4, the god roster, whether a Warden can be killed, and the last letter. All four
are locked in [`WORLD.md`](WORLD.md) with `[LOCKED — owner delegated; decided here.]`
next to each, so the provenance travels with the decision.

**Nothing on this list is waiting on the owner.** In order, all unblocked:

1. **The systems, not just the surfaces.** All four destination *surfaces* now exist and
   each has a law that earns it. What does not exist is what `WORLD.md` actually locks:
   each god holds a **system** of connected dimensions — *surface · under-layer ·
   far-layer, joined by that world's own portal logic*. Right now there are four lonely
   surfaces reachable only by command, so `FIRST_CROSSING` and `LETTER_DELIVERED` are
   still unreachable and chapters 3–5 are still gated. **The portal logic is the next
   real gate**, and it is bigger than any single world was.

   Also missing from every one of them: **terrain that is designed**. All four use
   vanilla noise with one fixed biome, and each file says so in its own javadoc.
2. ~~**Band 3 — EXODUS.**~~ **Built.** Patches of overworld obeying another god's law,
   anchored on the shrines. Three of the four laws leak; the Quiet One's needs
   client-side audio suppression this container cannot verify, and is named as a gap
   above rather than half-built. **Band 4 (ATTRITION) is the unbuilt half** and is
   unblocked: it wants a *"when was this last tended"* signal, for which the chunk
   attachment used by placement tracking is the obvious home.
3. ~~**Warden behaviour.**~~ **Patrol and inspect are built** — `WardenPatrolGoal` walks a
   fixed four-point beat, and `SiteReturn` files what it finds. **`cite` is the one verb
   still missing, and it is blocked on the owner**: see "Waiting on owner" — a Warden
   needs something to accuse you of, and the locked countermeasures are all about
   casting, which does not exist yet.
4. **More scenes.** Five exist (`warden_intake`, `warden_interrogation`, `shrine_keeper`,
   `shrine_keeper_intact`, `dream_audience`) and the machinery now has the range it was
   missing, so the shortage from here is content rather than plumbing. The four gods have
   regard lines but no scenes; the roster decision gives each of them four names to be
   called by, which is the material those scenes were short of.
5. **The dialogue screen.** A real GUI over `Conversations.Table`, replacing the chat
   rendering. Deliberately last: it is the one part this container cannot verify, and
   everything it would render is already proven server-side. Chat works meanwhile.
6. **`VERIFY:` markers — the API-specific ones are cleared.** ARCHITECTURE.md's three
   (registration, capabilities → data attachments, payload/handler registration),
   DATAGEN.md's item-model row and TEXTURING.md's paths are now **VERIFIED against
   26.2.0.67** — most of them by shapes this repo compiles and CI boots a server on.

   **Four remain, and none of them is debt.** Each now says what evidence would clear it,
   because "unverified" and "unverifiable here" were being conflated:

   * `MODELS.md` **render types** — every block here is an opaque cube and every item is
     `item/generated`, so nothing exercises them. Clearing it needs the first cutout or
     translucent model *and a client to look at it*: unlike the datagen shapes, this one
     cannot be settled by reading the jar, because the question is whether it renders
     correctly.
   * `MODELS.md` **tint sources** — there is no tinted texture. A note to whoever adds the
     first one, not debt: `palette_check.py` would fail a greyscale source for being
     off-palette, which is the check working.
   * `PLATFORM.md` **`gradle.properties` values** — a standing caveat that the *names* are
     stable and the *build numbers* must be checked at setup. Policy; it should never be
     cleared.
   * `VERIFICATION.md` **gametests** — overtaken rather than skipped. Block and entity
     behaviour is tested by booting a real server and driving it over RCON, which covers
     what gametests would and additionally proves the mod loads. Clearing it needs a
     behaviour a command cannot reach and a headless server cannot observe.

   The standing header caveat on each doc stays: that is policy, not debt.

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
