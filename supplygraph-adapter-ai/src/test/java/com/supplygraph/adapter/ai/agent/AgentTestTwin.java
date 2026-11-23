package com.supplygraph.adapter.ai.agent;

import com.supplygraph.application.agent.AgentInvestigationContext;
import com.supplygraph.application.agent.DisruptionIncident;
import com.supplygraph.application.tool.AgentTool;
import com.supplygraph.application.tool.ToolExecutionBoundary;
import com.supplygraph.application.tool.builtin.*;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.*;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.graph.*;
import com.supplygraph.domain.model.inventory.BalanceCategory;
import com.supplygraph.domain.model.inventory.InventoryAccountId;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import com.supplygraph.domain.model.twin.StateVersion;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * A small but complete network used to test that agent proposals depend on the twin:
 * SUP-A (disrupted) -> PLANT; SUP-B -> HUB -> PLANT. HUB and PLANT hold stock of MAT-X; PLANT makes 10 units/h.
 */
final class AgentTestTwin {
    static final WorkspaceId WS = WorkspaceId.of("WS-AGENT-TEST");

    static DigitalTwinSnapshot snapshot(double hubStock, double plantStock) {
        Material x = new Material(MaterialId.of("MAT-X"), "X-1", "Widget Resin", MaterialType.RAW_MATERIAL, UnitOfMeasure.PIECE);
        Material product = new Material(MaterialId.of("PROD-1"), "P-1", "Widget Assembly", MaterialType.FINISHED_PRODUCT, UnitOfMeasure.PIECE);
        BomEntry entry = BomEntry.of(product.getId(), x.getId(), Quantity.of(2, UnitOfMeasure.PIECE), BigDecimal.ZERO, 1);
        BillOfMaterials bom = new BillOfMaterials(List.of(x, product), List.of(entry));
        Supplier supA = new Supplier(NodeId.of("SUP-A"), "Supplier A", "Oslo", true, 1, Set.of(), 0.1);
        Supplier supB = new Supplier(NodeId.of("SUP-B"), "Supplier B", "Lyon", true, 1, Set.of(), 0.1);
        LogisticsHub hub = new LogisticsHub(NodeId.of("HUB"), "Central Hub", "Hamburg", true, true, 500, Set.of("ROAD"));
        ManufacturingSite plant = new ManufacturingSite(NodeId.of("PLANT"), "Main Plant", "Munich", true, Set.of("L1"), "ISO-7", 10);
        List<TransportationLane> lanes = List.of(
                new TransportationLane(EdgeId.of("SUP-A->PLANT"), NodeId.of("SUP-A"), NodeId.of("PLANT"), TransportMode.ROAD, Duration.ofHours(24), BigDecimal.valueOf(2), true),
                new TransportationLane(EdgeId.of("SUP-B->HUB"), NodeId.of("SUP-B"), NodeId.of("HUB"), TransportMode.RAIL, Duration.ofHours(48), BigDecimal.valueOf(1), true),
                new TransportationLane(EdgeId.of("HUB->PLANT"), NodeId.of("HUB"), NodeId.of("PLANT"), TransportMode.ROAD, Duration.ofHours(12), BigDecimal.valueOf(3), true));
        SupplyChainGraph graph = new SupplyChainGraph(List.of(supA, supB, hub, plant), lanes);
        Map<InventoryAccountId, Quantity> balances = new HashMap<>();
        if (hubStock > 0) balances.put(InventoryAccountId.of(NodeId.of("HUB"), x.getId(), BalanceCategory.ON_HAND_AVAILABLE), Quantity.of(BigDecimal.valueOf(hubStock), UnitOfMeasure.PIECE));
        if (plantStock > 0) balances.put(InventoryAccountId.of(NodeId.of("PLANT"), x.getId(), BalanceCategory.ON_HAND_AVAILABLE), Quantity.of(BigDecimal.valueOf(plantStock), UnitOfMeasure.PIECE));
        return new DigitalTwinSnapshot(WS, StateVersion.of(1), Instant.now(), graph, bom, balances);
    }

    static DisruptionIncident incident(Set<String> nodes, long hours) {
        return new DisruptionIncident("INC-TEST-0001", WS, "Supplier A outage", "Plant fire at supplier", "GENERIC", nodes, Set.of("MAT-X"), hours, "CRITICAL", Instant.now());
    }

    static AgentInvestigationContext context(DigitalTwinSnapshot s, DisruptionIncident i, Map<String, Object> params) {
        return new AgentInvestigationContext(i, s, params);
    }

    static ToolExecutionBoundary boundary(DigitalTwinSnapshot s) {
        List<AgentTool> tools = List.of(new QueryInventoryTool(() -> s), new QueryBomTool(() -> s), new QueryLaneStatusTool(() -> s), new QueryCapacityTool(Map::of));
        return new ToolExecutionBoundary(WS, tools);
    }

    private AgentTestTwin() {}
}
