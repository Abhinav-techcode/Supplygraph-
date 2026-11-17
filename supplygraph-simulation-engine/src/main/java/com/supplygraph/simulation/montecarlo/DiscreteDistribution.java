package com.supplygraph.simulation.montecarlo;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Discrete empirical distribution over distinct real values.
 */
public record DiscreteDistribution(
        List<Double> values,
        List<Double> probabilities
) implements ProbabilityDistribution {

    public DiscreteDistribution {
        Objects.requireNonNull(values, "values must not be null");
        Objects.requireNonNull(probabilities, "probabilities must not be null");
        if (values.size() != probabilities.size() || values.isEmpty()) {
            throw new IllegalArgumentException("Values and probabilities must have matching non-zero lengths");
        }
        double sum = 0.0;
        for (double p : probabilities) {
            if (p < 0.0) {
                throw new IllegalArgumentException("Probability cannot be negative: " + p);
            }
            sum += p;
        }
        if (Math.abs(sum - 1.0) > 0.001) {
            throw new IllegalArgumentException("Probabilities must sum to 1.0 (actual: " + sum + ")");
        }
        values = Collections.unmodifiableList(List.copyOf(values));
        probabilities = Collections.unmodifiableList(List.copyOf(probabilities));
    }

    @Override
    public DistributionType getType() {
        return DistributionType.DISCRETE;
    }

    @Override
    public double sample(Random random) {
        double u = random.nextDouble();
        double cumulative = 0.0;
        for (int i = 0; i < probabilities.size(); i++) {
            cumulative += probabilities.get(i);
            if (u <= cumulative || i == probabilities.size() - 1) {
                return values.get(i);
            }
        }
        return values.get(values.size() - 1);
    }

    @Override
    public void validate() {
        if (values.isEmpty() || values.size() != probabilities.size()) {
            throw new IllegalStateException("Invalid discrete distribution configuration");
        }
    }
}

