package com.supplygraph.concurguard.benchmark;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurGuardBenchmarkTest {

    @Test
    @DisplayName("Scientific benchmark demonstrates ConcurGuard superiority in preserving valid work and ensuring feasibility")
    void shouldExecuteComparativeBenchmark() {
        Instant now = Instant.parse("2026-11-16T12:00:00Z");
        ResourceAliasRegistry aliasRegistry = new ResourceAliasRegistry();
        ConcurGuardBenchmarkRunner runner = new ConcurGuardBenchmarkRunner(aliasRegistry);

        DigitalTwinSnapshot snapshot = new DigitalTwinSnapshot(
                WorkspaceId.of("WS-BENCHMARK"), StateVersion.of(1L), now,
                SupplyChainGraph.empty(), BillOfMaterials.empty(), Map.of());

        CapacityWindow win1 = CapacityWindow.of(now.plusSeconds(3600), Duration.ofHours(2));
        ResourceId machine = ResourceId.of("LINE-FAB-01");

        // 3 initial intents:
        // Intent 1: Unaffected procurement (50%)
        ActionIntent int1 = ActionIntent.builder()
                .id("B-PO")
                .agentId(AgentId.PROCUREMENT)
                .actionType(ActionType.EXPEDITE_PURCHASE_ORDER)
                .targetEntityId("PO-1")
                .expectedStateVersion(snapshot.version())
                .timeWindow(win1)
                .build();

        // Intent 2: Conflicted capacity claim A
        ActionIntent int2 = ActionIntent.builder()
                .id("B-CAP-A")
                .agentId(AgentId.PRODUCTION)
                .actionType(ActionType.RESERVE_CAPACITY)
                .targetEntityId("LINE-FAB-01")
                .expectedStateVersion(snapshot.version())
                .capacityClaim(CapacityClaim.of(machine, Quantity.of(100, UnitOfMeasure.PIECE), win1))
                .riskScore(0.6)
                .build();

        // Intent 3: Conflicted capacity claim B
        ActionIntent int3 = ActionIntent.builder()
                .id("B-CAP-B")
                .agentId(AgentId.RECOVERY_PLANNING)
                .actionType(ActionType.RESERVE_CAPACITY)
                .targetEntityId("LINE-FAB-01")
                .expectedStateVersion(snapshot.version())
                .capacityClaim(CapacityClaim.of(machine, Quantity.of(100, UnitOfMeasure.PIECE), win1))
                .riskScore(0.2)
                .build();

        Map<StrategyType, BenchmarkMetrics> benchmark = runner.evaluate(
                List.of(int1, int2, int3),
                snapshot,
                1,
                (invalidated, iter) -> {
                    // Selective replanner shifts only B-CAP-A
                    CapacityWindow shifted = CapacityWindow.of(win1.end().plusSeconds(3600), Duration.ofHours(2));
                    return List.of(ActionIntent.builder()
                            .id("B-CAP-A-SHIFTED")
                            .agentId(AgentId.PRODUCTION)
                            .actionType(ActionType.RESERVE_CAPACITY)
                            .targetEntityId("LINE-FAB-01")
                            .expectedStateVersion(snapshot.version())
                            .capacityClaim(CapacityClaim.of(machine, Quantity.of(100, UnitOfMeasure.PIECE), shifted))
                            .riskScore(0.2)
                            .build());
                },
                (currentPlan, iter) -> {
                    // Full replanner regenerates entire plan with shifted windows
                    CapacityWindow shifted = CapacityWindow.of(win1.end().plusSeconds(3600), Duration.ofHours(2));
                    return List.of(
                            int1,
                            ActionIntent.builder().id("B-CAP-A-NEW").agentId(AgentId.PRODUCTION).actionType(ActionType.RESERVE_CAPACITY)
                                    .targetEntityId("LINE-FAB-01").expectedStateVersion(snapshot.version()).capacityClaim(CapacityClaim.of(machine, Quantity.of(100, UnitOfMeasure.PIECE), shifted)).riskScore(0.2).build(),
                            int3
                    );
                }
        );

        BenchmarkMetrics noCoord = benchmark.get(StrategyType.NO_COORDINATION);
        BenchmarkMetrics fullReplan = benchmark.get(StrategyType.FULL_PLAN_REGENERATION);
        BenchmarkMetrics selective = benchmark.get(StrategyType.CONCURGUARD_SELECTIVE);

        // 1. Uncoordinated strategy fails plan feasibility
        assertThat(noCoord.planFeasible()).isFalse();

        // 2. Full replan succeeds but discards 100% of unaffected work during replanning iteration
        assertThat(fullReplan.planFeasible()).isTrue();
        assertThat(fullReplan.percentIntentsPreserved()).isEqualTo(0.0);

        // 3. ConcurGuard Selective preserves unaffected proposals (B-PO and B-CAP-B) while achieving plan feasibility!
        assertThat(selective.planFeasible()).isTrue();
        assertThat(selective.percentIntentsPreserved()).isGreaterThanOrEqualTo(66.0);
        assertThat(selective.conflictPrecision()).isEqualTo(1.0);
        assertThat(selective.conflictRecall()).isEqualTo(1.0);
    }
}
