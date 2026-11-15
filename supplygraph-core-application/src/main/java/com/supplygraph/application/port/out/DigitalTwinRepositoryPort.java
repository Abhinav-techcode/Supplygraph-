package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.twin.DigitalTwinState;

import java.util.Optional;

/**
 * Outbound repository port for persisting and retrieving digital twin aggregate state.
 */
public interface DigitalTwinRepositoryPort {
    void save(DigitalTwinState state);
    Optional<DigitalTwinState> findByWorkspaceId(WorkspaceId workspaceId);
    boolean existsByWorkspaceId(WorkspaceId workspaceId);
}

