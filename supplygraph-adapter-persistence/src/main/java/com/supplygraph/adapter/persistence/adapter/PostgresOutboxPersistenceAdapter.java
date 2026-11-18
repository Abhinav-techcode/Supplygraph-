package com.supplygraph.adapter.persistence.adapter;

import com.supplygraph.adapter.persistence.entity.OutboxEventJpaEntity;
import com.supplygraph.adapter.persistence.repository.SpringDataInboxRepository;
import com.supplygraph.adapter.persistence.repository.SpringDataOutboxRepository;
import com.supplygraph.application.port.out.InboxRepositoryPort;
import com.supplygraph.application.port.out.OutboxRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.outbox.OutboxEvent;
import com.supplygraph.domain.model.outbox.OutboxStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Component
public class PostgresOutboxPersistenceAdapter implements OutboxRepositoryPort, InboxRepositoryPort {
    private final SpringDataOutboxRepository repository;
    private final SpringDataInboxRepository inbox;

    public PostgresOutboxPersistenceAdapter(SpringDataOutboxRepository repository, SpringDataInboxRepository inbox) {
        this.repository = repository;
        this.inbox = inbox;
    }

    /** Joins the caller's transaction when present so the event commits atomically with the business change. */
    @Override @Transactional(propagation = Propagation.REQUIRED)
    public OutboxEvent insertIfAbsent(OutboxEvent e) {
        repository.insertIfAbsent(e.id(), e.workspaceId().value(), e.eventType(), e.aggregateId(), e.payload(), e.idempotencyKey(),
                e.correlationId(), e.status().name(), e.attempts(), e.nextAttemptAt(), e.lastError(), e.createdAt(), e.updatedAt(), e.replayCount());
        return repository.findByWorkspaceIdAndIdempotencyKey(e.workspaceId().value(), e.idempotencyKey()).map(this::toDomain).orElseThrow();
    }

    @Override @Transactional
    public OutboxEvent save(OutboxEvent e) {
        OutboxEventJpaEntity entity = repository.findById(e.id()).orElseGet(OutboxEventJpaEntity::new);
        entity.setId(e.id());
        entity.setWorkspaceId(e.workspaceId().value());
        entity.setEventType(e.eventType());
        entity.setAggregateId(e.aggregateId());
        entity.setPayload(e.payload());
        entity.setIdempotencyKey(e.idempotencyKey());
        entity.setCorrelationId(e.correlationId());
        entity.setStatus(e.status().name());
        entity.setAttempts(e.attempts());
        entity.setNextAttemptAt(e.nextAttemptAt());
        entity.setLastError(e.lastError());
        entity.setCreatedAt(e.createdAt());
        entity.setUpdatedAt(e.updatedAt());
        entity.setReplayCount(e.replayCount());
        return toDomain(repository.save(entity));
    }

    @Override @Transactional(readOnly = true)
    public Optional<OutboxEvent> findById(String id) { return repository.findById(id).map(this::toDomain); }

    @Override @Transactional(readOnly = true)
    public List<OutboxEvent> findByWorkspace(WorkspaceId ws, OutboxStatus status, int limit) {
        PageRequest page = PageRequest.of(0, Math.min(Math.max(limit, 1), 500));
        return (status == null ? repository.findByWorkspaceIdOrderByCreatedAtDesc(ws.value(), page)
                : repository.findByWorkspaceIdAndStatusOrderByCreatedAtDesc(ws.value(), status.name(), page))
                .stream().map(this::toDomain).toList();
    }

    @Override @Transactional
    public List<OutboxEvent> claimDue(Instant now, int limit) {
        List<OutboxEvent> claimed = new ArrayList<>();
        for (OutboxEventJpaEntity row : repository.lockDue(now, limit)) {
            OutboxEvent inFlight = toDomain(row).markInFlight(now);
            row.setStatus(inFlight.status().name());
            row.setAttempts(inFlight.attempts());
            row.setUpdatedAt(now);
            claimed.add(inFlight);
        }
        return claimed;
    }

    @Override @Transactional
    public int recoverStale(Instant staleBefore, Instant now) { return repository.recoverStale(staleBefore, now); }

    @Override @Transactional(readOnly = true)
    public Map<OutboxStatus, Long> countByStatus(WorkspaceId ws) {
        Map<OutboxStatus, Long> result = new EnumMap<>(OutboxStatus.class);
        for (OutboxStatus s : OutboxStatus.values()) result.put(s, 0L);
        for (Object[] row : repository.countByStatus(ws.value())) result.put(OutboxStatus.valueOf((String) row[0]), (Long) row[1]);
        return result;
    }

    @Override @Transactional(readOnly = true)
    public Map<OutboxStatus, Long> countAllByStatus() {
        Map<OutboxStatus, Long> result = new EnumMap<>(OutboxStatus.class);
        for (OutboxStatus s : OutboxStatus.values()) result.put(s, 0L);
        for (Object[] row : repository.countAllByStatus()) result.put(OutboxStatus.valueOf((String) row[0]), (Long) row[1]);
        return result;
    }

    @Override @Transactional(propagation = Propagation.REQUIRED)
    public boolean registerIfAbsent(WorkspaceId ws, String consumer, String messageId, Instant now) {
        return inbox.registerIfAbsent(ws.value(), consumer, messageId, now) == 1;
    }

    private OutboxEvent toDomain(OutboxEventJpaEntity e) {
        return new OutboxEvent(e.getId(), WorkspaceId.of(e.getWorkspaceId()), e.getEventType(), e.getAggregateId(), e.getPayload(),
                e.getIdempotencyKey(), e.getCorrelationId(), OutboxStatus.valueOf(e.getStatus()), e.getAttempts(),
                e.getNextAttemptAt(), e.getLastError(), e.getCreatedAt(), e.getUpdatedAt(), e.getReplayCount());
    }
}
