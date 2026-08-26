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
