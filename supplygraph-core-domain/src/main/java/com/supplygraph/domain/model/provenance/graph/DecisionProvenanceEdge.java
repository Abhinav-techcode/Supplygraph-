package com.supplygraph.domain.model.provenance.graph;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Directed edge representing causal or analytical derivation between provenance nodes.
 */
public record DecisionProvenanceEdge(
        String id,
        DecisionProvenanceNodeId sourceNodeId,
        DecisionProvenanceNodeId targetNodeId,
        DecisionProvenanceEdgeType type,
        String explanation,
        Instant timestamp
) implements Serializable {

    public DecisionProvenanceEdge {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(sourceNodeId, "sourceNodeId must not be null");
        Objects.requireNonNull(targetNodeId, "targetNodeId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    public static DecisionProvenanceEdge of(
            String id,
            DecisionProvenanceNodeId source,
            DecisionProvenanceNodeId target,
            DecisionProvenanceEdgeType type,
            String explanation,
            Instant timestamp
    ) {
        return new DecisionProvenanceEdge(id, source, target, type, explanation, timestamp);
    }
}

