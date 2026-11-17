package com.supplygraph.simulation.optimization;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Candidate alternative supplier or transportation lane available for recovery rerouting.
 */
public record AlternativeOption(
        String entityId,
        String name,
        BigDecimal availableCapacity,
        BigDecimal unitCost,
        long leadTimeSeconds,
        double riskScore
) {
    public AlternativeOption {
        Objects.requireNonNull(entityId, "entityId must not be null");
        name = name == null ? entityId : name;
        availableCapacity = availableCapacity == null ? BigDecimal.ZERO : availableCapacity;
        unitCost = unitCost == null ? BigDecimal.ZERO : unitCost;
    }
}

