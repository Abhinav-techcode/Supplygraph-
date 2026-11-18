package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.connector.ConnectorId;
import com.supplygraph.domain.model.sync.ReconciliationItem;

import java.util.List;
import java.util.Optional;

public interface ReconciliationRepositoryPort {
    ReconciliationItem save(ReconciliationItem item);
    Optional<ReconciliationItem> findById(String id);
    List<ReconciliationItem> findOpen(WorkspaceId ws);
    List<ReconciliationItem> findByWorkspace(WorkspaceId ws, int limit);
    boolean existsOpenFor(WorkspaceId ws, ConnectorId connector, String entityType, String entityKey, String field, String externalValue);
}
