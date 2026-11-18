package com.supplygraph.domain.model.sustainability;

import com.supplygraph.domain.model.graph.NodeId;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Result of forward genealogy recall impact analysis detailing tainted lot exposure across the network.
 */
public record RecallImpactResult(
        String taintedBatchNumber,
        List<TraceabilityNode> affectedDownstreamNodes,
        List<NodeId> impactedFacilityLocations,
        List<String> impactedCustomerShipments,
        double totalCompromisedVolume
) implements Serializable {
    public RecallImpactResult {
        Objects.requireNonNull(taintedBatchNumber, "Tainted batch number cannot be null");
        affectedDownstreamNodes = List.copyOf(affectedDownstreamNodes);
        impactedFacilityLocations = List.copyOf(impactedFacilityLocations);
        impactedCustomerShipments = List.copyOf(impactedCustomerShipments);
    }
}

