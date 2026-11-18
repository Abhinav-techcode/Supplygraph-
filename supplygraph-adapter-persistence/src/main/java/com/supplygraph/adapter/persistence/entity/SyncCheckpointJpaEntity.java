package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "sync_checkpoints")
public class SyncCheckpointJpaEntity {
    @Id @Column(name = "connector_id", length = 64) private String connectorId;
    @Column(name = "cursor_value") private String cursorValue;
    @Column(name = "last_synced_at") private Instant lastSyncedAt;
    @Column(name = "sync_count", nullable = false) private long syncCount;
    public String getConnectorId() { return connectorId; } public void setConnectorId(String v) { connectorId = v; }
    public String getCursorValue() { return cursorValue; } public void setCursorValue(String v) { cursorValue = v; }
    public Instant getLastSyncedAt() { return lastSyncedAt; } public void setLastSyncedAt(Instant v) { lastSyncedAt = v; }
    public long getSyncCount() { return syncCount; } public void setSyncCount(long v) { syncCount = v; }
}
