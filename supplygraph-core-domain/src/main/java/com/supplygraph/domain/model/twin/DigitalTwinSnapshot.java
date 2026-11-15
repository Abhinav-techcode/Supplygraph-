package com.supplygraph.domain.model.twin;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.BillOfMaterials;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.graph.SupplyChainGraph;
import com.supplygraph.domain.model.inventory.InventoryAccountId;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable point-in-time snapshot of digital twin network, inventory, and BOM state.
 */
public record DigitalTwinSnapshot(
        WorkspaceId workspaceId,
        StateVersion version,
        Instant timestamp,
        SupplyChainGraph graph,
        BillOfMaterials bom,
        Map<InventoryAccountId, Quantity> inventoryBalances
) {
    public DigitalTwinSnapshot {
        Objects.requireNonNull(workspaceId, "WorkspaceId must not be null");
        Objects.requireNonNull(version, "Version must not be null");
        Objects.requireNonNull(timestamp, "Timestamp must not be null");
        Objects.requireNonNull(graph, "Graph must not be null");
        Objects.requireNonNull(bom, "BOM must not be null");
        inventoryBalances = inventoryBalances == null ? Map.of() : Collections.unmodifiableMap(Map.copyOf(inventoryBalances));
    }
}

