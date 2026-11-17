package com.supplygraph.domain.model.plan;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Normalized comparison metrics across alternative recovery plans.
 */
public record PlanComparisonMetrics(
        BigDecimal totalCost,
        double recoveryDurationDays,
        double serviceLevelImpactPercentage,
        BigDecimal unmetDemandQuantity,
        BigDecimal inventoryConsumedQuantity,
        double capacityUtilizationPercentage,
        double operationalRiskScore,
        double robustnessScore
) {
    public PlanComparisonMetrics {
        totalCost = totalCost == null ? BigDecimal.ZERO : totalCost;
        unmetDemandQuantity = unmetDemandQuantity == null ? BigDecimal.ZERO : unmetDemandQuantity;
        inventoryConsumedQuantity = inventoryConsumedQuantity == null ? BigDecimal.ZERO : inventoryConsumedQuantity;
    }

    public static PlanComparisonMetrics defaultMetrics() {
        return new PlanComparisonMetrics(
                BigDecimal.ZERO, 0.0, 100.0, BigDecimal.ZERO, BigDecimal.ZERO, 50.0, 0.1, 0.9
        );
    }
}

