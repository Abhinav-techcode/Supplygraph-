package com.supplygraph.simulation.optimization;

/**
 * Normalized weights for multi-objective recovery optimization.
 */
public record OptimizationWeights(
        double costWeight,
        double delayWeight,
        double unmetDemandWeight,
        double riskWeight
) {
    public OptimizationWeights {
        if (costWeight < 0 || delayWeight < 0 || unmetDemandWeight < 0 || riskWeight < 0) {
            throw new IllegalArgumentException("Optimization weights must be non-negative");
        }
        double sum = costWeight + delayWeight + unmetDemandWeight + riskWeight;
        if (sum == 0.0) {
            throw new IllegalArgumentException("Sum of optimization weights cannot be zero");
        }
    }

    public static OptimizationWeights balanced() {
        return new OptimizationWeights(0.35, 0.25, 0.30, 0.10);
    }

    public static OptimizationWeights costCentric() {
        return new OptimizationWeights(0.70, 0.10, 0.15, 0.05);
    }

    public static OptimizationWeights speedCentric() {
        return new OptimizationWeights(0.10, 0.70, 0.15, 0.05);
    }
}

