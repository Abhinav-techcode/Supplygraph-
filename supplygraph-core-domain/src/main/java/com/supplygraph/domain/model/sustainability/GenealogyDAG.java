package com.supplygraph.domain.model.sustainability;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Directed Acyclic Graph (DAG) for bidirectional batch/lot genealogy and chain-of-custody tracking.
 */
public class GenealogyDAG implements Serializable {

    private final Map<TraceabilityNodeId, TraceabilityNode> nodes = new LinkedHashMap<>();
    private final Map<TraceabilityNodeId, List<GenealogyEdge>> outgoingEdges = new LinkedHashMap<>();
    private final Map<TraceabilityNodeId, List<GenealogyEdge>> incomingEdges = new LinkedHashMap<>();

    public GenealogyDAG() {}

    public void addNode(TraceabilityNode node) {
        Objects.requireNonNull(node, "Node cannot be null");
        nodes.put(node.id(), node);
        outgoingEdges.putIfAbsent(node.id(), new ArrayList<>());
        incomingEdges.putIfAbsent(node.id(), new ArrayList<>());
    }

    public void addEdge(GenealogyEdge edge) {
        Objects.requireNonNull(edge, "Edge cannot be null");
        if (!nodes.containsKey(edge.sourceNodeId()) || !nodes.containsKey(edge.targetNodeId())) {
            throw new IllegalArgumentException("Both endpoints of genealogy edge must exist in the graph");
        }
        outgoingEdges.get(edge.sourceNodeId()).add(edge);
        incomingEdges.get(edge.targetNodeId()).add(edge);
    }

    public Optional<TraceabilityNode> getNode(TraceabilityNodeId id) {
        return Optional.ofNullable(nodes.get(id));
    }

    public Optional<TraceabilityNode> findByBatchNumber(String batchNumber) {
        return nodes.values().stream()
                .filter(n -> n.batchNumber().equalsIgnoreCase(batchNumber))
                .findFirst();
    }

    /**
     * Backward genealogy traversal: from finished goods lot up to raw material origin.
     */
    public List<TraceabilityNode> backwardTrace(TraceabilityNodeId targetLotId) {
        if (!nodes.containsKey(targetLotId)) {
            return Collections.emptyList();
        }

        Set<TraceabilityNodeId> visited = new LinkedHashSet<>();
        Deque<TraceabilityNodeId> queue = new ArrayDeque<>();
        queue.add(targetLotId);

        while (!queue.isEmpty()) {
            TraceabilityNodeId current = queue.poll();
            if (visited.add(current)) {
                List<GenealogyEdge> upstream = incomingEdges.getOrDefault(current, Collections.emptyList());
                for (GenealogyEdge edge : upstream) {
                    if (!visited.contains(edge.sourceNodeId())) {
                        queue.add(edge.sourceNodeId());
                    }
                }
            }
        }

        return visited.stream().map(nodes::get).filter(Objects::nonNull).toList();
    }

    /**
     * Forward genealogy traversal (Recall Impact Analysis): from tainted raw material batch downstream to all affected finished goods and customer shipments.
     */
    public List<TraceabilityNode> forwardTrace(TraceabilityNodeId taintedBatchId) {
        if (!nodes.containsKey(taintedBatchId)) {
            return Collections.emptyList();
        }

        Set<TraceabilityNodeId> visited = new LinkedHashSet<>();
        Deque<TraceabilityNodeId> queue = new ArrayDeque<>();
        queue.add(taintedBatchId);

        while (!queue.isEmpty()) {
            TraceabilityNodeId current = queue.poll();
            if (visited.add(current)) {
                List<GenealogyEdge> downstream = outgoingEdges.getOrDefault(current, Collections.emptyList());
                for (GenealogyEdge edge : downstream) {
                    if (!visited.contains(edge.targetNodeId())) {
                        queue.add(edge.targetNodeId());
                    }
                }
            }
        }

        return visited.stream().map(nodes::get).filter(Objects::nonNull).toList();
    }

    public List<TraceabilityNode> getAllNodes() {
        return List.copyOf(nodes.values());
    }

    public List<GenealogyEdge> getAllEdges() {
        List<GenealogyEdge> all = new ArrayList<>();
        outgoingEdges.values().forEach(all::addAll);
        return Collections.unmodifiableList(all);
    }
}

