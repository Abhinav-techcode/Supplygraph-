package com.supplygraph.domain.model.benchmark;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed value object representing a research benchmark experiment identifier.
 */
public record ExperimentId(String value) implements Serializable {

    public ExperimentId {
        Objects.requireNonNull(value, "Experiment ID value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Experiment ID cannot be blank");
        }
    }

    public static ExperimentId generate() {
        return new ExperimentId("EXP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    public static ExperimentId of(String value) {
        return new ExperimentId(value.trim());
    }

    @Override
    public String toString() {
        return value;
    }
}

