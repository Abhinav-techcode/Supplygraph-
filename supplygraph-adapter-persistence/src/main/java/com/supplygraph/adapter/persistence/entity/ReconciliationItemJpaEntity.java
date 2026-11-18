package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "reconciliation_items")
public class ReconciliationItemJpaEntity {
    @Id @Column(length = 64) private String id;
    @Column(name = "workspace_id", length = 64, nullable = false) private String workspaceId;
    @Column(name = "connector_id", length = 64, nullable = false) private String connectorId;
    @Column(name = "entity_type", length = 64, nullable = false) private String entityType;
    @Column(name = "entity_key", length = 128, nullable = false) private String entityKey;
    @Column(name = "field_name", length = 64, nullable = false) private String fieldName;
    @Column(name = "external_value", columnDefinition = "TEXT") private String externalValue;
    @Column(name = "internal_value", columnDefinition = "TEXT") private String internalValue;
    @Column(name = "last_synced_value", columnDefinition = "TEXT") private String lastSyncedValue;
    @Column(length = 24, nullable = false) private String status;
    @Column(name = "resolved_by", length = 64) private String resolvedBy;
    @Column(name = "resolution_rationale", columnDefinition = "TEXT") private String resolutionRationale;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "resolved_at") private Instant resolvedAt;
    @Column(name = "correlation_id", length = 64) private String correlationId;
    @Column(nullable = false) private boolean simulated;
    public String getId() { return id; } public void setId(String v) { id = v; }
    public String getWorkspaceId() { return workspaceId; } public void setWorkspaceId(String v) { workspaceId = v; }
    public String getConnectorId() { return connectorId; } public void setConnectorId(String v) { connectorId = v; }
    public String getEntityType() { return entityType; } public void setEntityType(String v) { entityType = v; }
    public String getEntityKey() { return entityKey; } public void setEntityKey(String v) { entityKey = v; }
    public String getFieldName() { return fieldName; } public void setFieldName(String v) { fieldName = v; }
    public String getExternalValue() { return externalValue; } public void setExternalValue(String v) { externalValue = v; }
    public String getInternalValue() { return internalValue; } public void setInternalValue(String v) { internalValue = v; }
    public String getLastSyncedValue() { return lastSyncedValue; } public void setLastSyncedValue(String v) { lastSyncedValue = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public String getResolvedBy() { return resolvedBy; } public void setResolvedBy(String v) { resolvedBy = v; }
    public String getResolutionRationale() { return resolutionRationale; } public void setResolutionRationale(String v) { resolutionRationale = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt = v; }
    public Instant getResolvedAt() { return resolvedAt; } public void setResolvedAt(Instant v) { resolvedAt = v; }
    public String getCorrelationId() { return correlationId; } public void setCorrelationId(String v) { correlationId = v; }
    public boolean isSimulated() { return simulated; } public void setSimulated(boolean v) { simulated = v; }
}
