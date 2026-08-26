"""Verify the core self-test by breaking the engine on purpose.

VERIFICATION.md: "a check that has never failed is unverified." This runs each
mutation below against a scratch copy of core/, compiles it, and requires the
self-test to FAIL. A mutation that survives means the suite has a hole -- the
output names it and this script exits non-zero.

    python3 tools/mutate_check.py

Add a mutation whenever you add a guard. That is the whole discipline.
"""
import os, shutil, subprocess, sys, tempfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CORE = os.path.join(REPO, "core")
MAIN = "core/src/main/java/com/cadykaya/interregnum/core"

# (description, file, find, replace)
MUTATIONS = [
    ("dialogue: INITIATOR ignores the initiator",
     f"{MAIN}/dialogue/Conversation.java",
     "case INITIATOR -> picks.getOrDefault(initiator, picks.values().iterator().next());",
     "case INITIATOR -> picks.values().iterator().next();"),
    ("dialogue: UNANIMOUS silently falls back to majority",
     f"{MAIN}/dialogue/Conversation.java",
     "yield distinct.size() == 1 ? distinct.iterator().next() : null;",
     "yield picks.values().iterator().next();"),
    ("dialogue: graph validator waves through failures",
     f"{MAIN}/dialogue/DialogueGraph.java",
     "if (!failures.isEmpty())", "if (false && !failures.isEmpty())"),
    ("dialogue: VOTE tie does not favour the initiator",
     f"{MAIN}/dialogue/Conversation.java",
     "yield (top.size() > 1 && initiatorPick != null && top.contains(initiatorPick))\n                        ? initiatorPick : top.get(0);",
     "yield top.get(0);"),
    ("dialogue: stances lose the order people spoke in",
     f"{MAIN}/dialogue/Conversation.java",
     "        return Collections.unmodifiableMap(new LinkedHashMap<>(picks));",
     "        return Map.copyOf(picks);"),
    ("dialogue: a departed player's pick stays on the table",
     f"{MAIN}/dialogue/Conversation.java",
     "        picks.remove(participant);\n        return participants.remove(participant);",
     "        return participants.remove(participant);"),
    ("dialogue: the initiator can be removed, orphaning INITIATOR nodes",
     f"{MAIN}/dialogue/Conversation.java",
     "        if (participant.equals(initiator))\n"
     "            throw new IllegalArgumentException(\"cannot remove the initiator; end the conversation\");",
     ""),
    ("effects: everyone is judged on the winning option, not their own stance",
     f"{MAIN}/regard/RegardEffects.java",
     "            DialogueOption option = resolution.stanceOf(participant);",
     "            DialogueOption option = resolution.chosen();"),
    ("effects: a failed unanimous vote still gets recorded",
     f"{MAIN}/regard/RegardEffects.java",
     "        if (resolution.kind() != Resolution.Kind.ADVANCED) {\n"
     "            return applied;                    // nothing was settled, so nothing was said\n"
     "        }",
     ""),
    ("effects: the requested delta is reported instead of the applied one",
     f"{MAIN}/regard/RegardEffects.java",
     "                int delta = state.adjust(entry.getKey(), entry.getValue());",
     "                state.adjust(entry.getKey(), entry.getValue());\n"
     "                int delta = entry.getValue();"),
    ("facing: yaw is not negated, so everything faces backwards",
     f"{MAIN}/spatial/Facing.java",
     "        return (float) -Math.toDegrees(Math.atan2(dx, dz));",
     "        return (float) Math.toDegrees(Math.atan2(dx, dz));"),
    ("facing: the axes are swapped",
     f"{MAIN}/spatial/Facing.java",
     "        return (float) -Math.toDegrees(Math.atan2(dx, dz));",
     "        return (float) -Math.toDegrees(Math.atan2(dz, dx));"),
    ("chapter: high-water mark may regress",
     f"{MAIN}/chapter/ChapterState.java",
     "if (derived.band > highWater.band) highWater = derived;", "highWater = derived;"),
    ("chapter: deserialize trusts a saved chapter without re-deriving",
     f"{MAIN}/chapter/ChapterState.java",
     "st.recompute();", "if (false) st.recompute();"),
    ("chapter: prerequisites become any-of instead of all-of",
     f"{MAIN}/chapter/Chapter.java",
     "case EXODUS -> Set.of(Milestone.DEICIDE, Milestone.WARDEN_CONTACT,\n                                  Milestone.FIRST_CROSSING);",
     "case EXODUS -> Set.of(Milestone.FIRST_CROSSING);"),
    ("regard: deicide leaves no ceiling on survivors",
     f"{MAIN}/regard/RegardState.java",
     "                lowerCeiling(i, -10);", ""),
    ("regard: the victim's floor can be raised again",
     f"{MAIN}/regard/RegardState.java",
     "                lowerCeiling(i, MIN);", "                adjust(i, -100);"),
    ("regard: ghost relationship leaks to non-killers",
     f"{MAIN}/regard/RegardState.java",
     "if (i == Institution.THE_GHOST && !isKiller) return 0;", ""),
    ("regard: a deicide drags the villages down with the gods",
     f"{MAIN}/regard/RegardState.java",
     "            if (i == Institution.WARDENATE || i == Institution.THE_GHOST\n"
     "                    || i == Institution.VILLAGES) continue;",
     "            if (i == Institution.WARDENATE || i == Institution.THE_GHOST) continue;"),
    ("regard: adjust reports the requested delta, not the applied one",
     f"{MAIN}/regard/RegardState.java",
     "return after - before;", "return delta;"),

    # The crossing guards. Each of these is a way the band notices turn back into
    # the karma bar they exist to avoid, and none of them would raise an exception
    # or look wrong in a log -- they would just quietly tell the player the wrong
    # thing, which is why they are here rather than trusted by reading.
    ("crossing: every movement is reported, not just the ones that cross",
     f"{MAIN}/regard/Standings.java",
     "if (was != null && was != now) {", "if (was != null) {"),
    ("crossing: a fall is reported as a rise",
     f"{MAIN}/regard/BandChange.java",
     "return to.ordinal() > from.ordinal();", "return true;"),
    ("crossing: an event that crossed nothing is allowed through",
     f"{MAIN}/regard/BandChange.java",
     "if (from == to) {", "if (false) {"),
    # The ferry's bill of lading. Every one of these is a way the crossing stops
    # TEACHING -- which is the whole reason the checklist exists -- while still
    # letting or refusing exactly the right cargo, so none of them looks like a bug.
    ("ferry: the checklist stops at the first violation",
     f"{MAIN}/ferry/Manifest.java",
     "                out.add(new Violation(rule, entry.getKey(), entry.getValue(),\n"
     "                        law.rules().get(rule).reasonKey()));",
     "                out.add(new Violation(rule, entry.getKey(), entry.getValue(),\n"
     "                        law.rules().get(rule).reasonKey()));\n                return out;"),
    ("ferry: the checklist loses the count",
     f"{MAIN}/ferry/Manifest.java",
     "        Map<String, Integer> counts = new TreeMap<>();",
     "        Map<String, Integer> counts = new TreeMap<>() {\n"
     "            @Override public Integer merge(String k, Integer v,\n"
     "                    java.util.function.BiFunction<? super Integer, ? super Integer, ? extends Integer> f) {\n"
     "                return super.merge(k, v, (a, b) -> 1);\n            }\n        };"),
    ("ferry: a law that refuses nothing is allowed",
     f"{MAIN}/ferry/Law.java",
     "        if (rules.isEmpty()) {", "        if (false) {"),
    ("ferry: one block may be refused by two rules",
     f"{MAIN}/ferry/Law.java",
     "                if (other != null) {", "                if (false) {"),
    # DETERMINISTICALLY reversed, not merely unsorted. The obvious mutation here is
    # `Map.copyOf(blocks)`, and it is wrong: Java's immutable maps randomise their
    # iteration order with a per-JVM salt, so on roughly one run in six a three-key
    # manifest comes out in sorted order by luck and the mutation escapes. It escaped
    # on CI while passing locally, twice. A mutation must fail for the reason it names,
    # every time -- docs/LESSONS.md #19, which is about assertions and applies just as
    # hard to the deliberate bugs used to test them.
    ("ferry: the bill of lading reshuffles between crossings",
     f"{MAIN}/ferry/Manifest.java",
     "        blocks = Collections.unmodifiableMap(new TreeMap<>(blocks));",
     "        Map<String, Integer> flipped = new TreeMap<>(java.util.Comparator.reverseOrder());\n"
     "        flipped.putAll(blocks);\n"
     "        blocks = Collections.unmodifiableMap(flipped);"),

    # Band 3, the exodus. Which god leaks where is a pure function of coordinates, and
    # both of its failure modes are silent: a hash that favours one god hides three of
    # them, and a striping hash gives a player walking one direction the same law
    # forever. Either way the band looks implemented and teaches a quarter of what it
    # was for.
    ("exodus: one god leaks everywhere and the other three nowhere",
     f"{MAIN}/exodus/Exodus.java",
     "        return LAWS[Math.floorMod(h, LAWS.length)];",
     "        return LAWS[0];"),
    ("exodus: the leak is striped, so a walk meets one law forever",
     f"{MAIN}/exodus/Exodus.java",
     "        int h = chunkX * 0x9E3779B9 ^ chunkZ * 0x85EBCA6B;",
     "        int h = chunkX + chunkZ; if (true) return LAWS[Math.floorMod(h, LAWS.length)];"),
    ("exodus: the overworld leaks before band 3",
     f"{MAIN}/exodus/Exodus.java",
     "        return band >= BAND;", "        return true;"),

    # Band 4, attrition. Two of these three are the same defect wearing different
    # clothes: band 4 can only act on ground that is loaded, so if tending ever reaches
    # as far as loading does, there is nowhere left for it to act and the whole band is
    # inert. Nothing crashes, nothing logs, and the world simply never forgets anything.
    ("attrition: tending reaches as far as loading, so the band can never act anywhere",
     f"{MAIN}/attrition/Attrition.java",
     "    public static final int TEND_RADIUS_CHUNKS = 2;",
     "    public static final int TEND_RADIUS_CHUNKS = 12;"),
    ("attrition: everything is tended, so nothing ever frays",
     f"{MAIN}/attrition/Attrition.java",
     "        return Math.max(Math.abs(chunkDX), Math.abs(chunkDZ)) <= TEND_RADIUS_CHUNKS;",
     "        return true;"),
    ("attrition: ground goes stale the instant it is tended",
     f"{MAIN}/attrition/Attrition.java",
     "        return nowTick - lastTendedTick >= FRAY_AFTER_TICKS;",
     "        return true;"),
    ("attrition: the world forgets itself before band 4",
     f"{MAIN}/attrition/Attrition.java",
     "        return band >= BAND;", "        return true;"),

    # A node may mark the world. The dangerous direction is not a scene forgetting to
    # mark -- a live check catches that -- it is EVERY node marking, which would advance
    # the chapter on every line anybody speaks and end the progression before the first
    # scene did. And the count must accumulate: four gods, four letters, and a delivery
    # recorded as a flag could never tell one god answered from all four.
    ("dialogue: every node marks the world, so the chapter advances on every line",
     f"{MAIN}/dialogue/DialogueNode.java",
     "        return milestone != null;", "        return true;"),

    # Schools are learned in their worlds, so nothing is known by default. If an
    # untaught caster could cast, the reason to cross would be gone and every spell in
    # the mod would ship already in everybody's hands -- and nothing would look broken.
    ("magic: everybody can cast everything without being taught",
     f"{MAIN}/magic/Casting.java",
     "        return grimoire != null && grimoire.knows(school);",
     "        return true;"),
    ("magic: learning one school teaches all four",
     f"{MAIN}/magic/Grimoire.java",
     "    public boolean knows(School school) {\n        return known.contains(school);",
     "    public boolean knows(School school) {\n        if (!known.isEmpty()) return true;\n        return known.contains(school);"),
    ("magic: casting is free at home, so the Wardens are wrong",
     f"{MAIN}/magic/Casting.java",
     "        return inTheOverworld;", "        return false;"),

    ("magic: a Lighten zone has no edge, so it is weather rather than a spell",
     f"{MAIN}/magic/Zone.java",
     "        return Math.abs(px - x) <= radius\n                && Math.abs(py - y) <= radius\n                && Math.abs(pz - z) <= radius;",
     "        return true;"),
    ("magic: a zone never lapses, so half a minute of physics becomes terrain",
     f"{MAIN}/magic/Zone.java",
     "        return nowTick > expiresAtTick;", "        return false;"),

    ("magic: a span has no cap, so distance stops costing anything",
     f"{MAIN}/magic/Bridgeroot.java",
     "        int reach = Math.min(steps, MAX_SPAN);", "        int reach = steps;"),
    ("magic: a span starts under the caster's feet",
     f"{MAIN}/magic/Bridgeroot.java",
     "        for (int i = 1; i <= reach; i++) {", "        for (int i = 0; i <= reach; i++) {"),

    ("magic: two spells share a school, so one lesson hands out another god's verbs",
     f"{MAIN}/magic/Hush.java",
     "    public static final School SCHOOL = School.SILENCE;",
     "    public static final School SCHOOL = School.WEIGHT;"),

    ("magic: two spells of one school collapse onto the same key",
     f"{MAIN}/magic/Spell.java",
     "    STILL(School.SILENCE),", "    STILL(School.WEIGHT),"),

    # Quell is the Quiet One's THIRD, so the school it names is now load-bearing in a way
    # the first two were not: a School.SILENCE that is not Silence hands the Quiet One's
    # verb out with somebody else's lesson, and every existing Silence assertion still
    # passes because Hush and Still are untouched.
    ("magic: the Quiet One's third spell is taught by the wrong god",
     f"{MAIN}/magic/Quell.java",
     "    public static final School SCHOOL = School.SILENCE;",
     "    public static final School SCHOOL = School.TURNING;"),

    # A quelling that lapses immediately is the failure nothing else here would see: the
    # spell still "works" at the instant of the cast, and every check that reads the
    # command's reply still passes.
    ("magic: a quelling lapses the instant it is cast",
     f"{MAIN}/magic/Quell.java",
     "    public static long expiryAt(long nowTick) {\n        return nowTick + DURATION_TICKS;",
     "    public static long expiryAt(long nowTick) {\n        return nowTick;"),

    # The Anchorite's pair is the worse one: Lighten and Drop-forge are opposites, so
    # collapsing them onto one key produces a zone that lifts the weight it is waiting
    # for. Named separately from the Quiet One's because the assertion has to be about
    # the PAIR -- an earlier version of this guard counted schools and survived moving a
    # spell out of one, which is the whole reason both pairs are asserted by name.
    ("magic: the Anchorite's two spells collapse onto the same key",
     f"{MAIN}/magic/Spell.java",
     "    DROP_FORGE(School.WEIGHT),", "    DROP_FORGE(School.SILENCE),"),
    ("magic: a drop-forge reaches as far as a low-gravity field, and stops being a "
     "workbench",
     f"{MAIN}/magic/DropForge.java",
     "    public static final int RADIUS = 3;",
     "    public static final int RADIUS = 7;"),
    ("magic: a drop-forge lapses before its caster can fetch anything to drop in it",
     f"{MAIN}/magic/DropForge.java",
     "    public static final long DURATION_TICKS = 20L * 60;",
     "    public static final long DURATION_TICKS = 20L * 15;"),

    ("magic: a surge nobody can aim reaches further than the spell it is safe to stand in",
     f"{MAIN}/magic/Wildgrowth.java",
     "    public static final int RADIUS = 3;",
     "    public static final int RADIUS = 9;"),
    # The steles. Their inscription is a pure function of position, so the two ways it
    # can be wrong are "the same stele moves" and "every stele is the same" -- and both
    # are silent: the world still reads out a notice either way.
    ("steles: every stele in the world carries the same notice",
     f"{MAIN}/stele/Steles.java",
     "        return Math.floorMod(h, COUNT);",
     "        return 0;"),
    ("steles: a negative coordinate indexes backwards off the end of the notice list",
     f"{MAIN}/stele/Steles.java",
     "        return Math.floorMod(h, COUNT);",
     "        return h % COUNT;"),
    ("steles: the light rule is a threshold nobody can be on the wrong side of",
     f"{MAIN}/stele/Steles.java",
     "    public static final int READING_LIGHT = 7;",
     "    public static final int READING_LIGHT = 0;"),

    # Clasts. The count IS the mechanic -- WORLD.md locks "clasts are finite; the class
    # is a server negotiation" -- so the arithmetic that caps it gets the same treatment
    # as a block table.
    ("clasts: the crater holds the whole class, so the killer has it on the first day",
     f"{MAIN}/clast/Clasts.java",
     "    public static final int AT_CRATER = 3;",
     "    public static final int AT_CRATER = 7;"),
    ("clasts: the pool hands out whatever is asked for, so a world mints classes",
     f"{MAIN}/clast/Clasts.java",
     "        return Math.max(0, Math.min(want, TOTAL - Math.max(0, issued)));",
     "        return want;"),

    ("magic: one cast of Wildgrowth is worth less than one segment of sugar cane",
     f"{MAIN}/magic/Wildgrowth.java",
     "    public static final int PUSHES = 24;",
     "    public static final int PUSHES = 8;"),

    # The dead god's mail. The invariant here is about a SET -- three letters open with
    # a name, the fourth opens `To --` -- so nothing about an individual letter can
    # catch it going. If it goes, the mid-game's best reveal quietly stops being one and
    # the only people who find out are the readers who never see it work.
    ("letters: any number of letters may open unaddressed",
     f"{MAIN}/letters/Post.java",
     "        if (unnamed != UNADDRESSED) {", "        if (false) {"),
    ("letters: a blank addressee passes for an absent one",
     f"{MAIN}/letters/Letter.java",
     "        if (addressee.isPresent() && addressee.get().isBlank()) {",
     "        if (false) {"),
    ("letters: a letter with nothing written in it opens a questline",
     f"{MAIN}/letters/Letter.java",
     "        if (bodyKeys.isEmpty()) {", "        if (false) {"),

    # The standing gate. This is the first thing in the mod that READS regard, so its
    # failure mode is content appearing to players who have not earned it -- which
    # looks exactly like the game working and is the one bug a playtester cannot file.
    ("gate: the standing requirement is never consulted",
     f"{MAIN}/dialogue/DialogueOption.java",
     "return playerTags.containsAll(requiredTags) && standing.admits(regard);",
     "return playerTags.containsAll(requiredTags);"),
    ("gate: the floor is off by a band",
     f"{MAIN}/dialogue/StandingGate.java",
     "|| state.standing(e.getKey()).ordinal() < e.getValue().ordinal()) {",
     "|| state.standing(e.getKey()).ordinal() < e.getValue().ordinal() - 1) {"),
    ("gate: the ceiling is not enforced",
     f"{MAIN}/dialogue/StandingGate.java",
     "        for (var e : atMost.entrySet()) {",
     "        for (var e : atMost.entrySet()) { if (true) continue;"),
    ("gate: having no ghost counts as being on terms with it",
     f"{MAIN}/dialogue/StandingGate.java",
     "return institution != Institution.THE_GHOST || state.isKiller();",
     "return true;"),

    # Text variants. Failure here is a player being addressed as a stranger by an
    # institution that has a file on them -- quiet, plausible, and invisible to
    # anyone who has not played far enough to have a file.
    ("variant: the node's line ignores who is reading it",
     f"{MAIN}/dialogue/DialogueNode.java",
     "return TextVariant.choose(textVariants, regard, textKey);",
     "return textKey;"),
    ("variant: the last match wins instead of the first",
     f"{MAIN}/dialogue/TextVariant.java",
     "        for (TextVariant v : variants) {\n"
     "            if (v.gate().admits(regard)) {\n"
     "                return v.textKey();\n"
     "            }\n"
     "        }\n"
     "        return fallback;",
     "        String found = fallback;\n"
     "        for (TextVariant v : variants) {\n"
     "            if (v.gate().admits(regard)) {\n"
     "                found = v.textKey();\n"
     "            }\n"
     "        }\n"
     "        return found;"),
    ("variant: an unconditional variant is allowed to shadow the node",
     f"{MAIN}/dialogue/TextVariant.java",
     "        if (gate.isOpen()) {", "        if (false) {"),

    ("crossing: institutions are reported in whatever order a map iterates",
     f"{MAIN}/regard/Standings.java",
     "for (Institution i : Institution.values()) {\n            Standing was = before.get(i);",
     "for (Institution i : before.keySet().stream()\n                .sorted(java.util.Comparator.comparing(Enum::name)).toList()) {\n            Standing was = before.get(i);"),
]


