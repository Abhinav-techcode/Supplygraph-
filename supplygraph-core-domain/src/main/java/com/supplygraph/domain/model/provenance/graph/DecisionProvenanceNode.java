package com.supplygraph.domain.model.provenance.graph;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Node within the decision provenance graph tracking an exact step, artifact, or human action.
 */
public record DecisionProvenanceNode(
        DecisionProvenanceNodeId id,
        DecisionProvenanceNodeType type,
        String label,
        String entityReferenceId,
        Instant timestamp,
        Map<String, String> metadata
) implements Serializable {

    public DecisionProvenanceNode {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(label, "label must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static DecisionProvenanceNode of(
            DecisionProvenanceNodeId id,
            DecisionProvenanceNodeType type,
            String label,
            String entityReferenceId,
            Instant timestamp,
            Map<String, String> metadata
    ) {
        return new DecisionProvenanceNode(id, type, label, entityReferenceId, timestamp, metadata);
    }
}
