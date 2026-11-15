package com.supplygraph.domain.model.capacity;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Operating schedule defining active operating windows for a resource.
 */
public record OperatingSchedule(List<CapacityWindow> operatingWindows) {
    public OperatingSchedule {
        Objects.requireNonNull(operatingWindows, "Operating windows must not be null");
        operatingWindows = List.copyOf(operatingWindows);
    }

    public static OperatingSchedule continuous(CapacityWindow totalHorizon) {
        return new OperatingSchedule(List.of(totalHorizon));
    }

    public Duration totalOperatingDuration() {
        Duration total = Duration.ZERO;
        for (CapacityWindow w : operatingWindows) {
            total = total.plus(w.duration());
        }
        return total;
    }

    public boolean isOpenAt(java.time.Instant instant) {
        for (CapacityWindow w : operatingWindows) {
            if (w.contains(instant)) {
                return true;
            }
        }
        return false;
    }
}

