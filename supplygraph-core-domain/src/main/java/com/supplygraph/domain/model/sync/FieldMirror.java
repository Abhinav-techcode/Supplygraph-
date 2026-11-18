package com.supplygraph.domain.model.sync;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.connector.ConnectorId;

import java.time.Instant;

/** SupplyGraph's integration mirror of one external field: its current internal value and the value last agreed with the source. */
public record FieldMirror(WorkspaceId workspaceId, ConnectorId connectorId, String entityType, String entityKey, String field,
                          String internalValue, String lastSyncedValue, Instant updatedAt) {
    public FieldMirror withAgreedValue(String value, Instant now) {
        return new FieldMirror(workspaceId, connectorId, entityType, entityKey, field, value, value, now);
    }
    /** An internal (non-sync) change, e.g. an operator edit or validated execution observation. */
    public FieldMirror withInternalValue(String value, Instant now) {
        return new FieldMirror(workspaceId, connectorId, entityType, entityKey, field, value, lastSyncedValue, now);
    }

    /** Records that the external value was seen and deliberately not adopted, so the same value does not re-conflict. */
    public FieldMirror withAcknowledgedExternal(String external, Instant now) {
        return new FieldMirror(workspaceId, connectorId, entityType, entityKey, field, internalValue, external, now);
    }
}
