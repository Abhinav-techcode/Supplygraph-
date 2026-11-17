package com.supplygraph.application.benchmark;

import com.supplygraph.concurguard.model.*;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.BillOfMaterials;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.capacity.CapacityWindow;
import com.supplygraph.domain.model.capacity.ResourceId;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.graph.NodeId;
import com.supplygraph.domain.model.graph.SupplyChainGraph;
import com.supplygraph.domain.model.inventory.BalanceCategory;
import com.supplygraph.domain.model.inventory.InventoryAccountId;
import com.supplygraph.domain.model.benchmark.BenchmarkScenarioType;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import com.supplygraph.domain.model.twin.StateVersion;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.BiFunction;

/**
 * Factory generating reproducible scenario fixtures and ground-truth baselines across 9 evaluation scenarios.
 */
public class BenchmarkScenarioFactory {

    public record ScenarioFixture(
            BenchmarkScenarioType scenarioType,
            List<ActionIntent> candidateIntents,
            DigitalTwinSnapshot snapshot,
            int groundTruthConflicts,
            int groundTruthValidPreserved,
            BiFunction<Set<IntentId>, Integer, List<ActionIntent>> selectiveReplanner,
            BiFunction<Collection<ActionIntent>, Integer, List<ActionIntent>> fullReplanner
    ) {}

