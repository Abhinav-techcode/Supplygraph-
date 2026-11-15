package com.supplygraph.concurguard.conflict;

import com.supplygraph.concurguard.api.ConflictType;
import com.supplygraph.concurguard.model.*;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.BillOfMaterials;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.capacity.CapacityReservationId;
import com.supplygraph.domain.model.capacity.CapacityWindow;
import com.supplygraph.domain.model.capacity.ResourceId;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.graph.SupplyChainGraph;
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

class AllSevenConflictClassesTest {

    private ResourceAliasRegistry aliasRegistry;
    private ConcurGuardConflictDetector detector;
    private DigitalTwinSnapshot snapshot;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.parse("2026-11-16T12:00:00Z");
        aliasRegistry = new ResourceAliasRegistry();
        detector = new ConcurGuardConflictDetector(aliasRegistry);

        InventoryAccountId lithiumStock = InventoryAccountId.of(
                NodeId.of("WH-01"), MaterialId.of("LITHIUM-RAW"), BalanceCategory.ON_HAND_AVAILABLE);

        snapshot = new DigitalTwinSnapshot(
                WorkspaceId.of("WS-TEST"),
                StateVersion.of(5L),
                now,
                SupplyChainGraph.empty(),
                BillOfMaterials.empty(),
                Map.of(lithiumStock, Quantity.of(100, UnitOfMeasure.KILOGRAM))
        );
    }

    @Test
    @DisplayName("Class 1: Quantity Conflict detected when aggregate claims exceed available inventory")
    void shouldDetectQuantityConflict() {
        ResourceIdentity res = ResourceIdentity.of("MATERIAL", "LITHIUM-RAW", "WH-01");
        CapacityWindow win = CapacityWindow.of(now.plusSeconds(3600), Duration.ofHours(2));

        ActionIntent intent1 = ActionIntent.builder()
                .id("I-QTY-1")
                .agentId(AgentId.INVENTORY)
                .actionType(ActionType.REALLOCATE_INVENTORY)
                .targetEntityId("LITHIUM-RAW")
                .addResourceClaim(ResourceClaim.consumptive(res, Quantity.of(70, UnitOfMeasure.KILOGRAM), win))
                .build();

        ActionIntent intent2 = ActionIntent.builder()
                .id("I-QTY-2")
                .agentId(AgentId.PRODUCTION)
                .actionType(ActionType.REALLOCATE_INVENTORY)
                .targetEntityId("LITHIUM-RAW")
                .addResourceClaim(ResourceClaim.consumptive(res, Quantity.of(60, UnitOfMeasure.KILOGRAM), win))
                .build();

        // Total claimed = 130 kg > 100 kg available
        List<Conflict> conflicts = detector.detectAllConflicts(List.of(intent1, intent2), snapshot);

        assertThat(conflicts).anyMatch(c -> c.type() == ConflictType.QUANTITY_CONFLICT);
    }

    @Test
    @DisplayName("Class 2: Capacity Conflict detected on overlapping resource reservations")
    void shouldDetectCapacityConflict() {
        ResourceId machine = ResourceId.of("LINE-COATER-01");
        CapacityWindow window = CapacityWindow.of(now.plusSeconds(3600), Duration.ofHours(4));

        ActionIntent a = ActionIntent.builder()
                .id("I-CAP-1")
                .agentId(AgentId.PRODUCTION)
                .actionType(ActionType.RESERVE_CAPACITY)
                .targetEntityId("LINE-COATER-01")
                .capacityClaim(CapacityClaim.of(machine, Quantity.of(100, UnitOfMeasure.PIECE), window))
                .build();

        ActionIntent b = ActionIntent.builder()
                .id("I-CAP-2")
                .agentId(AgentId.RECOVERY_PLANNING)
                .actionType(ActionType.RESERVE_CAPACITY)
                .targetEntityId("LINE-COATER-01")
                .capacityClaim(CapacityClaim.of(machine, Quantity.of(50, UnitOfMeasure.PIECE), window))
                .build();

        List<Conflict> conflicts = detector.detectAllConflicts(List.of(a, b), snapshot);

        assertThat(conflicts).anyMatch(c -> c.type() == ConflictType.CAPACITY_CONFLICT);
    }

    @Test
    @DisplayName("Class 3: Temporal Overlap detected when prerequisite finishes after dependent starts")
    void shouldDetectTemporalConflict() {
        CapacityWindow prereqWindow = CapacityWindow.of(now.plusSeconds(7200), Duration.ofHours(3)); // [14:00, 17:00)
        CapacityWindow dependentWindow = CapacityWindow.of(now.plusSeconds(3600), Duration.ofHours(2)); // [13:00, 15:00)

        ActionIntent prereq = ActionIntent.builder()
                .id("I-PREREQ")
                .agentId(AgentId.PROCUREMENT)
                .actionType(ActionType.EXPEDITE_PURCHASE_ORDER)
                .targetEntityId("PO-100")
                .timeWindow(prereqWindow)
                .build();

        ActionIntent dependent = ActionIntent.builder()
                .id("I-DEP")
                .agentId(AgentId.PRODUCTION)
                .actionType(ActionType.SPLIT_PRODUCTION_BATCH)
                .targetEntityId("BATCH-200")
                .timeWindow(dependentWindow)
                .addDependency(IntentId.of("I-PREREQ"))
                .build();

        List<Conflict> conflicts = detector.detectAllConflicts(List.of(prereq, dependent), snapshot);

        assertThat(conflicts).anyMatch(c -> c.type() == ConflictType.TEMPORAL_OVERLAP &&
                c.explanation().contains("Temporal precedence inversion"));
    }

    @Test
    @DisplayName("Class 4: Semantic Alias Conflict detected when different names resolve to same asset")
    void shouldDetectSemanticAliasConflict() {
        ResourceIdentity canonicalLine = ResourceIdentity.of("LINE", "CATHODE-LINE-1", "MFG-GIGA");
        aliasRegistry.registerAlias("Cathode Line 1", canonicalLine);
        aliasRegistry.registerAlias("Coating Bay Alpha", canonicalLine);

        CapacityWindow window = CapacityWindow.of(now.plusSeconds(3600), Duration.ofHours(2));

        ActionIntent intentA = ActionIntent.builder()
                .id("I-ALIAS-A")
                .agentId(AgentId.PRODUCTION)
                .actionType(ActionType.RESERVE_CAPACITY)
                .targetEntityId("Cathode Line 1")
                .location("MFG-GIGA")
                .timeWindow(window)
                .build();

        ActionIntent intentB = ActionIntent.builder()
                .id("I-ALIAS-B")
                .agentId(AgentId.RECOVERY_PLANNING)
                .actionType(ActionType.RESERVE_CAPACITY)
                .targetEntityId("Coating Bay Alpha")
                .location("MFG-GIGA")
                .timeWindow(window)
                .build();

        List<Conflict> conflicts = detector.detectAllConflicts(List.of(intentA, intentB), snapshot);

        assertThat(conflicts).anyMatch(c -> c.type() == ConflictType.SEMANTIC_ALIAS);
    }

    @Test
    @DisplayName("Class 5: Dependency Conflict detected on circular deadlock")
    void shouldDetectDependencyConflict() {
        ActionIntent a = ActionIntent.builder()
                .id("I-CYCLE-A")
                .agentId(AgentId.LOGISTICS)
                .actionType(ActionType.REROUTE_SHIPMENT)
                .targetEntityId("TRUCK-1")
                .addDependency(IntentId.of("I-CYCLE-B"))
                .build();

        ActionIntent b = ActionIntent.builder()
                .id("I-CYCLE-B")
                .agentId(AgentId.PRODUCTION)
                .actionType(ActionType.SPLIT_PRODUCTION_BATCH)
                .targetEntityId("BATCH-1")
                .addDependency(IntentId.of("I-CYCLE-A"))
                .build();

        List<Conflict> conflicts = detector.detectAllConflicts(List.of(a, b), snapshot);

        assertThat(conflicts).anyMatch(c -> c.type() == ConflictType.DEPENDENCY_CONFLICT &&
                c.explanation().contains("circular dependency"));
    }

    @Test
    @DisplayName("Class 6: Stale State Conflict detected when intent targets obsolete version")
    void shouldDetectStaleStateConflict() {
        // Snapshot is at version v5. Intent formulated against obsolete v2.
        ActionIntent staleIntent = ActionIntent.builder()
                .id("I-STALE")
                .agentId(AgentId.INVENTORY)
                .actionType(ActionType.REALLOCATE_INVENTORY)
                .targetEntityId("LITHIUM-RAW")
                .expectedStateVersion(StateVersion.of(2L))
                .build();

        List<Conflict> conflicts = detector.detectAllConflicts(List.of(staleIntent), snapshot);

        assertThat(conflicts).anyMatch(c -> c.type() == ConflictType.STALE_STATE_CONFLICT);
    }

    @Test
    @DisplayName("Class 7: Industry Policy Violation detected on unapproved high-risk action")
    void shouldDetectIndustryPolicyConflict() {
        ActionIntent highRiskAction = ActionIntent.builder()
                .id("I-HIGH-RISK")
                .agentId(AgentId.QUALITY)
                .actionType(ActionType.SUBSTITUTE_MATERIAL)
                .targetEntityId("ACTIVE-PHARMA-ING")
                .riskScore(0.85) // High-risk requires explicit QA approval
                .requiresHumanApproval(true)
                .build();

        List<Conflict> conflicts = detector.detectAllConflicts(List.of(highRiskAction), snapshot);

        assertThat(conflicts).anyMatch(c -> c.type() == ConflictType.INDUSTRY_POLICY_VIOLATION);
    }
}
