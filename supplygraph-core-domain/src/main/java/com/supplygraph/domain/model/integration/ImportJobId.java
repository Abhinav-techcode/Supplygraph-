package com.supplygraph.domain.model.integration;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Strongly typed identifier for enterprise data import jobs.
 */
public record ImportJobId(String value) implements Serializable {

    public ImportJobId {
        Objects.requireNonNull(value, "Import job ID value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Import job ID cannot be blank");
        }
    }

    public static ImportJobId generate() {
        return new ImportJobId("IMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    public static ImportJobId of(String value) {
        return new ImportJobId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
