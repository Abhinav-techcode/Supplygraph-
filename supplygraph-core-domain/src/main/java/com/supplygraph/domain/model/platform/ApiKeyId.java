package com.supplygraph.domain.model.platform;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record ApiKeyId(String value) implements Serializable {
    public ApiKeyId {
        Objects.requireNonNull(value, "ApiKeyId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ApiKeyId cannot be blank");
        }
    }

    public static ApiKeyId generate() {
        return new ApiKeyId("KEY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

