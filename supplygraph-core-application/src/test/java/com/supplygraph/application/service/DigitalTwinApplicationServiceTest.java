package com.supplygraph.application.service;

import com.supplygraph.domain.error.ConflictException;
import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.port.out.InventoryLedgerRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.BillOfMaterials;
import com.supplygraph.domain.model.bom.FinishedProduct;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.graph.Supplier;
import com.supplygraph.domain.model.graph.SupplyChainGraph;
import com.supplygraph.domain.model.inventory.BalanceCategory;
import com.supplygraph.domain.model.inventory.InventoryAccountId;
import com.supplygraph.domain.model.inventory.InventoryTransaction;
import com.supplygraph.domain.model.inventory.TransactionId;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.DomainEvent;
import com.supplygraph.domain.model.twin.StateVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DigitalTwinApplicationServiceTest {

    private DigitalTwinRepositoryPort twinRepo;
    private InventoryLedgerRepositoryPort invRepo;
    private DigitalTwinApplicationService service;

    @BeforeEach
    void setUp() {
        twinRepo = mock(DigitalTwinRepositoryPort.class);
        invRepo = mock(InventoryLedgerRepositoryPort.class);
        service = new DigitalTwinApplicationService(twinRepo, invRepo);
    }

    @Test
    @DisplayName("Creates new digital twin and persists aggregate")
    void shouldCreateDigitalTwin() {
        WorkspaceId wsId = WorkspaceId.of("WS-APP-01");
        when(twinRepo.existsByWorkspaceId(wsId)).thenReturn(false);

        DigitalTwinState state = service.createDigitalTwin(wsId, SupplyChainGraph.empty(), BillOfMaterials.empty());

        assertThat(state.getWorkspaceId()).isEqualTo(wsId);
        assertThat(state.getCurrentVersion()).isEqualTo(StateVersion.of(0L));
        verify(twinRepo).save(any(DigitalTwinState.class));
    }

    @Test
    @DisplayName("Rejects duplicate creation for existing workspace")
    void shouldRejectDuplicateWorkspace() {
        WorkspaceId wsId = WorkspaceId.of("WS-APP-01");
        when(twinRepo.existsByWorkspaceId(wsId)).thenReturn(true);

        assertThatThrownBy(() -> service.createDigitalTwin(wsId, SupplyChainGraph.empty(), BillOfMaterials.empty()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Digital twin already exists");
    }

    @Test
    @DisplayName("Records inventory transaction and updates ports")
    void shouldRecordInventoryTransaction() {
        WorkspaceId wsId = WorkspaceId.of("WS-APP-02");
        Supplier supplier = new Supplier(NodeId.of("SUP-1"), "Supplier 1", "USA", true, 1, Set.of(), 0.05);
        FinishedProduct product = new FinishedProduct(
                MaterialId.of("FP-1"), "SKU-1", "Product 1", UnitOfMeasure.PIECE, "Line 1", 12);
        DigitalTwinState state = new DigitalTwinState(
                wsId, StateVersion.initial(), new SupplyChainGraph(List.of(supplier), List.of()), new BillOfMaterials(List.of(product), List.of()));

        when(twinRepo.findByWorkspaceId(wsId)).thenReturn(Optional.of(state));

        InventoryAccountId onHand = InventoryAccountId.of(supplier.getId(), product.getId(), BalanceCategory.ON_HAND_AVAILABLE);
        InventoryAccountId external = InventoryAccountId.of(NodeId.of("CLEARING"), product.getId(), BalanceCategory.EXTERNAL_OFFSET);
        InventoryTransaction tx = InventoryTransaction.transfer(
                TransactionId.of("TX-APP-1"), Instant.now(), "RECEIPT", "PO-1",
                external, onHand, Quantity.of(100, UnitOfMeasure.PIECE));
        DomainEvent evt = DomainEvent.of("EVT-APP-1", "RECEIPT", Instant.now(), Map.of());

        boolean recorded = service.recordInventoryTransaction(wsId, tx, evt);

        assertThat(recorded).isTrue();
        assertThat(state.getCurrentVersion()).isEqualTo(StateVersion.of(1L));
        verify(twinRepo).save(state);
        verify(invRepo).recordTransaction(wsId, tx);
        verify(invRepo, atLeastOnce()).updateBalance(eq(wsId), eq(onHand), any(Quantity.class));
    }
}

