package com.supplygraph.domain.model.twin;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.BillOfMaterials;
import com.supplygraph.domain.model.bom.FinishedProduct;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.graph.*;
import com.supplygraph.domain.model.inventory.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DigitalTwinStateTest {

    @Test
    @DisplayName("DigitalTwinState increments versions monotonically on state transitions")
    void shouldIncrementStateVersionsMonotonically() {
        WorkspaceId wsId = WorkspaceId.of("WS-TWIN-01");
        DigitalTwinState twin = new DigitalTwinState(wsId, StateVersion.initial(), null, null);

        assertThat(twin.getCurrentVersion()).isEqualTo(StateVersion.of(0L));

        // 1. Update Graph
        Supplier supplier = new Supplier(NodeId.of("SUP-1"), "Supplier 1", "USA", true, 1, Set.of(), 0.05);
        SupplyChainGraph graph = new SupplyChainGraph(List.of(supplier), List.of());
        DomainEvent event1 = DomainEvent.of("EVT-01", "GRAPH_INITIALIZED", Instant.now(), null, Map.of());

        boolean updatedGraph = twin.updateGraph(graph, event1);
        assertThat(updatedGraph).isTrue();
        assertThat(twin.getCurrentVersion()).isEqualTo(StateVersion.of(1L));

        // 2. Update BOM
        FinishedProduct product = new FinishedProduct(
                MaterialId.of("FP-1"), "SKU-1", "Product 1", UnitOfMeasure.PIECE, "Line 1", 12);
        BillOfMaterials bom = new BillOfMaterials(List.of(product), List.of());
        DomainEvent event2 = DomainEvent.of("EVT-02", "BOM_INITIALIZED", Instant.now(), null, Map.of());

        boolean updatedBom = twin.updateBom(bom, event2);
        assertThat(updatedBom).isTrue();
        assertThat(twin.getCurrentVersion()).isEqualTo(StateVersion.of(2L));

        // 3. Post Inventory
        InventoryAccountId onHand = InventoryAccountId.of(supplier.getId(), product.getId(), BalanceCategory.ON_HAND_AVAILABLE);
        InventoryAccountId external = InventoryAccountId.of(NodeId.of("CLEARING"), product.getId(), BalanceCategory.EXTERNAL_OFFSET);
        InventoryTransaction tx = InventoryTransaction.transfer(
                TransactionId.of("TX-INIT-01"), Instant.now(), "RECEIPT", "PO-1",
                external, onHand, Quantity.of(50, UnitOfMeasure.PIECE));
        DomainEvent event3 = DomainEvent.of("EVT-03", "INVENTORY_RECEIPT", Instant.now(), null, Map.of());

        boolean updatedInv = twin.postInventoryTransaction(tx, event3);
        assertThat(updatedInv).isTrue();
        assertThat(twin.getCurrentVersion()).isEqualTo(StateVersion.of(3L));

        assertThat(twin.getTransitionHistory()).hasSize(3);
        assertThat(twin.getTransitionHistory().get(0).fromVersion()).isEqualTo(StateVersion.of(0L));
        assertThat(twin.getTransitionHistory().get(0).toVersion()).isEqualTo(StateVersion.of(1L));
    }

    @Test
    @DisplayName("DigitalTwinState deduplicates duplicate domain events without state mutation")
    void shouldDeduplicateEventsIdempotently() {
        WorkspaceId wsId = WorkspaceId.of("WS-TWIN-02");
        DigitalTwinState twin = new DigitalTwinState(wsId, StateVersion.initial(), null, null);

        Supplier supplier = new Supplier(NodeId.of("SUP-1"), "Supplier 1", "USA", true, 1, Set.of(), 0.05);
        SupplyChainGraph graph = new SupplyChainGraph(List.of(supplier), List.of());
        DomainEvent event = DomainEvent.of("EVT-DUPLICATE-KEY", "GRAPH_INITIALIZED", Instant.now(), null, Map.of());

        boolean first = twin.updateGraph(graph, event);
        boolean duplicate = twin.updateGraph(graph, event);

        assertThat(first).isTrue();
        assertThat(duplicate).isFalse();
        assertThat(twin.getCurrentVersion()).isEqualTo(StateVersion.of(1L));
        assertThat(twin.getTransitionHistory()).hasSize(1);
    }

    @Test
    @DisplayName("Snapshots are immutable captures unaffected by subsequent ledger changes")
    void shouldProduceImmutableSnapshots() {
        WorkspaceId wsId = WorkspaceId.of("WS-TWIN-03");
        Supplier supplier = new Supplier(NodeId.of("SUP-1"), "Supplier 1", "USA", true, 1, Set.of(), 0.05);
        FinishedProduct product = new FinishedProduct(
                MaterialId.of("FP-1"), "SKU-1", "Product 1", UnitOfMeasure.PIECE, "Line 1", 12);
        SupplyChainGraph graph = new SupplyChainGraph(List.of(supplier), List.of());
        BillOfMaterials bom = new BillOfMaterials(List.of(product), List.of());

        DigitalTwinState twin = new DigitalTwinState(wsId, StateVersion.initial(), graph, bom);

        InventoryAccountId onHand = InventoryAccountId.of(supplier.getId(), product.getId(), BalanceCategory.ON_HAND_AVAILABLE);
        InventoryAccountId external = InventoryAccountId.of(NodeId.of("CLEARING"), product.getId(), BalanceCategory.EXTERNAL_OFFSET);

        // Initial deposit of 100 PIECES
        twin.postInventoryTransaction(
                InventoryTransaction.transfer(TransactionId.of("TX-1"), Instant.now(), "RECEIPT", "PO-1",
                        external, onHand, Quantity.of(100, UnitOfMeasure.PIECE)),
                DomainEvent.of("EVT-1", "RECEIPT", Instant.now(), Map.of()));

        // Capture snapshot at v1 with 100 PIECES
        DigitalTwinSnapshot snapshot = twin.createSnapshot();
        assertThat(snapshot.version()).isEqualTo(StateVersion.of(1L));
        assertThat(snapshot.inventoryBalances().get(onHand).amount()).isEqualByComparingTo("100.0000");

        // Subsequent transaction: consume 40 PIECES
        twin.postInventoryTransaction(
                InventoryTransaction.transfer(TransactionId.of("TX-2"), Instant.now(), "CONSUMPTION", "MO-1",
                        onHand, external, Quantity.of(40, UnitOfMeasure.PIECE)),
                DomainEvent.of("EVT-2", "CONSUME", Instant.now(), Map.of()));

        // Active twin state is now at v2 with 60 PIECES
        assertThat(twin.getCurrentVersion()).isEqualTo(StateVersion.of(2L));
        assertThat(twin.getInventoryLedger().getBalance(onHand, UnitOfMeasure.PIECE).amount()).isEqualByComparingTo("60.0000");

        // Snapshot remains untouched at v1 with 100 PIECES
        assertThat(snapshot.version()).isEqualTo(StateVersion.of(1L));
        assertThat(snapshot.inventoryBalances().get(onHand).amount()).isEqualByComparingTo("100.0000");
    }
}

