package com.supplygraph.domain.model.benchmark;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Reproducible record of an empirical benchmark trial comparing coordination baselines.
 */
public record BenchmarkExperimentRecord(
        ExperimentId id,
        String name,
        BenchmarkScenarioType scenario,
        BenchmarkBaseline baseline,
        String datasetReference,
        String twinVersion,
        long randomSeed,
        int trialRuns,
        Instant executedAt,
        BenchmarkMetrics metrics,
        String status,
        String limitationsNotes
) implements Serializable {

    public BenchmarkExperimentRecord {
        Objects.requireNonNull(id, "Experiment ID cannot be null");
        Objects.requireNonNull(name, "Experiment name cannot be null");
        Objects.requireNonNull(scenario, "Scenario cannot be null");
        Objects.requireNonNull(baseline, "Baseline cannot be null");
        Objects.requireNonNull(datasetReference, "Dataset reference cannot be null");
        Objects.requireNonNull(twinVersion, "Digital twin version cannot be null");
        Objects.requireNonNull(executedAt, "Execution timestamp cannot be null");
        Objects.requireNonNull(metrics, "Metrics cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
    }

    public static BenchmarkExperimentRecord create(
            String name,
            BenchmarkScenarioType scenario,
            BenchmarkBaseline baseline,
            String datasetReference,
            String twinVersion,
            long randomSeed,
            int trialRuns,
            BenchmarkMetrics metrics,
            String limitationsNotes
    ) {
        return new BenchmarkExperimentRecord(
                ExperimentId.generate(),
                name,
                scenario,
                baseline,
                datasetReference,
                twinVersion,
                randomSeed,
                trialRuns,
                Instant.now(),
                metrics,
                "COMPLETED",
                limitationsNotes != null ? limitationsNotes : "Controlled simulation environment"
        );
    }
}

