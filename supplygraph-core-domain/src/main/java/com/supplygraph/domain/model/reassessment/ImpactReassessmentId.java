package com.supplygraph.domain.model.reassessment;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifier for a continuous impact reassessment cycle.
 */
public record ImpactReassessmentId(String value) {
    public ImpactReassessmentId {
        Objects.requireNonNull(value, "ImpactReassessmentId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ImpactReassessmentId value must not be blank");
        }
    }

    public static ImpactReassessmentId of(String value) {
        return new ImpactReassessmentId(value);
    }

    public static ImpactReassessmentId generate() {
        return new ImpactReassessmentId("REASSESS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

