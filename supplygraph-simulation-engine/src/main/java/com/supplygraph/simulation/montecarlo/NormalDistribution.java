package com.supplygraph.simulation.montecarlo;

import java.util.Random;

/**
 * Truncated Normal distribution with mean, standard deviation, and bounding intervals.
 */
public record NormalDistribution(
        double mean,
        double standardDeviation,
        double minBound,
        double maxBound
) implements ProbabilityDistribution {

    public NormalDistribution {
        if (standardDeviation < 0) {
            throw new IllegalArgumentException("standardDeviation must be non-negative: " + standardDeviation);
        }
        if (minBound > maxBound) {
            throw new IllegalArgumentException("minBound cannot exceed maxBound: " + minBound + " > " + maxBound);
        }
    }

    @Override
    public DistributionType getType() {
        return DistributionType.NORMAL;
    }

    @Override
    public double sample(Random random) {
        double val = mean + random.nextGaussian() * standardDeviation;
        if (val < minBound) {
            return minBound;
        }
        if (val > maxBound) {
            return maxBound;
        }
        return val;
    }

    @Override
    public void validate() {
        if (standardDeviation < 0 || minBound > maxBound) {
            throw new IllegalStateException("Invalid NormalDistribution parameters");
        }
    }
}

