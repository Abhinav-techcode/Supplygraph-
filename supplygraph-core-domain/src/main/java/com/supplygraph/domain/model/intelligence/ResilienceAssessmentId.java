package com.supplygraph.domain.model.intelligence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Strongly typed identifier for network resilience assessments.
 */
public record ResilienceAssessmentId(String value) implements Serializable {

    public ResilienceAssessmentId {
        Objects.requireNonNull(value, "Resilience assessment ID value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Resilience assessment ID cannot be blank");
        }
    }

    public static ResilienceAssessmentId generate() {
        return new ResilienceAssessmentId("RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    public static ResilienceAssessmentId of(String value) {
        return new ResilienceAssessmentId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
