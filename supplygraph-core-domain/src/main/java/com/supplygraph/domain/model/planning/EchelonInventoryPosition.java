package com.supplygraph.domain.model.planning;

import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.NodeId;

import java.io.Serializable;
import java.util.Objects;

/**
 * Inventory position and buffering parameters at a specific echelon node.
 */
public record EchelonInventoryPosition(
        NodeId locationId,
        EchelonTier tier,
        MaterialId materialId,
        double onHandQuantity,
        double inTransitQuantity,
        double safetyStock,
        double reorderPoint,
        double maxStock,
        double carryingCostPerUnit
) implements Serializable {
    public EchelonInventoryPosition {
        Objects.requireNonNull(locationId, "Location ID cannot be null");
        Objects.requireNonNull(tier, "Echelon tier cannot be null");
        Objects.requireNonNull(materialId, "Material ID cannot be null");
    }

    public double getInventoryPosition() {
        return onHandQuantity + inTransitQuantity;
    }

    public boolean isReorderRequired() {
        return getInventoryPosition() <= reorderPoint;
    }

    public double calculateAnnualCarryingCost() {
        return onHandQuantity * carryingCostPerUnit;
    }
}
