package com.cadykaya.interregnum.core;

import com.cadykaya.interregnum.core.chapter.*;
import com.cadykaya.interregnum.core.dialogue.*;
import com.cadykaya.interregnum.core.ferry.*;
import com.cadykaya.interregnum.core.regard.*;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Zero-dependency self-test, run by tools/check_all.sh. Verified per VERIFICATION.md
 * by mutating the engine (initiator rule, unanimity rule, graph validator) and
 * watching the harness fail each time -- and the harness itself was caught once
 * reporting a pipeline's grep exit instead of the JVM's; measure the process you
 * mean. Asserts on EFFECTS (who won, where the conversation went), never inputs.
 */
public final class SelfTest {
    static int passed = 0;

    static void check(boolean cond, String what) {
        if (!cond) { System.err.println("FAIL: " + what); System.exit(1); }
        passed++;
    }

    static DialogueGraph graph(ResolutionRule rule) {
        return new DialogueGraph("q", List.of(
            new DialogueNode("q", "warden", "d.q", rule, List.of(
                new DialogueOption("a", "d.a", "ok", List.of()),
                new DialogueOption("b", "d.b", "bad", List.of()),
                new DialogueOption("t", "d.t", "ok", List.of("class/theoclast")))),
            new DialogueNode("ok", "warden", "d.ok", ResolutionRule.INITIATOR, List.of(
                new DialogueOption("bye", "d.bye", DialogueGraph.END, List.of()))),
            new DialogueNode("bad", "warden", "d.bad", ResolutionRule.INITIATOR, List.of())));
    }

    public static void main(String[] args) {
        // -- graph validation actually rejects broken graphs ------------------
        boolean threw = false;
        try {
            new DialogueGraph("q", List.of(new DialogueNode("q", "x", "k",
                ResolutionRule.INITIATOR,
                List.of(new DialogueOption("a", "k", "nowhere", List.of())))));
        } catch (IllegalArgumentException e) { threw = e.getMessage().contains("nowhere"); }
        check(threw, "dangling target rejected, named in the error");

        threw = false;
        try {
            new DialogueGraph("q", List.of(
                new DialogueNode("q", "x", "k", ResolutionRule.INITIATOR,
                    List.of(new DialogueOption("a", "k", DialogueGraph.END, List.of()))),
                new DialogueNode("orphan", "x", "k", ResolutionRule.INITIATOR, List.of())));
        } catch (IllegalArgumentException e) { threw = e.getMessage().contains("unreachable"); }
        check(threw, "unreachable node rejected");

        // -- tag gating -------------------------------------------------------
        var opt = graph(ResolutionRule.VOTE).start().options().get(2);
        check(!opt.visibleTo(java.util.Set.of()), "tagged option hidden from untagged player");
        check(opt.visibleTo(java.util.Set.of("class/theoclast")), "tagged option shown to Theoclast");

        // -- INITIATOR: initiator wins, dissent is preserved as stances -------
        var c = new Conversation(graph(ResolutionRule.INITIATOR), "kaya", List.of("kaya", "p2", "p3"));
        c.submit("p2", "b"); c.submit("p3", "b"); c.submit("kaya", "a");
        var r = c.resolve(new Random(1));
        check(r.kind() == Resolution.Kind.ADVANCED && r.chosen().id().equals("a"),
              "initiator beats majority under INITIATOR");
        check(r.stances().get("p2").equals("b"), "dissenting stance recorded for the table");
        check(c.current().id().equals("ok"), "conversation advanced to the winner's target");

        // -- VOTE: majority wins; tie breaks toward initiator -----------------
        c = new Conversation(graph(ResolutionRule.VOTE), "kaya", List.of("kaya", "p2", "p3"));
        c.submit("kaya", "a"); c.submit("p2", "b"); c.submit("p3", "b");
        check(c.resolve(new Random(1)).chosen().id().equals("b"), "majority wins vote");
        // The other player submits FIRST, so insertion order puts THEIR pick at the
        // head of the tally. Only a real tie-break rule can still pick the initiator's.
        // (Submitting the initiator first made this test pass with the rule deleted.)
        c = new Conversation(graph(ResolutionRule.VOTE), "kaya", List.of("kaya", "p2"));
        c.submit("p2", "b"); c.submit("kaya", "a");
        check(c.resolve(new Random(1)).chosen().id().equals("a"),
              "tie breaks toward initiator even when their pick is not first in tally order");

        // -- ROLL: deterministic under a seed, weighted by popularity ---------
        c = new Conversation(graph(ResolutionRule.ROLL), "kaya", List.of("kaya", "p2", "p3"));
        c.submit("kaya", "a"); c.submit("p2", "b"); c.submit("p3", "b");
        String first = c.resolve(new Random(42)).chosen().id();
        c = new Conversation(graph(ResolutionRule.ROLL), "kaya", List.of("kaya", "p2", "p3"));
        c.submit("kaya", "a"); c.submit("p2", "b"); c.submit("p3", "b");
        check(c.resolve(new Random(42)).chosen().id().equals(first), "roll reproducible under same seed");

        // -- UNANIMOUS: dissent REPROMPTs with stances; accord advances -------
        c = new Conversation(graph(ResolutionRule.UNANIMOUS), "kaya", List.of("kaya", "p2"));
        c.submit("kaya", "a"); c.submit("p2", "b");
        r = c.resolve(new Random(1));
        check(r.kind() == Resolution.Kind.REPROMPT && r.chosen() == null,
              "unanimous node re-prompts on dissent");
        check(r.stances().size() == 2, "reprompt hands the table everyone's stance to argue over");
        c.submit("kaya", "a"); c.submit("p2", "a");
        check(c.resolve(new Random(1)).chosen().id().equals("a"), "accord advances");

        // -- stances come back in the order people spoke ----------------------
        //
        // Who answered first, before the others fell in behind them, is most of what
        // an argument reads as -- so the order is content and has to be stable.
        //
        // Asserted by COMPARING TWO TABLES rather than by checking one against a
        // literal. Java's immutable maps iterate in a salted hash order that changes
        // between JVM runs, so a single assertion could pass or fail on luck. Two
        // tables with the same participants in different submission orders cannot:
        // an unordered map gives both the same order, an insertion-ordered one
        // cannot. (The first version of this test was the flaky kind.)
        var a1 = new Conversation(graph(ResolutionRule.INITIATOR), "kaya",
                                  List.of("kaya", "p2", "p3"));
        a1.submit("p2", "b"); a1.submit("p3", "b"); a1.submit("kaya", "a");
        var order1 = new java.util.ArrayList<>(a1.resolve(new Random(1)).stances().keySet());

        var a2 = new Conversation(graph(ResolutionRule.INITIATOR), "kaya",
                                  List.of("kaya", "p2", "p3"));
        a2.submit("kaya", "a"); a2.submit("p3", "b"); a2.submit("p2", "b");
        var order2 = new java.util.ArrayList<>(a2.resolve(new Random(1)).stances().keySet());

        check(order1.equals(List.of("p2", "p3", "kaya")), "stances follow the order people spoke");
        check(!order1.equals(order2), "a different speaking order gives a different stance order");

        // -- leaving the table ------------------------------------------------
        //
        // Asserted on the EFFECT -- who wins the vote afterwards -- rather than on
        // participants().size(). A remove() that dropped the name but kept the pick
        // would pass a size check and still let someone who quit swing the table.
        c = new Conversation(graph(ResolutionRule.VOTE), "kaya", List.of("kaya", "p2", "p3"));
        c.submit("p2", "b"); c.submit("p3", "b"); c.submit("kaya", "a");
        check(c.remove("p3"), "removing a participant reports they were there");
        check(!c.remove("p3"), "removing them twice reports they were not");
        check(c.resolve(new Random(1)).chosen().id().equals("a"),
              "a departed player's vote leaves with them, so the tie breaks to the initiator");

        // allSubmitted must follow the shrunken table, or a leaver deadlocks it.
        c = new Conversation(graph(ResolutionRule.VOTE), "kaya", List.of("kaya", "p2"));
        c.submit("kaya", "a");
        check(!c.allSubmitted(), "table is not complete while someone has not picked");
        c.remove("p2");
        check(c.allSubmitted(), "table completes once the absentee is off it");

        threw = false;
        try { c.remove("kaya"); } catch (IllegalArgumentException e) { threw = true; }
        check(threw, "the initiator cannot be removed; the conversation ends instead");

        // -- terminal prose ends the conversation -----------------------------
        c = new Conversation(graph(ResolutionRule.INITIATOR), "kaya", List.of("kaya"));
        c.submit("kaya", "b");
        c.resolve(new Random(1));
        check(c.ended(), "advancing into a terminal node ends the conversation");
        threw = false;
        try { c.submit("kaya", "a"); } catch (IllegalStateException e) { threw = true; }
        check(threw, "a finished conversation refuses picks");

        facingChecks();
        chapterChecks();
        regardChecks();
        crossingChecks();
        standingGateChecks();
        textVariantChecks();
        ferryChecks();
        effectChecks();
        System.out.println("SelfTest: " + passed + " checks passed");
    }

