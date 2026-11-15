package com.supplygraph.concurguard.benchmark;

import java.math.BigDecimal;

/**
 * Quantitative evaluation metrics recorded across arbitration strategies.
 */
public record BenchmarkMetrics(
        StrategyType strategy,
        double conflictPrecision,
        double conflictRecall,
        long arbitrationLatencyNanos,
        int replanningIterations,
        double percentIntentsPreserved,
        boolean planFeasible,
        BigDecimal totalCost
) {
    public double latencyMillis() {
        return arbitrationLatencyNanos / 1_000_000.0;
    }
}
