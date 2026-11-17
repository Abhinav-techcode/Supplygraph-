package com.supplygraph.domain.model.impact;

import com.supplygraph.domain.model.graph.EdgeId;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.graph.TransportMode;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Assessed transportation lane disruption, delay, and transit cost impact.
 */
public record LogisticsImpact(
        EdgeId laneId,
        NodeId sourceNodeId,
        NodeId targetNodeId,
        TransportMode transportMode,
        long delaySeconds,
        BigDecimal costIncrease
) {
    public LogisticsImpact {
        Objects.requireNonNull(laneId, "LaneId must not be null");
        Objects.requireNonNull(sourceNodeId, "SourceNodeId must not be null");
        Objects.requireNonNull(targetNodeId, "TargetNodeId must not be null");
        Objects.requireNonNull(transportMode, "TransportMode must not be null");
        costIncrease = costIncrease == null ? BigDecimal.ZERO : costIncrease;
    }
}

