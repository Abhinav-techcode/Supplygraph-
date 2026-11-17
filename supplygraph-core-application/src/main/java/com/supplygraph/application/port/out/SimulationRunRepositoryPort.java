package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.DisruptionId;
import com.supplygraph.simulation.engine.SimulationResult;

import java.util.List;
import java.util.Optional;

/**
 * Outbound repository port for persisting and retrieving discrete-event simulation runs.
 */
public interface SimulationRunRepositoryPort {
    void save(SimulationResult result, WorkspaceId workspaceId, DisruptionId disruptionId, String planId);
    Optional<SimulationResult> findById(String simulationId);
    List<SimulationResult> findByDisruptionId(DisruptionId disruptionId);
    List<SimulationResult> findAllByWorkspaceId(WorkspaceId workspaceId);
    long countByWorkspaceId(WorkspaceId workspaceId);
}

