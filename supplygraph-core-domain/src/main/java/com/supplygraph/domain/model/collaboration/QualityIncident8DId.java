package com.supplygraph.domain.model.collaboration;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record QualityIncident8DId(String value) implements Serializable {
    public QualityIncident8DId {
        Objects.requireNonNull(value, "QualityIncident8DId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("QualityIncident8DId cannot be blank");
        }
    }

    public static QualityIncident8DId generate() {
        return new QualityIncident8DId("8D-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

