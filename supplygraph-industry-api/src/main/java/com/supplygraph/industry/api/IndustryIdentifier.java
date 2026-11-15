package com.supplygraph.industry.api;

import java.util.Objects;

/**
 * Unique identifier for an Industry Extension Pack.
 */
public record IndustryIdentifier(String code) {
    public IndustryIdentifier {
        Objects.requireNonNull(code, "Industry code must not be null");
        if (code.isBlank()) {
            throw new IllegalArgumentException("Industry code must not be blank");
        }
    }

    public static IndustryIdentifier of(String code) {
        return new IndustryIdentifier(code.toUpperCase());
    }
}

