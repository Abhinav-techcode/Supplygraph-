package com.supplygraph.adapter.ai.workflow;

import com.supplygraph.adapter.ai.agent.*;
import com.supplygraph.application.agent.DisruptionIncident;
import com.supplygraph.application.agent.SpecializedAgent;
import com.supplygraph.application.recovery.AgenticRecoveryOrchestrator;
import com.supplygraph.application.recovery.RecoveryProposal;
import com.supplygraph.application.tool.AgentTool;
import com.supplygraph.application.tool.builtin.QueryBomTool;
import com.supplygraph.application.tool.builtin.QueryCapacityTool;
import com.supplygraph.application.tool.builtin.QueryInventoryTool;
import com.supplygraph.application.tool.builtin.QueryLaneStatusTool;
import com.supplygraph.concurguard.api.DefaultConcurGuardEngine;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.*;
import com.supplygraph.domain.model.capacity.*;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.graph.*;
import com.supplygraph.domain.model.inventory.BalanceCategory;
import com.supplygraph.domain.model.inventory.InventoryAccountId;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import com.supplygraph.domain.model.twin.StateVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("End-to-End Multi-Agent Disruption Recovery Workflow Tests")
class EndToEndDisruptionRecoveryWorkflowTest {

    private DefaultConcurGuardEngine concurGuardEngine;
    private List<SpecializedAgent> allSpecializedAgents;
    private WorkspaceId workspaceId;
    private DigitalTwinSnapshot snapshot;
    private List<AgentTool> registeredTools;
    private AgenticRecoveryOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        workspaceId = WorkspaceId.of("WS-E2E-ORCHESTRATION");
        concurGuardEngine = new DefaultConcurGuardEngine();

        // 8 specialized agents
        allSpecializedAgents = List.of(
                new CoordinatorAgent(),
                new RiskIntelligenceAgent(),
                new InventoryAgent(),
                new ProcurementAgent(),
                new LogisticsAgent(),
                new ProductionAgent(),
                new QualityAgent(),
                new RecoveryPlanningAgent()
        );

        // Standard materials
        Material lithium = new Material(MaterialId.of("MAT-LITHIUM-HYDROXIDE"), "LH-01", "Lithium Hydroxide", MaterialType.RAW_MATERIAL, UnitOfMeasure.KILOGRAM);
        Material cathode = new Material(MaterialId.of("MAT-CATHODE-NMC"), "NMC-01", "Cathode NMC811", MaterialType.INTERMEDIATE, UnitOfMeasure.KILOGRAM);
        Material wafer = new Material(MaterialId.of("MAT-SILICON-WAFER"), "SW-300", "300mm Silicon Wafer", MaterialType.RAW_MATERIAL, UnitOfMeasure.PIECE);
        Material chip = new Material(MaterialId.of("MAT-MICROCONTROLLER"), "MCU-7NM", "Automotive Microcontroller", MaterialType.COMPONENT, UnitOfMeasure.PIECE);
        Material apiMat = new Material(MaterialId.of("MAT-API-BIOLOGIC"), "API-400", "Biologic Active Ingredient", MaterialType.RAW_MATERIAL, UnitOfMeasure.LITER);
        Material vaccine = new Material(MaterialId.of("MAT-MRNA-VACCINE"), "VAC-MRNA", "mRNA Vaccine Vial", MaterialType.FINISHED_PRODUCT, UnitOfMeasure.PIECE);
        Material steel = new Material(MaterialId.of("MAT-STAMPING-STEEL"), "STEEL-600", "Cold Rolled Steel Coil", MaterialType.RAW_MATERIAL, UnitOfMeasure.KILOGRAM);
        Material chassis = new Material(MaterialId.of("MAT-CHASSIS-SUBASSY"), "CHASSIS-01", "Chassis Sub-Assembly", MaterialType.COMPONENT, UnitOfMeasure.PIECE);

