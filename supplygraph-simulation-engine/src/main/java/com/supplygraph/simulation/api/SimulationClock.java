package com.supplygraph.simulation.api;

import java.time.Instant;
import java.util.Objects;

/**
 * Deterministic virtual simulation clock for discrete-event simulation.
 */
public record SimulationClock(Instant currentTime, long sequenceNumber) {
    public SimulationClock {
        Objects.requireNonNull(currentTime, "SimulationClock currentTime must not be null");
        if (sequenceNumber < 0) {
            throw new IllegalArgumentException("sequenceNumber must be non-negative");
        }
    }

    public static SimulationClock initial(Instant startTime) {
        return new SimulationClock(startTime, 0L);
    }

    public SimulationClock advanceTo(Instant newTime) {
        if (newTime.isBefore(currentTime)) {
            throw new IllegalArgumentException("Cannot advance simulation clock backwards in time");
        }
        return new SimulationClock(newTime, sequenceNumber + 1);
    }
}

