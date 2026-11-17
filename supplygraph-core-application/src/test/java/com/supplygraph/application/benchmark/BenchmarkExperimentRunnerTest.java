package com.supplygraph.application.benchmark;

import com.supplygraph.application.port.out.BenchmarkExperimentRepositoryPort;
import com.supplygraph.domain.model.benchmark.BenchmarkBaseline;
import com.supplygraph.domain.model.benchmark.BenchmarkExperimentRecord;
import com.supplygraph.domain.model.benchmark.BenchmarkScenarioType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BenchmarkExperimentRunnerTest {

    private BenchmarkExperimentRepositoryPort experimentRepository;
    private BenchmarkExperimentRunner runner;

    @BeforeEach
    void setUp() {
        experimentRepository = mock(BenchmarkExperimentRepositoryPort.class);
        when(experimentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        runner = new BenchmarkExperimentRunner(experimentRepository);
    }

    @Test
    void runTrial_concurGuardSelectiveReplanning_achievesHighPrecisionAndPreservesValidActions() {
        BenchmarkExperimentRecord record = runner.runTrial(
                "ConcurGuard Shared Inventory Trial",
                BenchmarkScenarioType.SHARED_INVENTORY_CONTENTION,
                BenchmarkBaseline.CONCURGUARD_SELECTIVE_REPLANNING,
                42L,
                3
        );

        assertThat(record).isNotNull();
        assertThat(record.baseline()).isEqualTo(BenchmarkBaseline.CONCURGUARD_SELECTIVE_REPLANNING);
        assertThat(record.metrics().conflictPrecision()).isGreaterThanOrEqualTo(0.95);
        assertThat(record.metrics().conflictRecall()).isEqualTo(1.0);
        assertThat(record.metrics().validActionsPreserved()).isGreaterThan(0);
        assertThat(record.metrics().replanningScopeSize()).isLessThanOrEqualTo(2);
        assertThat(record.metrics().optimizationFeasibilityRate()).isEqualTo(1.0);

        verify(experimentRepository).save(any(BenchmarkExperimentRecord.class));
    }

    @Test
    void runTrial_uncoordinatedAgents_failsToPreventInvalidActions() {
        BenchmarkExperimentRecord record = runner.runTrial(
                "Uncoordinated Shared Inventory Trial",
                BenchmarkScenarioType.SHARED_INVENTORY_CONTENTION,
                BenchmarkBaseline.UNCOORDINATED_AGENTS,
                42L,
                1
        );

        assertThat(record.baseline()).isEqualTo(BenchmarkBaseline.UNCOORDINATED_AGENTS);
        assertThat(record.metrics().invalidActionsPrevented()).isEqualTo(0);
        assertThat(record.metrics().conflictPrecision()).isEqualTo(0.0);
        assertThat(record.metrics().falseNegativeRate()).isEqualTo(1.0);
    }

    @Test
    void runTrial_fullPlanRegeneration_preservesZeroActionsAcrossIterations() {
        BenchmarkExperimentRecord record = runner.runTrial(
                "All-or-Nothing Full Replan Trial",
                BenchmarkScenarioType.SHARED_INVENTORY_CONTENTION,
                BenchmarkBaseline.CONFLICT_DETECTION_WITHOUT_SELECTIVE_REPLANNING,
                42L,
                1
        );

        assertThat(record.baseline()).isEqualTo(BenchmarkBaseline.CONFLICT_DETECTION_WITHOUT_SELECTIVE_REPLANNING);
        assertThat(record.metrics().validActionsPreserved()).isEqualTo(0); // All actions discarded on conflict
        assertThat(record.metrics().replanningScopeSize()).isGreaterThan(1);
    }
}

