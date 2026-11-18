package com.supplygraph.domain.model.planning;

import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.NodeId;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Net requirement generated through multi-tier BOM explosion during MRP runs.
 */
public record NetRequirementItem(
        MaterialId componentMaterialId,
        NodeId consumingPlantId,
        double grossRequirement,
        double onHandAvailable,
        double scheduledReceipts,
        double netRequirement,
        LocalDate requiredDate
) implements Serializable {
    public NetRequirementItem {
        Objects.requireNonNull(componentMaterialId, "Component material ID cannot be null");
        Objects.requireNonNull(consumingPlantId, "Consuming plant ID cannot be null");
        Objects.requireNonNull(requiredDate, "Required date cannot be null");
    }
}
