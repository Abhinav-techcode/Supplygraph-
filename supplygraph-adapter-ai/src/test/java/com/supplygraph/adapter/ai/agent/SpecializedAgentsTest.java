package com.supplygraph.adapter.ai.agent;

import com.supplygraph.application.agent.*;
import com.supplygraph.application.tool.AgentTool;
import com.supplygraph.application.tool.ToolExecutionBoundary;
import com.supplygraph.application.tool.builtin.QueryBomTool;
import com.supplygraph.application.tool.builtin.QueryCapacityTool;
import com.supplygraph.application.tool.builtin.QueryInventoryTool;
import com.supplygraph.application.tool.builtin.QueryLaneStatusTool;
import com.supplygraph.concurguard.model.ActionIntent;
import com.supplygraph.concurguard.model.IntentId;
import com.supplygraph.concurguard.replanning.ReplanningScope;
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

@DisplayName("Specialized Bounded Agents Contract Tests")
class SpecializedAgentsTest {

    private WorkspaceId workspaceId;
    private DigitalTwinSnapshot snapshot;
    private DisruptionIncident incident;
    private AgentInvestigationContext context;
    private ToolExecutionBoundary toolBoundary;

    @BeforeEach
    void setUp() {
        workspaceId = WorkspaceId.of("WS-AGENT-TEST");

        // Setup BOM
        Material raw = new Material(MaterialId.of("RAW-LITHIUM"), "RM-01", "Raw Lithium", MaterialType.RAW_MATERIAL, UnitOfMeasure.KILOGRAM);
        Material cathode = new Material(MaterialId.of("CATHODE-PRECURSOR"), "CP-01", "Cathode Precursor", MaterialType.INTERMEDIATE, UnitOfMeasure.KILOGRAM);
        BomEntry entry = BomEntry.of(cathode.getId(), raw.getId(), Quantity.of(2, UnitOfMeasure.KILOGRAM), BigDecimal.valueOf(0.01), 1);
        BillOfMaterials bom = new BillOfMaterials(List.of(raw, cathode), List.of(entry));

        // Setup Graph
        NodeId supNode = NodeId.of("SUP-CHILE");
        NodeId mfgNode = NodeId.of("MFG-LEIPZIG");
        Supplier supplier = new Supplier(supNode, "Chile Lithium Mining", "Santiago", true, 1, Set.of("ISO14001"), 0.15);
        ManufacturingSite mfg = new ManufacturingSite(mfgNode, "Gigafactory Leipzig", "Leipzig", true, Set.of("LINE-1"), "ISO-8", 100);
        TransportationLane lane = new TransportationLane(
                EdgeId.of("LANE-CHILE-LEIPZIG"), supNode, mfgNode, TransportMode.OCEAN, Duration.ofDays(21), BigDecimal.valueOf(1.80), true
        );
        SupplyChainGraph graph = new SupplyChainGraph(List.of(supplier, mfg), List.of(lane));

        // Setup Inventory
        InventoryAccountId acc = InventoryAccountId.of(mfgNode, raw.getId(), BalanceCategory.ON_HAND_AVAILABLE);
        Map<InventoryAccountId, Quantity> balances = Map.of(acc, Quantity.of(10000, UnitOfMeasure.KILOGRAM));

        snapshot = new DigitalTwinSnapshot(workspaceId, StateVersion.of(1), Instant.now(), graph, bom, balances);

        // Setup Capacity
        ResourceId resId = ResourceId.of("LINE-MFG-LEIPZIG");
        CapacityWindow horizon = CapacityWindow.of(Instant.EPOCH, Instant.EPOCH.plus(Duration.ofDays(365)));
        ResourceCapacity capacity = ResourceCapacity.of(Quantity.of(100, UnitOfMeasure.KILOGRAM), OperatingSchedule.continuous(horizon));
        Map<ResourceId, CapacityCalendar> calendars = Map.of(resId, new CapacityCalendar(resId, capacity));

        incident = new DisruptionIncident(
                "INC-EV-01",
                workspaceId,
                "Lithium Raw Material Shipment Delayed",
                "Port congestion at Antofagasta delays raw lithium vessel by 14 days",
                "EV_BATTERY",
                Set.of("SUP-CHILE", "MFG-LEIPZIG"),
                Set.of("RAW-LITHIUM"),
                336L,
                "HIGH",
                Instant.now()
        );

        context = new AgentInvestigationContext(incident, snapshot, Map.of());

        List<AgentTool> tools = List.of(
                new QueryInventoryTool(() -> snapshot),
                new QueryBomTool(() -> snapshot),
                new QueryLaneStatusTool(() -> snapshot),
                new QueryCapacityTool(() -> calendars)
        );

        toolBoundary = new ToolExecutionBoundary(workspaceId, tools);
    }

    @Test
    @DisplayName("CoordinatorAgent: investigates incident and supports replanning")
    void testCoordinatorAgent() {
        CoordinatorAgent agent = new CoordinatorAgent();
        assertThat(agent.role()).isEqualTo(AgentRole.COORDINATOR);

        AgentProposal proposal = agent.investigate(context, toolBoundary);
        assertThat(proposal.agentRole()).isEqualTo(AgentRole.COORDINATOR);
        assertThat(proposal.intents()).hasSize(1);
        assertThat(proposal.evidence()).isNotEmpty();
        assertThat(proposal.confidence()).isGreaterThan(0.8);

        ReplanningScope scope = new ReplanningScope(Set.of(), Set.of(proposal.intents().get(0).id()), List.of(), 1);
        AgentProposal replanned = agent.replan(context, scope, toolBoundary);
        assertThat(replanned.intents()).hasSize(1);
        assertThat(replanned.intents().get(0).id().value()).contains("REV-1");
    }

