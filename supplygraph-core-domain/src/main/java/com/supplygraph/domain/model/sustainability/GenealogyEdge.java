package com.supplygraph.domain.model.sustainability;

import java.io.Serializable;
import java.util.Objects;

/**
 * Directed transformation or transfer edge between two genealogy nodes.
 */
public record GenealogyEdge(
        TraceabilityNodeId sourceNodeId,
        TraceabilityNodeId targetNodeId,
        double quantityTransferred,
        String transformationDetails
) implements Serializable {
    public GenealogyEdge {
        Objects.requireNonNull(sourceNodeId, "Source node ID cannot be null");
        Objects.requireNonNull(targetNodeId, "Target node ID cannot be null");
        Objects.requireNonNull(transformationDetails, "Transformation details cannot be null");
    }
}

