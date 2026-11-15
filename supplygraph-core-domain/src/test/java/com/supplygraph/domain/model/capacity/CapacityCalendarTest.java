package com.supplygraph.domain.model.capacity;

import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CapacityCalendarTest {

    @Test
    @DisplayName("CapacityWindow enforces half-open interval semantics [start, end)")
    void shouldAdhereToHalfOpenIntervalSemantics() {
        Instant t0 = Instant.parse("2026-11-16T10:00:00Z");
        Instant t1 = Instant.parse("2026-11-16T12:00:00Z");
        Instant t2 = Instant.parse("2026-11-16T14:00:00Z");

        CapacityWindow w1 = CapacityWindow.of(t0, t1); // [10:00, 12:00)
        CapacityWindow w2 = CapacityWindow.of(t1, t2); // [12:00, 14:00)

        // Boundary tests
        assertThat(w1.contains(t0)).isTrue();
        assertThat(w1.contains(t1)).isFalse(); // half-open end excluded!
        assertThat(w2.contains(t1)).isTrue();  // half-open start included!

        // Adjacent half-open intervals do NOT overlap!
        assertThat(w1.overlaps(w2)).isFalse();

        // Slightly overlapping intervals DO overlap
        CapacityWindow w3 = CapacityWindow.of(t0, t1.plusSeconds(1));
        assertThat(w3.overlaps(w2)).isTrue();
    }

    @Test
    @DisplayName("Calendar computes available remaining capacity accurately")
    void shouldComputeAvailableCapacity() {
        Instant base = Instant.parse("2026-11-16T08:00:00Z");
        CapacityWindow horizon = CapacityWindow.of(base, Duration.ofHours(8)); // 8-hour shift [08:00, 16:00)
        OperatingSchedule schedule = OperatingSchedule.continuous(horizon);

        // 50 WAFERS/HOUR throughput rate
        Quantity rate = Quantity.of(50, UnitOfMeasure.WAFER);
        ResourceCapacity capacity = ResourceCapacity.of(rate, schedule);
        ResourceId lithoScanner = ResourceId.of("RES-LITHO-01");
        CapacityCalendar calendar = new CapacityCalendar(lithoScanner, capacity);

        // Theoretical maximum for 4 hours = 4 * 50 = 200 WAFERS
        CapacityWindow morningWindow = CapacityWindow.of(base, Duration.ofHours(4)); // [08:00, 12:00)
        Quantity initialAvailable = calendar.getAvailableCapacity(morningWindow);
        assertThat(initialAvailable.amount()).isEqualByComparingTo("200.0000");

        // Book 80 WAFERS
        CapacityReservation res1 = new CapacityReservation(
                CapacityReservationId.of("RES-01"), lithoScanner, morningWindow,
                Quantity.of(80, UnitOfMeasure.WAFER), "ASIC-LOT-A");
        calendar.bookReservation(res1, false);

        Quantity remainingAfterRes1 = calendar.getAvailableCapacity(morningWindow);
        assertThat(remainingAfterRes1.amount()).isEqualByComparingTo("120.0000");

        // Book another 120 WAFERS -> remaining should be 0
        CapacityReservation res2 = new CapacityReservation(
                CapacityReservationId.of("RES-02"), lithoScanner, morningWindow,
                Quantity.of(120, UnitOfMeasure.WAFER), "ASIC-LOT-B");
        calendar.bookReservation(res2, false);

        Quantity fullyBooked = calendar.getAvailableCapacity(morningWindow);
        assertThat(fullyBooked.amount()).isEqualByComparingTo("0.0000");
    }

    @Test
    @DisplayName("Calendar rejects overbooking when allowOverbooking is false")
    void shouldRejectOverbooking() {
        Instant base = Instant.parse("2026-11-16T08:00:00Z");
        CapacityWindow window = CapacityWindow.of(base, Duration.ofHours(2)); // 2 hours
        ResourceCapacity capacity = ResourceCapacity.of(
                Quantity.of(10, UnitOfMeasure.HOUR), OperatingSchedule.continuous(window));
        ResourceId machine = ResourceId.of("M-01");
        CapacityCalendar calendar = new CapacityCalendar(machine, capacity);

        // Max theoretical = 2 * 10 = 20 hours
        // Attempt to reserve 25 hours -> overbooking
        CapacityReservation excessive = new CapacityReservation(
                CapacityReservationId.of("RES-EXCESS"), machine, window,
                Quantity.of(25, UnitOfMeasure.HOUR), "RUSH-JOB");

        assertThatThrownBy(() -> calendar.bookReservation(excessive, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Capacity over-allocation");
    }
}

