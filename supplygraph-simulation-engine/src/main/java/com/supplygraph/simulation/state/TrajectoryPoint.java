package com.supplygraph.simulation.state;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Single temporal observation point along a simulation trajectory.
 */
public record TrajectoryPoint(
        Instant timestamp,
        String entityKey,
        BigDecimal value,
        String metric
) {
    public TrajectoryPoint {
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(entityKey, "entityKey must not be null");
        Objects.requireNonNull(value, "value must not be null");
        metric = metric == null ? "GENERIC" : metric;
    }
}

