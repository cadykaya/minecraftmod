package com.cadykaya.interregnum.core.dialogue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Server-side state of one live conversation: which node, who is at the table,
 * what each has picked. The screen renders this; it never decides anything.
 *
 * Every participant's pick is shown to the whole table as their spoken stance --
 * that display is the entire "players argue with each other" mechanic, and it is
 * the caller's job. This class only guarantees the data is there.
 *
 * Determinism: ROLL uses the injected generator. Callers seed it from world seed
 * + conversation id so a resolution is reproducible in tests and replays.
 */
public final class Conversation {
    private final DialogueGraph graph;
    private final String initiator;
    private final Set<String> participants;
    private final Map<String, String> picks = new LinkedHashMap<>();
    private DialogueNode current;
    private boolean ended;

    public Conversation(DialogueGraph graph, String initiator, List<String> participants) {
        this.graph = Objects.requireNonNull(graph);
        this.initiator = Objects.requireNonNull(initiator);
        this.participants = new LinkedHashSet<>(participants);
        if (!this.participants.contains(initiator))
            throw new IllegalArgumentException("initiator must be a participant");
        this.current = graph.start();
        this.ended = current.terminal();
    }

    public DialogueNode current() { return current; }
    public boolean ended() { return ended; }
    /**
     * Who has said what so far, IN THE ORDER THEY SPOKE.
     *
     * The order is content, not incidental: a table showing stances is showing an
     * argument, and who answered first before the others fell in behind them is
     * most of what that argument reads as. `Map.copyOf` was wrong here -- it
     * returns an unordered map, so the display order changed between nodes and
     * between runs for no reason a player could see.
     */
    public Map<String, String> ordered() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(picks));
    }

    public Map<String, String> picks() { return ordered(); }

    /** Record a participant's pick. Repicking before resolution is allowed (changing your mind is legal). */
    public void submit(String participant, String optionId) {
        if (ended) throw new IllegalStateException("conversation is over");
        if (!participants.contains(participant))
            throw new IllegalArgumentException("not at this table: " + participant);
        if (current.options().stream().noneMatch(o -> o.id().equals(optionId)))
            throw new IllegalArgumentException("no option " + optionId + " on node " + current.id());
        picks.put(participant, optionId);
    }

    public boolean allSubmitted() { return picks.keySet().containsAll(participants); }

    public Set<String> participants() { return Set.copyOf(participants); }

    public String initiator() { return initiator; }

    /**
     * Take someone off the table: they logged off, walked away, or timed out.
     *
     * Their pick goes with them rather than standing as a vote in their absence.
     * Someone who left did not abstain into whatever the majority wanted -- they
     * are not there, and a VOTE that counted an absent player's earlier click
     * would let a player influence a table they had already quit.
     *
     * Refuses to remove the INITIATOR, because there is no sensible answer for a
     * node whose rule is INITIATOR once they are gone. The caller ends the
     * conversation instead: the Warden was talking to *them*.
     *
     * @return true if they were at the table.
     */
    public boolean remove(String participant) {
        if (participant.equals(initiator))
            throw new IllegalArgumentException("cannot remove the initiator; end the conversation");
        picks.remove(participant);
        return participants.remove(participant);
    }

    /**
     * Resolve the current node per its rule. Caller decides WHEN (usually on
     * allSubmitted() or a timeout that defaults absentees to abstention by
     * removing them from the table first).
     */
    public Resolution resolve(RandomGenerator roll) {
        if (ended) throw new IllegalStateException("conversation is over");
        if (picks.isEmpty()) throw new IllegalStateException("nothing submitted");
        Map<String, String> stances = ordered();

        String winnerId = switch (current.rule()) {
            case INITIATOR -> picks.getOrDefault(initiator, picks.values().iterator().next());
            case VOTE -> {
                Map<String, Integer> tally = new LinkedHashMap<>();
                picks.values().forEach(id -> tally.merge(id, 1, Integer::sum));
                int max = tally.values().stream().mapToInt(Integer::intValue).max().orElse(0);
                List<String> top = tally.entrySet().stream()
                        .filter(e -> e.getValue() == max).map(Map.Entry::getKey).toList();
                String initiatorPick = picks.get(initiator);
                yield (top.size() > 1 && initiatorPick != null && top.contains(initiatorPick))
                        ? initiatorPick : top.get(0);
            }
            case ROLL -> {
                List<String> all = List.copyOf(picks.values()); // weighted by popularity
                yield all.get(roll.nextInt(all.size()));
            }
            case UNANIMOUS -> {
                Set<String> distinct = new LinkedHashSet<>(picks.values());
                yield distinct.size() == 1 ? distinct.iterator().next() : null;
            }
        };

        if (winnerId == null) {
            picks.clear();                       // the table argues and re-picks
            return new Resolution(Resolution.Kind.REPROMPT, null, stances);
        }
        DialogueOption chosen = current.options().stream()
                .filter(o -> o.id().equals(winnerId)).findFirst().orElseThrow();
        picks.clear();
        if (chosen.targetNodeId().equals(DialogueGraph.END)) {
            ended = true;
        } else {
            current = graph.node(chosen.targetNodeId());
            if (current.terminal()) ended = true;
        }
        return new Resolution(Resolution.Kind.ADVANCED, chosen, stances);
    }
}
