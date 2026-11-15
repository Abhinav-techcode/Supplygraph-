package com.supplygraph.application.port.in;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.BillOfMaterials;
import com.supplygraph.domain.model.capacity.CapacityReservation;
import com.supplygraph.domain.model.graph.SupplyChainGraph;
import com.supplygraph.domain.model.inventory.InventoryTransaction;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.DomainEvent;

/**
 * Primary inbound use case boundary for Digital Twin state management.
 */
public interface DigitalTwinUseCase {
    DigitalTwinState createDigitalTwin(WorkspaceId workspaceId, SupplyChainGraph graph, BillOfMaterials bom);
    boolean recordInventoryTransaction(WorkspaceId workspaceId, InventoryTransaction transaction, DomainEvent triggerEvent);
    boolean reserveCapacity(WorkspaceId workspaceId, CapacityReservation reservation, DomainEvent triggerEvent, boolean allowOverbooking);
    DigitalTwinSnapshot getSnapshot(WorkspaceId workspaceId);
}

