package com.supplygraph.domain.model.planning;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record DemandForecastId(String value) implements Serializable {
    public DemandForecastId {
        Objects.requireNonNull(value, "DemandForecastId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("DemandForecastId cannot be blank");
        }
    }

    public static DemandForecastId generate() {
        return new DemandForecastId("DF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

