package com.supplygraph.simulation.engine;

import com.supplygraph.domain.model.twin.StateVersion;
import com.supplygraph.simulation.event.SimulationEvent;
import com.supplygraph.simulation.state.TrajectoryPoint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable outcome of a discrete-event simulation run.
 */
public record SimulationResult(
        String simulationId,
        String scenarioId,
        StateVersion sourceTwinVersion,
        Instant startTime,
        Instant endTime,
        int processedEventCount,
        List<SimulationEvent> eventTimeline,
        List<TrajectoryPoint> inventoryTrajectories,
        List<TrajectoryPoint> productionTrajectories,
        List<TrajectoryPoint> shortageTrajectories,
        double serviceLevelPercentage,
        BigDecimal totalShortageQuantity,
        long maxDelaySeconds,
        String status,
        List<String> warnings,
        List<String> assumptions
) {
    public SimulationResult {
        Objects.requireNonNull(simulationId, "simulationId must not be null");
        Objects.requireNonNull(sourceTwinVersion, "sourceTwinVersion must not be null");
        Objects.requireNonNull(startTime, "startTime must not be null");
        Objects.requireNonNull(endTime, "endTime must not be null");

        eventTimeline = eventTimeline == null ? List.of() : Collections.unmodifiableList(List.copyOf(eventTimeline));
        inventoryTrajectories = inventoryTrajectories == null ? List.of() : Collections.unmodifiableList(List.copyOf(inventoryTrajectories));
        productionTrajectories = productionTrajectories == null ? List.of() : Collections.unmodifiableList(List.copyOf(productionTrajectories));
        shortageTrajectories = shortageTrajectories == null ? List.of() : Collections.unmodifiableList(List.copyOf(shortageTrajectories));
        warnings = warnings == null ? List.of() : Collections.unmodifiableList(List.copyOf(warnings));
        assumptions = assumptions == null ? List.of() : Collections.unmodifiableList(List.copyOf(assumptions));

        totalShortageQuantity = totalShortageQuantity == null ? BigDecimal.ZERO : totalShortageQuantity;
        status = status == null ? "COMPLETED" : status;
    }
}

