package com.supplygraph.domain.model.inventory;

import java.util.Objects;
import java.util.UUID;

public record TransactionId(String value) {
    public TransactionId {
        Objects.requireNonNull(value, "TransactionId must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("TransactionId must not be blank");
        }
    }

    public static TransactionId generate() {
        return new TransactionId(UUID.randomUUID().toString());
    }

    public static TransactionId of(String value) {
        return new TransactionId(value);
    }
}

