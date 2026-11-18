package com.supplygraph.adapter.persistence.entity.platform;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sg_workspace_quotas")
public class WorkspaceQuotaJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false, unique = true)
    private String workspaceId;

    @Column(name = "max_nodes", nullable = false)
    private int maxNodes;

    @Column(name = "max_active_disruptions", nullable = false)
    private int maxActiveDisruptions;

    @Column(name = "max_concurrent_simulations", nullable = false)
    private int maxConcurrentSimulations;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public WorkspaceQuotaJpaEntity() {}

    public WorkspaceQuotaJpaEntity(String id, String workspaceId, int maxNodes, int maxActiveDisruptions, int maxConcurrentSimulations, boolean enabled, Instant updatedAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.maxNodes = maxNodes;
        this.maxActiveDisruptions = maxActiveDisruptions;
        this.maxConcurrentSimulations = maxConcurrentSimulations;
        this.enabled = enabled;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public int getMaxNodes() { return maxNodes; }
    public void setMaxNodes(int maxNodes) { this.maxNodes = maxNodes; }
    public int getMaxActiveDisruptions() { return maxActiveDisruptions; }
    public void setMaxActiveDisruptions(int maxActiveDisruptions) { this.maxActiveDisruptions = maxActiveDisruptions; }
    public int getMaxConcurrentSimulations() { return maxConcurrentSimulations; }
    public void setMaxConcurrentSimulations(int maxConcurrentSimulations) { this.maxConcurrentSimulations = maxConcurrentSimulations; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

