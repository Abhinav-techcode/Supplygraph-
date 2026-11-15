package com.supplygraph.domain.model.graph;

import java.util.Objects;
import java.util.UUID;

public record EdgeId(String value) {
    public EdgeId {
        Objects.requireNonNull(value, "EdgeId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("EdgeId must not be blank");
        }
    }

    public static EdgeId generate() {
        return new EdgeId(UUID.randomUUID().toString());
    }

    public static EdgeId of(String value) {
        return new EdgeId(value);
    }
}

