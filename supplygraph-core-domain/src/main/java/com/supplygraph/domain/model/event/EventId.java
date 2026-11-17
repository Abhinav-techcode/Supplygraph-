package com.supplygraph.domain.model.event;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for an operational event.
 */
public record EventId(String value) {
    public EventId {
        Objects.requireNonNull(value, "EventId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("EventId value must not be blank");
        }
    }

    public static EventId generate() {
        return new EventId("EVT-" + UUID.randomUUID());
    }

    public static EventId of(String value) {
        return new EventId(value);
    }
}

