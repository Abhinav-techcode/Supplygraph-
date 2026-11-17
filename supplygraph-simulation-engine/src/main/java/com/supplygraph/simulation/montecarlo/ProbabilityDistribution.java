package com.supplygraph.simulation.montecarlo;

import java.util.Random;

/**
 * Common contract for Monte Carlo sampling distributions.
 */
public interface ProbabilityDistribution {
    DistributionType getType();
    double sample(Random random);
    void validate();
}

