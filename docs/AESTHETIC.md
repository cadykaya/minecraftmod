# Design doctrine

> **Provenance: PORTED.** Adapted from DOWNTIME's `AESTHETIC.md`, which describes itself as
> *"project-agnostic on purpose — it applies to anything with a setting, and can be lifted
> into another project unchanged."* This is that lift, with the Minecraft-specific
> application added.
>
> The full ~150-entry reference shelf lives in `cadykaya/mario-3` `docs/AESTHETIC.md` and is
> worth reading whole. Condensed here to the part that makes decisions.

---

## The only question

> **Does the strange world possess stable logic that naturally creates funny suffering, or
> is the writer merely throwing weird objects, references, voices, and arbitrary rules at
> the audience?**

The target is **coherent, hostile weirdness**: a world whose rules are consistent,
comprehensible, and merciless. Not random. Not quirky. The comedy comes from **systems
working exactly as designed** on people who have to live inside them.

---

## The design test

For every strange idea, ask:

1. **Why does this exist here?**
2. **How have ordinary people adapted to it?**
3. **What institution profits from, regulates, worships, or misunderstands it?**
4. **What limitation prevents it from solving everything?**
5. **What earlier decision caused the current disaster?**
6. **Will anyone remember this afterward?**
7. **Could I replace it with a different random weird thing without changing the story?**

### Question 7 is the executioner

If a talking staircase could be swapped for a singing refrigerator and the scene still works
identically, it is **decorative nonsense**. If swapping it breaks the architecture, the local
economy, the character relationships, the puzzle design and the eventual catastrophe — the
weirdness is real.

> **Nothing ships until it survives question 7 in writing.**

### The tell: false specificity

Levels, ranks, spell names, currencies, guild rules and menus create the *appearance* of
structure. Ask whether they **constrain anyone**. If no one is ever prevented from doing
something, or forced into something, by the rule — the rule is set dressing wearing a
system's clothes.

### The corollary: never state the theme

The strongest anti-institutional works — *Papers, Please*, *Mother 3*, *LISA*, *Cruelty
Squad* — never have a character explain the critique. They implicate the audience through
**mechanism and complicity** instead.

> A speech about how the company is bad is weaker than **one number going up while a place
> goes quiet.**

---

## Applying this to a Minecraft mod

> **DOCTRINE.** New. The design test above is medium-agnostic; how it fails is not, and
> modded content fails it in specific, recognisable ways.

### Question 4 is the one mods fail

**"What limitation prevents it from solving everything?"**

This is the question the entire tech-mod genre struggles with, and it is why so many mods
make the base game worse rather than richer. A new ore that is strictly better than diamond
does not *add* a tier — it **deletes diamond from the game**. A dimension with abundant
resources and no cost deletes the overworld. A machine that automates a thing with no
constraint deletes the activity it automated.

> **Every addition must take something away, or it is not an addition — it is a replacement
> wearing an addition's clothes.**

Ask it as: *after this exists, what does a player stop doing?* If the answer is "the thing
this replaces, entirely, with no trade," redesign it.

### Question 7, in modded form

Swap your block for a differently-coloured block. Does anything change?

If your ore's only property is being a different colour with a different number, it is
**false specificity** — the exact tell above. Vanilla's ores are not distinguished by their
numbers, they are distinguished by *where they are*, *what they cost to get*, and *what only
they can do*.

### Question 2 is where the best modded content comes from

**"How have ordinary people adapted to it?"**

Minecraft's world is mostly uninhabited, so this question maps onto **structures, ruins,
loot and worldgen** rather than dialogue. The best modded content answers it environmentally:
somebody built this, it did not work, here is what is left. That is worldbuilding that costs
no writing and no NPCs, and it is exactly what the medium is good at.

### The vanilla-coherence constraint

The one genuinely new constraint, and it has no equivalent in a standalone game:

> **Your content sits inside somebody else's finished, coherent world, and it is judged
> against thirteen years of it.**

This is the design counterpart to [`PALETTE.md`](PALETTE.md)'s vanilla-adjacency rule. The
same three failures apply:

- **Invisible** — indistinguishable from vanilla, so nothing was added.
- **Clashing** — louder than anything Mojang ships, so it reads as an import.
- **Uncanny** — close enough to read as a *bug* rather than a deliberate addition.

Vanilla's own design is restrained, low-magic, and explains almost nothing. Content that
arrives with lore text, a rarity system and eight tiers is not adding to that world, it is
sitting on top of it.

> **Match vanilla's restraint, then be strange within it.** The strangeness lands harder
> against a quiet background — which is the same argument as *never state the theme*, made
> in level design.

---

## North stars and anti-references

Carried over, since they are the calibration:

**North stars:** *Mother 3*, *Deltarune*, *Psychonauts*, *Disco Elysium*, *The Stanley
Parable*, *The Good Place*, *Discworld*, *A Series of Unfortunate Events*.

**Sharpest anti-references:** *High on Life*, *Harry Potter's* convenience-based magic,
*Borderlands 3*, generic cheat-skill isekai, franchise-collage multiverse comedy.

The distinction in one line: **institutions that obey their own procedures** versus
**characters announcing that they are zany.**

---

## The working agreement this inherits

From DOWNTIME's handoff, and it is the reason the doctrine has teeth:

> The owner cares intensely about one thing: **weird with stable logic that creates funny
> suffering** — never random weirdness, never reference collage. **They dislike lazy
> weirdness more than they dislike anything else.**
>
> When in doubt, make the strange thing **load-bearing** — or cut it.

---

## Open

> **BLOCKING for any content work.** This mod has no subject yet.
>
> Until it has one, there is nothing for this document to judge, `PALETTE.md` has no
> semantic law, and `WORLDGEN.md`'s dimension question cannot be answered. Everything built
> so far — the palette machinery, the texture pipeline, the doc set — is deliberately
> subject-agnostic and survives whatever the answer turns out to be.
>
> **The first real decision is what the mod is about.** It should be run through the seven
> questions in writing, here, before any content is registered.
