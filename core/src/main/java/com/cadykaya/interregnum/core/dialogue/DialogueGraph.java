package com.cadykaya.interregnum.core.dialogue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An immutable conversation graph. Construction validates the whole graph and
 * throws on the failures that would otherwise surface mid-conversation in front
 * of a player: dangling targets, unreachable nodes, a missing start.
 *
 * Loader-side code (JSON -> this model) lives in the NeoForge module; this class
 * deliberately knows nothing about JSON, Minecraft, or the screen.
 */
public final class DialogueGraph {
    public static final String END = "$end";

    private final String startId;
    private final Map<String, DialogueNode> nodes;

    public DialogueGraph(String startId, List<DialogueNode> nodeList) {
        var byId = new java.util.LinkedHashMap<String, DialogueNode>();
        for (DialogueNode n : nodeList) {
            if (byId.put(n.id(), n) != null)
                throw new IllegalArgumentException("duplicate node id: " + n.id());
        }
        this.startId = startId;
        this.nodes = Map.copyOf(byId);
        validate();
    }

    public DialogueNode start() { return nodes.get(startId); }
    public DialogueNode node(String id) { return nodes.get(id); }
    public Set<String> nodeIds() { return nodes.keySet(); }

    private void validate() {
        if (!nodes.containsKey(startId))
            throw new IllegalArgumentException("start node missing: " + startId);
        var failures = new ArrayList<String>();
        for (DialogueNode n : nodes.values())
            for (DialogueOption o : n.options())
                if (!o.targetNodeId().equals(END) && !nodes.containsKey(o.targetNodeId()))
                    failures.add(n.id() + "/" + o.id() + " -> missing node " + o.targetNodeId());
        Set<String> reached = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>(List.of(startId));
        while (!queue.isEmpty()) {
            DialogueNode n = nodes.get(queue.poll());
            if (n == null || !reached.add(n.id())) continue;
            for (DialogueOption o : n.options())
                if (!o.targetNodeId().equals(END)) queue.add(o.targetNodeId());
        }
        for (String id : nodes.keySet())
            if (!reached.contains(id)) failures.add("unreachable node: " + id);
        if (!failures.isEmpty())
            throw new IllegalArgumentException("invalid dialogue graph:\n  " + String.join("\n  ", failures));
    }
}
