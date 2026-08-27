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
| Doc set | **complete** — 16 documents, see [`INDEX.md`](INDEX.md) |
| Live-world checks | **49**, every one mutation-verified, all in CI |
| Regard | recorded, persisted, **audible**, and **read** — bands, never numbers |
| Entities | Warden, Shrine-Keeper — both spec-driven, both judged in rotation |
| Magic | **four schools, ten spells**, learned in their gods' worlds and **cast by saying the word** |
| Bands | 1–4 built; the overworld unravels, leaks, and forgets |
| World-systems | **all four gods have two layers**, each joined by that god's own portal; no god has a far-layer |

### The audit, made permanent

Three consecutive passes each found a system **built, verified, green in CI, and
unreachable in play** — the Haunt's dream, the sealed letter, the warning steles. Finding
the fourth by hand was not a plan.

**Why every check was green, and rightly so.** `haunt_check.sh` drove the command seam;
`mail_check.sh` read the letters out of the data. Both were correct and both were complete
about what they cover. **A check that covers a path says nothing about whether anything
else reaches it** — and no test this project knows how to write would have told the
difference, because "is there a right-click handler that calls this" is not a property of
the thing being called.

So the route is written down and the writing is checked.
[`REACHABILITY.md`](REACHABILITY.md) lists every registered block, item and entity with
how a player touches it and a status from a fixed set — `PLAY`, `OP`, `SCENERY`, or
`BLOCKED: <question>`. `reachability_check.py` enforces three things and refuses to
pretend to a fourth:

* every registered id appears in the table;
* nothing appears in the table that is not registered — the direction a table goes stale,
  when content is deleted and its row survives promising a player something that is gone;
* every status is from the set, so "reached by" cannot decay into prose.

**It does not check whether a status is true**, and says so in its own docstring. Nothing
static can: `PLAY` is a claim about a handler somewhere and a wrong one is precisely the
bug. What it enforces is that somebody had to write the claim down, which is the whole of
what was missing all three times.

Watched failing three ways: an item registered and undocumented, a row for something that
does not exist, and a free-text status.

**One finding recorded rather than fixed.** `shrine_stone_carved`'s javadoc says it carries
*"a band of the dead god's script"* and nothing can read it — and unlike the steles, making
it readable would be the wrong move. `WORLD.md` marks the whole reading lane **[PROPOSED]**:
raw god-script read without transcription *marks* the reader. Shipping plain readable
inscriptions would settle that in the safe direction, which is the owner's to settle. It is
in "Waiting on owner" now.

### The steles say something

The block, its texture and its model have existed since the chapter-0 art pass. There was
**no text on any of them anywhere** — and the shrine-keeper has been telling players for
just as long that *"the steles are readable if you have the light for it; most people don't
bother, and I have never held it against anybody."* A shipped line of dialogue describing a
rule nothing implements is worse than a missing feature: it is the mod lying in its own
voice.

**Five notices, and every one is the Wardenate explaining a rule that is about to stop
being true.** Four are the locked vanilla-rules-as-policy entries — permitted airspace, the
sleep code, incineration at dawn, the world's floor — and the fifth says what to do if any
of them ever fails. `WORLD.md` calls the steles *"chapter 0 dressing that players read as
ruin flavour for hours, and which after the death is the only instruction anyone left
behind."* The fifth notice is that instruction, written by somebody who did not believe
they were writing it.

**Not one word changes at the deicide, and that is the joke.** `WORLD.md`'s locked comedy
list names *"steles that re-read differently"*. Text that swapped at the death would throw
that away: what re-reads differently is **the reader**. So an inscription is a constant and
nothing in `Steles` takes a chapter.

**Which notice stands where is a pure function of the coordinates** — band 3's idiom, *"a
hollow you walk out of is the same hollow next week"*. A stele you read yesterday says the
same thing today, two steles in one ruin can differ, and none of it costs a block state or
a saved field. `floorMod`, not `%`: a negative hash under `%` indexes backwards off the end
of the list, and works perfectly in every world anybody tests near spawn.

**And the light rule exists because the keeper says it does.** Seven: outdoors in the day
is always enough, a lit room is enough, a ruin at night or a buried stele is not. It gets
sharper after the death, with nobody left to turn the sun — a world whose god died at night
has steles it can no longer read.

**The bug the probe caught, which no amount of reading would have.** The first version
asked for the light *at the stele*. A stele is an opaque block and the inside of an opaque
block is dark in every world there has ever been — so a stele in open daylight reported
itself unreadable, and one buried in stone reported itself fine. Wrong everywhere, and
wrong in a way that reads as a rule rather than a bug. It takes the brightest of the six
neighbours now, which is also the honest question: *is there light on this thing anywhere.*

`stele_check.sh` asserts a lit stele reads out its notice **and its body** (a header with
nothing under it passes every count), that one stele reads the same twice, that six steles
do not all say one thing, and that an unlit one says it cannot be made out. Watched failing
on the light-at-the-stele bug (0 of 6 readable) and on a hash collapsed to a constant.

**One flake found and removed:** the buried stele read out perfectly on one run, because
the check asked before the engine had finished propagating light into the new stone. It
waits now — three seconds for a five-block cube is a bounded computation with enormous
margin rather than a threshold on a random variable.

### A letter that can be opened

`sealed_letter` had been `registerSimpleItem` since the first registry pass: **no
behaviour at all.** The letters themselves were fine — written, loaded, validated, and
readable through `interregnum letter read` — but the thing a player would be holding did
nothing when they used it, so the mid-game's best reveal was reachable only by an operator.

Unlike casting or attuning, the affordance was never an open question. You read a letter by
opening it. `SealedLetterItem.use` prints the page; the stack already carried which letter
it is, through a component that has existed all along.

**One renderer, two callers.** `LetterPage.of` builds the page and both the item and the
command go through it. The command used to render it inline, which was fine while nothing
else could open a letter — and the moment the item could, it would have been two renderers
to keep in step with only one of them reachable by a check. `mail_check.sh` now covers the
item's page as a side effect of covering the command's.

**The salutation is IN the letter and the page does not add one.** The obvious shape was to
render `To <addressee>` above the body. Every letter's first body line already *is* its
salutation — *"Ballast —"*, *"Rill —"*, and for the fourth, *"To —"* — so a rendered one
would print the name twice, above the `To —` that is the point of the whole set. The
`addressee` field is the machine-readable half of the same fact and earns its place
elsewhere: `Post` enforces that exactly one letter in the set is unaddressed, which is an
invariant about a SET and cannot be read off any single letter's text.

**[VERIFY] — the right-click, not the page.** No client here, so no player can use an item
on a headless server. The page it produces is verified, by `mail_check.sh`, through the
command that now shares it.

### I overwrote a file I thought I was creating

Worth its own note because none of the usual defences caught it. The component this needed
already existed, in a `ModComponents.java` I wrote from scratch and thereby replaced. The
build stayed green (same component, same registration), the checks stayed green (they read
letters through the command, which did not change), and `git status` said `M` rather than
`??` — which I did not look at, because I believed I had just created the file.

