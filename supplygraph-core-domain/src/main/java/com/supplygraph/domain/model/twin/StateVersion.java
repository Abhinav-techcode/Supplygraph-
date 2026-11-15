package com.supplygraph.domain.model.twin;

import java.util.Objects;

/**
 * Monotonically increasing state version for digital twin state machines.
 */
public record StateVersion(long value) implements Comparable<StateVersion> {
    public StateVersion {
        if (value < 0) {
            throw new IllegalArgumentException("StateVersion value must be non-negative: " + value);
        }
    }

    public static StateVersion initial() {
        return new StateVersion(0L);
    }

    public static StateVersion of(long value) {
        return new StateVersion(value);
    }

    public StateVersion next() {
        return new StateVersion(value + 1);
    }

    @Override
    public int compareTo(StateVersion o) {
        return Long.compare(this.value, o.value);
    }

    @Override
    public String toString() {
        return "v" + value;
    }
}

