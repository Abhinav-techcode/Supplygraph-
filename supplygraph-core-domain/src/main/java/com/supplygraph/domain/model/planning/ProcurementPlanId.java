package com.supplygraph.domain.model.planning;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record ProcurementPlanId(String value) implements Serializable {
    public ProcurementPlanId {
        Objects.requireNonNull(value, "ProcurementPlanId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ProcurementPlanId cannot be blank");
        }
    }

    public static ProcurementPlanId generate() {
        return new ProcurementPlanId("PP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

