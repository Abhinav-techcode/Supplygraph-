package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.DisruptionId;
import com.supplygraph.simulation.optimization.OptimizationResult;

import java.util.List;
import java.util.Optional;

/**
 * Outbound repository port for persisting and retrieving optimization solve outcomes.
 */
public interface OptimizationRunRepositoryPort {
    void save(OptimizationResult result, WorkspaceId workspaceId, DisruptionId disruptionId);
    Optional<OptimizationResult> findById(String runId);
    List<OptimizationResult> findByDisruptionId(DisruptionId disruptionId);
    List<OptimizationResult> findAllByWorkspaceId(WorkspaceId workspaceId);
    long countByWorkspaceId(WorkspaceId workspaceId);
}

