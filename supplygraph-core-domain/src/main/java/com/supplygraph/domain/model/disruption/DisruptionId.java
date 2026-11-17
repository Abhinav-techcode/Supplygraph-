package com.supplygraph.domain.model.disruption;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly typed identifier for a Disruption.
 */
public record DisruptionId(String value) {
    public DisruptionId {
        Objects.requireNonNull(value, "DisruptionId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("DisruptionId value must not be blank");
        }
    }

    public static DisruptionId of(String value) {
        return new DisruptionId(value);
    }

    public static DisruptionId generate() {
        return new DisruptionId(UUID.randomUUID().toString());
    }
}

