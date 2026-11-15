package com.supplygraph.domain.model.capacity;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Deterministic half-open time interval [start, end).
 * Enforces start < end.
 */
public record CapacityWindow(Instant start, Instant end) implements Comparable<CapacityWindow> {
    public CapacityWindow {
        Objects.requireNonNull(start, "Start time must not be null");
        Objects.requireNonNull(end, "End time must not be null");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Invalid half-open interval: start (" + start + ") must be before end (" + end + ")");
        }
    }

    public static CapacityWindow of(Instant start, Instant end) {
        return new CapacityWindow(start, end);
    }

    public static CapacityWindow of(Instant start, Duration duration) {
        Objects.requireNonNull(duration, "Duration must not be null");
        return new CapacityWindow(start, start.plus(duration));
    }

    public Duration duration() {
        return Duration.between(start, end);
    }

    public boolean contains(Instant instant) {
        Objects.requireNonNull(instant, "Instant must not be null");
        return !instant.isBefore(start) && instant.isBefore(end);
    }

    /**
     * Checks if this half-open interval [s1, e1) overlaps with another [s2, e2).
     * Overlap condition: max(s1, s2) < min(e1, e2).
     */
    public boolean overlaps(CapacityWindow other) {
        Objects.requireNonNull(other, "Other window must not be null");
        Instant maxStart = this.start.isAfter(other.start) ? this.start : other.start;
        Instant minEnd = this.end.isBefore(other.end) ? this.end : other.end;
        return maxStart.isBefore(minEnd);
    }

    /**
     * Returns the intersecting half-open window if overlapping.
     */
    public Optional<CapacityWindow> intersection(CapacityWindow other) {
        if (!overlaps(other)) {
            return Optional.empty();
        }
        Instant maxStart = this.start.isAfter(other.start) ? this.start : other.start;
        Instant minEnd = this.end.isBefore(other.end) ? this.end : other.end;
        return Optional.of(new CapacityWindow(maxStart, minEnd));
    }

    @Override
    public int compareTo(CapacityWindow o) {
        int cmp = this.start.compareTo(o.start);
        if (cmp != 0) return cmp;
        return this.end.compareTo(o.end);
    }
}

