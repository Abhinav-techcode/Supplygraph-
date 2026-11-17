package com.supplygraph.domain.model.security;

import java.io.Serializable;
import java.util.Objects;

/**
 * Strongly-typed value object representing a user identifier.
 */
public record UserId(String value) implements Serializable {

    public UserId {
        Objects.requireNonNull(value, "User identifier value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("User identifier cannot be blank");
        }
    }

    public static UserId of(String value) {
        return new UserId(value.trim());
    }

    @Override
    public String toString() {
        return value;
    }
}

