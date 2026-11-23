package com.supplygraph.application.outbox;

import com.supplygraph.domain.error.ConflictException;
import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.domain.error.ResourceNotFoundException;
import com.supplygraph.application.audit.AuditGovernanceService;
import com.supplygraph.application.port.out.OutboxRepositoryPort;
import com.supplygraph.application.security.SecurityContext;
import com.supplygraph.application.security.WorkspaceAccessGuard;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.audit.AuditAction;
import com.supplygraph.domain.model.outbox.BackoffPolicy;
import com.supplygraph.domain.model.outbox.OutboxEvent;
import com.supplygraph.domain.model.outbox.OutboxStatus;
import com.supplygraph.domain.model.security.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** Transactional outbox: enqueue (joins the caller's transaction), bounded dispatch, authorized replay. */
@Service
public class OutboxService {
    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);
    static final Duration STALE_AFTER = Duration.ofMinutes(5);

    private final OutboxRepositoryPort repository;
    private final WorkspaceAccessGuard guard;
    private final AuditGovernanceService audit;
    private final Map<String, OutboxHandler> handlers = new HashMap<>();
    private final BackoffPolicy backoff = BackoffPolicy.standard();

    public OutboxService(OutboxRepositoryPort repository, WorkspaceAccessGuard guard, AuditGovernanceService audit, List<OutboxHandler> handlerList) {
        this.repository = repository;
        this.guard = guard;
        this.audit = audit;
        for (OutboxHandler h : handlerList) {
            if (handlers.put(h.eventType(), h) != null) throw new ConflictException("Duplicate outbox handler for " + h.eventType());
        }
    }

    /** MANDATORY: must run inside the business transaction so state change and event commit or roll back together. */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent enqueue(WorkspaceId ws, String eventType, String aggregateId, String payload, String idempotencyKey, String correlationId) {
        return repository.insertIfAbsent(OutboxEvent.create(ws, eventType, aggregateId, payload, idempotencyKey, correlationId));
    }

    /** Processes up to {@code batchSize} due events. Returns the number delivered. Never throws for individual event failures. */
    public int dispatchBatch(int batchSize) {
        Instant now = Instant.now();
        repository.recoverStale(now.minus(STALE_AFTER), now);
        int delivered = 0;
        for (OutboxEvent claimed : repository.claimDue(now, Math.min(Math.max(batchSize, 1), 200))) {
            OutboxHandler handler = handlers.get(claimed.eventType());
            Instant t = Instant.now();
            try {
                if (handler == null) throw new DeliveryException("No handler registered for event type " + claimed.eventType(), false);
                handler.handle(claimed);
                repository.save(claimed.markDelivered(t));
                delivered++;
            } catch (DeliveryException e) {
                repository.save(claimed.markFailed(e.getMessage(), e.retryable(), backoff, t));
            } catch (RuntimeException e) {
                log.error("Unexpected failure delivering outbox event {} correlation={}", claimed.id(), claimed.correlationId(), e);
                repository.save(claimed.markFailed("Unexpected error: " + e.getClass().getSimpleName(), true, backoff, t));
            }
        }
        return delivered;
    }

    /** Replays a dead-lettered event. Requires EVENT_REPLAY in the event's workspace; audited. */
    public OutboxEvent replay(String eventId, String rationale) {
        OutboxEvent event = repository.findById(eventId).orElseThrow(() -> new ResourceNotFoundException("Outbox event not found: " + eventId));
        guard.enforceWorkspaceAndPermission(event.workspaceId(), Permission.EVENT_REPLAY);
        if (rationale == null || rationale.isBlank()) throw new ValidationException("A replay rationale is required");
        OutboxEvent replayed = repository.save(event.replay(Instant.now()));
        audit.logAction(SecurityContext.getCurrentUser().id(), event.workspaceId(), AuditAction.EVENT_REPLAYED, eventId,
                OutboxStatus.DEAD_LETTER.name(), OutboxStatus.PENDING.name(), rationale, null);
        return replayed;
    }

    public List<OutboxEvent> list(WorkspaceId ws, OutboxStatus status, int limit) {
        guard.enforceWorkspaceAccess(ws);
        return repository.findByWorkspace(ws, status, limit);
    }

    public Map<OutboxStatus, Long> counts(WorkspaceId ws) {
        guard.enforceWorkspaceAccess(ws);
        return repository.countByStatus(ws);
    }
}
