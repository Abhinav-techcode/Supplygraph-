package com.supplygraph.domain.model.sustainability;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record EnvironmentalScenarioId(String value) implements Serializable {
    public EnvironmentalScenarioId {
        Objects.requireNonNull(value, "EnvironmentalScenarioId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("EnvironmentalScenarioId cannot be blank");
        }
    }

    public static EnvironmentalScenarioId generate() {
        return new EnvironmentalScenarioId("ENV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

