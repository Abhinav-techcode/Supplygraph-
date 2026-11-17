package com.supplygraph.domain.model.pareto;

import java.io.Serializable;
import java.util.Objects;

/**
 * Strongly typed identifier for an evaluated solution on the Pareto frontier.
 */
public record ParetoSolutionId(String value) implements Serializable {
    public ParetoSolutionId {
        Objects.requireNonNull(value, "ParetoSolutionId must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ParetoSolutionId must not be blank");
        }
    }

    public static ParetoSolutionId of(String value) {
        return new ParetoSolutionId(value);
    }

    public static ParetoSolutionId generate() {
        return new ParetoSolutionId("PAR-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}
