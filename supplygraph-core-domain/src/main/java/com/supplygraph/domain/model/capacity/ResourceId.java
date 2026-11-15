package com.supplygraph.domain.model.capacity;

import java.util.Objects;
import java.util.UUID;

public record ResourceId(String value) {
    public ResourceId {
        Objects.requireNonNull(value, "ResourceId must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ResourceId must not be blank");
        }
    }

    public static ResourceId generate() {
        return new ResourceId(UUID.randomUUID().toString());
    }

    public static ResourceId of(String value) {
        return new ResourceId(value);
    }
}

