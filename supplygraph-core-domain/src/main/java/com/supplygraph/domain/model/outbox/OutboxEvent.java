package com.supplygraph.domain.model.outbox;

import com.supplygraph.domain.model.WorkspaceId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Transactional outbox record. Written in the same transaction as the business change.
 * State machine: PENDING -> IN_FLIGHT -> DELIVERED | PENDING(retry, backoff) | DEAD_LETTER; DEAD_LETTER -> PENDING only via authorized replay.
 */
public record OutboxEvent(
        String id,
        WorkspaceId workspaceId,
        String eventType,
        String aggregateId,
        String payload,
        String idempotencyKey,
        String correlationId,
        OutboxStatus status,
        int attempts,
        Instant nextAttemptAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt,
        int replayCount
) {
    public OutboxEvent {
        Objects.requireNonNull(id); Objects.requireNonNull(workspaceId); Objects.requireNonNull(status);
        Objects.requireNonNull(nextAttemptAt); Objects.requireNonNull(createdAt); Objects.requireNonNull(updatedAt);
        if (eventType == null || eventType.isBlank()) throw new IllegalArgumentException("eventType required");
        if (idempotencyKey == null || idempotencyKey.isBlank()) throw new IllegalArgumentException("idempotencyKey required");
        if (payload == null) payload = "";
    }

    public static OutboxEvent create(WorkspaceId ws, String eventType, String aggregateId, String payload, String idempotencyKey, String correlationId) {
        Instant now = Instant.now();
        return new OutboxEvent(UUID.randomUUID().toString(), ws, eventType, aggregateId, payload, idempotencyKey,
                correlationId != null ? correlationId : UUID.randomUUID().toString(),
                OutboxStatus.PENDING, 0, now, null, now, now, 0);
    }

    public OutboxEvent markInFlight(Instant now) {
        require(OutboxStatus.PENDING);
        return copy(OutboxStatus.IN_FLIGHT, attempts + 1, nextAttemptAt, lastError, now, replayCount);
    }

    public OutboxEvent markDelivered(Instant now) {
        require(OutboxStatus.IN_FLIGHT);
        return copy(OutboxStatus.DELIVERED, attempts, nextAttemptAt, null, now, replayCount);
    }

    /** Retryable failure: reschedules with backoff, or dead-letters once attempts are exhausted. */
    public OutboxEvent markFailed(String error, boolean retryable, BackoffPolicy policy, Instant now) {
        require(OutboxStatus.IN_FLIGHT);
        if (!retryable || attempts >= policy.maxAttempts()) {
            return copy(OutboxStatus.DEAD_LETTER, attempts, nextAttemptAt, error, now, replayCount);
        }
        return copy(OutboxStatus.PENDING, attempts, now.plus(policy.delayForAttempt(attempts)), error, now, replayCount);
    }

    /** Authorized replay of a dead-lettered event; resets attempts, keeps the idempotency key so consumers dedupe. */
    public OutboxEvent replay(Instant now) {
        require(OutboxStatus.DEAD_LETTER);
        return copy(OutboxStatus.PENDING, 0, now, lastError, now, replayCount + 1);
    }

    /** Recovers an event stranded IN_FLIGHT by a crashed worker. */
    public OutboxEvent recoverStale(Instant now) {
        require(OutboxStatus.IN_FLIGHT);
        return copy(OutboxStatus.PENDING, attempts, now, "Recovered after worker interruption", now, replayCount);
    }

    private void require(OutboxStatus expected) {
        if (status != expected) throw new IllegalStateException("Outbox event " + id + " is " + status + ", expected " + expected);
    }

    private OutboxEvent copy(OutboxStatus s, int a, Instant next, String err, Instant now, int replays) {
        return new OutboxEvent(id, workspaceId, eventType, aggregateId, payload, idempotencyKey, correlationId, s, a, next, err, createdAt, now, replays);
    }
}
