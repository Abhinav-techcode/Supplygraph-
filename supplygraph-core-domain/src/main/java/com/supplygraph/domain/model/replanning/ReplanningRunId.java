package com.supplygraph.domain.model.replanning;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifier for an adaptive agentic replanning execution.
 */
public record ReplanningRunId(String value) {
    public ReplanningRunId {
        Objects.requireNonNull(value, "ReplanningRunId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ReplanningRunId value must not be blank");
        }
    }

    public static ReplanningRunId of(String value) {
        return new ReplanningRunId(value);
    }

    public static ReplanningRunId generate() {
        return new ReplanningRunId("REPLAN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

