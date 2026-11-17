package com.supplygraph.domain.model.recovery;

import java.io.Serializable;
import java.util.Objects;

/**
 * Strongly typed identifier for a candidate recovery plan alternative.
 */
public record PlanAlternativeId(String value) implements Serializable {
    public PlanAlternativeId {
        Objects.requireNonNull(value, "PlanAlternativeId must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("PlanAlternativeId must not be blank");
        }
    }

    public static PlanAlternativeId of(String value) {
        return new PlanAlternativeId(value);
    }

    public static PlanAlternativeId generate() {
        return new PlanAlternativeId("ALT-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}