    /**
     * The rule that makes ensemble dialogue mean anything: you are judged on what
     * YOU said, not on what the table decided.
     */
    static void effectChecks() {
        var pro = new DialogueOption("a", "d.a", "ok", List.of(),
                java.util.Map.of(Institution.VILLAGES, 10));
        var anti = new DialogueOption("b", "d.b", "ok", List.of(),
                java.util.Map.of(Institution.VILLAGES, -10));
        var node = new DialogueNode("q", "x", "d.q", ResolutionRule.VOTE, List.of(pro, anti));
        var graph = new DialogueGraph("q", List.of(node,
                new DialogueNode("ok", "x", "d.ok", ResolutionRule.INITIATOR, List.of())));

        var c = new Conversation(graph, "kaya", List.of("kaya", "p2", "p3"));
        c.submit("kaya", "a"); c.submit("p2", "a"); c.submit("p3", "b");
        var r = c.resolve(new Random(1));
        check(r.chosen().id().equals("a"), "the majority carried");

        var states = new java.util.HashMap<String, RegardState>();
        for (String who : List.of("kaya", "p2", "p3")) states.put(who, new RegardState(false));
        var applied = RegardEffects.apply(r, states::get);

        // p3 lost the vote and is still on record for what they said. This is the
        // assertion the whole ensemble design rests on: if losing a vote moved p3 by
        // the WINNER's effect, every player would end up with the initiator's record
        // and dissent would be decoration.
        check(states.get("kaya").value(Institution.VILLAGES) == 10, "the winner's own stance moved them");
        check(states.get("p3").value(Institution.VILLAGES) == -10,
              "a dissenter is judged on their own stance, not on the table's decision");
        check(applied.get("p3").get(Institution.VILLAGES) == -10, "the applied delta is reported");

        // A REPROMPT settled nothing, so nobody has said anything to be held to.
        var u = new Conversation(new DialogueGraph("q", List.of(
                new DialogueNode("q", "x", "d.q", ResolutionRule.UNANIMOUS, List.of(pro, anti)),
                new DialogueNode("ok", "x", "d.ok", ResolutionRule.INITIATOR, List.of()))),
                "kaya", List.of("kaya", "p2"));
        u.submit("kaya", "a"); u.submit("p2", "b");
        var rr = u.resolve(new Random(1));
        var fresh = new java.util.HashMap<String, RegardState>();
        fresh.put("kaya", new RegardState(false));
        fresh.put("p2", new RegardState(false));
        RegardEffects.apply(rr, fresh::get);
        check(fresh.get("kaya").value(Institution.VILLAGES) == 0,
              "a reprompt records nothing -- the argument is not over");

        // Reported deltas are what LANDED, not what was asked for. A ceiling makes
        // the two differ, and a UI that showed the request would promise a change
        // that never happened.
        var capped = new RegardState(false);
        capped.lowerCeiling(Institution.VILLAGES, 3);
        var one = new Conversation(graph, "solo", List.of("solo"));
        one.submit("solo", "a");
        var r2 = one.resolve(new Random(1));
        var out = RegardEffects.apply(r2, k -> capped);
        check(capped.value(Institution.VILLAGES) == 3, "the ceiling clamped the change");
        check(out.get("solo").get(Institution.VILLAGES) == 3,
              "the reported delta is what landed, not what the data asked for");
    }

    /**
     * Minecraft's yaw convention, pinned. Asserted against the four cardinal
     * directions rather than against a formula, because "the formula matches the
     * formula" is what the broken version would also have passed.
     */
    static void facingChecks() {
        check(near(com.cadykaya.interregnum.core.spatial.Facing.yawToward(0, 1), 0),
              "looking along +z is yaw 0 (south)");
        check(near(com.cadykaya.interregnum.core.spatial.Facing.yawToward(-1, 0), 90),
              "looking along -x is yaw 90 (west)");
        check(near(com.cadykaya.interregnum.core.spatial.Facing.yawToward(1, 0), -90),
              "looking along +x is yaw -90 (east)");
        check(Math.abs(Math.abs(com.cadykaya.interregnum.core.spatial.Facing.yawToward(0, -1)) - 180) < 0.001,
              "looking along -z is yaw 180 (north)");

        // The shape the shrine actually uses: standing at an offset from something
        // and turning to look back at it. This is the case that was wrong.
        int[] offset = {1, 0};                       // keeper is EAST of the box
        float yaw = com.cadykaya.interregnum.core.spatial.Facing
                .yawToward(-offset[0], -offset[1]);
        check(near(yaw, 90), "standing east of a thing, you face west to look at it");
    }

    static boolean near(float a, double b) {
        return Math.abs(a - b) < 0.001;
    }

    static void chapterChecks() {
        var st = new ChapterState();
        check(st.chapter() == Chapter.DORMANT, "starts dormant");
        check(st.mechanicsDormant(), "chapter 0 declares mechanics dormant");
        check(st.band() == 0, "dormant band is 0");

        check(st.record(Milestone.DEICIDE), "first record of a milestone is new");
        check(!st.record(Milestone.DEICIDE), "re-recording is not new");
        check(st.chapter() == Chapter.VIGIL, "deicide advances to VIGIL");
        check(!st.mechanicsDormant(), "mechanics are no longer dormant after the death");

        // out-of-order milestones must not skip a chapter whose prerequisites are absent
        st.record(Milestone.FIRST_CROSSING);
        check(st.chapter() == Chapter.VIGIL,
              "crossing without warden contact does not reach EXODUS");
        st.record(Milestone.WARDEN_CONTACT);
        check(st.chapter() == Chapter.EXODUS,
              "the missing prerequisite arriving late unlocks the chapter it gated");

        // letters are repeatable and counted separately from the milestone set
        st.record(Milestone.LETTER_DELIVERED);
        st.record(Milestone.LETTER_DELIVERED);
        check(st.lettersDelivered() == 2, "letters count repeatably");
        check(st.chapter() == Chapter.ATTRITION, "first letter reaches ATTRITION");

        // monotonicity: a save that claims a lower chapter cannot lower the real one
        String saved = st.serialize();
        var back = ChapterState.deserialize(saved);
        check(back.chapter() == st.chapter(), "chapter survives a save round-trip");
        check(back.lettersDelivered() == 2, "letter count survives a save round-trip");
        check(back.milestones().equals(st.milestones()), "milestones survive round-trip");
        var tampered = ChapterState.deserialize(saved.replace("|ATTRITION", "|DORMANT"));
        check(tampered.chapter() == Chapter.ATTRITION,
              "a save claiming a lower chapter is corrected upward by its milestones");
        // The other direction, and the one the monotonic guard actually exists for:
        // a save whose milestones no longer justify its chapter must NOT regress.
        // The overworld does not heal because someone restored a backup.
        var stripped = ChapterState.deserialize("|0|ATTRITION");
        check(stripped.chapter() == Chapter.ATTRITION,
              "a high-water chapter survives its milestones going missing");
        check(stripped.band() == 4, "and keeps its unraveling band");
    }

    static void regardChecks() {
        var bystander = new RegardState(false);
        check(bystander.adjust(Institution.THE_GHOST, 50) == 0,
              "a non-killer has no ghost relationship to move");
        check(bystander.value(Institution.THE_GHOST) == 0, "and it stays at zero");
        var killer = new RegardState(true);
        check(killer.adjust(Institution.THE_GHOST, 20) == 20, "the killer's ghost regard moves");

        var p = new RegardState(false);
        check(p.standing(Institution.VILLAGES) == Standing.WARY, "neutral reads as WARY");
        p.adjust(Institution.VILLAGES, 50);
        check(p.standing(Institution.VILLAGES) == Standing.TRUSTED, "50 reads as TRUSTED");
        check(p.adjust(Institution.VILLAGES, 500) == 50, "clamped at MAX, reports real delta");
        check(p.value(Institution.VILLAGES) == RegardState.MAX, "and lands exactly on MAX");

        // the scar: deicide caps the survivors and bottoms out the victim, permanently
        var q = new RegardState(true);
        q.adjust(Institution.VERDANT, 80);
        q.adjust(Institution.ANCHORITE, 80);
        q.recordDeicide(Institution.VERDANT);
        check(q.value(Institution.VERDANT) == RegardState.MIN, "the victim bottoms out");
        check(q.adjust(Institution.VERDANT, 100) == 0, "and can never be raised again");
        check(q.value(Institution.ANCHORITE) == -10,
              "survivors are hit and then capped at the ceiling");
        q.adjust(Institution.ANCHORITE, 100);
        check(q.value(Institution.ANCHORITE) == -10, "no later kindness passes the ceiling");
        check(q.standing(Institution.ANCHORITE) == Standing.WARY, "which reads as WARY forever");
        check(q.value(Institution.WARDENATE) == -30, "the Wardenate takes a flat hit");

        // The people are NOT the pantheon. WORLD.md's four voices has the villagers
        // whispering *saint* for the killer, so a deicide that permanently floored
        // VILLAGES would put the mechanics in contradiction with locked lore and
        // flatten every village scene into a formality nobody can move. The gods
        // write you off; the people are genuinely undecided, and that is playable.
        var r0 = new RegardState(true);
        r0.adjust(Institution.VILLAGES, 40);
        r0.recordDeicide(Institution.VERDANT);
        check(r0.value(Institution.VILLAGES) == 40, "a deicide does not move the villages");
        check(r0.adjust(Institution.VILLAGES, 50) == 50,
              "and leaves no ceiling on them -- the people can still be won over");
        check(r0.standing(Institution.VILLAGES) == Standing.BELOVED,
              "all the way to BELOVED, which the gods can never be again");
        q.recordDeicide(Institution.ANCHORITE);
        check(q.value(Institution.WARDENATE) == -60,
              "a second deicide is more evidence, not a different crime");
    }

