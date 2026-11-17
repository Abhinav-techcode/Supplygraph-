package com.supplygraph.simulation.event;

import java.time.Instant;
import java.util.*;

/**
 * Deterministic event queue for discrete-event simulation.
 * Rejects events with timestamps prior to the current virtual clock.
 */
public class SimulationEventQueue {
    private final PriorityQueue<SimulationEvent> queue = new PriorityQueue<>();
    private final Set<String> cancelledEventIds = new HashSet<>();
    private final Set<String> enqueuedEventIds = new HashSet<>();

    public synchronized void enqueue(SimulationEvent event, Instant currentClockTime) {
        Objects.requireNonNull(event, "event must not be null");
        if (currentClockTime != null && event.timestamp().isBefore(currentClockTime)) {
            throw new IllegalArgumentException("Cannot schedule event in the past: eventTime="
                    + event.timestamp() + " < clock=" + currentClockTime);
        }
        if (enqueuedEventIds.contains(event.eventId())) {
            throw new IllegalArgumentException("Duplicate event ID already enqueued: " + event.eventId());
        }
        enqueuedEventIds.add(event.eventId());
        queue.add(event);
    }

    public synchronized Optional<SimulationEvent> pollNext() {
        while (!queue.isEmpty()) {
            SimulationEvent event = queue.poll();
            if (!cancelledEventIds.contains(event.eventId())) {
                return Optional.of(event);
            }
        }
        return Optional.empty();
    }

    public synchronized Optional<SimulationEvent> peek() {
        while (!queue.isEmpty()) {
            SimulationEvent top = queue.peek();
            if (cancelledEventIds.contains(top.eventId())) {
                queue.poll(); // Discard cancelled
            } else {
                return Optional.of(top);
            }
        }
        return Optional.empty();
    }

    public synchronized boolean cancel(String eventId) {
        if (enqueuedEventIds.contains(eventId)) {
            cancelledEventIds.add(eventId);
            return true;
        }
        return false;
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized boolean isEmpty() {
        return peek().isEmpty();
    }

    public synchronized void clear() {
        queue.clear();
        cancelledEventIds.clear();
        enqueuedEventIds.clear();
    }
}

