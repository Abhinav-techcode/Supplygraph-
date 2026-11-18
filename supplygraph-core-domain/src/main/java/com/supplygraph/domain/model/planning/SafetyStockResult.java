package com.supplygraph.domain.model.planning;

import java.io.Serializable;

/**
 * Result of stochastic safety stock and reorder point determination.
 */
public record SafetyStockResult(
        double baseSafetyStock,
        double leadTimeVariabilityBuffer,
        double demandVariabilityBuffer,
        double recommendedSafetyStock,
        double reorderPoint,
        double economicOrderQuantity
) implements Serializable {
    public SafetyStockResult {
        if (recommendedSafetyStock < 0 || reorderPoint < 0 || economicOrderQuantity < 0) {
            throw new IllegalArgumentException("Safety stock and reorder parameters cannot be negative");
        }
    }
}

