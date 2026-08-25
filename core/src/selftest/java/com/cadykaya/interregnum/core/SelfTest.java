package com.cadykaya.interregnum.core;

import com.cadykaya.interregnum.core.dialogue.*;

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
        c = new Conversation(graph(ResolutionRule.VOTE), "kaya", List.of("kaya", "p2"));
        c.submit("kaya", "a"); c.submit("p2", "b");
        check(c.resolve(new Random(1)).chosen().id().equals("a"), "tie breaks toward initiator");

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

        // -- terminal prose ends the conversation -----------------------------
        c = new Conversation(graph(ResolutionRule.INITIATOR), "kaya", List.of("kaya"));
        c.submit("kaya", "b");
        c.resolve(new Random(1));
        check(c.ended(), "advancing into a terminal node ends the conversation");
        threw = false;
        try { c.submit("kaya", "a"); } catch (IllegalStateException e) { threw = true; }
        check(threw, "a finished conversation refuses picks");

        System.out.println("SelfTest: " + passed + " checks passed");
    }
}
