package com.supplygraph.domain.model.plan;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly typed identifier for a RecoveryPlan.
 */
public record RecoveryPlanId(String value) {
    public RecoveryPlanId {
        Objects.requireNonNull(value, "RecoveryPlanId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("RecoveryPlanId value must not be blank");
        }
    }

    public static RecoveryPlanId of(String value) {
        return new RecoveryPlanId(value);
    }

    public static RecoveryPlanId generate() {
        return new RecoveryPlanId(UUID.randomUUID().toString());
    }
}

