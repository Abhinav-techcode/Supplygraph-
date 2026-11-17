package com.supplygraph.simulation.montecarlo;

import java.util.Random;

/**
 * Triangular distribution defined by minimum, mode, and maximum values.
 * Uses exact inverse CDF transformation.
 */
public record TriangularDistribution(double min, double mode, double max) implements ProbabilityDistribution {

    public TriangularDistribution {
        if (min > mode || mode > max) {
            throw new IllegalArgumentException("Violated triangular ordering: min <= mode <= max ("
                    + min + " <= " + mode + " <= " + max + ")");
        }
    }

    @Override
    public DistributionType getType() {
        return DistributionType.TRIANGULAR;
    }

    @Override
    public double sample(Random random) {
        if (min == max) {
            return min;
        }
        double u = random.nextDouble();
        double fc = (mode - min) / (max - min);

        if (u < fc) {
            return min + Math.sqrt(u * (max - min) * (mode - min));
        } else {
            return max - Math.sqrt((1.0 - u) * (max - min) * (max - mode));
        }
    }

    @Override
    public void validate() {
        if (min > mode || mode > max) {
            throw new IllegalStateException("Triangular distribution requires min <= mode <= max");
        }
    }
}

