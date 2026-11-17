package com.supplygraph.application.service;

import com.supplygraph.application.pareto.MultiObjectiveParetoService;
import com.supplygraph.application.port.out.*;
import com.supplygraph.application.resilience.AdvancedResilienceAnalyticsService;
import com.supplygraph.application.sensitivity.SensitivityAnalysisService;
import com.supplygraph.application.whatif.WhatIfAnalysisService;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.graph.SupplyChainGraph;
import com.supplygraph.domain.model.pareto.ParetoFrontier;
import com.supplygraph.domain.model.recovery.PlanAlternativeId;
import com.supplygraph.domain.model.recovery.RecoveryPlanAlternative;
import com.supplygraph.domain.model.resilience.advanced.AdvancedResilienceMetrics;
import com.supplygraph.domain.model.sensitivity.SensitivityAnalysisReport;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import com.supplygraph.domain.model.twin.StateVersion;
import com.supplygraph.domain.model.whatif.WhatIfComparisonResult;
import com.supplygraph.domain.model.whatif.WhatIfScenarioConfig;
import com.supplygraph.domain.model.whatif.WhatIfScenarioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WhatIfAndParetoAndSensitivityTest {

    @Test
    @DisplayName("WhatIfAnalysisService should evaluate scenario differentials without mutating state")
    void testWhatIfAnalysis() {
        WhatIfExperimentRepositoryPort repo = mock(WhatIfExperimentRepositoryPort.class);
        WhatIfAnalysisService service = new WhatIfAnalysisService(repo);

        WhatIfScenarioConfig config = WhatIfScenarioConfig.of(
                WhatIfScenarioId.of("WIF-TEST"),
                "Air Freight Surge",
                "Testing air freight under reduced supplier capacity",
                "WS-01",
                0.8,
                1.5,
                -2,
                "AIR",
                14,
                0.5,
                0.3,
                0.2
        );

        WhatIfComparisonResult result = service.executeScenario(config);

        assertThat(result).isNotNull();
        assertThat(result.scenarioName()).isEqualTo("Air Freight Surge");
        assertThat(result.costVariancePct()).isGreaterThan(0.0);
        assertThat(result.scenarioTimeToRecoverDays()).isLessThan(result.baselineTimeToRecoverDays());
        verify(repo, times(1)).save(any(WhatIfComparisonResult.class));
    }

    @Test
    @DisplayName("MultiObjectiveParetoService should identify Pareto-optimal non-dominated solutions")
    void testParetoFrontierComputation() {
        ParetoEvaluationRepositoryPort repo = mock(ParetoEvaluationRepositoryPort.class);
        MultiObjectiveParetoService service = new MultiObjectiveParetoService(repo);

        List<RecoveryPlanAlternative> alts = List.of(
                new RecoveryPlanAlternative(PlanAlternativeId.of("A1"), "Expensive Fast", "", BigDecimal.valueOf(100000), 24.0, 99.0, List.of()),
                new RecoveryPlanAlternative(PlanAlternativeId.of("A2"), "Cheap Slow", "", BigDecimal.valueOf(20000), 120.0, 85.0, List.of()),
                // Dominated by A1 (more expensive, slower, worse service)
                new RecoveryPlanAlternative(PlanAlternativeId.of("A3"), "Strictly Worse", "", BigDecimal.valueOf(110000), 30.0, 90.0, List.of())
        );

        ParetoFrontier frontier = service.evaluateAlternatives("DISR-01", alts);

        assertThat(frontier).isNotNull();
        assertThat(frontier.allSolutions()).hasSize(3);
        assertThat(frontier.frontierSolutions()).hasSize(2);
        assertThat(frontier.frontierSolutions().stream().anyMatch(s -> s.alternativeId().equals("A3"))).isFalse();
        verify(repo, times(1)).save(any(ParetoFrontier.class));
    }

    @Test
    @DisplayName("SensitivityAnalysisService should compute elasticity and robustness score")
    void testSensitivityAnalysis() {
        SensitivityAnalysisRepositoryPort repo = mock(SensitivityAnalysisRepositoryPort.class);
        SensitivityAnalysisService service = new SensitivityAnalysisService(repo);

        SensitivityAnalysisReport report = service.runSensitivitySuite(
                "WS-01",
                "ALT-01",
                BigDecimal.valueOf(50000),
                10.0,
                95.0
        );

        assertThat(report).isNotNull();
        assertThat(report.trials()).hasSize(25); // 5 parameters * 5 perturbations
        assertThat(report.overallRobustnessScore()).isBetween(0.0, 1.0);
        verify(repo, times(1)).save(any(SensitivityAnalysisReport.class));
    }

    @Test
    @DisplayName("AdvancedResilienceAnalyticsService should compute TTS, TTR, and composite resilience")
    void testAdvancedResilienceAnalytics() {
        AdvancedResilienceRepositoryPort repo = mock(AdvancedResilienceRepositoryPort.class);
        DigitalTwinRepositoryPort twinRepo = mock(DigitalTwinRepositoryPort.class);

        SupplyChainGraph graph = new SupplyChainGraph(List.of(), List.of());
        DigitalTwinState state = new DigitalTwinState(
                WorkspaceId.of("WS-RES"),
                StateVersion.initial(),
                graph,
                com.supplygraph.domain.model.bom.BillOfMaterials.empty()
        );
        when(twinRepo.findByWorkspaceId(WorkspaceId.of("WS-RES"))).thenReturn(Optional.of(state));

        AdvancedResilienceAnalyticsService service = new AdvancedResilienceAnalyticsService(repo, twinRepo);
        AdvancedResilienceMetrics metrics = service.evaluateWorkspaceResilience("WS-RES");

        assertThat(metrics).isNotNull();
        assertThat(metrics.timeToSurviveDays()).isGreaterThan(0.0);
        assertThat(metrics.timeToRecoverDays()).isGreaterThan(0.0);
        assertThat(metrics.compositeResilienceScore()).isBetween(0.0, 100.0);
        verify(repo, times(1)).save(any(AdvancedResilienceMetrics.class));
    }
}
