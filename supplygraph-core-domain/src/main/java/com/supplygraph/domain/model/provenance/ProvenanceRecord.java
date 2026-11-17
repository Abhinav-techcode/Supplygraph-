package com.supplygraph.domain.model.provenance;

import com.supplygraph.domain.model.WorkspaceId;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable provenance record tracking the origin, classification, and lifecycle version of any data asset.
 */
public record ProvenanceRecord(
        ProvenanceId id,
        WorkspaceId workspaceId,
        String entityId,
        String entityType,
        ProvenanceType type,
        String sourceReference,
        String stateVersion,
        Instant timestamp,
        Map<String, String> attributes
) implements Serializable {

    public ProvenanceRecord {
        Objects.requireNonNull(id, "Provenance ID cannot be null");
        Objects.requireNonNull(workspaceId, "Workspace ID cannot be null");
        Objects.requireNonNull(entityId, "Entity ID cannot be null");
        Objects.requireNonNull(entityType, "Entity type cannot be null");
        Objects.requireNonNull(type, "Provenance type cannot be null");
        Objects.requireNonNull(sourceReference, "Source reference cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        attributes = attributes != null
                ? Collections.unmodifiableMap(new HashMap<>(attributes))
                : Collections.emptyMap();
    }

    public static ProvenanceRecord of(
            WorkspaceId workspaceId,
            String entityId,
            String entityType,
            ProvenanceType type,
            String sourceReference,
            String stateVersion,
            Map<String, String> attributes
    ) {
        return new ProvenanceRecord(
                ProvenanceId.generate(),
                workspaceId,
                entityId,
                entityType,
                type,
                sourceReference,
                stateVersion != null ? stateVersion : "V1",
                Instant.now(),
                attributes
        );
    }
}