    @Test
    @DisplayName("RiskIntelligenceAgent: evaluates external disruption and reroutes freight")
    void testRiskIntelligenceAgent() {
        RiskIntelligenceAgent agent = new RiskIntelligenceAgent();
        assertThat(agent.role()).isEqualTo(AgentRole.RISK_INTELLIGENCE);

        AgentProposal proposal = agent.investigate(context, toolBoundary);
        assertThat(proposal.intents()).hasSize(1);
        assertThat(proposal.evidence()).anyMatch(e -> e.contains("Severe weather or disruption"));

        ReplanningScope scope = new ReplanningScope(Set.of(), Set.of(proposal.intents().get(0).id()), List.of(), 1);
        AgentProposal replanned = agent.replan(context, scope, toolBoundary);
        assertThat(replanned.intents().get(0).targetEntityId()).contains("LANE-SEC-AIR");
    }

    @Test
    @DisplayName("InventoryAgent: asserts quantity allocation claims under safe buffer limits")
    void testInventoryAgent() {
        InventoryAgent agent = new InventoryAgent();
        assertThat(agent.role()).isEqualTo(AgentRole.INVENTORY);

        AgentProposal proposal = agent.investigate(context, toolBoundary);
        assertThat(proposal.intents()).hasSize(1);
        ActionIntent intent = proposal.intents().get(0);
        assertThat(intent.resourceClaims()).isNotEmpty();

        ReplanningScope scope = new ReplanningScope(Set.of(), Set.of(intent.id()), List.of(), 1);
        AgentProposal replanned = agent.replan(context, scope, toolBoundary);
        assertThat(replanned.intents().get(0).resourceClaims().get(0).claimedQuantity().amount()).isEqualTo(BigDecimal.valueOf(250).setScale(4));
    }

    @Test
    @DisplayName("ProcurementAgent: secures vendor quotas and expedites purchase orders")
    void testProcurementAgent() {
        ProcurementAgent agent = new ProcurementAgent();
        assertThat(agent.role()).isEqualTo(AgentRole.PROCUREMENT);

        AgentProposal proposal = agent.investigate(context, toolBoundary);
        assertThat(proposal.intents()).hasSize(1);

        ReplanningScope scope = new ReplanningScope(Set.of(), Set.of(proposal.intents().get(0).id()), List.of(), 1);
        AgentProposal replanned = agent.replan(context, scope, toolBoundary);
        assertThat(replanned.intents().get(0).targetEntityId()).contains("SUPPLIER-ALT");
    }

    @Test
    @DisplayName("LogisticsAgent: analyzes freight corridors and dispatches multimodal transport")
    void testLogisticsAgent() {
        LogisticsAgent agent = new LogisticsAgent();
        assertThat(agent.role()).isEqualTo(AgentRole.LOGISTICS);

        AgentProposal proposal = agent.investigate(context, toolBoundary);
        assertThat(proposal.intents()).hasSize(1);

        ReplanningScope scope = new ReplanningScope(Set.of(), Set.of(proposal.intents().get(0).id()), List.of(), 1);
        AgentProposal replanned = agent.replan(context, scope, toolBoundary);
        assertThat(replanned.intents().get(0).targetEntityId()).contains("EXP");
    }

    @Test
    @DisplayName("ProductionAgent: asserts manufacturing line capacity claims")
    void testProductionAgent() {
        ProductionAgent agent = new ProductionAgent();
        assertThat(agent.role()).isEqualTo(AgentRole.PRODUCTION);

        AgentProposal proposal = agent.investigate(context, toolBoundary);
        assertThat(proposal.intents()).hasSize(1);
        assertThat(proposal.intents().get(0).capacityClaim()).isNotNull();

        ReplanningScope scope = new ReplanningScope(Set.of(), Set.of(proposal.intents().get(0).id()), List.of(), 1);
        AgentProposal replanned = agent.replan(context, scope, toolBoundary);
        assertThat(replanned.intents().get(0).capacityClaim().resourceId().value()).contains("BACKUP");
    }

    @Test
    @DisplayName("QualityAgent: enforces regulatory compliance and inspection holds")
    void testQualityAgent() {
        QualityAgent agent = new QualityAgent();
        assertThat(agent.role()).isEqualTo(AgentRole.QUALITY);

        AgentProposal proposal = agent.investigate(context, toolBoundary);
        assertThat(proposal.intents()).hasSize(1);
        assertThat(proposal.confidence()).isGreaterThan(0.95);

        ReplanningScope scope = new ReplanningScope(Set.of(), Set.of(proposal.intents().get(0).id()), List.of(), 1);
        AgentProposal replanned = agent.replan(context, scope, toolBoundary);
        assertThat(replanned.intents().get(0).targetEntityId()).contains("EXP");
    }

    @Test
    @DisplayName("RecoveryPlanningAgent: calculates pareto trade-offs across objectives")
    void testRecoveryPlanningAgent() {
        RecoveryPlanningAgent agent = new RecoveryPlanningAgent();
        assertThat(agent.role()).isEqualTo(AgentRole.RECOVERY_PLANNING);

        AgentProposal proposal = agent.investigate(context, toolBoundary);
        assertThat(proposal.intents()).hasSize(1);

        ReplanningScope scope = new ReplanningScope(Set.of(), Set.of(proposal.intents().get(0).id()), List.of(), 1);
        AgentProposal replanned = agent.replan(context, scope, toolBoundary);
        assertThat(replanned.confidence()).isGreaterThanOrEqualTo(0.95);
    }
}
