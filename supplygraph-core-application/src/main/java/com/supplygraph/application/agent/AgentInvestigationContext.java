package com.supplygraph.application.agent;

import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable context provided to specialized agents during disruption investigation and replanning.
 */
public record AgentInvestigationContext(
        DisruptionIncident incident,
        DigitalTwinSnapshot snapshot,
        Map<String, Object> parameters
) {
    public AgentInvestigationContext {
        Objects.requireNonNull(incident, "incident must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
    }
}
