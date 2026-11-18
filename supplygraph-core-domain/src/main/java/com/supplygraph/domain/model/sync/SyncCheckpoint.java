package com.supplygraph.domain.model.sync;

import com.supplygraph.domain.model.connector.ConnectorId;

import java.time.Instant;

public record SyncCheckpoint(ConnectorId connectorId, String cursor, Instant lastSyncedAt, long syncCount) {
    public SyncCheckpoint advance(String newCursor, Instant now) { return new SyncCheckpoint(connectorId, newCursor, now, syncCount + 1); }
    public boolean isInitial() { return lastSyncedAt == null; }
}
