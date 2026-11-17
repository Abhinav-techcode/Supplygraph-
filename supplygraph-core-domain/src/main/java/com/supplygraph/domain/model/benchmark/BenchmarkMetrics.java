package com.supplygraph.domain.model.benchmark;

import java.io.Serializable;

/**
 * Formal empirical evaluation metrics measured across benchmark experiment runs.
 */
public record BenchmarkMetrics(
        double conflictPrecision,
        double conflictRecall,
        double falsePositiveRate,
        double falseNegativeRate,
        int invalidActionsPrevented,
        int validActionsPreserved,
        int replanningScopeSize,
        long replanningExecutionTimeMs,
        long recoveryCompletionDays,
        double recoveryCostUsd,
        double serviceLevelPercentage,
        double optimizationFeasibilityRate,
        long simulationExecutionTimeMs,
        long agentExecutionTimeMs,
        long totalCoordinationOverheadMs
) implements Serializable {

    public static BenchmarkMetrics empty() {
        return new BenchmarkMetrics(
                0.0, 0.0, 0.0, 0.0,
                0, 0, 0, 0L, 0L, 0.0, 0.0, 0.0, 0L, 0L, 0L
        );
    }
}

