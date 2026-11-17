package com.supplygraph.simulation.montecarlo;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregated statistical distribution summary of a Monte Carlo analysis run.
 */
public record MonteCarloSummaryResult(
        String monteCarloRunId,
        long seed,
        int replicationCount,
        double meanDelayDays,
        double medianDelayDays,
        double p90DelayDays,
        double p95DelayDays,
        BigDecimal meanShortageQuantity,
        BigDecimal maxShortageQuantity,
        double slaViolationProbability,
        double recoveryWithinDeadlineProbability,
        BigDecimal estimatedMeanCost,
        String status,
        List<String> warnings
) {
    public MonteCarloSummaryResult {
        Objects.requireNonNull(monteCarloRunId, "monteCarloRunId must not be null");
        meanShortageQuantity = meanShortageQuantity == null ? BigDecimal.ZERO : meanShortageQuantity;
        maxShortageQuantity = maxShortageQuantity == null ? BigDecimal.ZERO : maxShortageQuantity;
        estimatedMeanCost = estimatedMeanCost == null ? BigDecimal.ZERO : estimatedMeanCost;
        status = status == null ? "COMPLETED" : status;
        warnings = warnings == null ? List.of() : Collections.unmodifiableList(List.copyOf(warnings));
    }
}

