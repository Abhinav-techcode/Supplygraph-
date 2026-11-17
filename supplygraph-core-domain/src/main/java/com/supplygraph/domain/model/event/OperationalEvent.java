package com.supplygraph.domain.model.event;

import com.supplygraph.domain.model.WorkspaceId;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Authoritative domain model representing an operational supply chain event.
 * Captures external telemetry, workflow milestones, and environment changes.
 */
public class OperationalEvent {

    private final EventId eventId;
    private final EventType eventType;
    private final EventSource source;
    private final Instant eventTimestamp;
    private final Instant receivedTimestamp;
    private final String affectedEntityId;
    private final String affectedEntityType;
    private final WorkspaceId workspaceId;
    private final Map<String, String> payload;
    private final EventProvenance provenance;
    private final int schemaVersion;
    private final String idempotencyKey;
    private EventStatus status;
    private EventRejectionReason rejectionReason;

    public OperationalEvent(
            EventId eventId,
            EventType eventType,
            EventSource source,
            Instant eventTimestamp,
            Instant receivedTimestamp,
            String affectedEntityId,
            String affectedEntityType,
            WorkspaceId workspaceId,
            Map<String, String> payload,
            EventProvenance provenance,
            int schemaVersion,
            String idempotencyKey,
            EventStatus status,
            EventRejectionReason rejectionReason
    ) {
        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.source = Objects.requireNonNull(source, "source must not be null");
        this.eventTimestamp = Objects.requireNonNull(eventTimestamp, "eventTimestamp must not be null");
        this.receivedTimestamp = receivedTimestamp != null ? receivedTimestamp : Instant.now();
        this.affectedEntityId = Objects.requireNonNull(affectedEntityId, "affectedEntityId must not be null");
        this.affectedEntityType = Objects.requireNonNull(affectedEntityType, "affectedEntityType must not be null");
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        this.payload = payload != null ? Map.copyOf(payload) : Collections.emptyMap();
        this.provenance = Objects.requireNonNull(provenance, "provenance must not be null");
        this.schemaVersion = schemaVersion <= 0 ? 1 : schemaVersion;
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        this.status = status != null ? status : EventStatus.INGESTED;
        this.rejectionReason = rejectionReason != null ? rejectionReason : EventRejectionReason.NONE;
    }

    public static OperationalEvent createIngested(
            EventType eventType,
            EventSource source,
            Instant eventTimestamp,
            String affectedEntityId,
            String affectedEntityType,
            WorkspaceId workspaceId,
            Map<String, String> payload,
            EventProvenance provenance,
            String idempotencyKey
    ) {
        return new OperationalEvent(
                EventId.generate(),
                eventType,
                source,
                eventTimestamp,
                Instant.now(),
                affectedEntityId,
                affectedEntityType,
                workspaceId,
                payload,
                provenance,
                1,
                idempotencyKey,
                EventStatus.INGESTED,
                EventRejectionReason.NONE
        );
    }

    public void markValidated() {
        if (this.status == EventStatus.REJECTED) {
            throw new IllegalStateException("Cannot validate a previously rejected event");
        }
        this.status = EventStatus.VALIDATED;
    }

    public void markProcessed() {
        this.status = EventStatus.PROCESSED;
    }

    public void markRejected(EventRejectionReason reason) {
        this.status = EventStatus.REJECTED;
        this.rejectionReason = Objects.requireNonNull(reason, "rejectionReason must not be null");
    }

    public void markDuplicate() {
        this.status = EventStatus.DUPLICATE;
        this.rejectionReason = EventRejectionReason.DUPLICATE_IDEMPOTENCY_KEY;
    }

    public EventId getEventId() {
        return eventId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public EventSource getSource() {
        return source;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public Instant getReceivedTimestamp() {
        return receivedTimestamp;
    }

    public String getAffectedEntityId() {
        return affectedEntityId;
    }

    public String getAffectedEntityType() {
        return affectedEntityType;
    }

    public WorkspaceId getWorkspaceId() {
        return workspaceId;
    }

    public Map<String, String> getPayload() {
        return payload;
    }

    public EventProvenance getProvenance() {
        return provenance;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public EventStatus getStatus() {
        return status;
    }

    public EventRejectionReason getRejectionReason() {
        return rejectionReason;
    }
}

