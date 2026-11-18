package com.supplygraph.domain.model.sustainability;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record CarbonEmissionRecordId(String value) implements Serializable {
    public CarbonEmissionRecordId {
        Objects.requireNonNull(value, "CarbonEmissionRecordId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("CarbonEmissionRecordId cannot be blank");
        }
    }

    public static CarbonEmissionRecordId generate() {
        return new CarbonEmissionRecordId("CARB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

