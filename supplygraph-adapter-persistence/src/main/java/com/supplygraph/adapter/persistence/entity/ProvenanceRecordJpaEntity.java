package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "provenance_records")
public class ProvenanceRecordJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "entity_id", length = 128, nullable = false)
    private String entityId;

    @Column(name = "entity_type", length = 64, nullable = false)
    private String entityType;

    @Column(name = "provenance_type", length = 64, nullable = false)
    private String provenanceType;

    @Column(name = "source_reference", length = 256, nullable = false)
    private String sourceReference;

    @Column(name = "state_version", length = 32, nullable = false)
    private String stateVersion;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "attributes_json", columnDefinition = "TEXT")
    private String attributesJson;

    protected ProvenanceRecordJpaEntity() {}

    public ProvenanceRecordJpaEntity(
            String id,
            String workspaceId,
            String entityId,
            String entityType,
            String provenanceType,
            String sourceReference,
            String stateVersion,
            Instant timestamp,
            String attributesJson
    ) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.entityId = entityId;
        this.entityType = entityType;
        this.provenanceType = provenanceType;
        this.sourceReference = sourceReference;
        this.stateVersion = stateVersion;
        this.timestamp = timestamp;
        this.attributesJson = attributesJson;
    }

    public String getId() { return id; }
    public String getWorkspaceId() { return workspaceId; }
    public String getEntityId() { return entityId; }
    public String getEntityType() { return entityType; }
    public String getProvenanceType() { return provenanceType; }
    public String getSourceReference() { return sourceReference; }
    public String getStateVersion() { return stateVersion; }
    public Instant getTimestamp() { return timestamp; }
    public String getAttributesJson() { return attributesJson; }
}

