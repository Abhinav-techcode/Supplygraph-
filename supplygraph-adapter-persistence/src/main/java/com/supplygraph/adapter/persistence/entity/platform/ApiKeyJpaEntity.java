package com.supplygraph.adapter.persistence.entity.platform;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sg_api_keys")
public class ApiKeyJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "key_name", length = 128, nullable = false)
    private String keyName;

    @Column(name = "hashed_token", length = 128, nullable = false, unique = true)
    private String hashedToken;

    @Column(name = "scopes_csv", length = 256, nullable = false)
    private String scopesCsv;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ApiKeyJpaEntity() {}

    public ApiKeyJpaEntity(String id, String workspaceId, String keyName, String hashedToken, String scopesCsv, boolean active, Instant createdAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.keyName = keyName;
        this.hashedToken = hashedToken;
        this.scopesCsv = scopesCsv;
        this.active = active;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getKeyName() { return keyName; }
    public void setKeyName(String keyName) { this.keyName = keyName; }
    public String getHashedToken() { return hashedToken; }
    public void setHashedToken(String hashedToken) { this.hashedToken = hashedToken; }
    public String getScopesCsv() { return scopesCsv; }
    public void setScopesCsv(String scopesCsv) { this.scopesCsv = scopesCsv; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

