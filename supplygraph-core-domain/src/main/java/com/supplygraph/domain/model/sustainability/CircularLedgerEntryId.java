package com.supplygraph.domain.model.sustainability;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record CircularLedgerEntryId(String value) implements Serializable {
    public CircularLedgerEntryId {
        Objects.requireNonNull(value, "CircularLedgerEntryId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("CircularLedgerEntryId cannot be blank");
        }
    }

    public static CircularLedgerEntryId generate() {
        return new CircularLedgerEntryId("CIRC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

