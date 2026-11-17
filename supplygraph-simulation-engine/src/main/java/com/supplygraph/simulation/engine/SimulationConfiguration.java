package com.supplygraph.simulation.engine;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable configuration parameters for a discrete-event simulation run.
 */
public record SimulationConfiguration(
        Instant startTime,
        Instant endTime,
        long stepSeconds,
        int maxEvents,
        boolean includeRecoveryPlan
) {
    public SimulationConfiguration {
        Objects.requireNonNull(startTime, "startTime must not be null");
        Objects.requireNonNull(endTime, "endTime must not be null");
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("endTime cannot be before startTime");
        }
        if (maxEvents <= 0) {
            throw new IllegalArgumentException("maxEvents must be strictly positive: " + maxEvents);
        }
        if (stepSeconds <= 0) {
            stepSeconds = 3600L; // default 1 hour
        }
    }

    public static SimulationConfiguration defaultHorizon(Instant start, long durationSeconds) {
        return new SimulationConfiguration(start, start.plusSeconds(durationSeconds), 3600L, 50000, true);
    }
}

