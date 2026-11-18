package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "decision_provenance_nodes")
public class DecisionProvenanceNodeJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "disruption_id", length = 64, nullable = false)
    private String disruptionId;

    @Column(name = "node_type", length = 32, nullable = false)
    private String nodeType;

    @Column(name = "label", length = 255, nullable = false)
    private String label;

    @Column(name = "entity_reference_id", length = 64)
    private String entityReferenceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    protected DecisionProvenanceNodeJpaEntity() {}

    public DecisionProvenanceNodeJpaEntity(
            String id, String workspaceId, String disruptionId, String nodeType,
            String label, String entityReferenceId, Instant createdAt, String metadataJson
    ) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.disruptionId = disruptionId;
        this.nodeType = nodeType;
        this.label = label;
        this.entityReferenceId = entityReferenceId;
        this.createdAt = createdAt;
        this.metadataJson = metadataJson;
    }

    public String getId() { return id; }
    public String getWorkspaceId() { return workspaceId; }
    public String getDisruptionId() { return disruptionId; }
    public String getNodeType() { return nodeType; }
    public String getLabel() { return label; }
    public String getEntityReferenceId() { return entityReferenceId; }
    public Instant getCreatedAt() { return createdAt; }
    public String getMetadataJson() { return metadataJson; }
}

