package com.supplygraph.application.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.outbox.OutboxEvent;
import com.supplygraph.domain.model.outbox.OutboxStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OutboxRepositoryPort {
    /** Inserts the event unless (workspace, idempotencyKey) already exists, in which case the existing event is returned. */
    OutboxEvent insertIfAbsent(OutboxEvent event);
    OutboxEvent save(OutboxEvent event);
    Optional<OutboxEvent> findById(String id);
    List<OutboxEvent> findByWorkspace(WorkspaceId ws, OutboxStatus status, int limit);
    /** Atomically claims up to {@code limit} due PENDING events (concurrent-safe) and marks them IN_FLIGHT in a committed transaction. */
    List<OutboxEvent> claimDue(Instant now, int limit);
    /** Resets events stuck IN_FLIGHT since before {@code staleBefore} back to PENDING. Returns count. */
    int recoverStale(Instant staleBefore, Instant now);
    Map<OutboxStatus, Long> countByStatus(WorkspaceId ws);
    /** Cross-workspace aggregate counts for process-level metrics (no workspace identities exposed). */
    Map<OutboxStatus, Long> countAllByStatus();
}
