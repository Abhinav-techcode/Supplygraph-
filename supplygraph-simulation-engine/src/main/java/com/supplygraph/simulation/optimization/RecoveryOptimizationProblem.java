package com.supplygraph.simulation.optimization;

import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Problem formulation passed to the recovery optimizer.
 */
public record RecoveryOptimizationProblem(
        Disruption disruption,
        DigitalTwinSnapshot snapshot,
        BigDecimal requiredDemandQuantity,
        List<AlternativeOption> alternativeSuppliers,
        List<AlternativeOption> alternativeLanes,
        OptimizationObjective objective,
        OptimizationWeights weights,
        int timeLimitSeconds
) {
    public RecoveryOptimizationProblem {
        Objects.requireNonNull(disruption, "disruption must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        requiredDemandQuantity = requiredDemandQuantity == null ? BigDecimal.valueOf(1000) : requiredDemandQuantity;
        alternativeSuppliers = alternativeSuppliers == null ? List.of() : Collections.unmodifiableList(List.copyOf(alternativeSuppliers));
        alternativeLanes = alternativeLanes == null ? List.of() : Collections.unmodifiableList(List.copyOf(alternativeLanes));
        objective = objective == null ? OptimizationObjective.WEIGHTED_MULTI_OBJECTIVE : objective;
        weights = weights == null ? OptimizationWeights.balanced() : weights;
        if (timeLimitSeconds <= 0) {
            timeLimitSeconds = 30;
        }
    }
}

