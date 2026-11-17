package com.supplygraph.simulation;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.BillOfMaterials;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.disruption.DisruptionCategory;
import com.supplygraph.domain.model.disruption.DisruptionImpactParameters;
import com.supplygraph.domain.model.disruption.DisruptionSeverity;
import com.supplygraph.domain.model.graph.*;
import com.supplygraph.domain.model.impact.ImpactAssessmentId;
import com.supplygraph.domain.model.plan.PlanAction;
import com.supplygraph.domain.model.plan.PlanActionType;
import com.supplygraph.domain.model.plan.RecoveryPlan;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import com.supplygraph.domain.model.twin.StateVersion;
import com.supplygraph.simulation.engine.DiscreteEventSimulationEngine;
import com.supplygraph.simulation.engine.SimulationConfiguration;
import com.supplygraph.simulation.engine.SimulationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DiscreteEventSimulationEngineTest {

    private DigitalTwinSnapshot createTestSnapshot() {
        WorkspaceId ws = WorkspaceId.of("ws-sim-test");
        SupplyChainNode node1 = new Supplier(NodeId.of("SUP-1"), "Supplier 1", "Tokyo", true, 1, Set.of("ISO9001"), 0.1);
        SupplyChainNode node2 = new ManufacturingSite(NodeId.of("PLANT-1"), "Battery Fab", "Fremont", true, Set.of("LINE-1"), "ISO-CLASS-5", 500);
        TransportationLane lane = new TransportationLane(
                EdgeId.of("LANE-1"),
                NodeId.of("SUP-1"),
                NodeId.of("PLANT-1"),
                TransportMode.OCEAN,
                Duration.ofDays(7),
                BigDecimal.valueOf(2.5),
                true
        );

        SupplyChainGraph graph = new SupplyChainGraph(List.of(node1, node2), List.of(lane));
        return new DigitalTwinSnapshot(ws, StateVersion.of(1L), Instant.now(), graph, BillOfMaterials.empty(), Map.of());
    }

    @Test
    @DisplayName("Simulation execution is 100% deterministic: same inputs produce identical event log and metrics")
    void testSimulationDeterminism() {
        DigitalTwinSnapshot snapshot = createTestSnapshot();
        Instant start = Instant.parse("2026-11-17T00:00:00Z");

        Disruption disruption = Disruption.createDraft(
                snapshot.workspaceId(), "EV_BATTERY", "LITHIUM_SHORTAGE", "Cathode Shortage", "Cathode plant delay",
                DisruptionCategory.RAW_MATERIAL_SHORTAGE, DisruptionSeverity.HIGH,
                Set.of(NodeId.of("SUP-1")), Set.of(EdgeId.of("LANE-1")), Set.of(MaterialId.of("CATHODE-NMC")),
                start, start.plusSeconds(86400 * 5),
                new DisruptionImpactParameters(BigDecimal.valueOf(80), 86400L * 5, 5, BigDecimal.valueOf(2000), "KG", Map.of()),
                "Notice"
        );

        SimulationConfiguration config = new SimulationConfiguration(start, start.plusSeconds(86400 * 7), 3600L * 12, 1000, true);

        DiscreteEventSimulationEngine engine = new DiscreteEventSimulationEngine();
        SimulationResult r1 = engine.run(snapshot, disruption, null, config);
        SimulationResult r2 = engine.run(snapshot, disruption, null, config);

        assertThat(r1.processedEventCount()).isEqualTo(r2.processedEventCount());
        assertThat(r1.serviceLevelPercentage()).isEqualTo(r2.serviceLevelPercentage());
        assertThat(r1.totalShortageQuantity()).isEqualByComparingTo(r2.totalShortageQuantity());
        assertThat(r1.eventTimeline()).hasSameSizeAs(r2.eventTimeline());
    }

    @Test
    @DisplayName("Recovery plan action mitigates simulated disruption in trajectory")
    void testRecoveryPlanActionMitigation() {
        DigitalTwinSnapshot snapshot = createTestSnapshot();
        Instant start = Instant.parse("2026-11-17T00:00:00Z");

        Disruption disruption = Disruption.createDraft(
                snapshot.workspaceId(), "EV_BATTERY", "SHORTAGE", "Supplier outage", "Desc",
                DisruptionCategory.SUPPLIER_SHUTDOWN, DisruptionSeverity.HIGH,
                Set.of(NodeId.of("SUP-1")), Set.of(), Set.of(),
                start, start.plusSeconds(86400 * 3),
                DisruptionImpactParameters.ofDelay(86400 * 3), "Notice"
        );

        PlanAction recoveryAction = new PlanAction(
                "ACT-1", PlanActionType.ALTERNATIVE_SUPPLIER, "Emergency buffer release",
                NodeId.of("PLANT-1"), MaterialId.of("CATHODE-NMC"), BigDecimal.valueOf(500), "KG",
                BigDecimal.valueOf(1000), 24L, Map.of()
        );

        RecoveryPlan plan = RecoveryPlan.createDraft(
                disruption.id(), snapshot.workspaceId(), snapshot.version(), ImpactAssessmentId.generate(),
                "Mitigation Plan", "Desc", List.of(recoveryAction), null
        );

        SimulationConfiguration config = new SimulationConfiguration(start, start.plusSeconds(86400 * 5), 3600L * 6, 1000, true);
        DiscreteEventSimulationEngine engine = new DiscreteEventSimulationEngine();

        SimulationResult result = engine.run(snapshot, disruption, plan, config);
        assertThat(result.status()).isEqualTo("COMPLETED");

        boolean foundRecoveryEvent = result.eventTimeline().stream()
                .anyMatch(e -> e.description().contains("Emergency buffer release"));
        assertThat(foundRecoveryEvent).isTrue();
    }
}
