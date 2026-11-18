package com.supplygraph.domain.model.planning;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record PlanningScenarioId(String value) implements Serializable {
    public PlanningScenarioId {
        Objects.requireNonNull(value, "PlanningScenarioId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("PlanningScenarioId cannot be blank");
        }
    }

    public static PlanningScenarioId generate() {
        return new PlanningScenarioId("PSC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

