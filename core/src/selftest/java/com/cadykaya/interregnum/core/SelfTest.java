package com.cadykaya.interregnum.core;

import com.cadykaya.interregnum.core.chapter.*;
import com.cadykaya.interregnum.core.dialogue.*;
import com.cadykaya.interregnum.core.regard.*;

import java.util.List;
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
}
