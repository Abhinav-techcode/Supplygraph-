package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "outbox_events")
public class OutboxEventJpaEntity {
    @Id @Column(length = 64) private String id;
    @Column(name = "workspace_id", length = 64, nullable = false) private String workspaceId;
    @Column(name = "event_type", length = 96, nullable = false) private String eventType;
    @Column(name = "aggregate_id", length = 128) private String aggregateId;
    @Column(nullable = false, columnDefinition = "TEXT") private String payload;
    @Column(name = "idempotency_key", length = 160, nullable = false) private String idempotencyKey;
    @Column(name = "correlation_id", length = 64, nullable = false) private String correlationId;
    @Column(length = 16, nullable = false) private String status;
    @Column(nullable = false) private int attempts;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    @Column(name = "last_error", columnDefinition = "TEXT") private String lastError;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "replay_count", nullable = false) private int replayCount;

    public String getId() { return id; } public void setId(String v) { id = v; }
    public String getWorkspaceId() { return workspaceId; } public void setWorkspaceId(String v) { workspaceId = v; }
    public String getEventType() { return eventType; } public void setEventType(String v) { eventType = v; }
    public String getAggregateId() { return aggregateId; } public void setAggregateId(String v) { aggregateId = v; }
    public String getPayload() { return payload; } public void setPayload(String v) { payload = v; }
    public String getIdempotencyKey() { return idempotencyKey; } public void setIdempotencyKey(String v) { idempotencyKey = v; }
    public String getCorrelationId() { return correlationId; } public void setCorrelationId(String v) { correlationId = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public int getAttempts() { return attempts; } public void setAttempts(int v) { attempts = v; }
    public Instant getNextAttemptAt() { return nextAttemptAt; } public void setNextAttemptAt(Instant v) { nextAttemptAt = v; }
    public String getLastError() { return lastError; } public void setLastError(String v) { lastError = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { updatedAt = v; }
    public int getReplayCount() { return replayCount; } public void setReplayCount(int v) { replayCount = v; }
}
