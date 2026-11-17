package com.supplygraph.simulation.optimization;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable outcome of a mathematical recovery optimization run.
 */
public record OptimizationResult(
        String optimizationRunId,
        SolverStatus solverStatus,
        double objectiveValue,
        List<OptimizationDecision> decisions,
        BigDecimal unmetDemandQuantity,
        double expectedDelayDays,
        BigDecimal estimatedCost,
        long solverRuntimeMillis,
        List<String> diagnostics,
        List<String> explanations
) {
    public OptimizationResult {
        Objects.requireNonNull(optimizationRunId, "optimizationRunId must not be null");
        Objects.requireNonNull(solverStatus, "solverStatus must not be null");
        decisions = decisions == null ? List.of() : Collections.unmodifiableList(List.copyOf(decisions));
        diagnostics = diagnostics == null ? List.of() : Collections.unmodifiableList(List.copyOf(diagnostics));
        explanations = explanations == null ? List.of() : Collections.unmodifiableList(List.copyOf(explanations));
        unmetDemandQuantity = unmetDemandQuantity == null ? BigDecimal.ZERO : unmetDemandQuantity;
        estimatedCost = estimatedCost == null ? BigDecimal.ZERO : estimatedCost;
    }
}

