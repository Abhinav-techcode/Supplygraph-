package com.supplygraph.simulation.event;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic discrete simulation event.
 * Ordering contract:
 * 1. Timestamp (chronological)
 * 2. Priority (lower integer = higher priority)
 * 3. Event ID (lexicographical tie-breaker)
 */
public record SimulationEvent(
        String eventId,
        Instant timestamp,
        int priority,
        SimulationEventType eventType,
        String entityId,
        String description,
        Map<String, Object> payload
) implements Comparable<SimulationEvent> {

    public SimulationEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        entityId = entityId == null ? "" : entityId;
        description = description == null ? "" : description;
        payload = payload == null ? Map.of() : Collections.unmodifiableMap(Map.copyOf(payload));
    }

    @Override
    public int compareTo(SimulationEvent o) {
        int timeCmp = this.timestamp.compareTo(o.timestamp);
        if (timeCmp != 0) {
            return timeCmp;
        }
        int prioCmp = Integer.compare(this.priority, o.priority);
        if (prioCmp != 0) {
            return prioCmp;
        }
        return this.eventId.compareTo(o.eventId);
    }
}

