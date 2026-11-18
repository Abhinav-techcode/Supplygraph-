package com.supplygraph.domain.model.sustainability;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record EsgScorecardId(String value) implements Serializable {
    public EsgScorecardId {
        Objects.requireNonNull(value, "EsgScorecardId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("EsgScorecardId cannot be blank");
        }
    }

    public static EsgScorecardId generate() {
        return new EsgScorecardId("ESG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

