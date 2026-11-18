package com.supplygraph.domain.model.planning;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record InventoryPlanId(String value) implements Serializable {
    public InventoryPlanId {
        Objects.requireNonNull(value, "InventoryPlanId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("InventoryPlanId cannot be blank");
        }
    }

    public static InventoryPlanId generate() {
        return new InventoryPlanId("IP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