    public static ScenarioFixture createFixture(BenchmarkScenarioType scenarioType, long seed) {
        Instant now = Instant.parse("2026-11-17T12:00:00Z");
        CapacityWindow win = CapacityWindow.of(now.plusSeconds(3600), Duration.ofHours(2));

        InventoryAccountId lithiumStock = InventoryAccountId.of(
                NodeId.of("WH-01"), MaterialId.of("INV-CATL-CELL"), BalanceCategory.ON_HAND_AVAILABLE);

        DigitalTwinSnapshot snapshot = new DigitalTwinSnapshot(
                WorkspaceId.of("WS-BENCHMARK"),
                StateVersion.of(1L),
                now,
                SupplyChainGraph.empty(),
                BillOfMaterials.empty(),
                Map.of(lithiumStock, Quantity.of(1000, UnitOfMeasure.PIECE))
        );

        ResourceIdentity sharedInv = ResourceIdentity.of("MATERIAL", "INV-CATL-CELL", "WH-01");
        ResourceId machine = ResourceId.of("LINE-FAB-01");

        return switch (scenarioType) {
            case SHARED_INVENTORY_CONTENTION -> {
                // Two agents compete for 800 pieces each from a 1000 piece stock
                ActionIntent intentA = ActionIntent.builder()
                        .id("INT-A1")
                        .agentId(AgentId.PRODUCTION)
                        .actionType(ActionType.REALLOCATE_INVENTORY)
                        .targetEntityId("INV-CATL-CELL")
                        .expectedStateVersion(snapshot.version())
                        .addResourceClaim(ResourceClaim.consumptive(sharedInv, Quantity.of(800, UnitOfMeasure.PIECE), win))
                        .estimatedCost(BigDecimal.valueOf(12000))
                        .riskScore(0.3)
                        .build();

                ActionIntent intentB = ActionIntent.builder()
                        .id("INT-B1")
                        .agentId(AgentId.INVENTORY)
                        .actionType(ActionType.REALLOCATE_INVENTORY)
                        .targetEntityId("INV-CATL-CELL")
                        .expectedStateVersion(snapshot.version())
                        .addResourceClaim(ResourceClaim.consumptive(sharedInv, Quantity.of(800, UnitOfMeasure.PIECE), win))
                        .estimatedCost(BigDecimal.valueOf(15000))
                        .riskScore(0.7) // Higher risk -> intentB arbitrated for replanning
                        .build();

                ActionIntent validIntent = ActionIntent.builder()
                        .id("INT-C1")
                        .agentId(AgentId.PROCUREMENT)
                        .actionType(ActionType.EXPEDITE_PURCHASE_ORDER)
                        .targetEntityId("PO-EXPEDITE-1")
                        .expectedStateVersion(snapshot.version())
                        .timeWindow(win)
                        .estimatedCost(BigDecimal.valueOf(2500))
                        .riskScore(0.1)
                        .build();

                List<ActionIntent> candidates = List.of(intentA, intentB, validIntent);

                BiFunction<Set<IntentId>, Integer, List<ActionIntent>> selectiveReplanner = (invalidIds, iter) -> {
                    List<ActionIntent> resolved = new ArrayList<>();
                    if (invalidIds.contains(IntentId.of("INT-B1"))) {
                        // Replanned intent B claims remaining 200 pieces
                        resolved.add(ActionIntent.builder()
                                .id("INT-B1-REPLANNED")
                                .agentId(AgentId.INVENTORY)
                                .actionType(ActionType.REALLOCATE_INVENTORY)
                                .targetEntityId("INV-CATL-CELL")
                                .expectedStateVersion(snapshot.version())
                                .addResourceClaim(ResourceClaim.consumptive(sharedInv, Quantity.of(200, UnitOfMeasure.PIECE), win))
                                .estimatedCost(BigDecimal.valueOf(8000))
                                .riskScore(0.2)
                                .build());
                    }
                    return resolved;
                };

                BiFunction<Collection<ActionIntent>, Integer, List<ActionIntent>> fullReplanner = (all, iter) -> List.of(
                        intentA,
                        ActionIntent.builder()
                                .id("INT-B1-FULL")
                                .agentId(AgentId.INVENTORY)
                                .actionType(ActionType.REALLOCATE_INVENTORY)
                                .targetEntityId("INV-CATL-CELL")
                                .expectedStateVersion(snapshot.version())
                                .addResourceClaim(ResourceClaim.consumptive(sharedInv, Quantity.of(200, UnitOfMeasure.PIECE), win))
                                .estimatedCost(BigDecimal.valueOf(8000))
                                .riskScore(0.2)
                                .build(),
                        validIntent
                );

                yield new ScenarioFixture(scenarioType, candidates, snapshot, 1, 2, selectiveReplanner, fullReplanner);
            }

            case SUPPLIER_CAPACITY_CONTENTION -> {
                ActionIntent intent1 = ActionIntent.builder()
                        .id("INT-SUP-1")
                        .agentId(AgentId.PRODUCTION)
                        .actionType(ActionType.RESERVE_CAPACITY)
                        .targetEntityId("LINE-FAB-01")
                        .expectedStateVersion(snapshot.version())
                        .capacityClaim(CapacityClaim.of(machine, Quantity.of(100, UnitOfMeasure.PIECE), win))
                        .riskScore(0.3)
                        .build();

                ActionIntent intent2 = ActionIntent.builder()
                        .id("INT-SUP-2")
                        .agentId(AgentId.RECOVERY_PLANNING)
                        .actionType(ActionType.RESERVE_CAPACITY)
                        .targetEntityId("LINE-FAB-01")
                        .expectedStateVersion(snapshot.version())
                        .capacityClaim(CapacityClaim.of(machine, Quantity.of(100, UnitOfMeasure.PIECE), win))
                        .riskScore(0.6)
                        .build();

                ActionIntent validIntent = ActionIntent.builder()
                        .id("INT-SUP-VALID")
                        .agentId(AgentId.PROCUREMENT)
                        .actionType(ActionType.EXPEDITE_PURCHASE_ORDER)
                        .targetEntityId("PO-SUP-01")
                        .expectedStateVersion(snapshot.version())
                        .timeWindow(win)
                        .riskScore(0.1)
                        .build();

                List<ActionIntent> candidates = List.of(intent1, intent2, validIntent);

                CapacityWindow shifted = CapacityWindow.of(win.end().plusSeconds(3600), Duration.ofHours(2));

                BiFunction<Set<IntentId>, Integer, List<ActionIntent>> selectiveReplanner = (invalidIds, iter) -> List.of(
                        ActionIntent.builder()
                                .id("INT-SUP-2-SHIFTED")
                                .agentId(AgentId.RECOVERY_PLANNING)
                                .actionType(ActionType.RESERVE_CAPACITY)
                                .targetEntityId("LINE-FAB-01")
                                .expectedStateVersion(snapshot.version())
                                .capacityClaim(CapacityClaim.of(machine, Quantity.of(100, UnitOfMeasure.PIECE), shifted))
                                .riskScore(0.2)
                                .build()
                );

                BiFunction<Collection<ActionIntent>, Integer, List<ActionIntent>> fullReplanner = (all, iter) -> List.of(
                        intent1,
                        ActionIntent.builder()
                                .id("INT-SUP-2-FULL")
                                .agentId(AgentId.RECOVERY_PLANNING)
                                .actionType(ActionType.RESERVE_CAPACITY)
                                .targetEntityId("LINE-FAB-01")
                                .expectedStateVersion(snapshot.version())
                                .capacityClaim(CapacityClaim.of(machine, Quantity.of(100, UnitOfMeasure.PIECE), shifted))
                                .riskScore(0.2)
                                .build(),
                        validIntent
                );

                yield new ScenarioFixture(scenarioType, candidates, snapshot, 1, 2, selectiveReplanner, fullReplanner);
            }

            default -> {
                ActionIntent intent1 = ActionIntent.builder()
                        .id("INT-DEF-1")
                        .agentId(AgentId.PRODUCTION)
                        .actionType(ActionType.RESERVE_CAPACITY)
                        .targetEntityId("LINE-FAB-01")
                        .expectedStateVersion(snapshot.version())
                        .capacityClaim(CapacityClaim.of(machine, Quantity.of(50, UnitOfMeasure.PIECE), win))
                        .riskScore(0.3)
                        .build();

                ActionIntent intent2 = ActionIntent.builder()
                        .id("INT-DEF-2")
                        .agentId(AgentId.RECOVERY_PLANNING)
                        .actionType(ActionType.RESERVE_CAPACITY)
                        .targetEntityId("LINE-FAB-01")
                        .expectedStateVersion(snapshot.version())
                        .capacityClaim(CapacityClaim.of(machine, Quantity.of(50, UnitOfMeasure.PIECE), win))
                        .riskScore(0.7)
                        .build();

                ActionIntent validIntent = ActionIntent.builder()
                        .id("INT-DEF-VALID")
                        .agentId(AgentId.LOGISTICS)
                        .actionType(ActionType.REROUTE_SHIPMENT)
                        .targetEntityId("TRUCK-01")
                        .expectedStateVersion(snapshot.version())
                        .timeWindow(win)
                        .riskScore(0.1)
                        .build();

                List<ActionIntent> candidates = List.of(intent1, intent2, validIntent);

                CapacityWindow shifted = CapacityWindow.of(win.end().plusSeconds(3600), Duration.ofHours(2));

                BiFunction<Set<IntentId>, Integer, List<ActionIntent>> selectiveReplanner = (invalidIds, iter) -> List.of(
                        ActionIntent.builder()
                                .id("INT-DEF-2-SHIFTED")
                                .agentId(AgentId.RECOVERY_PLANNING)
                                .actionType(ActionType.RESERVE_CAPACITY)
                                .targetEntityId("LINE-FAB-01")
                                .expectedStateVersion(snapshot.version())
                                .capacityClaim(CapacityClaim.of(machine, Quantity.of(50, UnitOfMeasure.PIECE), shifted))
                                .riskScore(0.2)
                                .build()
                );

                BiFunction<Collection<ActionIntent>, Integer, List<ActionIntent>> fullReplanner = (all, iter) -> List.of(
                        intent1,
                        ActionIntent.builder()
                                .id("INT-DEF-2-FULL")
                                .agentId(AgentId.RECOVERY_PLANNING)
                                .actionType(ActionType.RESERVE_CAPACITY)
                                .targetEntityId("LINE-FAB-01")
                                .expectedStateVersion(snapshot.version())
                                .capacityClaim(CapacityClaim.of(machine, Quantity.of(50, UnitOfMeasure.PIECE), shifted))
                                .riskScore(0.2)
                                .build(),
                        validIntent
                );

                yield new ScenarioFixture(scenarioType, candidates, snapshot, 1, 2, selectiveReplanner, fullReplanner);
            }
        };
    }
}
