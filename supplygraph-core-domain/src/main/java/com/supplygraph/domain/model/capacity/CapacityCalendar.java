package com.supplygraph.domain.model.capacity;

import com.supplygraph.domain.model.common.Quantity;

import java.util.*;

/**
 * Calendar managing time-dependent capacity reservations and availability.
 */
public class CapacityCalendar {
    private final ResourceId resourceId;
    private final ResourceCapacity capacity;
    private final List<CapacityReservation> reservations = new ArrayList<>();

    public CapacityCalendar(ResourceId resourceId, ResourceCapacity capacity) {
        this.resourceId = Objects.requireNonNull(resourceId, "ResourceId must not be null");
        this.capacity = Objects.requireNonNull(capacity, "ResourceCapacity must not be null");
    }

    public ResourceId getResourceId() {
        return resourceId;
    }

    public ResourceCapacity getCapacity() {
        return capacity;
    }

    public synchronized List<CapacityReservation> getAllReservations() {
        return List.copyOf(reservations);
    }

    /**
     * Finds all reservations overlapping with the specified query window [start, end).
     */
    public synchronized List<CapacityReservation> findOverlappingReservations(CapacityWindow window) {
        Objects.requireNonNull(window, "Window must not be null");
        List<CapacityReservation> result = new ArrayList<>();
        for (CapacityReservation r : reservations) {
            if (r.window().overlaps(window)) {
                result.add(r);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Calculates the total reserved quantity across all reservations that intersect the given window.
     */
    public synchronized Quantity getReservedQuantity(CapacityWindow window) {
        List<CapacityReservation> overlapping = findOverlappingReservations(window);
        Quantity sum = Quantity.zero(capacity.ratePerHour().unit());
        for (CapacityReservation r : overlapping) {
            sum = sum.add(r.quantity());
        }
        return sum;
    }

    /**
     * Calculates available capacity in the given window.
     * Available = max(0, theoreticalMax - reservedQuantity).
     */
    public synchronized Quantity getAvailableCapacity(CapacityWindow window) {
        Quantity maxTheoretical = capacity.calculateMaxOutput(window.duration());
        Quantity reserved = getReservedQuantity(window);
        if (reserved.compareTo(maxTheoretical) >= 0) {
            return Quantity.zero(maxTheoretical.unit());
        }
        return maxTheoretical.subtract(reserved);
    }

    /**
     * Books a capacity reservation.
     *
     * @param reservation the reservation to add
     * @param allowOverbooking whether to allow exceeding nominal capacity
     */
    public synchronized void bookReservation(CapacityReservation reservation, boolean allowOverbooking) {
        Objects.requireNonNull(reservation, "Reservation must not be null");
        if (!reservation.resourceId().equals(resourceId)) {
            throw new IllegalArgumentException(
                    "Reservation resource ID " + reservation.resourceId() + " does not match calendar resource " + resourceId);
        }

        if (!allowOverbooking) {
            Quantity available = getAvailableCapacity(reservation.window());
            if (available.compareTo(reservation.quantity()) < 0) {
                throw new IllegalStateException("Capacity over-allocation: Available capacity (" + available +
                        ") is less than requested reservation (" + reservation.quantity() + ") in window " + reservation.window());
            }
        }

        reservations.add(reservation);
    }

    /**
     * Cancels an existing reservation.
     */
    public synchronized boolean cancelReservation(CapacityReservationId reservationId) {
        return reservations.removeIf(r -> r.id().equals(reservationId));
    }
}

