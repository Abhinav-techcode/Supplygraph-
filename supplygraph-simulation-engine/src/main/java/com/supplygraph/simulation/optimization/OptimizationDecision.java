package com.supplygraph.simulation.optimization;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Concrete allocation or routing decision emitted by the optimization solver.
 */
public record OptimizationDecision(
        String decisionId,
        String decisionType,
        String description,
        String selectedSupplierId,
        String selectedLaneId,
        BigDecimal allocatedQuantity,
        long rescheduledLeadTimeSeconds,
        BigDecimal estimatedCost,
        double delayReductionDays
) {
    public OptimizationDecision {
        Objects.requireNonNull(decisionId, "decisionId must not be null");
        Objects.requireNonNull(decisionType, "decisionType must not be null");
        allocatedQuantity = allocatedQuantity == null ? BigDecimal.ZERO : allocatedQuantity;
        estimatedCost = estimatedCost == null ? BigDecimal.ZERO : estimatedCost;
        description = description == null ? "" : description;
    }
}

