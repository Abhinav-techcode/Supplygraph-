package com.supplygraph.simulation.montecarlo;

import com.supplygraph.simulation.engine.SimulationConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable configuration for a seeded Monte Carlo experiment.
 */
public record MonteCarloConfiguration(
        long seed,
        int replicationCount,
        int maxReplications,
        List<MonteCarloParameter> parameters,
        SimulationConfiguration simulationConfig
) {
    public MonteCarloConfiguration {
        Objects.requireNonNull(simulationConfig, "simulationConfig must not be null");
        if (replicationCount <= 0) {
            throw new IllegalArgumentException("replicationCount must be positive: " + replicationCount);
        }
        if (maxReplications <= 0 || replicationCount > maxReplications) {
            throw new IllegalArgumentException("replicationCount cannot exceed maxReplications ("
                    + replicationCount + " > " + maxReplications + ")");
        }
        parameters = parameters == null ? List.of() : Collections.unmodifiableList(List.copyOf(parameters));
    }

    public static MonteCarloConfiguration standard(long seed, int replications, SimulationConfiguration simConfig) {
        return new MonteCarloConfiguration(seed, replications, 10000, List.of(), simConfig);
    }
}

