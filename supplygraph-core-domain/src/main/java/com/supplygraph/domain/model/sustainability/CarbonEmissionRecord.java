package com.supplygraph.domain.model.sustainability;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.graph.NodeId;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable carbon emission activity record mapped to GHG Protocol scopes.
 */
public record CarbonEmissionRecord(
        CarbonEmissionRecordId id,
        WorkspaceId workspaceId,
        NodeId facilityOrLaneId,
        EmissionScope scope,
        String activityCategory,
        double activityAmount,
        String unitOfMeasure,
        double emissionFactorKgCo2e,
        double totalEmissionsKgCo2e,
        Instant recordedAt
) implements Serializable {
    public CarbonEmissionRecord {
        Objects.requireNonNull(id, "Carbon record ID cannot be null");
        Objects.requireNonNull(workspaceId, "Workspace ID cannot be null");
        Objects.requireNonNull(facilityOrLaneId, "Facility or lane ID cannot be null");
        Objects.requireNonNull(scope, "Emission scope cannot be null");
        Objects.requireNonNull(activityCategory, "Activity category cannot be null");
        Objects.requireNonNull(unitOfMeasure, "Unit of measure cannot be null");
        Objects.requireNonNull(recordedAt, "Recorded timestamp cannot be null");
        if (activityAmount < 0 || emissionFactorKgCo2e < 0 || totalEmissionsKgCo2e < 0) {
            throw new IllegalArgumentException("Emission activity parameters cannot be negative");
        }
    }

    public double getTotalEmissionsTons() {
        return totalEmissionsKgCo2e / 1000.0;
    }
}

