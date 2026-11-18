package com.supplygraph.application.service;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.*;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.disruption.*;
import com.supplygraph.domain.model.graph.*;
import com.supplygraph.domain.model.inventory.*;
import com.supplygraph.domain.model.plan.PlanActionType;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.StateVersion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class RecoveryOptionDeriverTest {
    private final WorkspaceId ws = WorkspaceId.of("ws");
    private static final MaterialId CHIP = MaterialId.of("CHIP");

    private static TransportationLane lane(String id, String f, String t, int hours, String cost) {
        return new TransportationLane(EdgeId.of(id), NodeId.of(f), NodeId.of(t), TransportMode.ROAD, Duration.ofHours(hours), new BigDecimal(cost), true);
    }

    /** FAB (disrupted) -> PLANT; SUP-2 -> HUB -> PLANT; WH holds buffer stock and reaches PLANT via HUB or a slow direct lane. */
    private DigitalTwinState twin(boolean withStock) {
        Supplier fab = new Supplier(NodeId.of("FAB"), "Fab", "Munich", true, 2, Set.of(), 0.15);
        Supplier alt = new Supplier(NodeId.of("SUP-2"), "Alt Supplier", "Hsinchu", true, 2, Set.of(), 0.10);
        LogisticsHub hub = new LogisticsHub(NodeId.of("HUB"), "Hub", "Frankfurt", true, true, 100, Set.of("ROAD"));
        Warehouse wh = new Warehouse(NodeId.of("WH"), "Warehouse", "Ulm", true, new BigDecimal("500"), false, false);
        ManufacturingSite plant = new ManufacturingSite(NodeId.of("PLANT"), "Plant", "Stuttgart", true, Set.of("L1"), "", 10);
        SupplyChainGraph g = new SupplyChainGraph(List.of(fab, alt, hub, wh, plant), List.of(
                lane("FAB-PLANT", "FAB", "PLANT", 6, "1.0"), lane("ALT-HUB", "SUP-2", "HUB", 30, "4.0"), lane("HUB-PLANT", "HUB", "PLANT", 4, "1.0"),
                lane("WH-HUB", "WH", "HUB", 2, "0.5"), lane("WH-PLANT", "WH", "PLANT", 20, "2.0")));
        Component chip = new Component(CHIP, "SKU", "Chip", UnitOfMeasure.PIECE, 7);
        DigitalTwinState s = new DigitalTwinState(ws, StateVersion.of(1), g, new BillOfMaterials(List.of(chip), List.of()));
        if (withStock) {
            post(s, "WH", 1000);
            post(s, "PLANT", 300);
        }
        return s;
    }

    private static void post(DigitalTwinState s, String node, int qty) {
        InventoryAccountId a = InventoryAccountId.of(NodeId.of(node), CHIP, BalanceCategory.ON_HAND_AVAILABLE), off = InventoryAccountId.of(NodeId.of(node), CHIP, BalanceCategory.EXTERNAL_OFFSET);
        Quantity q = Quantity.of(qty, UnitOfMeasure.PIECE);
        s.getInventoryLedger().postTransaction(new InventoryTransaction(TransactionId.generate(), Instant.now(), "SEED", node,
                List.of(new InventoryPosting("a" + node, a, PostingType.DEBIT, q), new InventoryPosting("b" + node, off, PostingType.CREDIT, q))));
    }

    private Disruption outage(String node, String lane) {
        Instant now = Instant.now();
        return Disruption.createDraft(ws, "EV", "T", "Fab outage", "d", DisruptionCategory.SUPPLIER_SHUTDOWN, DisruptionSeverity.HIGH,
                Set.of(NodeId.of(node)), lane == null ? Set.of() : Set.of(EdgeId.of(lane)), Set.of(CHIP), now, now.plusSeconds(86400 * 2),
                new DisruptionImpactParameters(BigDecimal.valueOf(100), 86400L * 2, 2, BigDecimal.valueOf(500), "PCS", Map.of()), "e");
    }

    @Test void derivesBufferSupplierAndResequencingOptionsFromTheTwin() {
        List<RecoveryOptionDeriver.Option> o = RecoveryOptionDeriver.derive(outage("FAB", "FAB-PLANT"), twin(true));
        assertThat(o).extracting(x -> x.action().actionType()).containsExactly(PlanActionType.EMERGENCY_BUFFER_RELEASE, PlanActionType.ALTERNATIVE_SUPPLIER, PlanActionType.PRODUCTION_RESCHEDULING);

        var buffer = o.get(0);
        assertThat(buffer.action().parameters()).containsEntry("sourceNode", "WH").containsEntry("targetNode", "PLANT");
        assertThat(buffer.action().parameters().get("route")).isEqualTo("WH > HUB > PLANT");           // 2h+4h beats the 20h direct lane
        assertThat(buffer.metrics().recoveryDurationDays()).isEqualTo(6.0 / 24.0);
        // requirement = 10 units/h * 24 * 2 days = 480; buffer 1000 covers all; cost = sum of lane costs (0.5+1.0)
        assertThat(buffer.action().quantity()).isEqualByComparingTo("480");
        assertThat(buffer.metrics().totalCost()).isEqualByComparingTo("1.50");
        assertThat(buffer.metrics().serviceLevelImpactPercentage()).isEqualTo(100.0);

        var supplier = o.get(1);
        assertThat(supplier.action().parameters()).containsEntry("supplierId", "SUP-2").containsEntry("procurementLeadDays", "7");
        assertThat(supplier.metrics().recoveryDurationDays()).isEqualTo((34 + 7 * 24) / 24.0);          // 30h+4h transit + 7 days lead time

        var resequence = o.get(2);
        assertThat(resequence.metrics().serviceLevelImpactPercentage()).isEqualTo(62.5);                // 300 on hand / 480 required
    }

    @Test void disruptedLanesAndNodesAreNeverUsed() {
        List<RecoveryOptionDeriver.Option> o = RecoveryOptionDeriver.derive(outage("FAB", "HUB-PLANT"), twin(true));
        // with HUB-PLANT blocked, the only usable route from WH is the direct 20h lane
        assertThat(o.get(0).action().parameters().get("route")).isEqualTo("WH > PLANT");
        assertThat(o).noneMatch(x -> x.action().actionType() == PlanActionType.ALTERNATIVE_SUPPLIER);  // SUP-2 can no longer reach PLANT
    }

    @Test void noStockMeansNoBufferOrResequencingOptionAndNothingIsInvented() {
        List<RecoveryOptionDeriver.Option> o = RecoveryOptionDeriver.derive(outage("FAB", "FAB-PLANT"), twin(false));
        assertThat(o).extracting(x -> x.action().actionType()).containsExactly(PlanActionType.ALTERNATIVE_SUPPLIER);
    }

    @Test void disruptionOutsideTheNetworkYieldsNoOptions() {
        assertThat(RecoveryOptionDeriver.derive(outage("UNKNOWN", null), twin(true))).isEmpty();
    }
}
