package com.supplygraph.domain.model.provenance;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed value object representing a provenance tracking record identifier.
 */
public record ProvenanceId(String value) implements Serializable {

    public ProvenanceId {
        Objects.requireNonNull(value, "Provenance ID value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Provenance ID cannot be blank");
        }
    }

    public static ProvenanceId generate() {
        return new ProvenanceId("PRV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    public static ProvenanceId of(String value) {
        return new ProvenanceId(value.trim());
    }

    @Override
    public String toString() {
        return value;
    }
}

