package com.supplygraph.domain.model.provenance.graph;

import java.io.Serializable;
import java.util.Objects;

/**
 * Strongly typed identifier for a node in the decision provenance graph.
 */
public record DecisionProvenanceNodeId(String value) implements Serializable {
    public DecisionProvenanceNodeId {
        Objects.requireNonNull(value, "DecisionProvenanceNodeId must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("DecisionProvenanceNodeId must not be blank");
        }
    }

    public static DecisionProvenanceNodeId of(String value) {
        return new DecisionProvenanceNodeId(value);
    }

    public static DecisionProvenanceNodeId generate() {
        return new DecisionProvenanceNodeId("DPN-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}
