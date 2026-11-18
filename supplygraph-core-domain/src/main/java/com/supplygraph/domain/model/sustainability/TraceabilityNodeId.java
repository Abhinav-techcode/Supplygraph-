package com.supplygraph.domain.model.sustainability;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record TraceabilityNodeId(String value) implements Serializable {
    public TraceabilityNodeId {
        Objects.requireNonNull(value, "TraceabilityNodeId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("TraceabilityNodeId cannot be blank");
        }
    }

    public static TraceabilityNodeId generate() {
        return new TraceabilityNodeId("TRC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

