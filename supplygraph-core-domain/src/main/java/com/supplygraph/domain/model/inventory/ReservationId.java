package com.supplygraph.domain.model.inventory;

import java.util.Objects;
import java.util.UUID;

public record ReservationId(String value) {
    public ReservationId {
        Objects.requireNonNull(value, "ReservationId must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ReservationId must not be blank");
        }
    }

    public static ReservationId generate() {
        return new ReservationId(UUID.randomUUID().toString());
    }

    public static ReservationId of(String value) {
        return new ReservationId(value);
    }
}

