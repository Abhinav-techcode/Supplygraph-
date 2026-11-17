package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.DisruptionId;
import com.supplygraph.simulation.montecarlo.MonteCarloSummaryResult;

import java.util.List;
import java.util.Optional;

/**
 * Outbound repository port for persisting and retrieving Monte Carlo risk analysis runs.
 */
public interface MonteCarloRunRepositoryPort {
    void save(MonteCarloSummaryResult result, WorkspaceId workspaceId, DisruptionId disruptionId, String planId);
    Optional<MonteCarloSummaryResult> findById(String runId);
    List<MonteCarloSummaryResult> findByDisruptionId(DisruptionId disruptionId);
    List<MonteCarloSummaryResult> findAllByWorkspaceId(WorkspaceId workspaceId);
    long countByWorkspaceId(WorkspaceId workspaceId);
}

