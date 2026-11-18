package com.supplygraph.domain.model.planning;

import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.NodeId;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Split allocation of procurement volume among qualified suppliers based on capacity, cost, and risk.
 */
public record SupplierAllocationShare(
        NodeId supplierId,
        MaterialId materialId,
        double allocationPercentage,
        double allocatedQuantity,
        BigDecimal unitCost,
        BigDecimal totalCost
) implements Serializable {
    public SupplierAllocationShare {
        Objects.requireNonNull(supplierId, "Supplier ID cannot be null");
        Objects.requireNonNull(materialId, "Material ID cannot be null");
        Objects.requireNonNull(unitCost, "Unit cost cannot be null");
        Objects.requireNonNull(totalCost, "Total cost cannot be null");
        if (allocationPercentage < 0.0 || allocationPercentage > 100.0) {
            throw new IllegalArgumentException("Allocation percentage must be between 0 and 100");
        }
    }
}
