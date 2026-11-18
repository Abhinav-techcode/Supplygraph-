package com.supplygraph.domain.model.provenance.graph;

import java.io.Serializable;
import java.util.*;

/**
 * End-to-end relational and navigable DAG capturing decision provenance from originating disruption
 * through telemetry, digital twin state, impact assessment, alerts, investigations, proposals,
 * ConcurGuard decisions, simulations, Monte Carlo analysis, optimizations, to human approvals.
 */
public record DecisionProvenanceGraph(
        String workspaceId,
        String rootDisruptionId,
        Map<DecisionProvenanceNodeId, DecisionProvenanceNode> nodes,
        List<DecisionProvenanceEdge> edges
) implements Serializable {

    public DecisionProvenanceGraph {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(rootDisruptionId, "rootDisruptionId must not be null");
        nodes = nodes == null ? Map.of() : Map.copyOf(nodes);
        edges = edges == null ? List.of() : List.copyOf(edges);
    }

    public static DecisionProvenanceGraph create(
            String workspaceId,
            String rootDisruptionId,
            List<DecisionProvenanceNode> nodeList,
            List<DecisionProvenanceEdge> edgeList
    ) {
        Map<DecisionProvenanceNodeId, DecisionProvenanceNode> map = new LinkedHashMap<>();
        if (nodeList != null) {
            for (DecisionProvenanceNode node : nodeList) {
                map.put(node.id(), node);
            }
        }
        return new DecisionProvenanceGraph(workspaceId, rootDisruptionId, map, edgeList);
    }

    public List<DecisionProvenanceNode> getAllNodes() {
        return List.copyOf(nodes.values());
    }

    public Optional<DecisionProvenanceNode> getNode(DecisionProvenanceNodeId id) {
        return Optional.ofNullable(nodes.get(id));
    }

    public List<DecisionProvenanceEdge> getOutgoingEdges(DecisionProvenanceNodeId source) {
        return edges.stream()
                .filter(e -> e.sourceNodeId().equals(source))
                .toList();
    }

    public List<DecisionProvenanceEdge> getIncomingEdges(DecisionProvenanceNodeId target) {
        return edges.stream()
                .filter(e -> e.targetNodeId().equals(target))
                .toList();
    }
}

