package com.supplygraph.domain.model.intelligence;

import java.io.Serializable;
import java.util.Objects;

/**
 * An individual evaluated dimension contributing to the composite Supply Chain Resilience Index.
 */
public record ResilienceFactor(
        String factorName,
        double score,
        double weight,
        double contribution,
        String explanation
) implements Serializable {

    public ResilienceFactor {
        Objects.requireNonNull(factorName, "Factor name cannot be null");
        if (explanation == null) {
            explanation = "Evaluated based on graph topology and inventory buffer constraints";
        }
    }

    public static ResilienceFactor of(String factorName, double score, double weight, String explanation) {
        double boundedScore = Math.max(0.0, Math.min(100.0, score));
        double contribution = boundedScore * weight;
        return new ResilienceFactor(factorName, boundedScore, weight, contribution, explanation);
    }
}

