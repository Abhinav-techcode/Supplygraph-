package com.supplygraph.simulation;

import com.supplygraph.simulation.event.SimulationEvent;
import com.supplygraph.simulation.event.SimulationEventQueue;
import com.supplygraph.simulation.event.SimulationEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimulationEventQueueTest {

    @Test
    @DisplayName("Simulation events are dequeued in strict deterministic order: timestamp -> priority -> eventId")
    void testDeterministicOrdering() {
        SimulationEventQueue queue = new SimulationEventQueue();
        Instant t1 = Instant.parse("2026-11-17T10:00:00Z");
        Instant t2 = Instant.parse("2026-11-17T11:00:00Z");

        // Same timestamp, different priority
        SimulationEvent e1 = new SimulationEvent("EVT-3", t1, 10, SimulationEventType.SHIPMENT_DEPARTS, "LANE-1", "Desc", Map.of());
        SimulationEvent e2 = new SimulationEvent("EVT-1", t1, 0, SimulationEventType.DISRUPTION_BEGINS, "NODE-1", "Desc", Map.of());
        // Same timestamp, same priority, different ID
        SimulationEvent e3 = new SimulationEvent("EVT-2", t1, 0, SimulationEventType.INVENTORY_BECOMES_UNAVAILABLE, "NODE-1", "Desc", Map.of());
        // Later timestamp
        SimulationEvent e4 = new SimulationEvent("EVT-4", t2, 0, SimulationEventType.DISRUPTION_RESOLVES, "NODE-1", "Desc", Map.of());

        queue.enqueue(e1, t1);
        queue.enqueue(e4, t1);
        queue.enqueue(e3, t1);
        queue.enqueue(e2, t1);

        assertThat(queue.pollNext()).contains(e2); // t1, prio 0, EVT-1
        assertThat(queue.pollNext()).contains(e3); // t1, prio 0, EVT-2
        assertThat(queue.pollNext()).contains(e1); // t1, prio 10, EVT-3
        assertThat(queue.pollNext()).contains(e4); // t2, prio 0, EVT-4
        assertThat(queue.pollNext()).isEmpty();
    }

    @Test
    @DisplayName("Scheduling event prior to current virtual clock is rejected")
    void testPastEventRejection() {
        SimulationEventQueue queue = new SimulationEventQueue();
        Instant currentClock = Instant.parse("2026-11-17T12:00:00Z");
        Instant pastTime = Instant.parse("2026-11-17T11:59:59Z");

        SimulationEvent pastEvent = new SimulationEvent("EVT-PAST", pastTime, 0, SimulationEventType.PRODUCTION_STARTS, "NODE-1", "Desc", Map.of());

        assertThatThrownBy(() -> queue.enqueue(pastEvent, currentClock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot schedule event in the past");
    }

    @Test
    @DisplayName("Cancelled events are silently pruned from the queue")
    void testEventCancellation() {
        SimulationEventQueue queue = new SimulationEventQueue();
        Instant now = Instant.now();

        SimulationEvent e1 = new SimulationEvent("EVT-CANCEL-ME", now, 1, SimulationEventType.PRODUCTION_STARTS, "SITE-1", "Desc", Map.of());
        SimulationEvent e2 = new SimulationEvent("EVT-KEEP-ME", now, 2, SimulationEventType.PRODUCTION_COMPLETES, "SITE-1", "Desc", Map.of());

        queue.enqueue(e1, now);
        queue.enqueue(e2, now);

        boolean cancelled = queue.cancel("EVT-CANCEL-ME");
        assertThat(cancelled).isTrue();

        Optional<SimulationEvent> next = queue.pollNext();
        assertThat(next).contains(e2);
        assertThat(queue.pollNext()).isEmpty();
    }
}

