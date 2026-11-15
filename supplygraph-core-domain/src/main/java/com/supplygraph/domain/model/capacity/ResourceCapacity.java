package com.supplygraph.domain.model.capacity;

import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Objects;

/**
 * Throughput or processing capacity rate of a resource.
 */
public record ResourceCapacity(
        Quantity ratePerHour,
        OperatingSchedule schedule
) {
    public ResourceCapacity {
        Objects.requireNonNull(ratePerHour, "Rate per hour must not be null");
        if (!ratePerHour.isPositive()) {
            throw new IllegalArgumentException("Capacity rate must be strictly positive");
        }
        Objects.requireNonNull(schedule, "Schedule must not be null");
    }

    public static ResourceCapacity of(Quantity ratePerHour, OperatingSchedule schedule) {
        return new ResourceCapacity(ratePerHour, schedule);
    }

    /**
     * Calculates the maximum theoretical output of this resource over a given duration.
     */
    public Quantity calculateMaxOutput(Duration duration) {
        Objects.requireNonNull(duration, "Duration must not be null");
        BigDecimal hours = BigDecimal.valueOf(duration.toSeconds())
                .divide(BigDecimal.valueOf(3600), 4, RoundingMode.HALF_UP);
        return ratePerHour.multiply(hours);
    }
}