    /**
     * The bill of lading, and what a destination makes of it.
     *
     * The checklist is the tutorial: a law only ever explains itself at the moment a
     * player has broken it, so these assertions are as much about the TEXT reaching
     * the player as about the arithmetic.
     */
    static void ferryChecks() {
        var quiet = new Law("quiet_one", Map.of(
                "no_sound", new Law.Rule(java.util.Set.of(
                        "minecraft:note_block", "minecraft:jukebox", "minecraft:bell"),
                        "interregnum.ferry.quiet_one.no_sound")));

        var plain = Manifest.of(List.of("minecraft:oak_planks", "minecraft:oak_planks",
                                        "minecraft:chest"));
        check(plain.total() == 3, "a manifest counts what it was given");
        check(plain.blocks().get("minecraft:oak_planks") == 2, "and groups duplicates");
        check(plain.admissible(quiet), "a silent cargo crosses");
        check(plain.validate(quiet).isEmpty(), "with nothing on the checklist");

        var noisy = Manifest.of(List.of("minecraft:oak_planks", "minecraft:note_block",
                                        "minecraft:note_block", "minecraft:jukebox"));
        var bad = noisy.validate(quiet);
        check(!noisy.admissible(quiet), "a cargo that can make a sound does not cross");
        check(bad.size() == 2, "every violation is reported, not just the first");

        // The COUNT is the closest the checklist gets to answering "where is it".
        var notes = bad.stream().filter(v -> v.blockId().equals("minecraft:note_block"))
                .findFirst().orElseThrow();
        check(notes.count() == 2, "and says how many, because two is a different search than twelve");
        check(notes.reasonKey().equals("interregnum.ferry.quiet_one.no_sound"),
              "and carries the reason, which is the whole point of a checklist");
        // The WHOLE list, not just the pair that happens to be violations. Comparing two
        // entries is a coin flip against any shuffle, and a coin-flip assertion is how
        // the mutation for this guard used to escape on CI (docs/LESSONS.md #19).
        var keys = new java.util.ArrayList<>(noisy.blocks().keySet());
        var sorted = new java.util.ArrayList<>(keys);
        java.util.Collections.sort(sorted);
        check(keys.equals(sorted),
              "in a stable order: a bill of lading that reshuffles is one nobody can trust");
        check(bad.get(0).blockId().compareTo(bad.get(1).blockId()) < 0,
              "and the checklist a player actually reads is in that order too");

        // A law that refuses nothing is not a law, and a rule that names nothing can
        // never appear on a checklist -- so it can never be seen to be wrong.
        boolean threw = false;
        try { new Law("empty", Map.of()); } catch (IllegalArgumentException e) { threw = true; }
        check(threw, "a law with no rules is refused");
        threw = false;
        try {
            new Law.Rule(java.util.Set.of(), "interregnum.ferry.nothing");
        } catch (IllegalArgumentException e) { threw = true; }
        check(threw, "a rule that names no blocks is refused");

        // One mistake, one line: a block refused twice would read as two problems.
        threw = false;
        try {
            new Law("double", Map.of(
                    "a", new Law.Rule(java.util.Set.of("minecraft:bell"), "k.a"),
                    "b", new Law.Rule(java.util.Set.of("minecraft:bell"), "k.b")));
        } catch (IllegalArgumentException e) { threw = true; }
        check(threw, "a block refused by two rules is refused at construction");

        // An empty ferry is legal, and says so rather than throwing. Somebody will
        // absolutely put a bare keel on a pad and press the thing.
        check(Manifest.of(List.of()).admissible(quiet), "an empty cargo crosses");
        check(Manifest.of(List.of()).total() == 0, "and weighs nothing");
    }

