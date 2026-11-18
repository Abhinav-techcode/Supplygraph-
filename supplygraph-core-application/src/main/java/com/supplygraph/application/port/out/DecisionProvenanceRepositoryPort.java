package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.provenance.graph.DecisionProvenanceGraph;

import java.util.Optional;

/**
 * Outbound persistence port for the decision provenance graph.
 */
public interface DecisionProvenanceRepositoryPort {

    void saveGraph(DecisionProvenanceGraph graph);

    Optional<DecisionProvenanceGraph> findByRootDisruptionId(String disruptionId);

    Optional<DecisionProvenanceGraph> findByWorkspaceId(String workspaceId);
}

