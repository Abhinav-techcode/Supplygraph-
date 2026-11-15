package com.supplygraph.domain.model.inventory;

import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.NodeId;

import java.util.Objects;
import java.util.Optional;

public record InventoryAccountId(
        NodeId nodeId,
        MaterialId materialId,
        BalanceCategory category,
        Optional<LotNumber> lotNumber
) {
    public InventoryAccountId {
        Objects.requireNonNull(nodeId, "NodeId must not be null");
        Objects.requireNonNull(materialId, "MaterialId must not be null");
        Objects.requireNonNull(category, "Category must not be null");
        Objects.requireNonNull(lotNumber, "LotNumber optional must not be null");
    }

    public static InventoryAccountId of(NodeId nodeId, MaterialId materialId, BalanceCategory category) {
        return new InventoryAccountId(nodeId, materialId, category, Optional.empty());
    }

    public static InventoryAccountId of(NodeId nodeId, MaterialId materialId, BalanceCategory category, LotNumber lot) {
        return new InventoryAccountId(nodeId, materialId, category, Optional.ofNullable(lot));
    }
}

