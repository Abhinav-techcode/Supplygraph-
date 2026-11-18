package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sg_genealogy_dags")
public class GenealogyDagJpaEntity {

    @Id
    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "nodes_json", columnDefinition = "TEXT", nullable = false)
    private String nodesJson;

    @Column(name = "edges_json", columnDefinition = "TEXT", nullable = false)
    private String edgesJson;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public GenealogyDagJpaEntity() {}

    public GenealogyDagJpaEntity(String workspaceId, String nodesJson, String edgesJson, Instant updatedAt) {
        this.workspaceId = workspaceId;
        this.nodesJson = nodesJson;
        this.edgesJson = edgesJson;
        this.updatedAt = updatedAt;
    }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getNodesJson() { return nodesJson; }
    public void setNodesJson(String nodesJson) { this.nodesJson = nodesJson; }
    public String getEdgesJson() { return edgesJson; }
    public void setEdgesJson(String edgesJson) { this.edgesJson = edgesJson; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

