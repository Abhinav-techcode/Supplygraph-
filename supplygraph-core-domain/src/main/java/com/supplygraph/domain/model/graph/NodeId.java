package com.supplygraph.domain.model.graph;

import java.util.Objects;
import java.util.UUID;

public record NodeId(String value) {
    public NodeId {
        Objects.requireNonNull(value, "NodeId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("NodeId must not be blank");
        }
    }

    public static NodeId generate() {
        return new NodeId(UUID.randomUUID().toString());
    }

    public static NodeId of(String value) {
        return new NodeId(value);
    }
}