    /**
     * The same beat, worded differently for somebody with a file.
     *
     * A node, not a scene: three copies of a conversation differing by one line would
     * drift, so the variants hang off the node and everything underneath is shared.
     */
    static void textVariantChecks() {
        var filed = new TextVariant("d.filed",
                new StandingGate(Map.of(), Map.of(Institution.WARDENATE, Standing.RESENTED)));
        var known = new TextVariant("d.known",
                new StandingGate(Map.of(Institution.WARDENATE, Standing.TRUSTED), Map.of()));
        var node = new DialogueNode("open", "warden", "d.base", ResolutionRule.INITIATOR,
                List.of(new DialogueOption("a", "d.a", DialogueGraph.END, List.of())),
                List.of(filed, known));

        var fresh = new RegardState(false);
        check(node.textFor(fresh).equals("d.base"),
              "somebody nobody has an opinion about reads the ordinary line");

        var resented = new RegardState(false);
        resented.adjust(Institution.WARDENATE, -40);
        check(node.textFor(resented).equals("d.filed"), "a file being open changes the line");

        var trusted = new RegardState(false);
        trusted.adjust(Institution.WARDENATE, 50);
        check(node.textFor(trusted).equals("d.known"), "so does a clean record");

        // The node is otherwise untouched: this is a rewording, not a fork. If any of
        // these moved, three standings would mean three conversations to maintain.
        check(node.options().size() == 1 && node.rule() == ResolutionRule.INITIATOR,
              "a variant changes the line and nothing else about the node");

        // A node with no variants is exactly what it always was, including for a
        // caller with no regard to hand.
        var plain = new DialogueNode("p", "warden", "d.p", ResolutionRule.INITIATOR, List.of());
        check(plain.textFor(trusted).equals("d.p") && plain.textFor(null).equals("d.p"),
              "a node with no variants always reads its own line");

        // FIRST match wins, in the author's order. Both of these admit a BELOVED
        // player; the earlier one is the answer, and an implementation that scored
        // "most specific" would have to invent a comparison that does not exist.
        var first = new TextVariant("d.first",
                new StandingGate(Map.of(Institution.WARDENATE, Standing.KNOWN), Map.of()));
        var second = new TextVariant("d.second",
                new StandingGate(Map.of(Institution.WARDENATE, Standing.TRUSTED), Map.of()));
        var ordered = new DialogueNode("o", "warden", "d.base", ResolutionRule.INITIATOR,
                List.of(), List.of(first, second));
        check(ordered.textFor(trusted).equals("d.first"),
              "the first matching variant wins, not the narrowest");

        // A variant with no condition would match everybody, shadowing the node's own
        // line and every variant after it. An author who wants that should edit the
        // node, so the engine refuses to load it.
        boolean threw = false;
        try {
            new TextVariant("d.always", StandingGate.OPEN);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        check(threw, "a variant with no standing condition is refused");
    }

    /**
     * Options that appear and disappear according to what somebody thinks of you.
     *
     * The first thing in the mod that READS regard rather than writing it. Until this
     * existed, standing was recorded, persisted, announced -- and consulted by
     * nothing, which is a feature finished everywhere except where it matters.
     */
    static void standingGateChecks() {
        var open = new DialogueOption("a", "d.a", "ok", List.of());
        check(open.standing().isOpen(), "an option with no gate is open");
        check(open.visibleTo(java.util.Set.of(), null),
              "an ungated option is visible to somebody with no record at all");

        // A FLOOR. Earned content.
        var trusted = new DialogueOption("t", "d.t", "ok", List.of(), Map.of(),
                new StandingGate(Map.of(Institution.WARDENATE, Standing.TRUSTED), Map.of()));
        var p = new RegardState(false);
        check(!trusted.visibleTo(java.util.Set.of(), p),
              "a floor hides the option from somebody who has not earned it");
        p.adjust(Institution.WARDENATE, 45);
        check(trusted.visibleTo(java.util.Set.of(), p),
              "and shows it once they have");
        p.adjust(Institution.WARDENATE, 40);          // TRUSTED -> BELOVED
        check(trusted.visibleTo(java.util.Set.of(), p),
              "a floor is at-least, not exactly -- rising past it does not lose the option");

        // A CEILING. Content you lose by being liked, which is what makes standing
        // read as a relationship that moved rather than a score that went up.
        var suspicious = new DialogueOption("s", "d.s", "ok", List.of(), Map.of(),
                new StandingGate(Map.of(), Map.of(Institution.VILLAGES, Standing.WARY)));
        var q = new RegardState(false);
        check(suspicious.visibleTo(java.util.Set.of(), q),
              "a ceiling admits somebody at the band");
        q.adjust(Institution.VILLAGES, 30);           // WARY -> KNOWN
        check(!suspicious.visibleTo(java.util.Set.of(), q),
              "and closes once they are thought better of");

        // Both ends at once: a window rather than a threshold.
        var window = new DialogueOption("w", "d.w", "ok", List.of(), Map.of(),
                new StandingGate(Map.of(Institution.VILLAGES, Standing.KNOWN),
                                 Map.of(Institution.VILLAGES, Standing.KNOWN)));
        var r = new RegardState(false);
        check(!window.visibleTo(java.util.Set.of(), r), "below the window: hidden");
        r.adjust(Institution.VILLAGES, 30);
        check(window.visibleTo(java.util.Set.of(), r), "inside the window: shown");
        r.adjust(Institution.VILLAGES, 45);
        check(!window.visibleTo(java.util.Set.of(), r), "above the window: hidden again");

        // THE_GHOST is an absence, not a nought. A non-killer's ghost regard reads as
        // WARY because its value is pinned at zero -- and a gate that accepted that
        // would show the dead god's private options to people who never met it.
        var ghostly = new DialogueOption("g", "d.g", "ok", List.of(), Map.of(),
                new StandingGate(Map.of(Institution.THE_GHOST, Standing.WARY), Map.of()));
        check(!ghostly.visibleTo(java.util.Set.of(), new RegardState(false)),
              "a ghost-gated option is hidden from somebody who never killed a god");
        check(ghostly.visibleTo(java.util.Set.of(), new RegardState(true)),
              "and shown to the one who did");
        // Including a CEILING gate, which would otherwise be satisfied by an absence
        // even more easily than a floor.
        var ghostCeiling = new DialogueOption("gc", "d.gc", "ok", List.of(), Map.of(),
                new StandingGate(Map.of(), Map.of(Institution.THE_GHOST, Standing.BELOVED)));
        check(!ghostCeiling.visibleTo(java.util.Set.of(), new RegardState(false)),
              "a ghost ceiling is not satisfied by having no ghost");

        // The two gates are ANDed. Passing one is not passing.
        var both = new DialogueOption("b", "d.b", "ok", List.of("class/theoclast"), Map.of(),
                new StandingGate(Map.of(Institution.WARDENATE, Standing.TRUSTED), Map.of()));
        var high = new RegardState(false);
        high.adjust(Institution.WARDENATE, 45);
        check(!both.visibleTo(java.util.Set.of(), high), "standing alone is not enough");
        check(!both.visibleTo(java.util.Set.of("class/theoclast"), new RegardState(false)),
              "the tag alone is not enough");
        check(both.visibleTo(java.util.Set.of("class/theoclast"), high), "both together are");

        // The short call must not quietly hide ungated options: a great deal of the
        // engine has no regard to hand and every ordinary option has to survive that.
        check(open.visibleTo(java.util.Set.of()),
              "the tags-only call still shows an option with no standing gate");
    }

    /**
     * The only regard event a player is ever told about.
     *
     * Everything here is about the difference between a change and a CROSSING. Regard
     * moves constantly and almost none of it is news; what a player hears is that
     * somebody's opinion of them moved into a different band, with no number attached.
     */
    static void crossingChecks() {
        var p = new RegardState(false);
        var before = Standings.snapshot(p);

        // Movement inside a band is not an event. This is the load-bearing case: get
        // it wrong and every conversation ends in a burst of notifications, which is
        // a karma bar with extra steps.
        p.adjust(Institution.VILLAGES, 5);
        check(Standings.since(before, p).isEmpty(),
              "moving without crossing a band is not news");

        p.adjust(Institution.VILLAGES, 20);          // 0 -> 25, WARY -> KNOWN
        var rose = Standings.since(before, p);
        check(rose.size() == 1, "crossing a band is exactly one event");
        check(rose.get(0).institution() == Institution.VILLAGES, "attributed to the right one");
        check(rose.get(0).from() == Standing.WARY && rose.get(0).to() == Standing.KNOWN,
              "and carries both sides of the line it crossed");
        check(rose.get(0).rose(), "the direction is part of the event");

        // Two steps in one go is ONE crossing, not two. A player who goes from WARY
        // to TRUSTED has not been told twice; they have been told once, correctly.
        var q = new RegardState(false);
        var q0 = Standings.snapshot(q);
        q.adjust(Institution.WARDENATE, 60);
        var leapt = Standings.since(q0, q);
        check(leapt.size() == 1, "skipping a band is still one crossing");
        check(leapt.get(0).from() == Standing.WARY && leapt.get(0).to() == Standing.TRUSTED,
              "reported from where they were to where they are");

        // Falling reads differently, and the type says so rather than the reader
        // having to work it out from two ordinals.
        var f = new RegardState(false);
        f.adjust(Institution.VILLAGES, 50);
        var f0 = Standings.snapshot(f);
        f.adjust(Institution.VILLAGES, -60);
        var fell = Standings.since(f0, f);
        check(fell.size() == 1 && !fell.get(0).rose(), "a fall is a crossing that did not rise");

        // Order is the declaration order of Institution, not a map's iteration order,
        // because these become lines a player reads in sequence (LESSONS #19).
        var m = new RegardState(false);
        var m0 = Standings.snapshot(m);
        m.adjust(Institution.VILLAGES, 30);
        m.adjust(Institution.WARDENATE, 30);
        var many = Standings.since(m0, m);
        check(many.size() == 2, "two institutions changing their minds is two events");
        check(many.get(0).institution() == Institution.WARDENATE
              && many.get(1).institution() == Institution.VILLAGES,
              "reported in Institution declaration order, not a map's");

        // A non-killer's ghost regard cannot move, so it can never be news.
        var b = new RegardState(false);
        var b0 = Standings.snapshot(b);
        b.adjust(Institution.THE_GHOST, 90);
        check(Standings.since(b0, b).isEmpty(), "the ghost says nothing to someone who did not kill it");

        // A ceiling that bites mid-fall still reports where the player actually
        // landed -- reporting the requested band would promise a change that the
        // ceiling refused.
        var c = new RegardState(true);
        c.adjust(Institution.VERDANT, 80);
        var c0 = Standings.snapshot(c);
        c.recordDeicide(Institution.ANCHORITE);
        var scars = Standings.since(c0, c);
        check(scars.stream().anyMatch(x -> x.institution() == Institution.VERDANT
                    && x.from() == Standing.BELOVED && !x.rose()),
              "a deicide is heard as a fall by every god who learns of it");

        // A crossing that did not cross is a message saying nothing happened.
        boolean threw = false;
        try {
            new BandChange(Institution.VILLAGES, Standing.KNOWN, Standing.KNOWN);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        check(threw, "a band change that changes no band is refused");

        // -- the dead god's mail ----------------------------------------------
        //
        // The rule these guard is about a SET, not a letter: three open with a name and
        // the fourth opens `To --`. No individual letter can be checked against it, and
        // if it breaks the mid-game's best reveal quietly stops working for readers who
        // will never know it was supposed to.
        var named = new com.cadykaya.interregnum.core.letters.Letter(
                "verdant", java.util.Optional.of("Rill"), "s", List.of("a"));
        var unnamed = new com.cadykaya.interregnum.core.letters.Letter(
                "quiet_one", java.util.Optional.empty(), "s", List.of("a"));
        check(named.named(), "a letter with an addressee is named");
        check(!unnamed.named(), "a letter without one is not");

        // Absence must be explicit absence. `To --` is a decision; `To ` is a typo, and
        // in a JSON file the two are one keystroke apart.
        threw = false;
        try {
            new com.cadykaya.interregnum.core.letters.Letter(
                    "x", java.util.Optional.of("  "), "s", List.of("a"));
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        check(threw, "a blank addressee is refused; an unaddressed letter is Optional.empty()");

        threw = false;
        try {
            new com.cadykaya.interregnum.core.letters.Letter(
                    "x", java.util.Optional.empty(), "s", List.of());
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        check(threw, "a letter with no body cannot open a questline");

        var post = new com.cadykaya.interregnum.core.letters.Post(java.util.Map.of(
                "verdant", named, "quiet_one", unnamed));
        check(post.size() == 2, "the post holds its letters");
        check(post.namesUsed().equals(List.of("Rill")),
              "only the named letters contribute a name, so the Quiet One spends nothing");

        // Two unaddressed, or none, and the reveal is gone.
        threw = false;
        try {
            new com.cadykaya.interregnum.core.letters.Post(java.util.Map.of(
                    "a", unnamed, "b", new com.cadykaya.interregnum.core.letters.Letter(
                            "b", java.util.Optional.empty(), "s", List.of("a"))));
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        check(threw, "two unaddressed letters are refused: exactly one opens `To --`");

        threw = false;
        try {
            new com.cadykaya.interregnum.core.letters.Post(java.util.Map.of("a", named));
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        check(threw, "a post where every letter is named is refused for the same reason");

        // -- band 3: which god leaks where -------------------------------------
        check(com.cadykaya.interregnum.core.exodus.Exodus.leaking(3),
              "the overworld leaks at band 3");
        check(!com.cadykaya.interregnum.core.exodus.Exodus.leaking(2),
              "and not before, so a band-2 world is still only losing its own structure");

        // A patch that changed god between visits would be weather, not a place to
        // learn a law in.
        var first = com.cadykaya.interregnum.core.exodus.Exodus.lawAt(37, -12);
        check(com.cadykaya.interregnum.core.exodus.Exodus.lawAt(37, -12) == first,
              "a shrine's leak never changes its mind");

        // THE assertion. A hash that always returned one god would make three of them
        // invisible and nothing else in the mod would fail -- the band would look
        // implemented and teach a quarter of the curriculum. Sampled, because the
        // property is about the distribution and not about any single coordinate.
        var seen = new java.util.HashSet<com.cadykaya.interregnum.core.exodus.Exodus.Law>();
        for (int x = -20; x <= 20; x++) {
            for (int z = -20; z <= 20; z++) {
                seen.add(com.cadykaya.interregnum.core.exodus.Exodus.lawAt(x, z));
            }
        }
        check(seen.size() == com.cadykaya.interregnum.core.exodus.Exodus.Law.values().length,
              "every god leaks somewhere: a hash favouring one would hide three of them "
              + "and nothing else would notice");

        // The ANTI-diagonal, and getting this wrong once is why the comment is here.
        //
        // The cheap hash somebody reaches for when simplifying this is `(x + z) % 4`,
        // and its defect is stripes: it is CONSTANT wherever x + z is constant. A first
        // version of this check walked x == z instead, where `(x + z) % 4` evaluates
        // 2x mod 4 and alternates -- so it varied, the assertion passed, and the
        // mutation went straight through. Walking the line x + z == k is the only one
        // that sees the stripe.
        var alongStripe = new java.util.HashSet<com.cadykaya.interregnum.core.exodus.Exodus.Law>();
        for (int x = -20; x <= 20; x++) {
            alongStripe.add(com.cadykaya.interregnum.core.exodus.Exodus.lawAt(x, 7 - x));
        }
        check(alongStripe.size() > 1,
              "the law varies along x + z == k, so the hash is not (x + z) % 4 wearing a "
              + "disguise -- that one paints diagonal stripes, and a player crossing them "
              + "meets one god's law for as long as they walk");

        attrition();
        marking();
        steles();
        clasts();
        grimoires();
        zones();
        spans();
    }

    /**
     * A span is continuous, capped, and does not start under the caster's feet. All three
     * fail quietly: a gap is only found by falling through it, an uncapped span makes the
     * overworld's casting cost a rounding error, and one that includes the origin
     * suffocates whoever cast it.
     */
    static void spans() {
        var span = com.cadykaya.interregnum.core.magic.Bridgeroot.span(20, 0, 0);
        int cap = com.cadykaya.interregnum.core.magic.Bridgeroot.MAX_SPAN;
        check(span.size() == cap,
              "a span toward something far away stops at the cap. Crossing anything large "
              + "should be several casts and therefore several costs; an unlimited span "
              + "makes the overworld's fraying a rounding error and the ban unenforceable");
        check(span.get(0)[0] == 1 && span.get(0)[1] == 0 && span.get(0)[2] == 0,
              "the first block is one step out, not the origin -- a spell that grew into "
              + "the space the caster occupies would suffocate them the first time they "
              + "used it correctly");

        // Continuity, asserted as adjacency rather than as a count. A count of twelve is
        // equally satisfied by twelve blocks with a hole in the middle, and the hole is
        // the whole failure: you find out about it while standing over it.
        var diagonal = com.cadykaya.interregnum.core.magic.Bridgeroot.span(9, 3, -6);
        int[] prev = {0, 0, 0};
        boolean contiguous = true;
        for (int[] p : diagonal) {
            int step = Math.max(Math.abs(p[0] - prev[0]),
                    Math.max(Math.abs(p[1] - prev[1]), Math.abs(p[2] - prev[2])));
            if (step != 1) {
                contiguous = false;
            }
            prev = p;
        }
        check(contiguous,
              "a span toward a diagonal target has no gaps: every block touches the last. "
              + "A bridge you can fall through is worse than no bridge");
        check(diagonal.get(diagonal.size() - 1)[0] == 9
              && diagonal.get(diagonal.size() - 1)[2] == -6,
              "and it arrives where it was aimed when the target is inside the cap");

        check(com.cadykaya.interregnum.core.magic.Bridgeroot.span(0, 0, 0).isEmpty(),
              "a span of no length grows nothing rather than one block underfoot");
        check(com.cadykaya.interregnum.core.magic.Bridgeroot.SCHOOL
                      == com.cadykaya.interregnum.core.magic.School.VERDANCY,
              "Bridgeroot belongs to the Verdant's school");
    }

    /**
     * A zone has an edge and a lifetime. Both are the difference between a spell and a
     * terrain feature, and both fail silently: a zone with no edge looks like the spell
     * working from inside it, and one that never lapses looks like it working for longer
     * than you were watching.
     */
    static void zones() {
        var z = com.cadykaya.interregnum.core.magic.Lighten.zoneAt(0, 64, 0, 1000);

        check(z.covers(0, 64, 0), "the point it was cast at is inside");
        int r = com.cadykaya.interregnum.core.magic.Lighten.RADIUS;
        check(z.covers(r, 64 + r, -r),
              "the far corner is inside -- Chebyshev, so the region is a cube and its "
              + "edge is something a player can find by walking");
        check(!z.covers(r + 1, 64, 0),
              "one block past the edge is OUTSIDE. A zone with no edge is not a spell, "
              + "it is weather -- and from inside it the two look identical, which is "
              + "why nothing else would catch this");

        check(!z.expired(1000), "it is in force the moment it is cast");
        long ends = 1000 + com.cadykaya.interregnum.core.magic.Lighten.DURATION_TICKS;
        check(!z.expired(ends),
              "it is still in force on the exact tick it expires -- 'expires at' reading "
              + "as 'expired at' loses the last tick to an off-by-one that only ever "
              + "shows up as the spell feeling a moment short");
        check(z.expired(ends + 1), "and lapsed the tick after");

        check(com.cadykaya.interregnum.core.magic.Lighten.SCHOOL
                      == com.cadykaya.interregnum.core.magic.School.WEIGHT,
              "Lighten belongs to the Anchorite's school, so learning the Turning does "
              + "not hand you the Anchorite's verbs");

        // FOUR schools, four distinct owners. The failure this catches is two spells
        // sharing one, which would make a single lesson open verbs from two gods and
        // quietly collapse the reason there are four journeys.
        var owners = new java.util.HashSet<com.cadykaya.interregnum.core.magic.School>();
        owners.add(com.cadykaya.interregnum.core.magic.Lighten.SCHOOL);
        owners.add(com.cadykaya.interregnum.core.magic.Hush.SCHOOL);
        owners.add(com.cadykaya.interregnum.core.magic.Bridgeroot.SCHOOL);
        owners.add(com.cadykaya.interregnum.core.magic.School.TURNING);   // Weather's
        check(owners.size() == com.cadykaya.interregnum.core.magic.School.values().length,
              "the four spells belong to four different schools. Two sharing one would "
              + "make a single lesson hand out another god's verbs, and there would be "
              + "no reason left for four journeys");

        // A Hush reaches further than a Lighten, and lasts less long. Lighten is a tool
        // you plan around; Hush is what you do when something has gone wrong.
        var h = com.cadykaya.interregnum.core.magic.Hush.zoneAt(0, 64, 0, 0);
        check(h.radius() > z.radius(),
              "a silence is wider than a low-gravity field -- a room you cannot fit a "
              + "fight inside is a room this spell does not help with");
        check(com.cadykaya.interregnum.core.magic.Hush.DURATION_TICKS
                      < com.cadykaya.interregnum.core.magic.Lighten.DURATION_TICKS,
              "and it holds for less time, or every encounter becomes a place you stand "
              + "until it is over");

        // Weather and Rewind are one table in two directions, so they must belong to the
        // same god. Split across two schools, half the Turning's kit would be taught by
        // somebody who does not own the law it reads.
        // A forge is smaller than a room and lasts longer than one. Both directions
        // matter and both are about what the spell ASKS OF YOU: a Lighten is used where
        // you are standing, a Drop-forge has to be set up and then supplied, so it is
        // narrow enough to be a workbench rather than a quarry and long enough to climb.
        check(com.cadykaya.interregnum.core.magic.DropForge.RADIUS
                      < com.cadykaya.interregnum.core.magic.Lighten.RADIUS,
              "a drop-forge is a workbench, not a region. Cast over a hillside it would "
              + "stop being an act and become a machine that processes terrain while "
              + "nobody watches");
        check(com.cadykaya.interregnum.core.magic.DropForge.DURATION_TICKS
                      > com.cadykaya.interregnum.core.magic.Lighten.DURATION_TICKS,
              "and it lasts longer, because it is the only spell in the kit that cannot "
              + "do anything until the caster has gone and fetched something. A spell "
              + "that expired during its own setup would read as broken, not as brief");
        check(com.cadykaya.interregnum.core.magic.DropForge.SCHOOL
                      == com.cadykaya.interregnum.core.magic.School.WEIGHT,
              "Drop-forge is the Anchorite's, so it is learned in the Anchorite's world "
              + "and nowhere else");

        // A surge nobody can aim is kept small, and a Lighten is the comparison because
        // a Lighten is the one that is SAFE to stand in. The wider a spell that applies
        // to everything reaches, the less of its own blast a caster can hold in their
        // head -- and the locked word for this school is `hazard`, which only means
        // anything while the hazard is still a decision.
        check(com.cadykaya.interregnum.core.magic.Wildgrowth.RADIUS
                      < com.cadykaya.interregnum.core.magic.Lighten.RADIUS,
              "Wildgrowth reaches no further than a low-gravity field. It cannot be "
              + "aimed and everything inside it is subject, so a radius a caster cannot "
              + "picture turns the hazard from a decision into an accident");
        // Calibrated against the plainest counter in the game: sugar cane advances one
        // segment on exactly sixteen random ticks. A cast worth less than one segment of
        // cane is not worth a journey to another world to learn.
        check(com.cadykaya.interregnum.core.magic.Wildgrowth.PUSHES > 16,
              "one cast of Wildgrowth is worth more than one segment of sugar cane. "
              + "Below that the school costs a crossing and buys less than standing "
              + "still does");
        check(com.cadykaya.interregnum.core.magic.Wildgrowth.SCHOOL
                      == com.cadykaya.interregnum.core.magic.School.VERDANCY,
              "Wildgrowth is the Verdant's, so it is learned in the Verdant's world and "
              + "nowhere else");

        check(com.cadykaya.interregnum.core.magic.Rewind.SCHOOL
                      == com.cadykaya.interregnum.core.magic.School.TURNING,
              "Rewind belongs to the Turning, the same school as Weather -- they are one "
              + "table read forwards and backwards, and a school that taught only one "
              + "direction would be teaching half a mechanism");

        // EVERY SPELL IS ITS OWN KEY. Spell zones were keyed by school until a school
        // turned out to have two zone spells, and the failure had no symptom: keyed by
        // school, Hush and Still become each other and both appear to work.
        //
        // The guard is that no two spells share an identity, which is trivially true of
        // an enum -- so what is actually asserted is that more than one spell maps to
        // some school, i.e. that the collision this key exists to prevent is REAL and not
        // hypothetical. A key nobody could collide on needs no protecting.
        var perSchool = new java.util.EnumMap<com.cadykaya.interregnum.core.magic.School,
                Integer>(com.cadykaya.interregnum.core.magic.School.class);
        for (var sp : com.cadykaya.interregnum.core.magic.Spell.values()) {
            perSchool.merge(sp.school(), 1, Integer::sum);
        }
        // Named explicitly rather than counted. A first version asserted "some school
        // teaches more than one spell", which sounds like the same thing and is not: it
        // survived moving Still to another school entirely, because Weather and Rewind
        // still shared the Turning. The assertion has to be about the PAIR that motivated
        // the key, not about the existence of any pair.
        check(com.cadykaya.interregnum.core.magic.Spell.HUSH.school()
                      == com.cadykaya.interregnum.core.magic.School.SILENCE
              && com.cadykaya.interregnum.core.magic.Spell.STILL.school()
                      == com.cadykaya.interregnum.core.magic.School.SILENCE,
              "Hush and Still are both the Quiet One's. They are the pair that forced "
              + "spell zones to stop being keyed by school -- keyed that way they become "
              + "each other, a silence stops falling blocks and a stillness mutes "
              + "creepers, and nothing fails anywhere");
        check(perSchool.values().stream().anyMatch(n -> n > 1),
              "and at least one school does teach more than one spell, so the key is "
              + "guarding a collision that is real rather than hypothetical");
        // The second pair, and a worse one. Lighten and Drop-forge are both the
        // Anchorite's and both open zones, and they are OPPOSITES: inside a Lighten
        // nothing falls, and a Drop-forge is ground where things land. Keyed by school
        // they would have been the same zone, so casting either would have lifted the
        // weight the other was waiting for -- a spell that silently cannot work.
        check(com.cadykaya.interregnum.core.magic.Spell.LIGHTEN.school()
                      == com.cadykaya.interregnum.core.magic.School.WEIGHT
              && com.cadykaya.interregnum.core.magic.Spell.DROP_FORGE.school()
                      == com.cadykaya.interregnum.core.magic.School.WEIGHT,
              "Lighten and Drop-forge are both the Anchorite's, and both are zones. "
              + "Keyed by school they would be one zone that both lifts falling blocks "
              + "and waits for them to land, which is a spell that cannot work and "
              + "never says so");
        check(perSchool.size() == com.cadykaya.interregnum.core.magic.School.values().length,
              "every school teaches at least one spell, so no god's world is a journey "
              + "toward nothing");

        // QUELL, and the two things about it that nothing else in the kit would catch.
        //
        // The school first. It is the Quiet One's third, which means every existing
        // Silence assertion above -- and both of the pair guards -- still pass with this
        // one taught by the wrong god: Hush and Still are untouched, some school still
        // teaches more than one spell, and every school still teaches at least one. A
        // third spell in a school is exactly where "the pair is asserted, so the school
        // is covered" stops being true.
        check(com.cadykaya.interregnum.core.magic.Quell.SCHOOL
                      == com.cadykaya.interregnum.core.magic.School.SILENCE
              && com.cadykaya.interregnum.core.magic.Spell.QUELL.school()
                      == com.cadykaya.interregnum.core.magic.School.SILENCE,
              "Quell is the Quiet One's, in both places that say so. A spell whose class "
              + "and whose enum entry disagree about its god would be learned in one "
              + "world and keyed to another");

        // And that a quelling actually lasts. `holds` is the whole spell after the cast
        // -- the projectile rule reads nothing else -- and a duration of zero leaves a
        // spell that works at the instant it is cast and nowhere after it, which every
        // check that reads the command's reply would still call a success.
        long qCast = 1000L;
        long qEnds = com.cadykaya.interregnum.core.magic.Quell.expiryAt(qCast);
        check(com.cadykaya.interregnum.core.magic.Quell.holds(qEnds, qCast),
              "a quelling holds at the moment it is cast");
        check(com.cadykaya.interregnum.core.magic.Quell.holds(qEnds, qEnds - 1),
              "and on the last tick before it lapses");
        check(!com.cadykaya.interregnum.core.magic.Quell.holds(qEnds, qEnds),
              "and not on the tick it expires -- an off-by-one here is a spell that "
              + "outlives its own duration by a tick forever");
        check(qEnds - qCast == com.cadykaya.interregnum.core.magic.Quell.DURATION_TICKS
              && com.cadykaya.interregnum.core.magic.Quell.DURATION_TICKS > 0,
              "a cast lasts its stated duration, and the duration is not nothing. Zero "
              + "would leave a spell that succeeds at the instant of the cast and does "
              + "nothing after it, which the command's reply cannot tell apart from the "
              + "spell working");

        // It is the longest of the three Silence spells, and it should be: it is aimed,
        // it costs a cast per creature, and it does nothing at all to the seven other
        // things in the room. Shorter than the silence, it would be strictly worse than
        // the silence for the same price.
        check(com.cadykaya.interregnum.core.magic.Quell.DURATION_TICKS
                      > com.cadykaya.interregnum.core.magic.Hush.DURATION_TICKS,
              "a quelling outlasts a silence. A silence covers a room for a cast; this "
              + "covers one creature, so at the same duration it would be the same price "
              + "for strictly less");
        check(com.cadykaya.interregnum.core.magic.Quell.REACH
                      < com.cadykaya.interregnum.core.magic.Hush.RADIUS,
              "and it reaches less far, because it has to be aimed. At a range where a "
              + "caster could not say which creature they meant, 'that one' stops being "
              + "what the spell does");

        // LOFT. The Anchorite's third, so the same argument as Quell applies to its
        // school: the Lighten/Drop-forge pair guard above passes untouched with this one
        // taught by the wrong god, because that guard is about those two spells.
        check(com.cadykaya.interregnum.core.magic.Loft.SCHOOL
                      == com.cadykaya.interregnum.core.magic.School.WEIGHT
              && com.cadykaya.interregnum.core.magic.Spell.LOFT.school()
                      == com.cadykaya.interregnum.core.magic.School.WEIGHT,
              "Loft is the Anchorite's, in both places that say so. Weight is the school "
              + "about what things weigh, and carrying a building is that sentence");

        // "Small" is the locked word and the cap is the only thing that makes it mean
        // anything, so the boundary is asserted on both sides. An off-by-one here is a
        // spell that carries one more block than it says it does, forever, and nothing
        // else in the mod would ever notice.
        int cap = com.cadykaya.interregnum.core.magic.Loft.MAX_BLOCKS;
        check(!com.cadykaya.interregnum.core.magic.Loft.tooLarge(cap),
              "a structure of exactly the cap is carried. A cap that refuses its own "
              + "stated size is a cap that is really one less, and the number in the "
              + "documentation would be wrong rather than the code");
        check(com.cadykaya.interregnum.core.magic.Loft.tooLarge(cap + 1),
              "and one block more is refused");
        check(!com.cadykaya.interregnum.core.magic.Loft.tooLarge(1) && cap > 1,
              "a single block is carriable, and the cap is not one");

        // Far below a ferry's hull, and the gap is the point rather than a side effect.
        // At a hull's size this spell would be a ferry that needs no keel, no dock and no
        // checklist -- and the crossing laws would become a thing a player could decline
        // to use. The ferry's cap lives in the game module, so what is asserted here is
        // the property that matters: this is a SMALL number, of the size somebody can
        // picture, not a structure limit that happens to be generous.
        check(cap <= 128,
              "a loft is small enough to picture. The moment it can hold a building the "
              + "size of a ferry's hull, it IS a ferry -- one with no keel, no dock and "
              + "no checklist to teach anybody the world they are arriving in");
        check(com.cadykaya.interregnum.core.magic.Loft.REACH
                      < com.cadykaya.interregnum.core.magic.Lighten.RADIUS,
              "and a caster stands at the thing they are picking up. This is the one "
              + "spell that takes a specific building rather than a volume, so a reach "
              + "wider than a room would make 'that one' unanswerable");

        // THE SPOKEN WORD. `WORLD.md` locks casting as something you say out loud in chat,
        // which puts the whole affordance one method away from every sentence any player
        // has ever typed. These are the guards on that.
        var inc = com.cadykaya.interregnum.core.magic.Incantation.class;
        check(com.cadykaya.interregnum.core.magic.Incantation
                      .wordFor(com.cadykaya.interregnum.core.magic.Spell.DROP_FORGE)
                      .equals("drop-forge")
              && inc != null,
              "a spell's word is its own name with underscores as hyphens, so the word a "
              + "player types is the word the design document uses");

        // Every word round-trips, and no two spells answer to the same one. Derived from
        // the enum rather than listed, so a new spell is speakable the moment it exists --
        // and this is what would catch two of them colliding.
        var words = new java.util.HashSet<String>();
        boolean roundTrips = true;
        for (var sp : com.cadykaya.interregnum.core.magic.Spell.values()) {
            String w = com.cadykaya.interregnum.core.magic.Incantation.wordFor(sp);
            words.add(w);
            if (com.cadykaya.interregnum.core.magic.Incantation.of(w) != sp) {
                roundTrips = false;
            }
        }
        check(roundTrips && words.size()
                      == com.cadykaya.interregnum.core.magic.Spell.values().length,
              "every spell has a word, every word names it back, and no two spells answer "
              + "to the same one -- a collision would make one of them uncastable with "
              + "nothing anywhere reporting which");

        // THE ONE THAT PROTECTS CHAT. A substring match would make every conversation
        // about magic a hazard, and the first thing anybody would learn is to stop
        // discussing it. Both halves are here: a sentence containing the word, and a
        // longer word that merely begins with it.
        check(com.cadykaya.interregnum.core.magic.Incantation
                      .of("I said weather and nothing happened") == null
              && com.cadykaya.interregnum.core.magic.Incantation.of("weatherproof") == null
              && com.cadykaya.interregnum.core.magic.Incantation.of("hushed") == null,
              "talking about a spell is not casting it. The whole message has to BE the "
              + "word -- chat is where players discuss the game, and a magic system that "
              + "fires on a substring is one nobody can talk near");
        check(com.cadykaya.interregnum.core.magic.Incantation.of("  HUSH ")
                      == com.cadykaya.interregnum.core.magic.Spell.HUSH,
              "and case and surrounding space are forgiven, because a capital at the start "
              + "of a line is something a player gets for free rather than something they "
              + "meant");
        check(com.cadykaya.interregnum.core.magic.Incantation.of(null) == null
              && com.cadykaya.interregnum.core.magic.Incantation.of("") == null
              && com.cadykaya.interregnum.core.magic.Incantation.of("   ") == null,
              "nothing said is nothing cast");

        // Aimed or not is a real division, not per-spell taste: a spell that makes a place
        // you are standing in centres on you, and a spell you do something to centres on
        // what you are looking at. Asserted as a partition so that collapsing it either
        // way fails -- a silence you can stand outside and shoot into is a bubble.
        check(!com.cadykaya.interregnum.core.magic.Incantation
                      .aimed(com.cadykaya.interregnum.core.magic.Spell.HUSH)
              && !com.cadykaya.interregnum.core.magic.Incantation
                      .aimed(com.cadykaya.interregnum.core.magic.Spell.LIGHTEN)
              && com.cadykaya.interregnum.core.magic.Incantation
                      .aimed(com.cadykaya.interregnum.core.magic.Spell.WEATHER)
              && com.cadykaya.interregnum.core.magic.Incantation
                      .aimed(com.cadykaya.interregnum.core.magic.Spell.QUELL),
              "a room you stand in centres on the speaker and a thing you name centres on "
              + "what they are looking at. A silence you could aim would be a bubble you "
              + "could stand outside of and shoot into, which is the one thing a silence "
              + "must not be");

        // THE SERVER-REAL MANIFESTATION. `WORLD.md` calls it "a credibility problem" and
        // explicitly not a sanity bar, and the entire difference between those two things
        // is a rate. Both bounds below are the design rather than tuning taste.
        long mean = com.cadykaya.interregnum.core.haunt.Manifestation.meanTicksBetween();
        check(mean > 20L * 60 * 5,
              "a door moves less often than every five minutes. More often than that and "
              + "it is weather: nobody's account of it is in question, because there is "
              + "nothing left to doubt");
        check(mean < 20L * 60 * 60,
              "and more often than once an hour. A beat tuned so fine that a session "
              + "never contains one exists only in the source code");
        check(com.cadykaya.interregnum.core.haunt.Manifestation.due(0)
              && com.cadykaya.interregnum.core.haunt.Manifestation.due(
                      com.cadykaya.interregnum.core.haunt.Manifestation.INTERVAL_TICKS)
              && !com.cadykaya.interregnum.core.haunt.Manifestation.due(1),
              "the world asks on the interval and not on every tick. Asking every tick "
              + "would move the rarity entirely into the odds and make the cost of the "
              + "feature scale with how often it is supposed to happen");
        check(com.cadykaya.interregnum.core.haunt.Manifestation.REACH
                      <= com.cadykaya.interregnum.core.magic.Hush.RADIUS,
              "and the ghost reaches no further than a room. A bystander who saw the "
              + "door move has to have seen who was standing there, or it is just a "
              + "door moving");
    }

    /**
     * Nothing is known by default, and knowledge only ever grows. The first is the whole
     * progression -- if an untaught player could cast, crossing would buy nothing.
     */
    /**
     * The clast pool. `WORLD.md` locks "clasts are finite -- the class is a server
     * negotiation", and finite is the whole mechanic, so the arithmetic that enforces it
     * is worth more assertions than its four lines suggest.
     */
    /**
     * The steles. Their inscriptions are a pure function of position, so the properties
     * worth asserting are the ones a hash gets wrong: stability, spread, and behaviour
     * west of zero.
     */
    static void steles() {
        var S = com.cadykaya.interregnum.core.stele.Steles.class;
        int count = com.cadykaya.interregnum.core.stele.Steles.COUNT;
        check(count > 1, "there is more than one inscription, or every stele in the world "
              + "is the same notice and reading a second one teaches nothing");

        // Stable. The whole reason the choice is a function of position rather than a
        // roll: a stele you read yesterday says the same thing today.
        check(com.cadykaya.interregnum.core.stele.Steles.inscriptionAt(12, 64, -37)
                      == com.cadykaya.interregnum.core.stele.Steles.inscriptionAt(12, 64, -37),
              "one stele reads the same twice. A stele whose text moved would make the "
              + "ruin a slot machine and every quotation of it a lie");

        // IN RANGE EVERYWHERE, including west and north of the origin. floorMod rather
        // than %: a negative hash under % indexes backwards off the end of the list, and
        // it works perfectly in every world anybody tests near spawn.
        //
        // ONE assertion over the whole sweep rather than one per position. A `check` in
        // the body would be honest and would also add eight thousand to the number this
        // file reports, and a count that a loop can inflate stops being a signal about
        // how much is actually guarded.
        String outOfRange = null;
        for (int x = -2000; x <= 2000 && outOfRange == null; x += 37) {
            for (int z = -2000; z <= 2000; z += 53) {
                int i = com.cadykaya.interregnum.core.stele.Steles.inscriptionAt(x, 64, z);
                if (i < 0 || i >= count) {
                    outOfRange = "(" + x + ", 64, " + z + ") is index " + i;
                    break;
                }
            }
        }
        check(outOfRange == null,
              "an inscription index is outside the " + count + " that exist: " + outOfRange
              + ". floorMod rather than % is the whole of this: a negative hash under % "
              + "indexes backwards off the end of the list, and it works perfectly in "
              + "every world anybody tests near spawn");

        // And it uses all of them. A hash that technically varies but lands on two of
        // five would leave three notices nobody ever reads, which is a content bug that
        // no crash and no check would otherwise report.
        var seen = new java.util.HashSet<Integer>();
        for (int x = -500; x <= 500; x += 7) {
            for (int z = -500; z <= 500; z += 11) {
                seen.add(com.cadykaya.interregnum.core.stele.Steles.inscriptionAt(x, 64, z));
            }
        }
        check(seen.size() == count,
              "only " + seen.size() + " of " + count + " inscriptions appear anywhere in a "
              + "thousand blocks square. The rest are written, shipped, and unreachable");

        // The light. The keeper says "if you have the light for it", so the rule has to
        // exist and has to bite somewhere a player will actually be.
        check(!com.cadykaya.interregnum.core.stele.Steles.legible(0),
              "a stele in the dark cannot be read, which is what the shrine-keeper says");
        check(com.cadykaya.interregnum.core.stele.Steles.legible(15),
              "a stele in daylight can be read");
        check(com.cadykaya.interregnum.core.stele.Steles.READING_LIGHT > 0
              && com.cadykaya.interregnum.core.stele.Steles.READING_LIGHT < 15,
              "the reading light is a threshold a player can be on both sides of. At 0 the "
              + "keeper's line is decoration; at 15 only noon outdoors would do");
    }

    static void clasts() {
        int total = com.cadykaya.interregnum.core.clast.Clasts.TOTAL;
        check(total > 0, "there is at least one clast, or the class cannot exist at all");
        // The crater's share is bounded BOTH ways, and both bounds are design.
        check(com.cadykaya.interregnum.core.clast.Clasts.AT_CRATER < total,
              "the crater does not hold every clast. A killer who could pick up the whole "
              + "class by standing where they already are would make 'anyone may attune "
              + "one' untrue on the first day");
        check(com.cadykaya.interregnum.core.clast.Clasts.AT_CRATER
                      > com.cadykaya.interregnum.core.clast.Clasts.AT_SHRINE,
              "and it holds more than any one shrine does -- it is where the death "
              + "happened, and the overflow is locked as detonating outward FROM there");
        // The pool empties, and stays empty. A shrine loading after the last clast is
        // gone is the ordinary case rather than an error: it is what a world that has
        // been picked over looks like.
        check(com.cadykaya.interregnum.core.clast.Clasts.issue(0, 1) == 1,
              "the first request is met");
        check(com.cadykaya.interregnum.core.clast.Clasts.issue(total, 1) == 0,
              "a request made after the last clast has gone is met with nothing, and is "
              + "not an error -- shrines keep loading long after the pool is empty");
        check(com.cadykaya.interregnum.core.clast.Clasts.issue(total - 1, 4) == 1,
              "a request for more than remains gets what remains, not what it asked for. "
              + "This is the line that makes the count a CAP rather than a suggestion");
        check(com.cadykaya.interregnum.core.clast.Clasts.issue(0, 0) == 0
              && com.cadykaya.interregnum.core.clast.Clasts.issue(0, -3) == 0,
              "a site that wants nothing gets nothing, and cannot put clasts back into "
              + "the pool by asking for a negative number");
        check(com.cadykaya.interregnum.core.clast.Clasts.remaining(total + 5) == 0,
              "remaining never goes below zero, however the count got there -- a negative "
              + "remainder would read as a debt the world owes somebody");
        // The sum of one full distribution must not exceed the pool. Stated as the thing
        // it protects: every shrine in a world, plus the crater, cannot mint a class.
        int afterCrater = com.cadykaya.interregnum.core.clast.Clasts.issue(
                0, com.cadykaya.interregnum.core.clast.Clasts.AT_CRATER);
        int issued = afterCrater;
        for (int i = 0; i < 1000; i++) {
            issued += com.cadykaya.interregnum.core.clast.Clasts.issue(
                    issued, com.cadykaya.interregnum.core.clast.Clasts.AT_SHRINE);
        }
        check(issued == total,
              "a thousand shrines hand out exactly " + total + " clasts between them. A "
              + "world with forty shrines must not hand out forty classes, or the "
              + "negotiation WORLD.md locks never happens");
    }

    static void grimoires() {
        var g = new com.cadykaya.interregnum.core.magic.Grimoire();
        check(g.empty() && !g.knows(com.cadykaya.interregnum.core.magic.School.TURNING),
              "a new grimoire knows nothing. WORLD.md locks schools as learned in their "
              + "worlds; a default-known school would make the journey buy nothing");
        check(!com.cadykaya.interregnum.core.magic.Casting.permitted(
                      g, com.cadykaya.interregnum.core.magic.School.TURNING),
              "casting is refused to somebody who has not been taught");
        check(!com.cadykaya.interregnum.core.magic.Casting.permitted(
                      null, com.cadykaya.interregnum.core.magic.School.TURNING),
              "a caster with no record at all is refused rather than waved through -- "
              + "fails closed, like every other gate in this mod");

        check(g.learn(com.cadykaya.interregnum.core.magic.School.TURNING),
              "learning something new reports that it was new");
        check(!g.learn(com.cadykaya.interregnum.core.magic.School.TURNING),
              "learning it twice reports that it was not, so a scene replayed does not "
              + "read as a second teaching");
        check(com.cadykaya.interregnum.core.magic.Casting.permitted(
                      g, com.cadykaya.interregnum.core.magic.School.TURNING),
              "casting is permitted once taught");
        check(!g.knows(com.cadykaya.interregnum.core.magic.School.SILENCE),
              "learning one school teaches only that one -- four gods, four journeys, "
              + "and a single lesson must not open all of them");

        // Round-trips by NAME, so reordering the enum cannot silently reassign what
        // somebody knows. An ordinal format would survive the reorder and be wrong.
        var back = com.cadykaya.interregnum.core.magic.Grimoire.deserialize(g.serialize());
        check(back.knows(com.cadykaya.interregnum.core.magic.School.TURNING)
              && back.size() == 1,
              "a grimoire survives being written and read back");
        var junk = com.cadykaya.interregnum.core.magic.Grimoire.deserialize(
                java.util.List.of("turning", "a_school_that_was_removed"));
        check(junk.knows(com.cadykaya.interregnum.core.magic.School.TURNING)
              && junk.size() == 1,
              "a name that no longer exists is dropped rather than thrown -- a school "
              + "removed in a later version must not cost somebody their save");

        // The overworld ban, stated where casting is described.
        check(com.cadykaya.interregnum.core.magic.Casting.drawsOnTheCorpse(true)
              && !com.cadykaya.interregnum.core.magic.Casting.drawsOnTheCorpse(false),
              "casting draws on the corpse at home and not in a living god's world. That "
              + "asymmetry is the whole economics of the mid-game: the ban forces travel "
              + "by law AND by cost");
    }

    /**
     * A node may mark the world. Almost none do, and that asymmetry is the guard: the
     * failure worth catching is not "this node forgot to mark", it is "every node marks",
     * which would advance the chapter on every line anybody says.
     */
    static void marking() {
        var opts = java.util.List.<com.cadykaya.interregnum.core.dialogue.DialogueOption>of();
        var plain = new com.cadykaya.interregnum.core.dialogue.DialogueNode(
                "n", "who", "key",
                com.cadykaya.interregnum.core.dialogue.ResolutionRule.INITIATOR, opts);
        check(!plain.marks(),
              "an ordinary node marks nothing. If nodes marked by default the chapter "
              + "would advance on every line anybody spoke, and the whole progression "
              + "would be over before the first scene ended");
        check(plain.milestone() == null, "an ordinary node carries no milestone");

        var marker = new com.cadykaya.interregnum.core.dialogue.DialogueNode(
                "n", "who", "key",
                com.cadykaya.interregnum.core.dialogue.ResolutionRule.INITIATOR, opts,
                java.util.List.of(), Milestone.LETTER_DELIVERED);
        check(marker.marks() && marker.milestone() == Milestone.LETTER_DELIVERED,
              "a node given a milestone reports it -- this is how a delivery scene makes "
              + "LETTER_DELIVERED mean anything, and Chapter gates the back half of the "
              + "game on that count");

        // The count is what a chapter gate reads, so repeats must accumulate. Four gods,
        // four letters: a delivery system that recorded "some letter was delivered" as a
        // single fact could never tell one god answered from all four.
        var st = new ChapterState();
        st.record(Milestone.DEICIDE);
        st.record(Milestone.LETTER_DELIVERED);
        st.record(Milestone.LETTER_DELIVERED);
        check(st.lettersDelivered() == 2,
              "delivering two letters counts two. LETTER_DELIVERED is the one repeatable "
              + "milestone and the count is what the chapter gates read; collapsing it to "
              + "a flag would make one god answered indistinguishable from all four");
    }

    /**
     * Band 4 decides WHERE the world forgets itself, and every assertion here is about
     * the gap between two radii rather than about either one -- because that gap is the
     * mechanic, and either number alone can be changed without it being visible.
     */
    static void attrition() {
        check(!com.cadykaya.interregnum.core.attrition.Attrition.fraying(3),
              "the world does not start forgetting itself before band 4 -- a band that "
              + "fires early leaves the escalation with nothing left to escalate to");
        check(com.cadykaya.interregnum.core.attrition.Attrition.fraying(4)
              && com.cadykaya.interregnum.core.attrition.Attrition.fraying(5),
              "band 4 and everything after it frays");

        // Standing on it tends it; so does being a couple of chunks away. This is the
        // half a player performs on purpose.
        check(com.cadykaya.interregnum.core.attrition.Attrition.tends(0, 0),
              "the chunk a player is standing in is tended, or the counter-move to the "
              + "apocalypse is not available even to somebody doing it deliberately");
        check(com.cadykaya.interregnum.core.attrition.Attrition.tends(2, -2),
              "the corner of the tended square is tended -- Chebyshev, so the tended "
              + "region is a square like every other chunk distance in the game");

        // THE ASSERTION THAT MATTERS. Tending has to be strictly more intimate than
        // loading, or band 4 cancels itself out: attrition can only touch loaded ground,
        // and if everything loaded were also tended there would be nowhere it could ever
        // act. A single number changed to "whatever the view distance is" would leave
        // every other check here green and the band silently inert.
        check(!com.cadykaya.interregnum.core.attrition.Attrition.tends(4, 0),
              "ground four chunks away is NOT tended. It is still loaded, and that ring "
              + "-- present but unattended -- is the only place attrition can ever act. "
              + "If tending reached as far as loading, band 4 would be a no-op forever "
              + "and nothing else in this file would notice");

        long fray = com.cadykaya.interregnum.core.attrition.Attrition.FRAY_AFTER_TICKS;
        check(!com.cadykaya.interregnum.core.attrition.Attrition.stale(1000, 1000),
              "ground tended this instant is not stale");
        check(!com.cadykaya.interregnum.core.attrition.Attrition.stale(1000, 1000 + fray - 1),
              "ground is not stale one tick before its time -- an off-by-one here makes "
              + "the whole threshold meaningless in the safe direction, which is the "
              + "direction nobody investigates");
        check(com.cadykaya.interregnum.core.attrition.Attrition.stale(1000, 1000 + fray),
              "ground untended for the full span is stale");
        check(com.cadykaya.interregnum.core.attrition.Attrition.stale(0, fray * 10),
              "ground untended for a very long time is still stale -- staleness is a "
              + "comparison, not a window that closes behind you");
    }
}
