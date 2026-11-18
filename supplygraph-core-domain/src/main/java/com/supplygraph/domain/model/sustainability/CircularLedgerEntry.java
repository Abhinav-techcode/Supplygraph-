package com.supplygraph.domain.model.sustainability;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.MaterialId;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Closed-loop recycling and circular economy transaction entry.
 */
public record CircularLedgerEntry(
        CircularLedgerEntryId id,
        WorkspaceId workspaceId,
        MaterialId materialId,
        CircularFlowType flowType,
        double intakeQuantity,
        double recoveryYieldPercentage,
        double recoveredQuantity,
        double virginMaterialAvoidedKg,
        Instant recordedAt
) implements Serializable {
    public CircularLedgerEntry {
        Objects.requireNonNull(id, "Circular entry ID cannot be null");
        Objects.requireNonNull(workspaceId, "Workspace ID cannot be null");
        Objects.requireNonNull(materialId, "Material ID cannot be null");
        Objects.requireNonNull(flowType, "Flow type cannot be null");
        Objects.requireNonNull(recordedAt, "Recorded timestamp cannot be null");
        if (intakeQuantity < 0 || recoveryYieldPercentage < 0 || recoveryYieldPercentage > 100) {
            throw new IllegalArgumentException("Invalid circular flow quantities");
        }
    }
}

