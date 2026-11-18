package com.supplygraph.domain.model.sustainability;

import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.NodeId;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Node in the batch/lot genealogy chain of custody.
 */
public record TraceabilityNode(
        TraceabilityNodeId id,
        String batchNumber,
        TraceabilityNodeType type,
        MaterialId materialId,
        NodeId locationId,
        double quantity,
        String unitOfMeasure,
        String custodyHolder,
        Instant recordedAt
) implements Serializable {
    public TraceabilityNode {
        Objects.requireNonNull(id, "Traceability node ID cannot be null");
        Objects.requireNonNull(batchNumber, "Batch number cannot be null");
        Objects.requireNonNull(type, "Traceability type cannot be null");
        Objects.requireNonNull(materialId, "Material ID cannot be null");
        Objects.requireNonNull(locationId, "Location ID cannot be null");
        Objects.requireNonNull(unitOfMeasure, "Unit of measure cannot be null");
        Objects.requireNonNull(custodyHolder, "Custody holder cannot be null");
        Objects.requireNonNull(recordedAt, "Recorded timestamp cannot be null");
    }
}

