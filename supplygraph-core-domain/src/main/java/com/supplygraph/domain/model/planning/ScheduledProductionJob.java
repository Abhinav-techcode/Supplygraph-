package com.supplygraph.domain.model.planning;

import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.NodeId;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Scheduled manufacturing job placed onto a specific finite capacity work center and shift.
 */
public record ScheduledProductionJob(
        String jobId,
        MaterialId productMaterialId,
        NodeId workCenterId,
        LocalDate scheduledDate,
        int shiftNumber,
        double plannedQuantity,
        double changeoverHours,
        double runHours,
        int sequenceNumber
) implements Serializable {
    public ScheduledProductionJob {
        Objects.requireNonNull(jobId, "Job ID cannot be null");
        Objects.requireNonNull(productMaterialId, "Product material ID cannot be null");
        Objects.requireNonNull(workCenterId, "Work center ID cannot be null");
        Objects.requireNonNull(scheduledDate, "Scheduled date cannot be null");
        if (shiftNumber < 1 || shiftNumber > 4) {
            throw new IllegalArgumentException("Shift number must be between 1 and 4");
        }
        if (plannedQuantity <= 0) {
            throw new IllegalArgumentException("Planned quantity must be positive");
        }
    }

    public double getTotalOccupiedHours() {
        return changeoverHours + runHours;
    }
}
