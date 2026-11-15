package com.supplygraph.domain.model.bom;

import java.util.Objects;
import java.util.UUID;

public record MaterialId(String value) {
    public MaterialId {
        Objects.requireNonNull(value, "MaterialId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("MaterialId must not be blank");
        }
    }

    public static MaterialId generate() {
        return new MaterialId(UUID.randomUUID().toString());
    }

    public static MaterialId of(String value) {
        return new MaterialId(value);
    }
}

