package com.supplygraph.application.graph;

import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.port.out.DisruptionRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.BillOfMaterials;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.disruption.*;
import com.supplygraph.domain.model.graph.*;
import com.supplygraph.domain.model.inventory.*;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.StateVersion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NetworkGraphViewServiceTest {
    private final WorkspaceId ws = WorkspaceId.of("ws");

    private DigitalTwinState twin() {
        Supplier a = new Supplier(NodeId.of("A"), "A", "X", true, 2, Set.of(), 0.1), b = new Supplier(NodeId.of("B"), "B", "X", true, 1, Set.of(), 0.1);
        ManufacturingSite p = new ManufacturingSite(NodeId.of("P"), "P", "X", true, Set.of(), "", 50);
        Warehouse w = new Warehouse(NodeId.of("W"), "W", "X", true, new BigDecimal("100"), false, false);
        TransportationLane ab = lane("AB", "A", "B"), bp = lane("BP", "B", "P"), ap = lane("AP", "A", "P"), pw = lane("PW", "P", "W");
        DigitalTwinState s = new DigitalTwinState(ws, StateVersion.of(3), new SupplyChainGraph(List.of(a, b, p, w), List.of(ab, bp, ap, pw)), BillOfMaterials.empty());
        InventoryAccountId acct = InventoryAccountId.of(NodeId.of("P"), MaterialId.of("M"), BalanceCategory.ON_HAND_AVAILABLE);
        InventoryAccountId off = InventoryAccountId.of(NodeId.of("P"), MaterialId.of("M"), BalanceCategory.EXTERNAL_OFFSET);
        Quantity q = Quantity.of(40, UnitOfMeasure.PIECE);
        s.getInventoryLedger().postTransaction(new InventoryTransaction(TransactionId.generate(), Instant.now(), "SEED", "t",
                List.of(new InventoryPosting("1", acct, PostingType.DEBIT, q), new InventoryPosting("2", off, PostingType.CREDIT, q))));
        return s;
    }

    private static TransportationLane lane(String id, String from, String to) {
        return new TransportationLane(EdgeId.of(id), NodeId.of(from), NodeId.of(to), TransportMode.ROAD, Duration.ofHours(5), BigDecimal.ONE, true);
    }

    private Disruption disruption(String node, DisruptionStatus status) {
        Instant now = Instant.now();
        Disruption d = Disruption.createDraft(ws, "EV", "T", "Outage at " + node, "d", DisruptionCategory.SUPPLIER_SHUTDOWN, DisruptionSeverity.HIGH,
                Set.of(NodeId.of(node)), Set.of(EdgeId.of("BP")), Set.of(), now, now.plusSeconds(3600),
                new DisruptionImpactParameters(BigDecimal.TEN, 3600L, 1, BigDecimal.TEN, "PCS", Map.of()), "e");
        return status == DisruptionStatus.DRAFT ? d : d.validate(now).activate(now);
    }

    private NetworkGraphViewService service(Disruption... active) {
        DigitalTwinRepositoryPort tp = mock(DigitalTwinRepositoryPort.class);
        when(tp.findByWorkspaceId(eq(ws))).thenReturn(Optional.of(twin()));
        DisruptionRepositoryPort dp = mock(DisruptionRepositoryPort.class);
        when(dp.findByWorkspaceIdAndStatus(any(), eq(DisruptionStatus.ACTIVE))).thenReturn(List.of(active));
        return new NetworkGraphViewService(tp, dp);
    }

    @Test void layersFollowLongestPathSoEveryLaneGoesLeftToRight() {
        NetworkGraphView v = service().build(ws);
        Map<String, Integer> layer = new HashMap<>();
        v.nodes().forEach(n -> layer.put(n.id(), n.layer()));
        assertThat(layer).containsEntry("A", 0).containsEntry("B", 1).containsEntry("P", 2).containsEntry("W", 3);
        v.lanes().forEach(l -> assertThat(layer.get(l.fromId())).isLessThan(layer.get(l.toId())));
        assertThat(v.layers()).hasSize(4);
        assertThat(v.twinVersion()).isEqualTo(3);
    }

    @Test void disruptionOverlayComesFromActiveDisruptionsOnly() {
        NetworkGraphView quiet = service().build(ws);
        assertThat(quiet.disruptedNodeCount()).isZero();
        assertThat(quiet.lanes()).noneMatch(NetworkGraphView.Lane::disrupted);

        NetworkGraphView hit = service(disruption("B", DisruptionStatus.ACTIVE)).build(ws);
        assertThat(hit.nodes()).filteredOn(NetworkGraphView.Node::disrupted).extracting(NetworkGraphView.Node::id).containsExactly("B");
        assertThat(hit.nodes().stream().filter(n -> n.id().equals("B")).findFirst().orElseThrow().disruptionTitles()).containsExactly("Outage at B");
        assertThat(hit.lanes()).filteredOn(NetworkGraphView.Lane::disrupted).extracting(NetworkGraphView.Lane::id).containsExactly("BP");
        assertThat(hit.activeDisruptions()).hasSize(1);
    }

    @Test void inventoryAndCapacityComeFromTheTwin() {
        NetworkGraphView v = service().build(ws);
        NetworkGraphView.Node p = v.nodes().stream().filter(n -> n.id().equals("P")).findFirst().orElseThrow();
        assertThat(p.onHandUnits()).isEqualTo(40.0);
        assertThat(p.capacityText()).isEqualTo("50 units/h");
        assertThat(v.nodes().stream().filter(n -> n.id().equals("A")).findFirst().orElseThrow().tierLabel()).isEqualTo("Tier 2");
    }

    @Test void missingTwinYieldsAnEmptyViewNotFictionalData() {
        DigitalTwinRepositoryPort tp = mock(DigitalTwinRepositoryPort.class);
        when(tp.findByWorkspaceId(any())).thenReturn(Optional.empty());
        NetworkGraphView v = new NetworkGraphViewService(tp, mock(DisruptionRepositoryPort.class)).build(ws);
        assertThat(v.isEmpty()).isTrue();
    }
}
