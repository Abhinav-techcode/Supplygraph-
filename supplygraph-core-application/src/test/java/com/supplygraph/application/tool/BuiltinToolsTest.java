package com.supplygraph.application.tool;

import com.supplygraph.application.agent.AgentIdentity;
import com.supplygraph.application.agent.AgentRole;
import com.supplygraph.application.tool.builtin.QueryBomTool;
import com.supplygraph.application.tool.builtin.QueryCapacityTool;
import com.supplygraph.application.tool.builtin.QueryInventoryTool;
import com.supplygraph.application.tool.builtin.QueryLaneStatusTool;
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

class BuiltinToolsTest {

    private WorkspaceId workspaceId;
    private DigitalTwinSnapshot snapshot;
    private Map<ResourceId, CapacityCalendar> calendars;
    private AgentIdentity adminIdentity;

    @BeforeEach
    void setUp() {
        workspaceId = WorkspaceId.of("WS-TOOL-TEST");
        adminIdentity = AgentIdentity.of(AgentRole.COORDINATOR,
                Set.of("query_inventory", "query_bom", "query_lane_status", "query_capacity"));

        // Setup BOM
        Material parent = new Material(MaterialId.of("BATTERY-PACK"), "BP-001", "Battery Pack", MaterialType.FINISHED_PRODUCT, UnitOfMeasure.PIECE);
        Material child = new Material(MaterialId.of("LITHIUM-CELL"), "LC-001", "Lithium Cell", MaterialType.RAW_MATERIAL, UnitOfMeasure.PIECE);
        BomEntry entry = BomEntry.of(parent.getId(), child.getId(), Quantity.of(96, UnitOfMeasure.PIECE), BigDecimal.valueOf(0.02), 1);
        BillOfMaterials bom = new BillOfMaterials(List.of(parent, child), List.of(entry));

        // Setup Graph
        NodeId nodeA = NodeId.of("SUPPLIER-1");
        NodeId nodeB = NodeId.of("PLANT-1");
        Supplier sNode = new Supplier(nodeA, "Supplier North", "Berlin", true, 1, Set.of("ISO9001"), 0.1);
        ManufacturingSite mNode = new ManufacturingSite(nodeB, "Gigafactory 1", "Leipzig", true, Set.of("LINE-1"), "ISO-7", 200);
        TransportationLane lane = new TransportationLane(
                EdgeId.of("LANE-A-B"), nodeA, nodeB, TransportMode.ROAD, Duration.ofHours(12), BigDecimal.valueOf(2.50), true
        );
        SupplyChainGraph graph = new SupplyChainGraph(List.of(sNode, mNode), List.of(lane));

        // Setup Inventory
        InventoryAccountId account = InventoryAccountId.of(nodeB, child.getId(), BalanceCategory.ON_HAND_AVAILABLE);
        Map<InventoryAccountId, Quantity> balances = Map.of(account, Quantity.of(5000, UnitOfMeasure.PIECE));

        snapshot = new DigitalTwinSnapshot(workspaceId, StateVersion.of(1), Instant.now(), graph, bom, balances);

        // Setup Capacity Calendar
        ResourceId resId = ResourceId.of("LINE-PACK-ASSY");
        CapacityWindow horizon = CapacityWindow.of(Instant.EPOCH, Instant.EPOCH.plus(Duration.ofDays(365)));
        ResourceCapacity capacity = ResourceCapacity.of(Quantity.of(50, UnitOfMeasure.PIECE), OperatingSchedule.continuous(horizon));
        CapacityCalendar calendar = new CapacityCalendar(resId, capacity);
        calendars = Map.of(resId, calendar);
    }

    @Test
    @DisplayName("QueryInventoryTool: correctly inspects snapshot inventory balances")
    void testQueryInventoryTool() {
        QueryInventoryTool tool = new QueryInventoryTool(() -> snapshot);
        ToolExecutionRequest req = new ToolExecutionRequest(
                "query_inventory", adminIdentity, workspaceId, Map.of("materialId", "LITHIUM-CELL")
        );

        ToolExecutionResult result = tool.execute(req);

        assertThat(result.success()).isTrue();
        assertThat(result.data()).containsEntry("materialId", "LITHIUM-CELL");
        assertThat(result.data().get("availableQuantity").toString()).contains("5000");
    }

    @Test
    @DisplayName("QueryBomTool: correctly returns components and explosion metadata")
    void testQueryBomTool() {
        QueryBomTool tool = new QueryBomTool(() -> snapshot);
        ToolExecutionRequest req = new ToolExecutionRequest(
                "query_bom", adminIdentity, workspaceId, Map.of("materialId", "BATTERY-PACK")
        );

        ToolExecutionResult result = tool.execute(req);

        assertThat(result.success()).isTrue();
        assertThat(result.data()).containsEntry("materialId", "BATTERY-PACK");
        List<?> comps = (List<?>) result.data().get("components");
        assertThat(comps).hasSize(1);
    }

    @Test
    @DisplayName("QueryLaneStatusTool: correctly returns lane operational status and lead times")
    void testQueryLaneStatusTool() {
        QueryLaneStatusTool tool = new QueryLaneStatusTool(() -> snapshot);
        ToolExecutionRequest req = new ToolExecutionRequest(
                "query_lane_status", adminIdentity, workspaceId, Map.of("originNodeId", "SUPPLIER-1")
        );

        ToolExecutionResult result = tool.execute(req);

        assertThat(result.success()).isTrue();
        assertThat(result.data()).containsEntry("matchingLanesCount", 1);
    }

    @Test
    @DisplayName("QueryCapacityTool: returns theoretical rate and available capacity")
    void testQueryCapacityTool() {
        QueryCapacityTool tool = new QueryCapacityTool(() -> calendars);
        ToolExecutionRequest req = new ToolExecutionRequest(
                "query_capacity", adminIdentity, workspaceId, Map.of("resourceId", "LINE-PACK-ASSY")
        );

        ToolExecutionResult result = tool.execute(req);

        assertThat(result.success()).isTrue();
        assertThat(result.data()).containsEntry("resourceId", "LINE-PACK-ASSY");
        assertThat(result.data()).containsEntry("ratePerHour", "50.0000");
    }
}