What was lost was a rule with no check behind it: *the component must never carry the
addressee, because a stack in a hotbar is a string a player can see, and the names are
meant to be unheard until the letter is opened.* Restored from git.
[`LESSONS.md` #39](LESSONS.md#39-cat-is-not-a-way-to-create-a-file).

### The ghost could not reach anybody

`WORLD.md`, locked, on the Haunt: *"dream-audiences: **sleep** sometimes routes the killer
to a small dimension where the ghost…"* The scene, the gate and the once-only rule had all
existed for a long time. The only thing that could reach them was
`/interregnum haunt dream`. **The mod's best beat could not happen in play**, and nothing
said so — `haunt_check.sh` was green throughout, because it drives the command seam, which
is exactly what it is for.

This is the same shape as the seven questions in "Waiting on owner" — a system built and
verified and unreachable — with one difference that makes it mine rather than the owner's:
**the affordance is locked.** `WORLD.md` names sleep. There was nothing to decide.

**It fires whether or not the bed would have worked, and that is the whole thing.** The
death stops the daylight cycle — locked, and the entire announcement of the death. So if
the god dies in the afternoon, **night never comes**, and a rule of "the dream arrives when
you successfully sleep" would have made the Haunt unreachable in precisely the worlds it is
about. `HauntSleepEvents` never looks at the vanilla problem: you lie down in a world where
the sun has not moved since you did it, and the thing you cannot stop thinking about takes
you anyway. Two locked beats that would otherwise contradict each other, and the
contradiction is the better version of both.

The refusal is `OTHER_PROBLEM`, the one that carries no message, because the world does not
narrate what it is doing to this player. Every other outcome leaves the event exactly as
vanilla decided it: `TheHaunt.offer` already refuses a non-killer, a living god, a second
dream and a player mid-conversation, so the handler adds no rule of its own and there is
one implementation of the gate to get wrong.

**[VERIFY] — the trigger, not the scene.** No client here, so no player can right-click a
bed on a headless server and that branch is not exercised. What it calls is: `haunt_check.sh`
drives `TheHaunt.offer` and asserts the ghost reaches its killer once and nobody else ever.
The handler is deliberately three lines of adapter over that — the arrangement `Deicide`
documents, and for the same reason. Clearing it needs one player, one bed, and a client to
hold them.

### More of the world to lose

Nineteen conversions where there were nine. Bands 1 and 2 are the only part of this mod a
player can reach today — everything past the deicide is behind one of the seven questions in
"Waiting on owner" — so the unraveling is where content is worth adding, and it was thin: a
thin place took away four kinds of ground plant, and the overworld cracked five kinds of
block.

**Band 1 finishes its own chain.** Cornflowers and oxeye daisies wilt like the poppies and
dandelions already did, and **a dead bush eventually crumbles to nothing.** That last one is
the point: until now a thin place converged on a field of dead bushes and stopped, which
reads as a state rather than a process. Now it goes all the way to bare ground, and a player
who keeps coming back can see how far along it is.

**Band 2 loosens more rules.** Birch and spruce canopies thin as oak already did; andesite,
diorite and granite crack to cobblestone and join the existing cobble-to-gravel chain;
sandstone slumps to sand, which then falls, which is the first conversion in the table whose
consequence is not the block it names. And **dirt paths forget they were walked on** — the
one addition that takes away a human mark rather than a natural one, which is what band 2's
locked *"rule loosening"* is really about.

All of it is inside the standing guarantees, and `unraveling_check.py` enforces them: every
`from` is a naturally-generated vanilla block, nothing player-built appears on either side,
no rule reverses another, and every chance stays in the range the existing table already
used. The oscillation guard was watched failing on a deliberate `sand → sandstone`.

**A stale number in a check, removed rather than updated.** `unravel_check.sh` asserted
`"2 band(s), 9 conversion(s) in force"` with the nine typed into the shell script, so adding
a conversion failed a correct build — which is the way round that gets a guard deleted
rather than fixed. It counts the rules out of `bands.json` now, which is what the assertion
always meant: *everything in the file loaded*.

### The god shatters

`WORLD.md`, locked: *"The god's power enters its killer. An ordinary Minecraft body cannot
hold it. The overflow detonates outward, scattering **splinters** at shrines and the
crater."* And: *"the shattered god-pieces are **clasts** (item). Anyone may attune one;
**clasts are finite** — the class is a server negotiation."*

The item had existed since the first registry pass and **nothing in the world produced
one.** `PlayerTags` said so in its own javadoc: *"the Theoclast class does not exist yet —
no clast can be attuned, so no player can truthfully hold it."*

**Finite is the mechanic, so the count is the mechanic.** Everything else this mod produces
falls out of a rule applied to whatever is there — the unraveling converts what it finds,
the Verdant grows what can grow. This does not. There is a number, it is small, and when it
is gone it is gone, because *"the class is a server negotiation"* only means anything if
there are fewer clasts than people who want one. That is why the pool is one per world
rather than a per-shrine yield: a world with forty shrines would otherwise hand out forty
classes.

`Clasts.TOTAL = 7` — **[NEEDS PLAYTEST]**, and `WORLD.md` marks it so. Seven: small enough
that a server of any size has to decide who gets one, odd so it cannot be split evenly by
two factions, and more than the four gods so a full set is not the obvious goal. A starting
position, and one constant, because that is what makes it cheap to change.

Three land in the crater at the moment of death — the largest single share, since that is
where it happened, but not most of them, or a killer could pick up the whole class standing
still. The rest are at shrines, one each, handed over **as a shrine is found** rather than
at the instant of the death: the deicide can only reach loaded chunks, which is the same
constraint the statues have and the same solution, and it is the better beat for the same
reason — a player who walks to a shrine days later and finds something on the step has
*found* it.

**They do not despawn.** There are seven in a world, ever, and a finite thing that can be
lost to a five-minute timer is not finite, it is random. It also reads correctly: a piece of
a god does not rot.

**A shrine is marked whether or not it paid.** The statues need no equivalent — a woken
statue is a different block, so waking is self-marking. Scattering is not, and a shrine
chunk that loaded, unloaded and loaded again would take a second clast, so a player walking
back and forth could drain a world's allowance at one shrine. A shrine that loaded after the
pool ran dry is marked too: it is finished with, not waiting, and rescanning its sections
forever for an answer that cannot change costs a scan per load.

**Two things the check got wrong before the mod did.** The despawn assertion was written
against `Lifespan` — `ItemEntity.setUnlimitedLifetime` writes the `-32768` sentinel into
**`Age`** and leaves `Lifespan` at its default 6000, because the tick loop stops counting
rather than raising the cap; a check reading `Lifespan` reports a despawning item that is
not despawning. And the "found twice" leg used a forceload remove/add cycle, which unloads a
chunk only sometimes — it passed once and failed on a clean tree. It is three server runs
with `KEEP_WORLD=1` now, because a shrine being *found* is the thing under test and a
restart is the only way to guarantee the finding.

Still not built: **attuning**. `WORLD.md` says *"anyone may attune one"* and does not say
how, which makes the affordance a mechanic rather than a detail — the same shape as the
spell-casting question. It is in "Waiting on owner".

### The way home

`WORLD.md`: *"Travel between systems is only by ferry."* Until this increment that was a
one-way sentence — four crossing laws, four destinations, and no law whose destination was
the overworld. A player who sailed to a god's world could hop between gods forever and
never get back. Not a design decision; a missing half.

**The return is not a fifth law, and the reason is the interesting part.** `Law` refuses a
law with no rules and says why: *"a crossing that refuses nothing is not a law"* — a
checklist that can never refuse anything can never be seen to be broken. So a home law
would need something to refuse, and **the overworld has nobody left to refuse it.** Every
other checklist is a god's policy about its own world; inventing one for the world whose god
this player killed would be inventing an authority the fiction has spent the whole game
removing. And `WORLD.md` says what a checklist is *for* — *"teaches each world's rule before
arrival"* — and the overworld's rule is the one the player already lives under.

So `interregnum ferry home <keel>` is a mail service returning a vessel to the depot it
left. `Voyages` files the leg on departure, keyed by the keel's arrival position, and
**spends it on use**. A keel that never sailed gets the answer a desk gives: *no return leg
on file.*

**The check passed, and then two mutations walked through it.** The first version sailed one
ferry out and home and asserted it landed where it started. Keying the record by *world*
instead of by keel passed that. So did never deleting a spent leg. Both are properties of
the relationship between instances, and one instance makes them unobservable — the way a
one-element list cannot tell you whether a sort is stable.

The check now sails **two** ferries to one world from two origins before either returns, and
asserts the returning one lands on its own coordinates rather than the other's; and for the
deletion it plants a fresh keel by hand on a landing a ferry has already left from, which is
the hazard the deletion actually exists for — a stale record attached to a position
teleports whatever is standing there to a stranger's dock. Both mutations die against that.
[`LESSONS.md` #38](LESSONS.md#38-one-instance-cannot-test-a-per-instance-property).

### The far pad

`WORLD.md`: *"a keel block captures the structure, validates it against the destination's
law, and re-places it at **the far pad**."* There was no pad. The arrival position was a
command argument an operator typed — so the ferry did not go anywhere in particular, it
went wherever you said, and a mail service whose destination is a parameter is not one.

**The same dock, four times, in whatever was to hand.** Every pad is the identical
seven-by-seven apron with the identical three-by-three landing and four corner posts. Only
the material differs, and it differs because the Post built each one out of what that world
had — stone and polished andesite for the Quiet One, polished deepslate and deepslate tiles
for the Anchorite, mossy cobble and mossy brick for the Verdant, stone brick and *cracked*
stone brick for the Hearth-Turner, which arrives already old because in that world a new
thing would be the only object without a past.

That an institution does not redesign its dock per god is the joke the rest of the
bureaucracy runs on, said in blocks. It is also the only navigational aid any of these
worlds has: you can step off a ferry in a world where nothing answers and still know where
the landing square is.

**Not claimed, and rebuilt when it has gone.** The ledger records what a *player* placed,
and nobody placed this — so the Verdant grows over its dock and the Turning ages it, which
is correct and in character and not survivable for a landing square a crossing has to find.
`FerryPad.ensure` rebuilds a dock whose landing has gone, which reads as the last of the
Post still doing its job.

**The berth is not a queue.** A second crossing to a world whose dock already has a ferry on
it is refused. Without that it comes down *on* the first and silently replaces whatever
shared a coordinate — a hull deleted by another hull, reported nowhere. One dock per world
is the design; a queue would be a mechanic, and inventing one is not mine to do.

**The bug the check caught, and it is worth keeping.** The first version asked the surface
heightmap where the ground was. That works exactly once: building the dock *raises* the
surface, so the second crossing measured a different height, found no landing there, and
built a second dock one block above the first. **A position derived from the world cannot be
derived from a world the thing has already changed.** `ensure` now scans the column for its
own landing material before computing anything, which is immune by construction — and it
also survives a player building on the apron, or the Verdant growing over it.

`pad_check.sh` counts landing blocks with `fill ... replace` over a volume far bigger than a
dock: exactly 13 in each world sailed to (nine plus four posts), and **zero in a world no
ferry has been to**, which is what stops a pad built eagerly everywhere from satisfying the
first two counts. Watched failing on the heightmap version (three docks, three crossings)
and again with the berth guard removed.

### The first thing in the mod a player can touch

`WORLD.md`, locked: *"a keel block captures the structure, validates it against the
destination's law, and re-places it at the far pad. **The validation checklist teaches each
world's rule before arrival** — the Quiet One's crossing: no note blocks, no jukebox, muffle
your animals."*

The middle clause shipped a long time ago and was reachable **only from a command**. Which
means the beat that sentence is actually about — a player learning what a god is like by
being refused by its paperwork — had never once happened in play. Touching the keel now
runs the capture and hands back the docket.

**All four crossings, every time, and not just the one you meant.** A player told about one
destination learns one rule; a player handed the whole page learns that the four gods refuse
*different* things, which is the reconnaissance band 3 exists to begin and the reason the
letters are worth reading. It costs four map lookups over a census.

**It does not sail, and that is deliberate** — see "Waiting on owner" below. Nothing in
`WORLD.md` says how a player names the destination and the options are not interchangeable.
This ships the half that is locked.

**The docket lives in `FerryDocket`, not in the block**, for the reason every command seam
here exists: a right-click cannot be driven from a headless server, so a page only the
block could produce is a page no check could read. `interregnum ferry inspect` and the
keel's right-click call the same method and nothing else — the same arrangement
`interregnum learn` has with the dialogue node that teaches a school.

`inspection_check.sh` asserts what the page is FOR rather than what it prints: all four
crossings named, **the four disagreeing about one hull** (a page where they agree is a page
with one law behind it, and teaches nothing), the block *and the count* on every refusal,
the god's own reason line beneath it, every violation rather than the first, a one-line
answer for a bare keel, and the same page twice for one unchanged hull. Watched failing by
validating every destination against one law, and again by truncating the violation list.

**26.2 removed `LivingEntity#displayClientMessage`**; the replacement is
`ServerPlayer#sendSystemMessage`, on a different type — which matters because a block's
`useWithoutItem` hands you a plain `Player`. Row added to
[`PLATFORM.md`](PLATFORM.md#things-26x-renamed-and-where-each-one-bit).

### Four worlds that no longer look alike

Three of the god-worlds generated `minecraft:the_void` and the Verdant's generated
`minecraft:plains`, so on the ground the register's four distinct places were three
identical grey rooms and one meadow carrying vanilla's entire mob spawn list. Each now has
a biome of its own.

**The names are a split the mod already had.** `WORLD.md`'s register gives each world two:
the SUBJECT line on the dead god's letters (`GREEN AUTHORITY`, `MASS AUTHORITY`,
`TEMPORAL AUTHORITY`, `UNRESPONSIVE`) and what people actually call it (*the Long Green*,
*Old Heavy*, *the Turning*). The dimension ids were already the first set; the biome ids are
the second, so the debug screen shows the colloquial name and the ferry's paperwork shows
the civil-service one. **The Quiet One's cannot take that name, because it does not have
one** — its register column reads *"they will not say it"* — so its biome is `unanswered`,
named for the silence rather than for whoever is being silent. The same move the fourth
letter makes when it opens `To —`.

**Every colour is a literal step off `assets/palette.json`**, and `biome_check.py` fails
the build on any that is not. A sky is art direction; the palette system existed to stop art
direction being decided one file at a time, and stopping it at textures was arbitrary. The
Quiet One's water is the colour of its stone (nothing there is a different substance from
anything else); the Anchorite's is the `metal` family, which the palette glosses as *"cold,
manufactured; holds its shape"*; the Hearth-Turner's is `brass` in a late afternoon never
allowed to become evening; and the Verdant's is the brightest foliage step on the grass,
the leaves **and** the sky, because a place with one colour left is a place something has
gone wrong in.

**Nothing spawns in any of them**, which is a decision rather than an omission — three were
already empty by accident of using `the_void`, and the Verdant's had inherited the plains
spawn list. **Only the Verdant's generates features**, because accelerating bare stone is
nothing.

Still not designed: the *shape*. All four are vanilla overworld noise, and `ModDimensions`
says so.

**26.2 moved most of a biome's look out of the biome.** `BiomeSpecialEffects` now carries
water and vegetation colours only; fog, sky, water-fog and the ambient sound loops are
environment attributes, set through `Biome.BiomeBuilder#setAttribute`. The record still
exists and still compiles, so a biome written from any pre-26 guide builds cleanly and has
no sky. In [`PLATFORM.md`](PLATFORM.md#things-26x-renamed-and-where-each-one-bit).

### The gate that regenerated nothing

Found by accident here, and it had been open since datagen landed. CI's rule for generated
files is *regenerate everything, then `git diff --exit-code`* — which catches a source that
changed without its output being regenerated, and does **not** catch a generated file that
somebody edited by hand and committed. `HashCache` skips writing a file whose newly
generated hash matches the cached one without ever looking at the file on disk, and the
cache was committed alongside the output.

Proven rather than reasoned about: a `_hand_edited` key added to a committed loot table
survived a green `runServerData`, and the diff came back clean. With the cache deleted
first, the same experiment came back dirty.

The cache directory is now gitignored and CI deletes it before regenerating, so every file
is written every time. [`LESSONS.md` #37](LESSONS.md#37-datagens-cache-makes-regenerate-and-diff-blind-to-a-hand-edited-file).

### Everything here, at once

***Wildgrowth*** — the Verdant's second. `WORLD.md`: *"Wildgrowth — and in the Verdant's own
world, accelerating growth is a **hazard**."*

**The fourth caller of one law.** It runs `Verdant.quicken`, which is the same acceleration
the Verdant's world applies to every chunk it holds and the same one band 3's leaks apply to
a patch of overworld that has forgotten whose it is. Meet a god's law as a place, meet it
again as a wrongness leaking into your world, then be the one doing it — four times now.

**It accelerates; it does not choose.** `Verdant` refuses to keep a list of growable blocks
and so does this: a cast asks the world to tick, hard, in a small volume. Every crop,
sapling, vine, moss and mushroom is covered without being named, and so is whatever the next
game drop adds. Which is why the locked word is *hazard* — a surge you cannot aim at the
wheat and away from the jungle closes the path behind you. In the Verdant's own world it is
nearly pointless (everything already grows at eight times the rate you know) **and** free,
because a living god replenishes what casting spends. The spell costs most exactly where it
does most.

**No probability anywhere.** Every position inside gets exactly `PUSHES` ticks in a fixed
order, so a cast does the same thing twice. The three systems that grow a world on a clock
use probability because a rate is what weather is; this is an act, and an act that varies is
a gamble. `PUSHES = 24` is calibrated against the plainest counter in the game — sugar cane
advances one segment on exactly sixteen random ticks — so one cast is plainly worth more
than one segment and plainly less than a forest.

### The ledger gates what you did not aim at

The sharper form of [`LESSONS.md` #35](LESSONS.md#35-a-check-written-from-the-implementation-will-defend-the-bug),
forced by the first spell in the kit that sweeps a volume. *"The ledger gates the world, not
the caster"* is right for a spell that names one block; said that flatly it would hand an
area spell a licence over other people's greenhouses.

This is not a new rule — it is what the code has always done, now stated:

| | aimed at | ledger |
|---|---|---|
| `Weather`, `Rewind` | one named block | **off** — your own wall included |
| `Drop-forge` | the block your weight lands on | **off** — you chose where to drop it |
| a cast's **fraying** | a volume nobody pointed at | **on**, since the day it was written |
| `Wildgrowth` | a volume | **on** |

So your neighbour's leaves do not decay because you wanted your wheat in early, and nothing
about a spell aimed at one block has changed.

**Verified, both halves watched failing.** Deleting the ledger gate fires `CLAIMED_GREW`;
dropping `PUSHES` below one cane segment kills `CAST_GREW`. `random_tick_speed 0` — read
back from the server — makes the control categorical rather than a margin: with vanilla's
ticking off, the only thing in that world that can advance a cane is `Verdant.quicken`.

**And the check's first run failed on its own scenery.** The cane stood on a single sand
block in mid-air; sand falls, the column collapsed the tick after it was placed, and the
setup probe passed because it ran before gravity did. Dirt on a stone floor now, with the
water sunk into the bed so the source cannot flow away from the block it must be next to.

### The ferry did not eat the planet

A red CI run reported *"the ground under the dock is gone — the ferry took the world with
it."* It had not. The keel sits directly on the seabed block `ferry_check.sh` watches, and
an opaque block over a grass block is how **vanilla** kills grass — on a random tick, on
its own. Roughly one run in thirty, and the file had been living on it since it was
written. Reproduced with `random_tick_speed` at 400 before anything was changed.

`gamerule random_tick_speed 0`, read back from the server, removes the confound; that is
the third check to need it. The larger half is that **the probe could fail two ways and the
message asserted which** — a capture leaves AIR, which the file already knew ten lines up.
Two probes now: air names the ferry, anything else says look at the block tables and names
nobody. [`LESSONS.md` #36](LESSONS.md#36-a-probe-that-can-fail-two-ways-will-blame-the-wrong-one).

### A spell that crushes nothing

***Drop-forge*** — the Anchorite's second. `WORLD.md`: *"**Weight** (Anchorite): … *Drop-forge*
— **crafting by crushing**."*

The spell does not crush anything, and that is the design. It makes a few metres of ground
somewhere an impact *means* something — and an impact is not something it provides. You
have to go and get the weight, get it above the thing, and let it go. Cast into an empty
room it does nothing at all, for a full minute, and then lapses.

That is the strongest available reading of the locked doctrine that a spell's *"combat use
falls out of its world use, never the reverse"*. There is nothing here to aim. What changes
is what gravity is **for** inside a small patch of ground.

**Its own school is the other half of it.** *Lighten* takes weight away so a thing can be
moved; *Drop-forge* is what that thing is for once it is above where you want it. Lighten
the anvil, walk it up, drop it — one motion, two spells. And they **cannot overlap**: inside
a Lighten nothing falls, so a forge under a low-gravity field is a forge with the hammer
floating over it. A player who tries both at once learns the Anchorite in four seconds.

**The fourth table, and the first that nobody's world runs.** The unraveling loosens, the
Turning weathers, attrition generalises — all three on a clock. Crushing waits. It answers
one question, the way the other three do: *what does this do under force?* The world has two
answers, so the table has two halves. **Rock shatters** — stone, cobble, gravel, sand.
**Loose and soft matter packs** — snow, ice, packed ice, blue ice. A block does one or the
other, never both, so the file can never become "crushing does whatever was convenient here".
`chance` is 1.0 throughout, which is the difference between an act and weather.

**It bites down, and that is left in.** Cobblestone crushes to gravel, and gravel falls — so
a weight dropped on a cobble floor makes the floor beneath itself fall, follows it, and
crushes again until the chain runs out or the drop leaves the zone. Bounded by the radius
rather than by a special case, and the clearest possible demonstration of what the cast
actually did.

**The ledger question, one increment after getting it wrong.** A crush only ever happens
because somebody cast a spell here and then fed it by hand, so `Crush` has no claim check at
all: the ledger gates the world, not the caster. That is now the second place the principle
is applied rather than the first place it was noticed, and `dropforge_check.sh` asserts it
from the feature rather than from the code — a drop-forge crushes a wall its caster built.

**A comment promoted to a guard.** The table's safety note used to end *"the only thing
standing between a misfire and somebody's wall is that walls are not in this file"* — which
is true, and was enforced by nothing. `crushing_check.py` now enforces it: no block a player
builds with may appear on the LEFT of an arrow (the right may be anything, since producing
it is the point), no ore either, and `chance` must be exactly 1.0, because a crush is an act
and an act that sometimes does nothing reads as a broken spell rather than as a rate.
Watched failing on a worked `from` and on a fractional chance.

**Two mutations, both watched failing.** Keying zones by school again leaves `LIGHTEN_HELD`
and `LIGHTEN_INTACT` green and kills `FORGE_CRUSHED` — the forge hovers its own hammer,
exactly as predicted, and nothing anywhere reports an error. Adding a claim check to `Crush`
kills only `CLAIMED_CRUSHED`. The check also carries a control column twenty blocks from any
spell, without which "the stone became cobblestone" is equally satisfied by any of the three
other block tables running in that same world while the check is going.

### The ledger gates the world, not the caster

A shipped bug, found by designing the *next* thing rather than by any check — and the check
that should have caught it was the reason it survived.

**What was wrong.** `Hearth.step` refused any block in the claim ledger, and *Weather* —
the Turning's first spell, "age a block one step" — went through `Hearth.step`. So the one
spell a builder would most obviously aim at their own wall was the one thing it could not
touch, and it failed *silently*: cast, nothing happens, no message. `WORLD.md` sells magic
as a builder's palette; this was the opposite of the pitch.

**Why the ledger exists.** So that the *world* — the unraveling, attrition, the Turning's
clock — may warp everything it likes and never take a block somebody placed. That
guarantee is about the world acting on its own. It was never about a player pointing a
spell at their own cobblestone and asking for it to be older.

So the rule is now stated once and true everywhere: **the ledger gates the world, not the
caster.** `Hearth.step(level, pos)` still spares placed blocks and is what the world's three
sweeps call. `Hearth.step(level, pos, false)` is what a spell calls.

**The two directions are asserted in two different files, deliberately.**
`turning_check.sh` proves the world still spares a build; `casting_check.sh` proves a caster
may aim at one. Either one alone is satisfiable by getting it uniformly wrong in either
direction, which is exactly how the bug lived.

**The part worth carrying.** `casting_check.sh` had *asserted the bug*
(`CLAIMED_SPARED`), eloquently, with a failure message invoking the mod's oldest guarantee.
It was written by reading `Hearth.step` and describing what the code did, so it could not
disagree with the code — and a check that cannot disagree is not evidence, it is a lock on
whatever was there. [`LESSONS.md` #35](LESSONS.md#35-a-check-written-from-the-implementation-will-defend-the-bug).

### Band 4: the world forgets what it was

Both halves now. The *tending* signal is below; this is what it gates.

**What gets subtracted is distinction.** `WORLD.md`: *"Biome-specific detail reverts to
its plainest equivalent. Your forest stops being a forest — not destroyed, **generalised**.
Ores return to stone."* So every rule answers one question — what is the plainest thing
this could be? — and nothing in the table makes rubble. A birch forest becomes an oak
wood, then trees, then somewhere with some trees on it. Loss by generalisation reads
sadder than loss by destruction, and it is why band 4 is not band 2 with bigger numbers.

**Third use of one mechanism, and by now it is one type.** The unraveling loosens, the
Turning weathers, attrition generalises — the same `ConversionDef`, and now the same
`StepTable`, which was extracted from the Turning's copy rather than written a third
time. What differs is only what the three tables contain, which is the whole content.

**The gates all live in `Generalise.step`**, and briefly they did not. The command tested
dimension, band and staleness so it could report a precise reason, while the law tested
only the claim ledger. That is a check testing its own harness: `attrition_check.sh`
asserts tended ground is spared, and with the gate in the command, deleting it from the
sweep would have left the check green and the law gone. Both gates are now
mutation-verified in the law itself.

`attrition_check.sh` is categorical throughout this half, and gets to be for a reason the
Verdant's growth did not: nothing in vanilla turns a birch log into an oak log, or podzol
into dirt, or diamond ore into stone. There is no background process to separate the mod
from, so one conversion where none was permitted is the law escaping its gates. It proves
the chain (podzol → coarse dirt → dirt, two links in order), the ores, the band gate, and
the two refusals — tended ground and anyone's placed block.

### The world can be held together by living in it

Band 4's first half. `WORLD.md`: *"It frays where nobody tends. Regions people visit and
keep hold their definition. This makes the 'take the job' ending literal rather than
thematic — holding the world together shrine by shrine is exactly the counter-move. The
apocalypse becomes a thing you can argue with."*

**Tending is simply being somewhere.** No item, no ritual, no button. A chunk carries the
game time anybody was last near it; a player who lives in their base keeps their base
without ever learning the system exists.

**The gap between two radii is the entire mechanic**, and it exists because band 4
otherwise cancels itself out. Attrition can only act on *loaded* ground — placement
tracking answers "claimed" for an unloaded chunk and so protects it absolutely — and
loaded means near a player. But it must act where *nobody tends*. Taken naively those
leave nowhere at all. The resolution: tending is intimate (two chunks) while loading
reaches much further, so the ring between them is ground that is present but unattended.
**The fringe of your world frays while its heart holds.** A base you live in keeps its
forest; the forest eight chunks out — loaded every day, walked through never — goes
quietly generic.

**First sight counts as tending.** Ground with no stamp is not ancient ground, it is
ground nobody has looked at yet, so chunk load stamps it. Without that, a player
exploring at band 4 would find fresh land that was *already* plain, which reads as broken
worldgen rather than as a world forgetting. Attrition has to be something you watch
happen to a place you knew.

`attrition_check.sh` proves the ring is real in a running world: after tending, the chunk
under you reads 0 ticks and the chunk four out reads ~84, and both age. Two things it
deliberately does **not** assert, stated rather than omitted: the twenty-minute threshold
being *crossed* (no CI run waits that out, and `/time add` moves dayTime while the stamp
reads gameTime — the arithmetic is covered in core, including both sides of the boundary),
and the tick handler itself (it walks `level.players()` and a headless server has none, so
the command seam runs — calling the *same* `Tending.tendAround`, not a copy).

### A key that was unique only because one thing used it

***Still*** — the Quiet One's second. `WORLD.md`: *"freeze primed TNT / falling block
**mid-state**."* The last word is the spell: the thing is already happening and it stops,
holding the state it was in. Nothing is deleted and nothing is defused — when the zone
lapses, the sand falls and the TNT goes off. A spell that removed the hazard would be a
damage button with extra steps.

It is a different verb from its own school-mate. **Hush is about information** (nothing
hears, nothing notices, no fuse completes because a fuse is a sound); **Still is about
motion** (what is already in flight stops). A creeper walking at you is unaffected by
Still; a falling anvil is unaffected by Hush. They overlap on exactly one object — primed
TNT — and treat it differently: one denies it the sound it needs, the other the moment.

**Building it surfaced a latent bug, which is the part worth keeping.** Spell zones were
keyed by `School`. That was correct while each school had at most one zone spell, and Hush
and Still are *both* Silence — so keyed by school they become each other: a silence would
stop falling blocks and a stillness would mute creepers, **with nothing failing anywhere
and both spells appearing to work from inside either one.**

This is the **second time** that shape has come up. The first was one list of zones per
world, which broke as soon as a second spell of any school opened one. Both are the same
signature: *a key that is unique today because only one thing uses it.* Zones are now keyed
by `Spell`, and `still_check.sh` is the only place the difference has a symptom — it drops
sand into a Hush and asserts it lands.

### The first spell aimed at a creature

***Quell*** — the Quiet One's third. `WORLD.md`: *"strip one ability (a blaze that cannot
ignite)."* The example decides the reading: **Quell takes away the throwing arm.** A
projectile whose owner is quelled is refused entry to the world, at the moment it would
join it, so it is never seen at all. A blaze that cannot ignite is that rule applied to a
blaze; a skeleton that cannot loose an arrow is the same rule, not a second case.

It would have been easy to write this as a spell with a mode per mob — no teleport for an
enderman, no climb for a spider — and that is four spells wearing one name. *"Strip ONE
ability"* is singular, and one ability defined uniformly is the only reading where the
word means anything. The kit has names left in it for the others.

**It is the first spell in the mod that is not a place.** Hush and Still are rooms you
make; this is done to a creature and travels with it, so a blaze quelled here is still
quelled in the room it flies to. That is the whole reason both shapes exist, and it is why
`Quelled` is keyed by entity id rather than by position — it is the sibling of `Zones`,
not a user of it.

`quell_check.sh` has three controls, which is one more than any other spell check and all
three earn it: a second blaze **three blocks away** still shoots (so "no fireball" is not a
broken summon, an unloaded chunk, or a mod that stopped every projectile in the game), a
fireball with **no owner** still appears (so the rule is about being quelled and not about
being a fireball), and the unquelled blaze's fireball appears **forty-five blocks out** (so
the quelled blaze's silence thirty blocks from the cast is the spell and not the distance).
Mutation run: making the cancel never fire was caught.

Nothing audible is claimed. Same wall as Hush — the Quiet One's most characteristic effects
live on a client and this container has none.

### The Theoclast class exists

`WORLD.md`, locked: *"a rite at a shrine, and the keeper has to agree to witness it."* Hold
a clast out to a shrine-keeper and that is the rite — the same empty-hand / full-hand split
the ferry keel uses.

**The first time regard gates something a player wants.** Standing has decided prices,
greetings and which replies are offered; none of those is a door. This one is: below the
bar you are carrying a piece of a god you cannot use, and the way through is to go and be
somebody the villages will vouch for. The keeper **is** the villages, so the question was
always answerable — there was just nothing to ask it about.

The bar is `KNOWN`, the first band above indifference, and low on purpose: the scarce
thing is the clast (seven in a world, ever) and gating a rare item behind a long errand as
well would turn *"the class is a server negotiation"* into a chore against a meter. A
stranger is refused because an institution's opinion of somebody it has never dealt with
is an **absence** — and asking uses `peek`, so being refused does not bring a file into
existence.

**The clast is consumed only on success.** A rite that ate one for saying no would destroy
one of seven irreplaceable objects in exchange for the word no.

**And the note in `PlayerTags` came true.** That class returned nothing and said why:
*"the only tag any written scene uses is `class/theoclast`, and the Theoclast class does
not exist yet… when attunement lands, it lands here, and every scene already written starts
offering its gated lines with no edit to the scenes."* The Warden's intake scene has carried
a Theoclast-only reply since long before anybody could see it. It appears now, and not one
line of that scene changed.

**One signature had to invert to make it visible.** `PlayerTags.of` took a `ServerPlayer`;
what a player IS turns out to be a property of their **record**, not of a body — and the
only thing that can ask on a headless server is `talk show`, which has an id. A tag lookup
that needed a body would have made the whole class invisible to every check, exactly as it
was invisible to this one until the primary form became `(server, uuid)`.

`rite_check.sh` asserts both edges of the gate and shows the same scene to two players, one
attuned and one not — because "the option is visible" is equally satisfied by a gate that
has stopped gating, and from the attuned player's side those look identical. Mutations:
witnessing a stranger, and raising the bar out of reach.

### The ferry reads the letter

**Ask with an empty hand, go with a full one.** Touching a keel bare-handed hands back the
docket for all four crossings — the locked *"the validation checklist teaches each world's
rule before arrival"*, unchanged. Holding a letter out to it sails that god's crossing.

`WORLD.md`: *"the route to them is its unanswered correspondence"* and *"you are the only
one carrying their mail."* Both are now the navigation rather than flavour: **you cannot
reach a god you are not carrying post for.** The letters are the map, which makes the
shrine-keeper's hand-over the moment the map exists.

**One sequence, two callers.** The whole crossing used to live inside the command handler
where an operator typed the law. It is now `Sailing`, and the command and the block are
both four lines over it — a crossing has eight ways to refuse, and two copies would have
been two ferries that disagreed about what a berth is.

**The design error that nearly shipped, and the reason it is written down twice.** The
appealing rule was: the ferry sails where the letter is *addressed*, so the one that opens
`To —` cannot be routed and the boat does not move — the endgame's opening question
arriving as a mechanism. It reads beautifully and it is wrong. **The unaddressed letter is
the Quiet One's**, and `WORLD.md` says why in as many words: *"the Quiet One has no name in
the letters, and that is the whole character."* Routing on the addressee would have made
that god's world **permanently unreachable, silently, by the only affordance there is.**

So routing is by the letter's **id**, the addressee is never consulted, and the blank line
stays what it was written to be. The long version is in `Routing`'s javadoc, because the
wrong version is the more appealing one and nothing downstream would have caught it.

What does catch the class of problem: `letters_check.py` now asserts the set-level fact the
right version depends on — **every letter names a crossing and every crossing has a
letter.** A letter with no law is one a player can carry and never deliver; a law with no
letter is a world nothing can route to. Java cannot see either: it is a null at runtime, in
one world, for one player, indistinguishable from a refusal that means something.

`carry_check.sh` sails **two** letters from **identical** hulls to **two** worlds, because
one crossing on its own is equally satisfied by a ferry that always goes to the same place.
Mutation run: routing every letter to one fixed crossing was caught by exactly that.

### The post comes back, and somebody has been holding it

Four letters had been written, validated and readable for a long time, and **nothing in the
world produced one.** Now a shrine-keeper does.

`WORLD.md`, locked: the letters were sent, none was answered, and they came back. The keeper
has been holding them since — and the scene is that wait ending.

**The keeper does not know the player killed anything.** Nobody does; the Deicide
advancement is hidden and never reaches chat. So they are not accusing and not thanking.
They are doing the one thing left in the job description: the post came back, the post has
to go somewhere, and here is a person. *"Because you're here and nobody else is, and that's
the whole of the criteria."* The player knows exactly why it is being handed to them and
the keeper does not, and the scene runs on that gap.

**It outranks both other openings.** `openingScene()` picks the mail ahead of the ledger and
the intact scenes, because somebody holding a box they have kept for years does not open
with the housekeeping. Two conditions, both load-bearing: the god must be dead (no vacancy
before that, and a keeper handing out the round on a Tuesday gives away the opening), and
the mail must not have changed hands already.

**Refusing does not spend it.** The milestone hangs on the *accepting* node. There is one
set of letters in a world — the same rule the clasts run on — so a refusal that marked
`MAIL_RECEIVED` would destroy the mail permanently and silently, and the keeper would go
back to talking about the offering box forever. `dialogue_check.py` cannot catch that: a
milestone on a terminal `declined` node is perfectly valid data. The live check is what
catches it, and the mutation run proves so.

**Handed to the initiator, not the table — the opposite rule from `teach`.** A god
addressing a room teaches the room, and a lesson is not diminished by being heard twice. A
hand-over is not that: four physical letters, and a keeper holding out an envelope is
holding it out to *somebody*. The two effects disagree on purpose, and the disagreement is
the difference between an audience and a transaction.

**[VERIFY] the letters arriving in an inventory.** A headless server has no players, so the
hand-over has nobody to hand to; `Conversations` logs that case and records the milestone
anyway, which is what keeps the beat once-only. Every decision *around* the transfer is
checked. Clearing it needs one player and a client.

### Casting is a spoken word

Ten spells existed, verified, for a long time, and **a player could reach none of them** —
the command was the only way in. That is over. You say *weather*, and the block you are
looking at ages.

**The word is the spell's own name.** No invented incantation, for three reasons in order
of weight. It is the *register*: this world runs on dockets and statements, magic here is
not mystical but **stated**, and a Warden's citation can quote you verbatim. An invented
word would need a language, and the only language this world has is the god-script — which
`WORLD.md` now locks as a hazard to read, so an incantation drawn from it would make every
cast an exposure, a collision nobody designed. And it is discoverable: being taught a school
tells you its verbs, and there is nothing else to look up.

**Nothing is cancelled — the word goes to chat exactly as typed.** The obvious
implementation swallows it to keep the channel tidy and destroys the feature: the locked
reason casting is speech is that the offence is *audible*. A Warden in the room has
witnessed it, a bystander can repeat what you said, and casting quietly in a cellar becomes
a real choice. A word nobody else sees is a keybind with extra steps.

**The whole message, or nothing.** `Incantation.of` matches the entire trimmed message and
never a substring. Not fussiness: chat is where players talk *about* the game, and
*"I hushed the room and it still blew up"* must not silence anybody. A magic system that
fires on a substring turns every conversation about magic into a hazard, and the first thing
anyone would learn is to stop discussing it.

**Aimed or not is a real division.** A spell that makes a place you are standing in centres
on the speaker — *Hush*, *Still*, *Lighten*. A spell you do something to centres on what
you are looking at. *Drop-forge* is the exception among the zones and earns it: it is ground
you prepare and then go and fetch a weight for, and one centred on the caster would be a
forge you are standing in the middle of.

**The spells do not know how they were triggered, and must not learn.** Every
`*Spell.cast(...)` takes the arguments it took before `Speech` existed. That is not
tidiness — `WORLD.md` marks the affordance **[NEEDS PLAYTEST]** in as many words, because
typing a word to cast may simply feel bad and nobody can find that out in a container with
no client. The cost of being wrong is one file.

`speech_check.sh` reads the *world* rather than the command's reply. Mutation run: matching
a substring instead of the whole message was caught — and caught by the world assertion,
not the outcome one, because other messages in the run produce `NOT_A_WORD` legitimately.
Two assertions where one looked sufficient.

### The one spell with a middle

***Loft*** — the Anchorite's third. `WORLD.md`: *"make a small structure weightless and
carry it."* Carrying is picking a thing up, walking, and putting it down, so this is the
only spell in the mod with **two casts**, and the distance is whatever the caster's own
legs covered in between. There is no range on the setting down, and there should not be:
the range IS the walk.

**The capture is the ferry's, and that is why this was cheap.** `Ferry` solved *"how do you
capture a boat without capturing the planet"* by walking only what a player PLACED, so a
hull resting on the seabed lifts off it. A shed standing on a hillside is the same problem
and takes the same answer. The walk is repeated rather than shared because the two differ
in the one place that matters — a ferry's walk starts at a keel and admits it without a
claim, since the keel is the consent; a loft has no keel, so the block the caster names
must itself be something somebody built.

**Three things it refuses, each for its own reason.** `MAX_BLOCKS` is 64 against the
ferry's 4096, and the gap is the design: at a hull's size this would be a ferry that needs
no keel, no dock and no checklist, and the crossing laws would become optional. A set-down
into occupied space is refused whole rather than overwriting, because the ferry writes onto
a pad it built and this lands wherever a player is standing. And **a load may only be set
down in the world it was lifted from** — `WORLD.md` locks *"travel between systems is only
by ferry"*, so a workshop walked through a portal would be a second way to do the one thing
the ferry exists for.

**Nothing expires.** Every other spell in the mod lapses and this one must not: a loft that
ran out would drop a house wherever its owner happened to be standing. Weightless means it
costs nothing to hold, so it costs nothing to hold — including across a restart, which is
why `Lofted` is the first and only spell state saved with the world.

That last property is also where the check was wrong on the first try, and the mistake was
worth more than the spell. See [`LESSONS.md` #40](LESSONS.md#40-a-persistence-check-on-a-brand-new-world-tests-nothing):
a two-run lift-restart-assert passed with `setDirty()` deleted, because `computeIfAbsent`
marks a **newly created** instance dirty and a fresh world therefore saves whatever is put
in it. `loft_check.sh` runs three servers — one to bring the file into being, one to lift
into a store loaded from disk, one to find the shed still in hand — and the same mutation
dies immediately.

### The ageing table runs backwards too

***Rewind*** — the Turning's second, *"repair by un-aging"*. One table, two directions:
*Weather* reads it forwards, this reads it back. The same doctrine that made the Turning
and the unraveling share a registry, which is why the school's second spell cost almost no
new machinery.

**It may touch what a player built, and that is the decision.** Every other system here
consults the claim ledger and refuses anything somebody placed — the unraveling, attrition,
the Turning's own clock, *Weather*. Rewind does not, because **the ledger exists to stop
the world eating your work, not to stop you working on it.** Refusing would make the spell
useless at exactly its purpose: you do not un-crack a cave, you mend your own wall. The
check asserts both sides — the Turning's clock still refuses that same block — so it is a
distinction rather than a hole.

**And some blocks have no single past.** Plain deepslate wears into cobbled deepslate and
deepslate tiles crumble into it, so asking what a piece of it used to be has two answers.
Rewind refuses rather than choosing, which is the most characterful thing in the school:
**the god whose entire law is keeping every version of everything is precisely the one that
will not invent one.**

### Four schools, and the one whose combat use most obviously falls out of its world use

***Hush*** — the Quiet One's, and the fourth. `WORLD.md`: *"true no-sound zone: sculk
blind, mobs cannot alert, **a creeper that cannot hiss cannot detonate**."*

A creeper's fuse **is** a sound. Take the sound away and the mechanism has nothing to
complete. That is the mod's doctrine arriving at its most literal: Hush is not a defensive
ability, it is silence — which happens to be fatal to a thing that kills by announcing
itself. A player works that out about two seconds after being told what it does to sound,
and that moment is worth more than any tooltip.

**Two of the three clauses are enforced; the third is not claimed.** Mobs inside acquire
no targets, and a creeper's fuse is wound back every tick so it can chase and loom and
never arrive. The *audible* silence, and sculk going blind to it, are client-side — the
same wall band 3 met, and for the same reason: the Quiet One's law is the one law whose
most characteristic form lives on a client.

The deliberate case needed a decision. A creeper struck with flint and steel sets an
`ignited` flag with a public setter and **no public way to clear it**, so for that case the
tick is cancelled outright — which freezes it, more than silence would do. The honest
trade, because the alternative is a hole in a locked promise; and a lit creeper standing
perfectly still in a silent field is, as it happens, exactly what this god should look
like.

**Zones are now keyed by school**, which the second spell to open one made necessary. A
single pool would have meant every zone did everything — and from inside either spell that
looks exactly like both of them working. `hush_check.sh` asserts a creeper in a *Lighten*
field detonates normally.

**All four questlines now open onto their school.** Weather changes a block, Lighten
encloses, Bridgeroot creates, Hush forbids.

### Three spells, three shapes, and what you grow is yours

***Bridgeroot*** — the Verdant's. `WORLD.md`: *"grow a living span toward your gaze, **real
persistent blocks**."*

Those last three words are the design brief, and they are unusual. Most games' bridge
spells are temporary platforms that evaporate — a *movement ability* wearing a spell's
clothes. This one leaves actual world behind: you can build out of it, and somebody can
walk across it a year later.

**Which makes the load-bearing decision the claim ledger.** Every block a span leaves is
recorded exactly as if you had placed it by hand, so the unraveling, the Turning and band
4's attrition all refuse it — they consult that same ledger. It is the only reading that
makes "real persistent blocks" mean anything: a bridge the world dissolves next chapter is
a temporary platform with extra steps, and a player who lost one that way would never
trust the spell again. **Growing something and having it be yours** is what lets Verdancy
be a building school rather than a traversal one.

It never replaces anything — a span grows into air and stops at the first block it meets.
Stopping short is legible; boring through terrain, or through what somebody built, is not.

**The three shapes are now the point.** *Weather* changes a block. *Lighten* encloses a
region. *Bridgeroot* creates. Having all three is what shows the school system carries
genuinely different kinds of verb rather than one mechanism with three names — and all
three share exactly two rules: you must have been taught it, and at home it costs.

Three of the four questlines now open onto their school. Only the Quiet One's has no spell
yet; *Hush* is named and locked, and its region form is the same thing band 3 cannot leak
for want of client-side audio.

### A second spell, and the school system stops being one special case

***Lighten*** — the Anchorite's. `WORLD.md`: *"shared low-gravity zone, **mobs float
too**."* Those last three words are the spell. It is not a buff you put on yourself; it is
a piece of the world briefly obeying the Anchorite's law, and everything inside is subject
— you, the skeleton chasing you, the gravel over your head. That is what satisfies the
locked doctrine that a spell's *"combat use falls out of its world use, never the
reverse"*: **you cannot aim Lighten at anybody**, only change the rules where they stand.

**It is the god's own law, borrowed.** The zone does not implement floating — it makes
`Anchorite.lift` apply where it otherwise would not. There are now three callers and one
law: the Mass Authority, a band-3 patch of overworld that has forgotten whose it is, and a
person who has learned how to ask. **That progression is the school system's whole
argument** — you meet the law as a place, meet it again as a wrongness leaking into your
own world, and the third time you are the one doing it.

Two shapes now exist and they are deliberately different kinds. *Weather* changes a block
and is done. *Lighten* opens a **zone**: a cube with an edge you can walk out of (the same
reasoning that gives band 3's leaks one) and a lifetime of half a minute. Zones are held
in memory and do not survive a restart — a spell whose effect outlived the server could
strand somebody inside a field cast by a player who has since left.

The Anchorite's delivery scene teaches Weight, so two of the four questlines now open onto
their school.

### Magic is learned in its god's world, and only there

`WORLD.md`, locked: *"Schools, one per god, **learned in their worlds**."* The last three
words are the progression, and they are now enforced: **nothing is known by default.** An
untaught caster is refused outright — so the reason to recommission the ferry is not a
stat bonus, it is that the verbs themselves are over there.

A per-player `Grimoire` persists with the world, alongside regard, on the overworld's
storage — because what somebody knows is a fact about them, not about where they are
standing. A player who learns the Turning in the Hearth-Turner's world still knows it at
home, which is the entire premise of the overworld ban being a *choice* rather than a wall.

**It only ever grows.** There is no unlearning and no method to do it. A school is
something you understand about how the world works; the Wardenate can make casting a
citable offence and a god can refuse to teach you the rest, but neither can reach into
your head. The consequences are enforced where casting happens, not by confiscation.

**A scene teaches it**, through a `teaches` field on a node — the same shape as
`milestone`, and on the node for the same reason: being taught is a fact about where the
conversation *arrived*. The Hearth-Turner's accepting ending teaches the Turning, so
delivering its letter now opens onto something. **Everyone at the table learns**, not just
the initiator: a god that taught one of four people standing in front of it would hand the
group a protagonist, which is the thing the table exists to prevent.

`delivery_check.sh` asks the same caster to cast the same spell on the same block before
any scene and after all four. Before: `unlearned`. After: cobblestone. The only thing
between the two attempts is the conversation.

### The first spell, and why the Wardens are right

`WORLD.md` locks the doctrine — *"**Every spell is a world-verb.** No damage buttons with
particle effects"* — and names the spell: *"**Weather** — age blocks: instant
mossy/cracked/oxidized — magic as a builder's palette."*

Weather is the cleanest possible first case for that rule because it has **no combat use
at all**. It turns stone into cobble into mossy cobble. The first thing the mod teaches
you to do with magic is decorate.

It is the ageing table, **aimed**. `WORLD.md`'s locked reuse note — *"the block-aging
registry powering the Turning **is the same system that runs the unraveling**. One
mechanism; a school and an apocalypse"* — means the spell calls `Hearth.step`, the same
method the Hearth-Turner's world runs on its own clock. So every promise that table
already makes holds for free, including the oldest one in the mod: **you cannot Weather
somebody's wall.**

**And in the overworld it costs.** Locked: *"With the god dead, all overworld casting
draws on the corpse… Heavy casting visibly frays its surroundings. The Wardens' law is
right, and the player can **discover** it is right. Off-world, living gods replenish what
casting spends."* So a successful cast frays the ground around the caster through the
unraveling — the same machinery spending the same residue, so the cost is legible in a
currency the player has been reading since chapter one, and it **rises with the band**
without anyone tuning it.

That is unusual enough to state plainly: **the enforcement agency is not wrong.** Every
instinct says the law banning your powers is arbitrary and the fun is in breaking it. Here
it is a correct reading of a real hazard. Nobody ever says so — the player casts at home,
sees the ground go, casts off-world, sees it not, and works it out.

Two decisions worth keeping:

- **A miss is free.** Aiming at a block the table has no rule for spends nothing. A spell
  that frayed the world for a miss would punish experimenting with it, and the ban would
  read as arbitrary after all.
- **The cost does not consult the band's scope**, which is the one place casting departs
  from the passive unraveling. A scope describes where the world comes apart *on its own*;
  fraying from a cast is the caster drawing on the residue where they stand. Made to obey
  the scope, band-1 casting would be visibly free anywhere away from a shrine and the ban
  would look invented.

**Not built: how a player learns it.** The command is the seam, as it is for `unravel at`
and `turning age`. `WORLD.md`'s *"schools, one per god, learned in their worlds"* is the
next increment, and it is what the questline middles have been short of.

### Delivering a letter now moves the world

`LETTER_DELIVERED` had been in `core` since the chapter machine was written — counted by
`ChapterState`, gating chapters 3 to 5 in `Chapter` — and a grep for it across the whole
game module returned nothing. All four delivery scenes shipped, each the opening of a
god's questline, each walked end to end by a live check, and delivering all four advanced
the world by exactly nothing.

Every check passed and every one was true: the scene played, the regard moved, the ceiling
held. None of them asked whether the rest of the system noticed, because a check written
alongside a feature inherits that feature's scope. `LESSONS.md` #32.

**A node may now carry a `milestone`**, and arriving at it records that milestone. It
hangs on the *node* rather than the option deliberately: "the letter was delivered" is a
fact about the conversation having arrived somewhere, not about which sentence got it
there — three routes into one ending should record one delivery, and the route where the
players refuse the errand should record none. `dialogue_check.py` refuses a milestone on a
non-terminal node for the same reason, and derives the valid names from core's enum rather
than keeping a copy.

`delivery_check.sh` now reads the count: **`letters delivered: 0 -> 4`.**

### All four gods' letters can be delivered

**Four openings, four unbearable requests, one machinery** — and the fourth is the one
that could not be written like the others.

**The Quiet One never speaks.** Not once, in its whole scene. Every line is a description
of what does not happen, because the world is built on the difference between refusing
*loudly* — which is what the Nether and the End do to a bed — and declining to react at
all. The scene is the players, in a silence, deciding what to do about it.

It **must not resolve the ambiguity**, and that is a hard constraint. `WORLD.md`: whether
the dead god never got close enough to have a name for this one, or had one and struck it
out, *"is never answered; the letter itself is the only evidence and it is ambiguous on
purpose."* The moment the Quiet One explains itself, *"the one who never wrote back"*
stops being a question about the dead god and becomes a fact about this one.

Its transgression is the fourth distinct kind: **signing for the letter yourself.**
Recording a delivery as received when nothing acknowledged it — filing a receipt on behalf
of somebody who will not speak. Nobody should be able to decide alone that a silence
counted as consent.

**The payoff is mechanical and never stated.** Regard with this god moves exactly as it
does in the other three scenes and nothing in the text ever says so: you are treated
differently afterwards by something that never told you it noticed. That is the intended
experience, and it is *indistinguishable from the consequences being silently broken* —
so `delivery_check.sh` reads the number. It is the one assertion in the mod that sees
something the player deliberately cannot.

The other three: take away the Verdant's defence, tell the Anchorite the job is over, ask
the Hearth-Turner to let a record *leave*. None of the four is "be sad about your
relative".

The **Hearth-Turner** is locked as *"the one who kept every version of the argument…
the exposition god, but earned: it is not telling you because the plot needs it told, it
is telling you because it has never been able to stop."* Its letter asks for *"the version
where I was wrong… because I no longer have it and I have looked,"* and asks not to have
it corrected — knowing that asking a god of memory not to correct a record is its own kind
of request.

So the copy is already held out when you arrive. It has been ready for an age; nobody ever
asked; and the one who finally asked is dead, so a request this god can fulfil perfectly
has nowhere to go. **The exposition is something the player can turn off**, which is the
only honest way to ship an exposition scene: letting it run is generous, cutting it off is
reasonable, and neither is punished. A god that cannot stop talking is only
characterisation if the player is the one who stops it.

The three UNANIMOUS nodes are deliberately different transgressions — take away the
Verdant's defence, tell the Anchorite the job is over, ask the Hearth-Turner to let a
record *leave*. None of them is "be sad about your relative".

The Verdant and the Anchorite are the pair that set the pattern: **two openings, one
machinery.** The Verdant opens mid-argument, defending a coverage arrangement nobody
mentioned. The Anchorite does not defend anything — it has been holding something for an
age and asks *what the load is* before it asks who you are.

The Anchorite's locked source is its own letter, which shipped long before the scene:
*"You held the corner of it while I set the rest. I do not think either of us has said so
since."* — filed under `SUBJECT: MASS AUTHORITY — no matter arising`, the form you use to
report that there is nothing to report, because *"the undersigned has no procedure for the
other kind."*

So the relationship is not an estrangement. It is a shared piece of work neither of them
ever acknowledged, and the scene follows the consequence: nobody came back to say the
setting was finished, so the Anchorite never let go. Its whole world is a place where
unanchored things rise and do not stop — already built, already what the ferry's boarding
notice promises — because the only thing it is still holding down is that one corner. The
UNANIMOUS node is telling it the setter is dead and it can put the corner down.

Neither scene spends its god's name; `letters_check.py` still passes.

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

**The hazard found while testing that is now checked** — and the note that said it
could not be was wrong. The regard engine *clamps* to the ceiling, so an author can write
`+25` into an option, a capped player silently receives `+0`, and the choice reads as
consequential while doing nothing. I recorded that as uncatchable on the grounds that
whether a player is capped is runtime state.

True, and beside the point: the **worst case is arithmetic**. `dialogue_check.py` now
walks every path through every scene, takes the most generous route per god, and refuses
any scene where the post-deicide floor plus that route would pass the ceiling. The
constants are read out of `RegardState.java` rather than copied, so reshaping
`recordDeicide` fails the check loudly instead of leaving it validating against numbers
that no longer exist.

It was found by walking into it: the Anchorite's delivery scene was written at +40
against a floor of −45 and a ceiling of −10, so its final choice was worth nothing to the
only audience the scene is about. Retuned to +30.

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

### The Haunt does something a bystander can see

`WORLD.md`, locked: *"**Rarely, a manifestation is server-real** — a bystander sees the
door move too. Not a sanity bar: a **credibility problem**."*

That distinction is the whole feature, and until now the mod only had the half that
undermines it. Every other manifestation on the locked list is rendered for one player,
and a mod that only ever did that has built a sanity meter: the killer sees things,
everybody knows the killer sees things, and nobody has to decide anything. The rare ones
being *real* is what turns it into something two people can disagree about.

**A door, because the locked text says a door.** It is the right object precisely because
it is the most ordinary one: it moves on its own for six mundane reasons, somebody who
reports it sounds like somebody reporting a draught, and the killer is the only one in the
room who knows it was not. Trapdoors and fence gates are deliberately excluded — they
would double the hit rate for nothing, since the beat is not "something opened".

**The claim ledger does not gate this**, and the exception is worth stating because it is
the first one. The ledger exists so the unraveling cannot *unmake* somebody's work; opening
a door unmakes nothing, and a ghost that could only move doors the player had not hung
would be a poltergeist with a property deed — it would never touch the one door in the
world that would mean anything.

**The rate is the design**, so both bounds are asserted in `SelfTest` rather than left as
taste: more often than every five minutes and it is weather, less often than once an hour
and it exists only in the source code. Checked every ten seconds, one in ninety — a mean of
about fifteen minutes, which means a few in a long session and none at all in a short one.
That unevenness is the mechanism: a thing that happens on a schedule is a mechanic, and a
thing that happens sometimes is a rumour. **[NEEDS PLAYTEST]**, more than most rates here —
what is being tuned is how plausible a person sounds when they say a door opened by itself.

`manifest_check.sh` reads the door's state back out of the world rather than trusting the
command's own reply, and asserts the toggle in both directions and a door forty blocks away
untouched at both moments. Mutation run: setting the door to the state it was already in —
returning MOVED and changing nothing — was caught.

### The Haunt comes back

`WORLD.md` locks the dream-audiences as *"rage, then bargaining, then the discovery that
the ghost **needs** its killer"*, and the first dream is only the bargaining. The second
one exists now: `dream_audience_two`, gated on **ENFORCEMENT** rather than on time or on
nights slept.

The gate and the subject are the same fact, which is why it is the right gate. ENFORCEMENT
means the deicide happened *and* a Warden has spoken to somebody — so the player has met
one, and the scene is about them: the god wrote the policy the Wardens are still enforcing
and it cannot amend the policy, because amending an order turns out to require being alive.
Under that is the ask, and the ask is very small: **the god cannot see its own estate any
more and would like to be told what the world is doing.** It never raises its voice at the
killer. A ghost that shouted would be a boss; this is a bereavement.

`TheHaunt.offer` takes no "which dream" parameter and must not: which one is due is a fact
about the world, not a caller's choice — a sleeping player cannot pick, so neither can the
command. `force` re-issues whichever is *currently* due; it is not a way to replay an
earlier scene out of order.

`Outcome.NOT_YET` is new and is not `ALREADY` dressed differently: nothing has been spent,
the world simply has not got there. Collapsing them would let an operator reading the log
believe a lost scene had been delivered.

**How the first dream went is read back as regard, not as a flag.** The opening line has
two `text_variants` on `THE_GHOST` — a killer the god resents gets a colder room. Only the
*wording* moves; no option is hidden by standing, because a player who timed out of the
first dream holds no record at all and a gated option would be a dead end rather than a
scene. `tools/haunt_check.sh` walks both dreams and asserts the cold opening specifically:
both scenes open on "Executor", which is the point of the word and useless as an assertion.
Mutation run: dropping the ENFORCEMENT clause was caught.

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

### A rot surface nothing can check

`docs/HANDOFF.md`'s counts are guarded on every push by `ci_claims_check.py`, which
counts the workflow rather than trusting the table. **The pull request description carries
the same numbers and nothing checks it**, because it does not live in the repository — and
it has now gone stale four times: at 48 commits and again at 67, both in the flattering
direction, the second time claiming nothing was waiting on the owner while two decisions
were; again at 83, this time by simply falling behind — it still described four spells
when there were eight, and 28 live checks when there were 32; and again at 96, the same
way, still saying eight spells and five open questions when there were nine and seven.

The last two are the more instructive. A description that overclaims gets caught by anyone
who reads the code; one that *underclaims* reads as modest and correct, and nobody looks.
The failure repeats because it is not a mistake anybody makes — it is what happens when a
document is edited only on purpose and the tree moves on its own.

There is no fix available from CI. The mitigation is this note: **when the counts in the
table above change, the PR description changes too**, by hand, and the "Waiting on you"
section at the bottom of it is checked against "Waiting on owner" below. The description
now states the commit it was counted against, so the next reader can tell at a glance how
far behind it is instead of having to diff it.

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

**Nothing. All seven are decided.**

The owner handed the whole list back: *"i think i trust you to come up with fun creative
and most importantly interesting answers for these, do you truly need me for them?"* — and
the honest answer was no. The standing rule that sent them here in the first place (*new
scope is the owner's call*) was written for questions about what the world **is**: the
roster, whether a Warden can die, what the last letter turns out to be. Those had no answer
derivable from anything already written.

These seven were not that. `WORLD.md` already locked what a spell is, what a portal system
is, what the letters are. What was missing every time was the **affordance** — how a player
reaches a thing already decided — and an affordance is craft. The rule had gone stale, and
kept collecting questions it was never meant to hold.

All seven are now written into [`WORLD.md`](WORLD.md) marked
`[LOCKED — owner delegated; decided here.]`, so the provenance travels with the decision.
Presented before building and approved: *"It all looks good."*

| # | Question | Decided |
|---|---|---|
| 1 | Each god's portal logic | **Each god's portal is opened by the school that god teaches.** Not four mechanisms — one rule with four faces, and it means you cannot go deeper into a god's system until that god has taught you |
| 2 | How the ferry is told where to go | **It sails where the letter in your hand belongs.** No letter, no voyage. (Routed by the letter's id — *not* its addressee; see the correction in [`WORLD.md`](WORLD.md)) |
| 3 | Where the mail comes from | **Returned undelivered**, held by a shrine-keeper who has been waiting to give it to somebody. The god *did* write |
| 4 | Attuning a clast | **A rite at a shrine, witnessed by the keeper** — so standing with the villages gates the class |
| 5 | Casting a spell | **A spoken word**, in chat, in earshot of anyone present. The offence is the act |
| 6 | Reading raw god-script | **Dangerous, and "marks" means the ghost gets louder** — it raises the server-real manifestation rate. Nothing else |
| 7 | The six unnamed spells | All six defined. *Held-breath* costs you your voice and therefore your spells; *Rot* is never aimed at a creature |

**The one that also answers itself:** casting being audible gives the Wardenate something
legible to cite, which settles the smaller citation question below in the same stroke.

**What is still genuinely the owner's:** vetoes, and **playtest feel**. This container has
no game client, so it cannot tell whether typing a word to cast is satisfying or irritating
— which is why decision 5 is built as one seam over spells that do not know how they were
triggered. Swapping it later costs the seam and nothing else.

### Also open, and smaller

**Where does everything the Anchorite's world lets go of end up?** *"They do not stop"*
leaves a question the mod never answers: **where do they go?** If everything that rises out
of that world is *received* somewhere — a counter, a ledger, a clerk who will return your
property once you have described it correctly — then items could rise, the law would be
literally true, and losing your inventory would be the start of an errand rather than the
end of a session. The Anchorite's Warden dockets already read `SUBJECT: MASS AUTHORITY`; an
authority that catches everything the world lets go of is the same joke as the rest of the
mod, and the retrieval scene would be one of the few places a player *wants* to talk to
bureaucracy. Costs a room, an inventory, and a conversation.

**Not built, and not built until asked** — unlike the seven above, this one *reverses a
ruling the owner already gave* (*"I dont want death to cost the whole inventory"*), and a
delegation to decide open questions is not permission to reopen closed ones.


### Settled by decision 5 rather than answered separately

**What should a Warden be able to cite you for?** This used to be its own open question,
because `WORLD.md`'s locked countermeasures are all about *casting* and there was no way to
catch anybody casting. **Making the spell a spoken word answers it.** The offence is
audible, it happens in front of witnesses, and a Warden standing in the room has grounds
without any new mechanic — which is exactly what the locked countermeasures assumed and
nothing could previously deliver.

The two magic-free alternatives stay on the shelf rather than being built: the **sleep
code** (the Wardenate still enforcing bedtime for a god that is not watching) and
**permitted airspace** (the better joke — the enforcement mechanism died with the god while
the policy did not, but nothing currently raises the height limit, so nobody can break it).
Both remain available as second and third offences once `cite` exists.

### Not a decision -- a gap this container cannot close

**Playtesting, and looking at the Warden.** This container has no game client, so two
things about the model are unverifiable here and are not claimed: how it looks
**animated**, and how it looks **lit**. `tools/entity_view.py` covers shape and paint; it
cannot cover those. The render has been sent for review.

**And now one more, which decision 5 created:** whether **typing a word to cast** is
satisfying or irritating. It cannot be settled here at all. The affordance is one seam over
spells that do not know how they were triggered, so if it plays badly the swap is cheap —
but somebody has to play it.

**Answered:** license is **MIT** (`LICENSE`, `gradle.properties`). `main` branch exists;
work flows to `claude/minecraft-mod-dev-rp0x8j` and PRs into `main`.

## Open questions

### Proposed, needs the owner's yes

*(Empty. Everything that was here has been decided — see*
*["Waiting on owner"](#waiting-on-owner) above and the*
*`[LOCKED — owner delegated; decided here.]` entries in [`WORLD.md`](WORLD.md).*
*The portal table and the six spell definitions now live there, where a decision belongs,*
*rather than here where a proposal does.)*

**One thing remains proposed and is deliberately not in that list**: the Anchorite's
lost-property office — see ["Also open, and smaller"](#also-open-and-smaller). It is
different in kind from the seven, because it *reverses a ruling the owner already gave*
rather than filling a gap they never ruled on. A delegation to answer open questions is not
permission to reopen closed ones.

### Answered this session

- **The "Warden" name collision**: **the name stays**, owner-delegated. The collision
  only bites if *Warden* is what the thing **is**, and it never was -- a Warden never
  calls itself one in any shipped line, it says *"this unit"* every time, and the word
  appears only in the Wardenate's own paperwork. A rank an office grants, not a species,
  with its own four-voices row: *this unit* / **Warden** / *a docket* / *a posting*.
- **Does the Anchorite's law lift dropped items?**: **no.** The owner's words: *"I dont
  want death to cost the whole inventory."* Only `FallingBlockEntity` rises -- exactly
  the set the ferry's boarding notice names. There is a proposed version that keeps the
  law whole without the punishment; it is in "Also open, and smaller" above.
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

**What do the six unnamed spells do?** `WORLD.md`'s school lists name twelve verbs and
describe only six of them. The described ones are built — *Weather*, *Rewind*, *Lighten*,
*Drop-forge*, *Bridgeroot*, *Wildgrowth*, *Hush*, *Still*, *Quell* (*"a blaze that cannot
ignite"*) and *Loft* (*"make a small structure weightless and carry it"*).

*Loft* is built. That was the last one with a description.

**Six are bare names with nothing attached: *Hedge*, *Graft*, *Moor*, *Held-breath*,
*Ripen*, *Rot*.** Deciding what one of those does is designing a mechanic, not
implementing one, so they wait. Sketches to react to, each read off its school rather
than invented next to it:

| Spell | School | A shape that fits the school |
|---|---|---|
| *Hedge* | Verdancy | a living wall that grows where you draw it and thickens if struck |
| *Graft* | Verdancy | join two plants, or a plant to a block, so one feeds the other |
| *Moor* | Weight | the opposite of *Lighten*: fix a thing where it is, against any push |
| *Held-breath* | Silence | your own sound taken away — nothing tracks you while you hold it |
| *Ripen* | The Turning | age a living thing forward: crop, sapling, animal |
| *Rot* | The Turning | age one forward past its end |

None of these is more than a guess at the owner's intent, and *Rot* in particular could be
the darkest verb in the mod or the least interesting one depending on what it may be aimed
at. **Nothing is built from this table until it comes back.**

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

**Three things on this list ARE waiting on the owner** — see "Waiting on owner" below.
Everything else is unblocked. In order:

1. **The systems, not just the surfaces.** *(Still the largest thing unbuilt — but one
   god now has a system rather than a surface.)*

   **Two of the four portals are built**, each with an under-layer behind it.

   * **The Anchorite's shaft** (`interregnum:mass_authority_lower`) is a *Lighten* zone's
     footprint taken through the height of the world: let go inside it for two seconds and
     you go through. Nothing is placed — the door is the spell, cast in the one world where
     the spell's law is also the world's. The return leg was the design and a check found
     it: you arrive standing on the floor down there, so below, the shaft lifts everything
     inside it. `tools/descent_check.sh`.
   * **The Verdant's grown door** (`interregnum:green_authority_lower`) has a lifespan. You
     plant a sapling and the world remembers; it is not a door until it is a tree; one cast
     of *Wildgrowth* ripens it and so does patience; fell the trunk and it is gone in the
     same instant. You pass through by **standing still under it** — the exact counterpart
     of letting go. `tools/grove_check.sh`.

   * **The Hearth-Turner's door** (`interregnum:temporal_authority_lower`) is a hole in a
     wall, and it is open only while the six blocks framing it are at one age. That world's
     sky is stopped, so the hour is not a time of day — it is a stage in the Turning's own
     chain, `stone_bricks → cracked → mossy`, and the open hour is the **middle** link,
     the only one reachable from both directions. *Weather* ages a fresh frame into it;
     *Rewind* brings an old one back. You pass through by **walking in**, because all the
     difficulty here was in making the hour. The far side is stamped with a matching door,
     since nothing over there builds anything. `tools/hour_check.sh`.

   * **The Quiet One's silence** (`interregnum:unresponsive_lower`) is a cast *Hush* zone
     that nothing has disturbed for five seconds. It listens the way sculk listens —
     `VanillaGameEvent`, the game's own server-side vibration model — because the audible
     half of that god's law lives on a client and this mod has never claimed it. Every game
     event counts, with no allow-list, and a noise **resets** rather than delays. It is the
     only door with no second condition and the only **shared** one: a stranger's footstep
     closes yours. `tools/silence_check.sh`.

   **All four portals are built.** Four doors, four verbs — *let go, stand still, walk in,
   make no sound.*

   **What is left of the grammar: the far-layers.** No god has one. `WORLD.md` locks
   *surface · under-layer · far-layer* and nothing has been decided about what a far-layer
   **is** beyond being further in — which is a design question, not a build. The four
   under-layers are also still on placeholder terrain, sharing their surface's biome; that
   decision and the terrain one want making together (see the terrain item).

   Also missing from all five: **terrain that is designed.** They use vanilla noise with
   one fixed biome, and each file says so in its own javadoc. The under-layer shares its
   surface's biome deliberately — a colour decision about a place whose shape is not
   designed yet is two decisions that want making together.

2. ~~**Bands 3 and 4.**~~ **Both built.** The exodus leaks three of the four laws into
   the overworld (the Quiet One's needs client-side audio this container cannot verify,
   and is named as a gap above). Attrition has both halves — the tending signal and the
   generalisation table. What remains of either is *content*: more conversions, more
   leaking laws. Machinery is done.

3. ~~**Warden behaviour.**~~ **Patrol and inspect are built.** `WardenPatrolGoal` walks a
   fixed four-point beat; `SiteReturn` files what it finds. **`cite` is the one verb still
   missing and it waits on the owner**: a Warden needs something to accuse you of, and the
   locked countermeasures are all about casting, which does not exist yet.

4. ~~**The delivery scenes.**~~ **All four built**, recording `LETTER_DELIVERED` and
   teaching their god's school. Chapters 3–5 gate on that count.

5. ~~**Magic.**~~ **Four schools, ten spells**, all learned in their
   gods' worlds and all costing the overworld what they do not cost a living god's.
   *Weather* changes a block and *Rewind* changes it back; *Lighten* encloses a region and
   *Drop-forge* makes one where impacts count; *Bridgeroot* creates blocks and
   *Wildgrowth* hurries the ones already there; *Hush* forbids, *Still* holds, *Quell*
   takes one creature's throwing arm and lets it keep it wherever it goes, and *Loft*
   picks a small building up and carries it. Ten different verbs, which is what shows the
   school system is a system rather than one mechanism with four names.

   **Two things it is short of, one of them the owner's:**

   * **No way to cast in play.** Every spell is reachable only by command. `WORLD.md`
     locks what spells *are* and says nothing about the affordance that triggers one —
     item, key, gesture, spoken word — decided, and **built**: you say the word, in chat,
     in front of whoever is there. See "Casting is a spoken word" below. Ten spells are
     now reachable in play, which closes what was the single biggest gap in the mod.
   * **The rest of the kits.** *Hedge*, *Graft*, *Moor*, *Held-breath*, *Ripen*, *Rot* —
     six names that carried no description, **now defined** in [`WORLD.md`](WORLD.md) under
     "The six that were only names". Unblocked, and the cheapest content left: the
     machinery each needs is already built and proven. *Held-breath* should wait for the
     casting affordance, because taking your voice away only costs something once your
     voice is what casts.

6. **The dialogue screen.** A real GUI over `Conversations.Table`, replacing the chat
   rendering. Deliberately last: it is the one part this container cannot verify, and
   everything it would render is already proven server-side. Chat works meanwhile.
7. **`VERIFY:` markers — the API-specific ones are cleared.** ARCHITECTURE.md's three
   (registration, capabilities → data attachments, payload/handler registration),
   DATAGEN.md's item-model row and TEXTURING.md's paths are now **VERIFIED against
   26.2.0.67** — most of them by shapes this repo compiles and CI boots a server on.

   **Six remain, and none of them is debt.** Each now says what evidence would clear it,
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
   * `SealedLetterItem` **the right-click that opens a letter** — the page it produces is
     verified through the command that shares `LetterPage`; the use that reaches it is not,
     because a headless server has no player to use an item.
   * `HauntSleepEvents` **the dream's trigger** — the branch that routes a sleeping killer
     to the ghost. Every rule it applies is verified through the command seam; the
     right-click that reaches it is not, because a headless server has no player to make
     one. Clearing it needs one player, one bed, and a client.
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
