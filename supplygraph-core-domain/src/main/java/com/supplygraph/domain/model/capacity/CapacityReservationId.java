package com.supplygraph.domain.model.capacity;

import java.util.Objects;
import java.util.UUID;

public record CapacityReservationId(String value) {
    public CapacityReservationId {
        Objects.requireNonNull(value, "CapacityReservationId must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("CapacityReservationId must not be blank");
        }
    }

    public static CapacityReservationId generate() {
        return new CapacityReservationId(UUID.randomUUID().toString());
    }

    public static CapacityReservationId of(String value) {
        return new CapacityReservationId(value);
    }
}

