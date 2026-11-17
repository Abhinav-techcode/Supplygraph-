package com.supplygraph.domain.model.explainability;

import java.io.Serializable;
import java.util.Objects;

/**
 * Strongly typed identifier for an explainable recovery recommendation.
 */
public record RecommendationId(String value) implements Serializable {
    public RecommendationId {
        Objects.requireNonNull(value, "RecommendationId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("RecommendationId value must not be blank");
        }
    }

    public static RecommendationId of(String value) {
        return new RecommendationId(value);
    }

    public static RecommendationId generate() {
        return new RecommendationId("REC-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}
