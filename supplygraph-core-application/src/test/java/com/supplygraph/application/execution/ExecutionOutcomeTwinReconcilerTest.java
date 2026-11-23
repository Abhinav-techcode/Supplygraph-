package com.supplygraph.application.execution;

import com.supplygraph.application.audit.AuditGovernanceService;
import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.*;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.event.*;
import com.supplygraph.domain.model.graph.*;
import com.supplygraph.domain.model.inventory.*;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.StateVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Verified execution outcome moves stock in the twin (balanced double entry)")
class ExecutionOutcomeTwinReconcilerTest {
    private final WorkspaceId ws = WorkspaceId.of("WS-REC");
    private final Map<WorkspaceId, DigitalTwinState> store = new HashMap<>();
    private ExecutionOutcomeTwinReconciler reconciler;

    private InventoryAccountId acc(String node) { return InventoryAccountId.of(NodeId.of(node), MaterialId.of("MAT"), BalanceCategory.ON_HAND_AVAILABLE); }

    @BeforeEach
    void setUp() {
        Supplier a = new Supplier(NodeId.of("SRC"), "Source", "X", true, 1, Set.of(), 0.1);
        ManufacturingSite b = new ManufacturingSite(NodeId.of("DST"), "Dest", "Y", true, Set.of("L"), "NONE", 10);
        SupplyChainGraph g = new SupplyChainGraph(List.of(a, b), List.of(new TransportationLane(EdgeId.of("L1"), NodeId.of("SRC"), NodeId.of("DST"), TransportMode.ROAD, Duration.ofHours(5), BigDecimal.ONE, true)));
        BillOfMaterials bom = new BillOfMaterials(List.of(new Component(MaterialId.of("MAT"), "M", "Mat", UnitOfMeasure.PIECE, 3)), List.of());
        DigitalTwinState twin = new DigitalTwinState(ws, StateVersion.of(1), g, bom);
        Quantity q = Quantity.of(1000, UnitOfMeasure.PIECE);
        twin.getInventoryLedger().postTransaction(new InventoryTransaction(TransactionId.generate(), Instant.now(), "OPENING", "t",
                List.of(new InventoryPosting("p1", acc("SRC"), PostingType.DEBIT, q), new InventoryPosting("p2", InventoryAccountId.of(NodeId.of("SRC"), MaterialId.of("MAT"), BalanceCategory.EXTERNAL_OFFSET), PostingType.CREDIT, q))));
        store.put(ws, twin);
        DigitalTwinRepositoryPort repo = new DigitalTwinRepositoryPort() {
            public void save(DigitalTwinState s) { store.put(s.getWorkspaceId(), s); }
            public Optional<DigitalTwinState> findByWorkspaceId(WorkspaceId w) { return Optional.ofNullable(store.get(w)); }
            public boolean existsByWorkspaceId(WorkspaceId w) { return store.containsKey(w); }
        };
        reconciler = new ExecutionOutcomeTwinReconciler(repo, Mockito.mock(AuditGovernanceService.class));
    }

    private OperationalEvent event(Map<String, String> payload, String key) {
        return OperationalEvent.createIngested(EventType.RECOVERY_ACTION_COMPLETED, EventSource.MANUAL_OPERATOR, Instant.now(), "DST", "FACILITY", ws, payload,
                new EventProvenance("TEST", "tester", "B", "T", Instant.now()), key);
    }

    private double balance(String node) { return store.get(ws).getInventoryLedger().getBalance(acc(node), UnitOfMeasure.PIECE).amount().doubleValue(); }

    @Test
    @DisplayName("Stock leaves the source, arrives at the target, and the twin version advances")
    void movesStock() {
        boolean changed = reconciler.reconcile(event(Map.of("quantity", "400", "materialId", "MAT", "sourceNodeId", "SRC", "targetNodeId", "DST"), "k1"));
        assertThat(changed).isTrue();
        assertThat(balance("SRC")).isEqualTo(600.0);
        assertThat(balance("DST")).isEqualTo(400.0);
        assertThat(store.get(ws).getCurrentVersion().value()).isEqualTo(2);
    }

    @Test
    @DisplayName("A transfer larger than the source's stock is refused and nothing changes")
    void refusesOverdraft() {
        boolean changed = reconciler.reconcile(event(Map.of("quantity", "5000", "materialId", "MAT", "sourceNodeId", "SRC", "targetNodeId", "DST"), "k2"));
        assertThat(changed).isFalse();
        assertThat(balance("SRC")).isEqualTo(1000.0);
        assertThat(balance("DST")).isZero();
        assertThat(store.get(ws).getCurrentVersion().value()).isEqualTo(1);
    }

    @Test
    @DisplayName("Evidence without a material or both facilities changes nothing (no assumed defaults)")
    void incompleteEvidenceChangesNothing() {
        assertThat(reconciler.reconcile(event(Map.of("quantity", "10", "targetNodeId", "DST"), "k3"))).isFalse();
        assertThat(reconciler.reconcile(event(Map.of("quantity", "10", "materialId", "MAT", "targetNodeId", "DST"), "k4"))).isFalse();
        assertThat(balance("SRC")).isEqualTo(1000.0);
    }

    @Test
    @DisplayName("Other event types are ignored")
    void ignoresOtherEvents() {
        OperationalEvent other = OperationalEvent.createIngested(EventType.SUPPLIER_DELIVERY_DELAYED, EventSource.MANUAL_OPERATOR, Instant.now(), "DST", "FACILITY", ws, Map.of("quantity", "10"),
                new EventProvenance("TEST", "tester", "B", "T", Instant.now()), "k5");
        assertThat(reconciler.reconcile(other)).isFalse();
    }
}
