package com.supplygraph.domain.model.impact;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly typed identifier for an ImpactAssessment.
 */
public record ImpactAssessmentId(String value) {
    public ImpactAssessmentId {
        Objects.requireNonNull(value, "ImpactAssessmentId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ImpactAssessmentId value must not be blank");
        }
    }

    public static ImpactAssessmentId of(String value) {
        return new ImpactAssessmentId(value);
    }

    public static ImpactAssessmentId generate() {
        return new ImpactAssessmentId(UUID.randomUUID().toString());
    }
}

