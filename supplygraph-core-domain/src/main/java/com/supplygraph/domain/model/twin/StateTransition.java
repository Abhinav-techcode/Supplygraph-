package com.supplygraph.domain.model.twin;

import java.time.Instant;
import java.util.Objects;

/**
 * Audit record of a discrete state transition between two state versions.
 */
public record StateTransition(
        StateVersion fromVersion,
        StateVersion toVersion,
        DomainEvent triggerEvent,
        Instant appliedAt
) {
    public StateTransition {
        Objects.requireNonNull(fromVersion, "fromVersion must not be null");
        Objects.requireNonNull(toVersion, "toVersion must not be null");
        if (toVersion.value() <= fromVersion.value()) {
            throw new IllegalArgumentException(
                    "toVersion (" + toVersion + ") must be strictly greater than fromVersion (" + fromVersion + ")");
        }
        Objects.requireNonNull(triggerEvent, "triggerEvent must not be null");
        Objects.requireNonNull(appliedAt, "appliedAt must not be null");
    }
}

