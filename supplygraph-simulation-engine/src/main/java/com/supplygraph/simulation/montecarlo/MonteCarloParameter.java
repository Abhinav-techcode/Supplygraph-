package com.supplygraph.simulation.montecarlo;

import java.util.Objects;

/**
 * Stochastic parameter bound to an entity and probability distribution.
 */
public record MonteCarloParameter(
        String parameterName,
        String entityId,
        ProbabilityDistribution distribution
) {
    public MonteCarloParameter {
        Objects.requireNonNull(parameterName, "parameterName must not be null");
        Objects.requireNonNull(distribution, "distribution must not be null");
        entityId = entityId == null ? "GLOBAL" : entityId;
        distribution.validate();
    }
}

