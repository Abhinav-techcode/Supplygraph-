package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.connector.ConnectorExecution;
import com.supplygraph.domain.model.connector.ConnectorId;

import java.util.List;
import java.util.Optional;

public interface ConnectorExecutionRepositoryPort {
    ConnectorExecution save(ConnectorExecution execution);
    Optional<ConnectorExecution> findById(String id);
    List<ConnectorExecution> findByConnector(ConnectorId id, int limit);
    List<ConnectorExecution> findByWorkspace(WorkspaceId ws, int limit);
    /** True when a retry of {@code executionId} was already recorded (prevents double retry). */
    boolean hasRetry(String executionId);
}
