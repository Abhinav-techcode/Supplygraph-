package com.supplygraph.domain.model.sync;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.connector.ConnectorId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Record-level reconciliation outcome. {@code simulated} marks items produced by sandbox connectors. */
public record ReconciliationItem(
        String id, WorkspaceId workspaceId, ConnectorId connectorId, String entityType, String entityKey, String field,
        String externalValue, String internalValue, String lastSyncedValue, ReconciliationStatus status,
        String resolvedBy, String resolutionRationale, Instant createdAt, Instant resolvedAt, String correlationId, boolean simulated
) {
    public ReconciliationItem {
        Objects.requireNonNull(id); Objects.requireNonNull(workspaceId); Objects.requireNonNull(connectorId);
        Objects.requireNonNull(status); Objects.requireNonNull(createdAt);
    }

    public static ReconciliationItem create(WorkspaceId ws, ConnectorId cid, String type, String key, String field, String external,
                                            String internal, String lastSynced, ReconciliationStatus status, String correlationId, boolean simulated) {
        Instant now = Instant.now();
        boolean closed = !status.isOpen();
        return new ReconciliationItem(UUID.randomUUID().toString(), ws, cid, type, key, field, external, internal, lastSynced, status,
                closed ? "SYSTEM" : null, closed ? "Automatic decision by field authority policy" : null, now, closed ? now : null, correlationId, simulated);
    }

    /** Human resolution of an open conflict. */
    public ReconciliationItem resolve(boolean acceptExternal, String user, String rationale, Instant now) {
        if (!status.isOpen()) throw new IllegalStateException("Reconciliation item " + id + " is already " + status);
        if (user == null || user.isBlank()) throw new IllegalArgumentException("resolver required");
        if (rationale == null || rationale.isBlank()) throw new IllegalArgumentException("rationale required");
        return new ReconciliationItem(id, workspaceId, connectorId, entityType, entityKey, field, externalValue, internalValue, lastSyncedValue,
                acceptExternal ? ReconciliationStatus.ACCEPTED : ReconciliationStatus.REJECTED, user, rationale, createdAt, now, correlationId, simulated);
    }
}
