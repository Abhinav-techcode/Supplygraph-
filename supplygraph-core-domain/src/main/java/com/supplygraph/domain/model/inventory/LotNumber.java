package com.supplygraph.domain.model.inventory;

import java.util.Objects;

public record LotNumber(String value) {
    public LotNumber {
        Objects.requireNonNull(value, "LotNumber must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("LotNumber must not be blank");
        }
    }

    public static LotNumber of(String value) {
        return new LotNumber(value);
    }
}