        BomEntry bomEv = BomEntry.of(cathode.getId(), lithium.getId(), Quantity.of(3, UnitOfMeasure.KILOGRAM), BigDecimal.valueOf(0.01), 1);
        BomEntry bomSemi = BomEntry.of(chip.getId(), wafer.getId(), Quantity.of(1, UnitOfMeasure.PIECE), BigDecimal.valueOf(0.05), 1);
        BomEntry bomPharma = BomEntry.of(vaccine.getId(), apiMat.getId(), Quantity.of(1, UnitOfMeasure.LITER), BigDecimal.valueOf(0.005), 1);
        BomEntry bomAuto = BomEntry.of(chassis.getId(), steel.getId(), Quantity.of(150, UnitOfMeasure.KILOGRAM), BigDecimal.valueOf(0.02), 1);

        BillOfMaterials bom = new BillOfMaterials(
                List.of(lithium, cathode, wafer, chip, apiMat, vaccine, steel, chassis),
                List.of(bomEv, bomSemi, bomPharma, bomAuto)
        );

        // Network nodes & lanes
        NodeId supplier1 = NodeId.of("NODE-SUP-PORT");
        NodeId plant1 = NodeId.of("NODE-MFG-PLANT");
        Supplier sup = new Supplier(supplier1, "Global Port Hub", "Rotterdam", true, 1, Set.of("ISO9001"), 0.1);
        ManufacturingSite mfg = new ManufacturingSite(plant1, "Central Assembly Plant", "Munich", true, Set.of("LINE-A"), "ISO-6", 250);
        TransportationLane lane = new TransportationLane(
                EdgeId.of("LANE-ROTTERDAM-MUNICH"), supplier1, plant1, TransportMode.ROAD, Duration.ofHours(18), BigDecimal.valueOf(1.95), true
        );
        SupplyChainGraph graph = new SupplyChainGraph(List.of(sup, mfg), List.of(lane));

        // Inventories
        InventoryAccountId accLithium = InventoryAccountId.of(plant1, lithium.getId(), BalanceCategory.ON_HAND_AVAILABLE);
        InventoryAccountId accWafer = InventoryAccountId.of(plant1, wafer.getId(), BalanceCategory.ON_HAND_AVAILABLE);
        InventoryAccountId accApi = InventoryAccountId.of(plant1, apiMat.getId(), BalanceCategory.ON_HAND_AVAILABLE);
        InventoryAccountId accSteel = InventoryAccountId.of(plant1, steel.getId(), BalanceCategory.ON_HAND_AVAILABLE);

        Map<InventoryAccountId, Quantity> balances = Map.of(
                accLithium, Quantity.of(15000, UnitOfMeasure.KILOGRAM),
                accWafer, Quantity.of(5000, UnitOfMeasure.PIECE),
                accApi, Quantity.of(800, UnitOfMeasure.LITER),
                accSteel, Quantity.of(80000, UnitOfMeasure.KILOGRAM)
        );

        snapshot = new DigitalTwinSnapshot(workspaceId, StateVersion.of(1), Instant.now(), graph, bom, balances);

        // Capacities
        ResourceId resLine = ResourceId.of("LINE-NODE-MFG-PLANT");
        CapacityWindow horizon = CapacityWindow.of(Instant.EPOCH, Instant.EPOCH.plus(Duration.ofDays(365)));
        ResourceCapacity capacity = ResourceCapacity.of(Quantity.of(150, UnitOfMeasure.PIECE), OperatingSchedule.continuous(horizon));
        Map<ResourceId, CapacityCalendar> calendars = Map.of(resLine, new CapacityCalendar(resLine, capacity));

        registeredTools = List.of(
                new QueryInventoryTool(() -> snapshot),
                new QueryBomTool(() -> snapshot),
                new QueryLaneStatusTool(() -> snapshot),
                new QueryCapacityTool(() -> calendars)
        );

