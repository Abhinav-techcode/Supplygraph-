package com.supplygraph.simulation;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.bom.BillOfMaterials;
import com.supplygraph.domain.model.disruption.Disruption;
import com.supplygraph.domain.model.disruption.DisruptionCategory;
import com.supplygraph.domain.model.disruption.DisruptionImpactParameters;
import com.supplygraph.domain.model.disruption.DisruptionSeverity;
import com.supplygraph.domain.model.graph.SupplyChainGraph;
import com.supplygraph.domain.model.twin.DigitalTwinSnapshot;
import com.supplygraph.domain.model.twin.StateVersion;
import com.supplygraph.simulation.optimization.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OrToolsRecoveryOptimizerTest {

    private DigitalTwinSnapshot createSnapshot() {
        return new DigitalTwinSnapshot(
                WorkspaceId.of("ws-opt"),
                StateVersion.of(1L),
                Instant.now(),
                SupplyChainGraph.empty(),
                BillOfMaterials.empty(),
                Map.of()
        );
    }

    private Disruption createDisruption() {
        return Disruption.createDraft(
                WorkspaceId.of("ws-opt"), "SEMICONDUCTOR", "FOUNDRY_OUTAGE", "TSMC Fab 14 Outage", "Power outage",
                DisruptionCategory.MANUFACTURING_SITE_OUTAGE, DisruptionSeverity.CRITICAL,
                Set.of(), Set.of(), Set.of(),
                Instant.now(), Instant.now().plusSeconds(86400 * 10),
                DisruptionImpactParameters.empty(), "Fab dispatch"
        );
    }

    @Test
    @DisplayName("Optimizer successfully allocates capacity to satisfy demand across alternatives")
    void testFeasibleOptimizationProblem() {
        DigitalTwinSnapshot snapshot = createSnapshot();
        Disruption disruption = createDisruption();

        AlternativeOption opt1 = new AlternativeOption("SUP-BACKUP-1", "Dresden Fab", BigDecimal.valueOf(600), BigDecimal.valueOf(150), 86400L * 2, 0.1);
        AlternativeOption opt2 = new AlternativeOption("SUP-BACKUP-2", "Singapore Fab", BigDecimal.valueOf(800), BigDecimal.valueOf(180), 86400L * 3, 0.15);

        RecoveryOptimizationProblem problem = new RecoveryOptimizationProblem(
                disruption,
                snapshot,
                BigDecimal.valueOf(1000),
                List.of(opt1, opt2),
                List.of(),
                OptimizationObjective.WEIGHTED_MULTI_OBJECTIVE,
                OptimizationWeights.balanced(),
                10
        );

        OrToolsRecoveryOptimizer optimizer = new OrToolsRecoveryOptimizer();
        OptimizationResult result = optimizer.solve(problem);

        assertThat(result.solverStatus().isFeasibleOrOptimal()).isTrue();
        assertThat(result.decisions()).isNotEmpty();
        assertThat(result.unmetDemandQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.estimatedCost()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Empty alternatives returns INFEASIBLE without fabricating decisions")
    void testInfeasibleOptimizationProblem() {
        DigitalTwinSnapshot snapshot = createSnapshot();
        Disruption disruption = createDisruption();

        RecoveryOptimizationProblem problem = new RecoveryOptimizationProblem(
                disruption,
                snapshot,
                BigDecimal.valueOf(500),
                List.of(), // Zero alternatives
                List.of(),
                OptimizationObjective.MINIMIZE_COST,
                OptimizationWeights.costCentric(),
                5
        );

        OrToolsRecoveryOptimizer optimizer = new OrToolsRecoveryOptimizer();
        OptimizationResult result = optimizer.solve(problem);

        assertThat(result.solverStatus()).isEqualTo(SolverStatus.INFEASIBLE);
        assertThat(result.decisions()).isEmpty();
        assertThat(result.unmetDemandQuantity()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(result.diagnostics()).anyMatch(d -> d.contains("No alternative suppliers"));
    }
}

