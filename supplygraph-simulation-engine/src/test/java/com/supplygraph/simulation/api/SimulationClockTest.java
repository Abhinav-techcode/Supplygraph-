package com.supplygraph.simulation.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Simulation Clock Determinism Tests")
class SimulationClockTest {

    @Test
    @DisplayName("Should advance simulation clock forward monotonically")
    void shouldAdvanceClockForward() {
        Instant t0 = Instant.parse("2026-11-16T00:00:00Z");
        SimulationClock clock = SimulationClock.initial(t0);

        assertThat(clock.currentTime()).isEqualTo(t0);
        assertThat(clock.sequenceNumber()).isZero();

        Instant t1 = t0.plus(Duration.ofHours(2));
        SimulationClock advanced = clock.advanceTo(t1);

        assertThat(advanced.currentTime()).isEqualTo(t1);
        assertThat(advanced.sequenceNumber()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should forbid moving simulation clock backward in time")
    void shouldForbidMovingBackward() {
        Instant t0 = Instant.parse("2026-11-16T12:00:00Z");
        SimulationClock clock = SimulationClock.initial(t0);

        Instant backward = t0.minus(Duration.ofMinutes(10));
        assertThatThrownBy(() -> clock.advanceTo(backward))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot advance simulation clock backwards");
    }
}

