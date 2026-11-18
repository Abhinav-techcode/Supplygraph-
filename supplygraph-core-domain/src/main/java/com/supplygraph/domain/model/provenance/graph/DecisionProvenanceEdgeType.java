package com.supplygraph.domain.model.provenance.graph;

/**
 * Directed relationship types in the decision provenance graph.
 */
public enum DecisionProvenanceEdgeType {
    CAUSES("Causes"),
    INFORMS("Informs"),
    ASSESSES("Assesses"),
    TRIGGERS("Triggers"),
    INVESTIGATES("Investigates"),
    PROPOSES("Proposes"),
    ARBITRATES("Arbitrates"),
    SIMULATES("Simulates"),
    EVALUATES("Evaluates"),
    OPTIMIZES("Optimizes"),
    APPROVES("Approves"),
    REJECTS("Rejects");

    private final String displayName;

    DecisionProvenanceEdgeType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

