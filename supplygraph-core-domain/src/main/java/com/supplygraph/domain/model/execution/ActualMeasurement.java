package com.supplygraph.domain.model.execution;

import java.time.Instant;
import java.util.Objects;

/** An observed real-world outcome value. {@code simulated} measurements are retained but never count as real deviation. */
public record ActualMeasurement(String workflowId, OutcomeMetric metric, double value, String source, boolean simulated, String recordedBy, Instant recordedAt) {
    public ActualMeasurement {
        Objects.requireNonNull(workflowId); Objects.requireNonNull(metric); Objects.requireNonNull(recordedAt);
        if (Double.isNaN(value) || Double.isInfinite(value)) throw new IllegalArgumentException("value must be finite");
        if (source == null || source.isBlank()) throw new IllegalArgumentException("source required");
    }
}
