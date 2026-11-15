package com.supplygraph.concurguard.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly typed immutable identifier for an ActionIntent.
 */
public record IntentId(String value) {
    public IntentId {
        Objects.requireNonNull(value, "IntentId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("IntentId value must not be blank");
        }
    }

    public static IntentId generate() {
        return new IntentId(UUID.randomUUID().toString());
    }

    public static IntentId of(String value) {
        return new IntentId(value);
    }
}
