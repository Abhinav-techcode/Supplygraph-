package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.disruption.DisruptionId;
import com.supplygraph.domain.model.disruption.DisruptionStatus;

import java.util.List;
import java.util.Optional;

/**
 * Outbound repository port for persisting and retrieving disruption incidents.
 */
public interface DisruptionRepositoryPort {
    void save(Disruption disruption);
    Optional<Disruption> findById(DisruptionId id);
    List<Disruption> findAllByWorkspaceId(WorkspaceId workspaceId);
    List<Disruption> findByWorkspaceIdAndStatus(WorkspaceId workspaceId, DisruptionStatus status);
    long countActiveByWorkspaceId(WorkspaceId workspaceId);
}

