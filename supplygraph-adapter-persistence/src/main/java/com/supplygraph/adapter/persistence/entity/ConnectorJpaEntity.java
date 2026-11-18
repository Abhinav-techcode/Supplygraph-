package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "connectors")
public class ConnectorJpaEntity {
    @Id @Column(length = 64) private String id;
    @Column(name = "workspace_id", length = 64, nullable = false) private String workspaceId;
    @Column(length = 120, nullable = false) private String name;
    @Column(length = 32, nullable = false) private String category;
    @Column(length = 32, nullable = false) private String mode;
    @Column(name = "adapter_type", length = 64, nullable = false) private String adapterType;
    @Column(length = 32, nullable = false) private String status;
    @Column(name = "base_uri", length = 512) private String baseUri;
    @Column(name = "credential_ref", length = 128) private String credentialRef;
    @Column(name = "allowed_hosts", nullable = false) private String allowedHosts = "";
    @Column(name = "timeout_millis") private Integer timeoutMillis;
    @Column(name = "max_response_bytes") private Long maxResponseBytes;
    @Column(name = "max_retries") private Integer maxRetries;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(nullable = false) private long version;

    public String getId() { return id; } public void setId(String v) { id = v; }
    public String getWorkspaceId() { return workspaceId; } public void setWorkspaceId(String v) { workspaceId = v; }
    public String getName() { return name; } public void setName(String v) { name = v; }
    public String getCategory() { return category; } public void setCategory(String v) { category = v; }
    public String getMode() { return mode; } public void setMode(String v) { mode = v; }
    public String getAdapterType() { return adapterType; } public void setAdapterType(String v) { adapterType = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public String getBaseUri() { return baseUri; } public void setBaseUri(String v) { baseUri = v; }
    public String getCredentialRef() { return credentialRef; } public void setCredentialRef(String v) { credentialRef = v; }
    public String getAllowedHosts() { return allowedHosts; } public void setAllowedHosts(String v) { allowedHosts = v; }
    public Integer getTimeoutMillis() { return timeoutMillis; } public void setTimeoutMillis(Integer v) { timeoutMillis = v; }
    public Long getMaxResponseBytes() { return maxResponseBytes; } public void setMaxResponseBytes(Long v) { maxResponseBytes = v; }
    public Integer getMaxRetries() { return maxRetries; } public void setMaxRetries(Integer v) { maxRetries = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { updatedAt = v; }
    public long getVersion() { return version; } public void setVersion(long v) { version = v; }
}
