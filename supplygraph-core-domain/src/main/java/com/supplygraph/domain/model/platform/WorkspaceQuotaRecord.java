package com.supplygraph.domain.model.platform;

import com.supplygraph.domain.model.WorkspaceId;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public record WorkspaceQuotaRecord(
        WorkspaceQuotaId id,
        WorkspaceId workspaceId,
        int maxNodes,
        int maxActiveDisruptions,
        int maxConcurrentSimulations,
        boolean enabled,
        Instant updatedAt
) implements Serializable {
    public WorkspaceQuotaRecord {
        Objects.requireNonNull(id, "WorkspaceQuotaId cannot be null");
        Objects.requireNonNull(workspaceId, "WorkspaceId cannot be null");
        Objects.requireNonNull(updatedAt, "UpdatedAt cannot be null");
        if (maxNodes < 0 || maxActiveDisruptions < 0 || maxConcurrentSimulations < 0) {
            throw new IllegalArgumentException("Quota limits must be non-negative");
        }
    }

    public WorkspaceQuotaRecord withEnabled(boolean newEnabled) {
        return new WorkspaceQuotaRecord(id, workspaceId, maxNodes, maxActiveDisruptions, maxConcurrentSimulations, newEnabled, Instant.now());
    }

    public WorkspaceQuotaRecord withLimits(int newMaxNodes, int newMaxActiveDisruptions, int newMaxConcurrentSimulations) {
        return new WorkspaceQuotaRecord(id, workspaceId, newMaxNodes, newMaxActiveDisruptions, newMaxConcurrentSimulations, enabled, Instant.now());
    }
}

