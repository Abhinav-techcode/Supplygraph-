package com.supplygraph.domain.model.planning;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record ProductionScheduleId(String value) implements Serializable {
    public ProductionScheduleId {
        Objects.requireNonNull(value, "ProductionScheduleId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ProductionScheduleId cannot be blank");
        }
    }

    public static ProductionScheduleId generate() {
        return new ProductionScheduleId("PS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

