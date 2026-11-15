package com.supplygraph.domain.model.twin;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event driving deterministic digital twin state transitions.
 */
public record DomainEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        StateVersion version,
        Map<String, String> payload
) {
    public DomainEvent {
        if (eventId == null || eventId.isBlank()) {
            eventId = UUID.randomUUID().toString();
        }
        Objects.requireNonNull(eventType, "EventType must not be null");
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");
        payload = payload == null ? Map.of() : Collections.unmodifiableMap(Map.copyOf(payload));
    }

    public static DomainEvent of(String eventType, Instant occurredAt, Map<String, String> payload) {
        return new DomainEvent(UUID.randomUUID().toString(), eventType, occurredAt, null, payload);
    }

    public static DomainEvent of(String eventId, String eventType, Instant occurredAt, Map<String, String> payload) {
        return new DomainEvent(eventId, eventType, occurredAt, null, payload);
    }

    public static DomainEvent of(String eventId, String eventType, Instant occurredAt, StateVersion version, Map<String, String> payload) {
        return new DomainEvent(eventId, eventType, occurredAt, version, payload);
    }
}
