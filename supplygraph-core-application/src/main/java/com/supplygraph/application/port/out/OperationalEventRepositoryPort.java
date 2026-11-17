package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.event.EventId;
import com.supplygraph.domain.model.event.OperationalEvent;

import java.util.List;
import java.util.Optional;

/**
 * Outbound repository port for persisting and retrieving operational events.
 */
public interface OperationalEventRepositoryPort {
    void save(OperationalEvent event);
    Optional<OperationalEvent> findById(EventId eventId);
    Optional<OperationalEvent> findByIdempotencyKey(String idempotencyKey);
    List<OperationalEvent> findByWorkspaceId(WorkspaceId workspaceId, int limit);
    List<OperationalEvent> findRecent(int limit);
}

