package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "decision_provenance_edges")
public class DecisionProvenanceEdgeJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "disruption_id", length = 64, nullable = false)
    private String disruptionId;

    @Column(name = "source_node_id", length = 64, nullable = false)
    private String sourceNodeId;

    @Column(name = "target_node_id", length = 64, nullable = false)
    private String targetNodeId;

    @Column(name = "edge_type", length = 32, nullable = false)
    private String edgeType;

    @Column(name = "explanation", columnDefinition = "TEXT", nullable = false)
    private String explanation;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DecisionProvenanceEdgeJpaEntity() {}

    public DecisionProvenanceEdgeJpaEntity(
            String id, String workspaceId, String disruptionId, String sourceNodeId,
            String targetNodeId, String edgeType, String explanation, Instant createdAt
    ) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.disruptionId = disruptionId;
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.edgeType = edgeType;
        this.explanation = explanation;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getWorkspaceId() { return workspaceId; }
    public String getDisruptionId() { return disruptionId; }
    public String getSourceNodeId() { return sourceNodeId; }
    public String getTargetNodeId() { return targetNodeId; }
    public String getEdgeType() { return edgeType; }
    public String getExplanation() { return explanation; }
    public Instant getCreatedAt() { return createdAt; }
}
