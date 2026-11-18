package com.supplygraph.domain.model.planning;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Historical demand observation point for time-series forecasting.
 */
public record HistoricalDemandPoint(
        LocalDate date,
        double quantity
) implements Serializable {
    public HistoricalDemandPoint {
        Objects.requireNonNull(date, "Historical observation date cannot be null");
        if (quantity < 0) {
            throw new IllegalArgumentException("Demand quantity cannot be negative");
        }
    }
}

