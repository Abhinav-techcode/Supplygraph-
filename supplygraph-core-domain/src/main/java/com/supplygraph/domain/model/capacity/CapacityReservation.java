package com.supplygraph.domain.model.capacity;

import com.supplygraph.domain.model.common.Quantity;

import java.util.Objects;

/**
 * Reservation claim reserving resource capacity across a specific half-open window.
 */
public record CapacityReservation(
        CapacityReservationId id,
        ResourceId resourceId,
        CapacityWindow window,
        Quantity quantity,
        String purpose
) {
    public CapacityReservation {
        Objects.requireNonNull(id, "ReservationId must not be null");
        Objects.requireNonNull(resourceId, "ResourceId must not be null");
        Objects.requireNonNull(window, "CapacityWindow must not be null");
        Objects.requireNonNull(quantity, "Quantity must not be null");
        if (!quantity.isPositive()) {
            throw new IllegalArgumentException("Reserved capacity quantity must be strictly positive");
        }
        Objects.requireNonNull(purpose, "Purpose must not be null");
    }

    public boolean overlapsWith(CapacityReservation other) {
        Objects.requireNonNull(other, "Other reservation must not be null");
        return this.resourceId.equals(other.resourceId) && this.window.overlaps(other.window);
    }
}

