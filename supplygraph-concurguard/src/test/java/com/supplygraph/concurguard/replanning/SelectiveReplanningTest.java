package com.supplygraph.concurguard.replanning;

import com.supplygraph.concurguard.arbitration.ConcurGuardArbitrator;
import com.supplygraph.concurguard.conflict.ConcurGuardConflictDetector;
import com.supplygraph.concurguard.model.*;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.BillOfMaterials;
import com.supplygraph.domain.model.capacity.CapacityWindow;
import com.supplygraph.domain.model.capacity.ResourceId;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;
import com.supplygraph.domain.model.graph.SupplyChainGraph;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import com.supplygraph.domain.model.twin.StateVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class SelectiveReplanningTest {

    private ResourceAliasRegistry aliasRegistry;
    private ConcurGuardConflictDetector detector;
    private ConcurGuardArbitrator arbitrator;
    private SelectiveReplanningCoordinator coordinator;
    private DigitalTwinSnapshot snapshot;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.parse("2026-11-16T12:00:00Z");
        aliasRegistry = new ResourceAliasRegistry();
        detector = new ConcurGuardConflictDetector(aliasRegistry);
        arbitrator = new ConcurGuardArbitrator();
        coordinator = new SelectiveReplanningCoordinator(detector, arbitrator, 3);

        snapshot = new DigitalTwinSnapshot(
                WorkspaceId.of("WS-REPLAN"),
                StateVersion.of(1L),
                now,
                SupplyChainGraph.empty(),
                BillOfMaterials.empty(),
                Map.of()
        );
    }

    @Test
    @DisplayName("Selective replanning preserves unaffected proposals and only regenerates affected subgraph")
    void shouldIsolateMinimalAffectedSubgraphAndPreserveValidProposals() {
        CapacityWindow win1 = CapacityWindow.of(now.plusSeconds(3600), Duration.ofHours(2));
        CapacityWindow win2 = CapacityWindow.of(now.plusSeconds(18000), Duration.ofHours(2));

        // 1. Unaffected independent procurement order
        ActionIntent cleanProcurement = ActionIntent.builder()
                .id("I-CLEAN-PO")
                .agentId(AgentId.PROCUREMENT)
                .actionType(ActionType.EXPEDITE_PURCHASE_ORDER)
                .targetEntityId("PO-CATHODE-101")
                .expectedStateVersion(snapshot.version())
                .timeWindow(win1)
                .build();

        // 2. Unaffected independent warehouse staging
        ActionIntent cleanWarehouse = ActionIntent.builder()
                .id("I-CLEAN-WH")
                .agentId(AgentId.LOGISTICS)
                .actionType(ActionType.REROUTE_SHIPMENT)
                .targetEntityId("TRUCK-BAY-4")
                .expectedStateVersion(snapshot.version())
                .timeWindow(win1)
                .build();

        // 3. Competing capacity reservation A (collides with B)
        ResourceId coater = ResourceId.of("RES-COATER-1");
        ActionIntent conflictingA = ActionIntent.builder()
                .id("I-CONF-A")
                .agentId(AgentId.PRODUCTION)
                .actionType(ActionType.RESERVE_CAPACITY)
                .targetEntityId("RES-COATER-1")
                .expectedStateVersion(snapshot.version())
                .capacityClaim(CapacityClaim.of(coater, Quantity.of(100, UnitOfMeasure.PIECE), win1))
                .riskScore(0.5) // Higher risk, will lose contention
                .build();

        // 4. Competing capacity reservation B (wins contention)
        ActionIntent conflictingB = ActionIntent.builder()
                .id("I-CONF-B")
                .agentId(AgentId.RECOVERY_PLANNING)
                .actionType(ActionType.RESERVE_CAPACITY)
                .targetEntityId("RES-COATER-1")
                .expectedStateVersion(snapshot.version())
                .capacityClaim(CapacityClaim.of(coater, Quantity.of(100, UnitOfMeasure.PIECE), win1))
                .riskScore(0.2) // Lower risk, will win
                .build();

        // 5. Dependent downstream packaging batch that depends on conflictingA
        ActionIntent dependentDownstream = ActionIntent.builder()
                .id("I-DEP-ON-A")
                .agentId(AgentId.PRODUCTION)
                .actionType(ActionType.SPLIT_PRODUCTION_BATCH)
                .targetEntityId("BATCH-PKG-1")
                .expectedStateVersion(snapshot.version())
                .timeWindow(win2)
                .addDependency(IntentId.of("I-CONF-A"))
                .build();

        List<ActionIntent> initialPool = List.of(cleanProcurement, cleanWarehouse, conflictingA, conflictingB, dependentDownstream);

        // Replan supplier: Reschedules conflictingA to win2 + 4 hours, and shifts dependent accordingly
        ReplanningResult result = coordinator.coordinate(initialPool, snapshot, (invalidated, iteration) -> {
            assertThat(invalidated).contains(IntentId.of("I-CONF-A"), IntentId.of("I-DEP-ON-A"));
            assertThat(invalidated).doesNotContain(IntentId.of("I-CLEAN-PO"), IntentId.of("I-CLEAN-WH"), IntentId.of("I-CONF-B"));

            CapacityWindow shiftedWin = CapacityWindow.of(now.plusSeconds(28800), Duration.ofHours(2));
            ActionIntent resolvedA = ActionIntent.builder()
                    .id("I-CONF-A-REPLANNED")
                    .agentId(AgentId.PRODUCTION)
                    .actionType(ActionType.RESERVE_CAPACITY)
                    .targetEntityId("RES-COATER-1")
                    .expectedStateVersion(snapshot.version())
                    .capacityClaim(CapacityClaim.of(coater, Quantity.of(100, UnitOfMeasure.PIECE), shiftedWin))
                    .riskScore(0.2)
                    .build();

            ActionIntent resolvedDep = ActionIntent.builder()
                    .id("I-DEP-REPLANNED")
                    .agentId(AgentId.PRODUCTION)
                    .actionType(ActionType.SPLIT_PRODUCTION_BATCH)
                    .targetEntityId("BATCH-PKG-1")
                    .expectedStateVersion(snapshot.version())
                    .timeWindow(CapacityWindow.of(shiftedWin.end().plusSeconds(3600), Duration.ofHours(2)))
                    .addDependency(IntentId.of("I-CONF-A-REPLANNED"))
                    .build();

            return List.of(resolvedA, resolvedDep);
        });

        assertThat(result.success()).isTrue();
        assertThat(result.preservedIntents()).contains(IntentId.of("I-CLEAN-PO"), IntentId.of("I-CLEAN-WH"));
        assertThat(result.finalPlan()).hasSize(5);
    }

    @Test
    @DisplayName("Selective replanning detects no-progress deadlock and escalates cleanly")
    void shouldEscalateOnNoProgress() {
        ResourceId machine = ResourceId.of("RES-MILL-1");
        CapacityWindow win = CapacityWindow.of(now.plusSeconds(3600), Duration.ofHours(2));

        ActionIntent a = ActionIntent.builder()
                .id("I-STALL-A")
                .agentId(AgentId.PRODUCTION)
                .actionType(ActionType.RESERVE_CAPACITY)
                .targetEntityId("RES-MILL-1")
                .expectedStateVersion(snapshot.version())
                .capacityClaim(CapacityClaim.of(machine, Quantity.of(50, UnitOfMeasure.HOUR), win))
                .riskScore(0.3)
                .build();

        ActionIntent b = ActionIntent.builder()
                .id("I-STALL-B")
                .agentId(AgentId.RECOVERY_PLANNING)
                .actionType(ActionType.RESERVE_CAPACITY)
                .targetEntityId("RES-MILL-1")
                .expectedStateVersion(snapshot.version())
                .capacityClaim(CapacityClaim.of(machine, Quantity.of(50, UnitOfMeasure.HOUR), win))
                .riskScore(0.5)
                .build();

        // Replan supplier keeps proposing the exact same conflicting claim
        ReplanningResult result = coordinator.coordinate(List.of(a, b), snapshot, (invalidated, iter) -> List.of(b));

        assertThat(result.success()).isFalse();
        assertThat(result.escalationReason()).isPresent();
        assertThat(result.escalationReason().get()).contains("ESCALATION_NO_PROGRESS");
    }
}
