package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "operational_events")
public class OperationalEventJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "event_type", length = 64, nullable = false)
    private String eventType;

    @Column(name = "source", length = 64, nullable = false)
    private String source;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @Column(name = "received_timestamp", nullable = false)
    private Instant receivedTimestamp;

    @Column(name = "affected_entity_id", length = 128, nullable = false)
    private String affectedEntityId;

    @Column(name = "affected_entity_type", length = 64, nullable = false)
    private String affectedEntityType;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "payload_json", columnDefinition = "TEXT", nullable = false)
    private String payloadJson;

    @Column(name = "provenance_json", columnDefinition = "TEXT", nullable = false)
    private String provenanceJson;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    @Column(name = "idempotency_key", length = 128, nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "rejection_reason", length = 64, nullable = false)
    private String rejectionReason;

    protected OperationalEventJpaEntity() {}

    public OperationalEventJpaEntity(
            String id, String eventType, String source,
            Instant eventTimestamp, Instant receivedTimestamp,
            String affectedEntityId, String affectedEntityType,
            String workspaceId, String payloadJson, String provenanceJson,
            int schemaVersion, String idempotencyKey, String status, String rejectionReason
    ) {
        this.id = id;
        this.eventType = eventType;
        this.source = source;
        this.eventTimestamp = eventTimestamp;
        this.receivedTimestamp = receivedTimestamp;
        this.affectedEntityId = affectedEntityId;
        this.affectedEntityType = affectedEntityType;
        this.workspaceId = workspaceId;
        this.payloadJson = payloadJson;
        this.provenanceJson = provenanceJson;
        this.schemaVersion = schemaVersion;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.rejectionReason = rejectionReason;
    }

    public String getId() { return id; }
    public String getEventType() { return eventType; }
    public String getSource() { return source; }
    public Instant getEventTimestamp() { return eventTimestamp; }
    public Instant getReceivedTimestamp() { return receivedTimestamp; }
    public String getAffectedEntityId() { return affectedEntityId; }
    public String getAffectedEntityType() { return affectedEntityType; }
    public String getWorkspaceId() { return workspaceId; }
    public String getPayloadJson() { return payloadJson; }
    public String getProvenanceJson() { return provenanceJson; }
    public int getSchemaVersion() { return schemaVersion; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
}

