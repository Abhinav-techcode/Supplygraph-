package com.supplygraph.domain.model;

import com.supplygraph.domain.model.benchmark.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BenchmarkDomainTest {

    @Test
    void benchmarkMetrics_recordsEmpiricalResultsAccurately() {
        BenchmarkMetrics metrics = new BenchmarkMetrics(
                0.95, // 95% precision
                0.90, // 90% recall
                0.05, // 5% false positive rate
                0.10, // 10% false negative rate
                8,    // 8 invalid actions prevented
                12,   // 12 valid actions preserved
                2,    // replanning scope = 2 nodes
                125L, // 125ms replanning execution
                4L,   // 4 days recovery
                15000.0, // $15,000 recovery cost
                98.5, // 98.5% service level
                1.0,  // 100% feasibility
                45L,  // 45ms simulation time
                210L, // 210ms agent time
                18L   // 18ms ConcurGuard coordination overhead
        );

        assertThat(metrics.conflictPrecision()).isEqualTo(0.95);
        assertThat(metrics.conflictRecall()).isEqualTo(0.90);
        assertThat(metrics.validActionsPreserved()).isEqualTo(12);
        assertThat(metrics.invalidActionsPrevented()).isEqualTo(8);
        assertThat(metrics.totalCoordinationOverheadMs()).isEqualTo(18L);
    }

    @Test
    void benchmarkScenarioAndBaseline_coverAllRequiredSpecifications() {
        assertThat(BenchmarkScenarioType.values()).hasSize(9);
        assertThat(BenchmarkBaseline.values()).hasSize(4);

        BenchmarkExperimentRecord record = BenchmarkExperimentRecord.create(
                "ConcurGuard Evaluation on Shared Inventory",
                BenchmarkScenarioType.SHARED_INVENTORY_CONTENTION,
                BenchmarkBaseline.CONCURGUARD_SELECTIVE_REPLANNING,
                "FIXTURE-EV-INV-01",
                "V2",
                42L,
                10,
                BenchmarkMetrics.empty(),
                "Deterministic trial run"
        );

        assertThat(record.id()).isNotNull();
        assertThat(record.scenario()).isEqualTo(BenchmarkScenarioType.SHARED_INVENTORY_CONTENTION);
        assertThat(record.baseline()).isEqualTo(BenchmarkBaseline.CONCURGUARD_SELECTIVE_REPLANNING);
        assertThat(record.trialRuns()).isEqualTo(10);
        assertThat(record.status()).isEqualTo("COMPLETED");
    }
}

