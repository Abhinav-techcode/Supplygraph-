package com.supplygraph.domain.model.monitoring;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for a detected risk alert.
 */
public record RiskAlertId(String value) {
    public RiskAlertId {
        Objects.requireNonNull(value, "RiskAlertId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("RiskAlertId value must not be blank");
        }
    }

    public static RiskAlertId of(String value) {
        return new RiskAlertId(value);
    }

    public static RiskAlertId generate() {
        return new RiskAlertId("ALT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

