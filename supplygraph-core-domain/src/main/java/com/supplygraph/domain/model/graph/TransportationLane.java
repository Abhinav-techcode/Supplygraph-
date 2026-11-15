package com.supplygraph.domain.model.graph;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;

/**
 * Directed edge representing a transportation lane between two supply chain nodes.
 */
public class TransportationLane {
    private final EdgeId id;
    private final NodeId sourceNodeId;
    private final NodeId targetNodeId;
    private final TransportMode transportMode;
    private final Duration nominalTransitTime;
    private final BigDecimal costPerKg;
    private boolean operational;

    public TransportationLane(EdgeId id, NodeId sourceNodeId, NodeId targetNodeId,
                              TransportMode transportMode, Duration nominalTransitTime,
                              BigDecimal costPerKg, boolean operational) {
        this.id = Objects.requireNonNull(id, "EdgeId must not be null");
        this.sourceNodeId = Objects.requireNonNull(sourceNodeId, "Source NodeId must not be null");
        this.targetNodeId = Objects.requireNonNull(targetNodeId, "Target NodeId must not be null");
        if (sourceNodeId.equals(targetNodeId)) {
            throw new IllegalArgumentException("Source and target nodes cannot be identical (no self-loops)");
        }
        this.transportMode = Objects.requireNonNull(transportMode, "TransportMode must not be null");
        this.nominalTransitTime = Objects.requireNonNull(nominalTransitTime, "Nominal transit time must not be null");
        if (nominalTransitTime.isNegative() || nominalTransitTime.isZero()) {
            throw new IllegalArgumentException("Transit time must be positive");
        }
        this.costPerKg = Objects.requireNonNull(costPerKg, "Cost per kg must not be null");
        if (costPerKg.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cost cannot be negative");
        }
        this.operational = operational;
    }

    public EdgeId getId() {
        return id;
    }

    public NodeId getSourceNodeId() {
        return sourceNodeId;
    }

    public NodeId getTargetNodeId() {
        return targetNodeId;
    }

    public TransportMode getTransportMode() {
        return transportMode;
    }

    public Duration getNominalTransitTime() {
        return nominalTransitTime;
    }

    public BigDecimal getCostPerKg() {
        return costPerKg;
    }

    public boolean isOperational() {
        return operational;
    }

    public void setOperational(boolean operational) {
        this.operational = operational;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransportationLane that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TransportationLane{" +
                "id=" + id +
                ", source=" + sourceNodeId +
                ", target=" + targetNodeId +
                ", mode=" + transportMode +
                ", transitTime=" + nominalTransitTime +
                ", operational=" + operational +
                '}';
    }
}