        orchestrator = new AgenticRecoveryOrchestrator(concurGuardEngine, allSpecializedAgents, registeredTools);
    }

    @Test
    @DisplayName("Scenario 1: EV Battery Manufacturing - Port delay and cathode cell line contention")
    void testEvBatteryDisruptionRecovery() {
        DisruptionIncident incident = new DisruptionIncident(
                "INC-EV-BATTERY-001",
                workspaceId,
                "Rotterdam Port Congestion - Lithium Hydroxide Shipment Delayed",
                "Severe customs backlog at Rotterdam delays critical battery precursor vessel",
                "EV_BATTERY",
                Set.of("NODE-SUP-PORT", "NODE-MFG-PLANT"),
                Set.of("MAT-LITHIUM-HYDROXIDE"),
                168L,
                "HIGH",
                Instant.now()
        );

        RecoveryProposal proposal = orchestrator.coordinateRecovery(incident, snapshot);

        assertThat(proposal).isNotNull();
        assertThat(proposal.incidentId()).isEqualTo("INC-EV-BATTERY-001");
        assertThat(proposal.approvedIntents()).isNotEmpty();
        assertThat(proposal.totalEstimatedCost()).isPositive();
        assertThat(proposal.recoveryTimeObjectiveHours()).isGreaterThanOrEqualTo(168L);
        assertThat(proposal.auditTrail()).isNotEmpty();
        assertThat(proposal.riskAssessmentSummary()).contains("Disruption severity [HIGH]");

        // Zero-mutation invariant: DigitalTwin snapshot remains strictly at v1
        assertThat(snapshot.version().value()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Scenario 2: Pharmaceutical Cold Chain - Temperature excursion and inspection hold")
    void testPharmaColdChainDisruptionRecovery() {
        DisruptionIncident incident = new DisruptionIncident(
                "INC-PHARMA-COLD-002",
                workspaceId,
                "Biologic API Cold Chain Excursion Alert",
                "Transit refrigeration sensor recorded 8°C excursion during cross-dock transfer",
                "PHARMA",
                Set.of("NODE-SUP-PORT", "NODE-MFG-PLANT"),
                Set.of("MAT-API-BIOLOGIC"),
                72L,
                "CRITICAL",
                Instant.now()
        );

        RecoveryProposal proposal = orchestrator.coordinateRecovery(incident, snapshot);

        assertThat(proposal).isNotNull();
        assertThat(proposal.incidentId()).isEqualTo("INC-PHARMA-COLD-002");
        assertThat(proposal.approvedIntents()).isNotEmpty();
        assertThat(proposal.overallConfidence()).isGreaterThan(0.60);
        assertThat(proposal.auditTrail()).anyMatch(a -> a.toolName().equals("query_inventory"));
    }

    @Test
    @DisplayName("Scenario 3: Semiconductor Manufacturing - Fab wafer shortage and cleanroom line allocation")
    void testSemiconductorDisruptionRecovery() {
        DisruptionIncident incident = new DisruptionIncident(
                "INC-SEMI-FAB-003",
                workspaceId,
                "Photolithography Chemical Purity Alert",
                "Batch contamination in photoresist requires immediate wafer inspection hold",
                "SEMICONDUCTOR",
                Set.of("NODE-MFG-PLANT"),
                Set.of("MAT-SILICON-WAFER"),
                96L,
                "CRITICAL",
                Instant.now()
        );

        RecoveryProposal proposal = orchestrator.coordinateRecovery(incident, snapshot);

        assertThat(proposal).isNotNull();
        assertThat(proposal.incidentId()).isEqualTo("INC-SEMI-FAB-003");
        assertThat(proposal.approvedIntents()).isNotEmpty();
        assertThat(proposal.totalEstimatedCost()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Scenario 4: Automotive Component - Tier-1 stamping die failure and expedited multimodal dispatch")
    void testAutomotiveStampingDisruptionRecovery() {
        DisruptionIncident incident = new DisruptionIncident(
                "INC-AUTO-STAMP-004",
                workspaceId,
                "Tier-1 Press Line Die Fracture",
                "Major tooling die fracture on 2000T stamping press halts chassis sub-assembly",
                "AUTOMOTIVE",
                Set.of("NODE-SUP-PORT", "NODE-MFG-PLANT"),
                Set.of("MAT-STAMPING-STEEL"),
                120L,
                "HIGH",
                Instant.now()
        );

        RecoveryProposal proposal = orchestrator.coordinateRecovery(incident, snapshot);

        assertThat(proposal).isNotNull();
        assertThat(proposal.incidentId()).isEqualTo("INC-AUTO-STAMP-004");
        assertThat(proposal.approvedIntents()).isNotEmpty();
        assertThat(proposal.arbitratedTradeOffs()).isNotNull();
    }
}
