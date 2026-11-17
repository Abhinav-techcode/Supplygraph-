package com.supplygraph.simulation.montecarlo;

import java.util.Random;

/**
 * Continuous uniform distribution [min, max].
 */
public record UniformDistribution(double min, double max) implements ProbabilityDistribution {

    public UniformDistribution {
        if (min > max) {
            throw new IllegalArgumentException("min cannot exceed max: " + min + " > " + max);
        }
    }

    @Override
    public DistributionType getType() {
        return DistributionType.UNIFORM;
    }

    @Override
    public double sample(Random random) {
        if (min == max) {
            return min;
        }
        return min + random.nextDouble() * (max - min);
    }

    @Override
    public void validate() {
        if (min > max) {
            throw new IllegalStateException("Uniform min must be <= max");
        }
    }
}

