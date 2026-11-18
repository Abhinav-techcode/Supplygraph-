package com.supplygraph.domain.model.sensitivity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Strongly typed identifier for an individual sensitivity trial.
 */
public record SensitivityTrialId(String value) implements Serializable {
    public SensitivityTrialId {
        Objects.requireNonNull(value, "SensitivityTrialId must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("SensitivityTrialId must not be blank");
        }
    }

    public static SensitivityTrialId of(String value) {
        return new SensitivityTrialId(value);
    }

    public static SensitivityTrialId generate() {
        return new SensitivityTrialId("SEN-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