def run(workdir):
    """-> (compiled, selftest_exit). Measures the JVM's exit, never a pipe's."""
    build = os.path.join(workdir, "build")
    os.makedirs(build, exist_ok=True)
    srcs = []
    for r, _, fs in os.walk(os.path.join(workdir, "core", "src")):
        srcs += [os.path.join(r, f) for f in fs if f.endswith(".java")]
    cj = subprocess.run(["javac", "-d", build] + srcs, capture_output=True, text=True)
    if cj.returncode != 0:
        return False, None
    rj = subprocess.run(["java", "-cp", build,
                         "com.cadykaya.interregnum.core.SelfTest"],
                        capture_output=True, text=True)
    return True, rj.returncode


def main():
    ok, code = run_in_place()
    if not ok or code != 0:
        print("FAIL: the unmutated self-test does not pass; fix that first")
        return 1
    print(f"baseline: self-test passes\n")

    survivors = []
    for desc, rel, find, repl in MUTATIONS:
        with tempfile.TemporaryDirectory() as td:
            shutil.copytree(CORE, os.path.join(td, "core"))
            p = os.path.join(td, rel.replace("core/", "core/", 1))
            src = open(p).read()
            if find not in src:
                print(f"  [STALE ] {desc}\n           pattern no longer present in {rel}")
                survivors.append(desc + " (stale pattern)")
                continue
            open(p, "w").write(src.replace(find, repl, 1))
            compiled, exit_code = run(td)
            if not compiled:
                print(f"  [SKIP  ] {desc} (mutant did not compile)")
                continue
            if exit_code == 0:
                print(f"  [SURVIVED] {desc}")
                survivors.append(desc)
            else:
                print(f"  [caught] {desc}")

    print()
    if survivors:
        print(f"FAIL: {len(survivors)} mutation(s) not caught by the self-test:")
        for s in survivors:
            print("  -", s)
        return 1
    print(f"OK: all {len(MUTATIONS)} mutations caught")
    return 0


def run_in_place():
    with tempfile.TemporaryDirectory() as td:
        shutil.copytree(CORE, os.path.join(td, "core"))
        return run(td)


if __name__ == "__main__":
    sys.exit(main())
