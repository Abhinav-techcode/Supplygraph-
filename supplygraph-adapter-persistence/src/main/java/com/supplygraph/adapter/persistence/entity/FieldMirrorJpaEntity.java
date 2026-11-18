package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "field_mirrors")
public class FieldMirrorJpaEntity {
    @Id @Column(length = 64) private String id;
    @Column(name = "workspace_id", length = 64, nullable = false) private String workspaceId;
    @Column(name = "connector_id", length = 64, nullable = false) private String connectorId;
    @Column(name = "entity_type", length = 64, nullable = false) private String entityType;
    @Column(name = "entity_key", length = 128, nullable = false) private String entityKey;
    @Column(name = "field_name", length = 64, nullable = false) private String fieldName;
    @Column(name = "internal_value", columnDefinition = "TEXT") private String internalValue;
    @Column(name = "last_synced_value", columnDefinition = "TEXT") private String lastSyncedValue;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    public String getId() { return id; } public void setId(String v) { id = v; }
    public String getWorkspaceId() { return workspaceId; } public void setWorkspaceId(String v) { workspaceId = v; }
    public String getConnectorId() { return connectorId; } public void setConnectorId(String v) { connectorId = v; }
    public String getEntityType() { return entityType; } public void setEntityType(String v) { entityType = v; }
    public String getEntityKey() { return entityKey; } public void setEntityKey(String v) { entityKey = v; }
    public String getFieldName() { return fieldName; } public void setFieldName(String v) { fieldName = v; }
    public String getInternalValue() { return internalValue; } public void setInternalValue(String v) { internalValue = v; }
    public String getLastSyncedValue() { return lastSyncedValue; } public void setLastSyncedValue(String v) { lastSyncedValue = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { updatedAt = v; }
}
