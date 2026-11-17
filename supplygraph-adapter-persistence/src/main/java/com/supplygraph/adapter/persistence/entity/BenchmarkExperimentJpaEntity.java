package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "benchmark_experiments")
public class BenchmarkExperimentJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "name", length = 256, nullable = false)
    private String name;

    @Column(name = "scenario", length = 64, nullable = false)
    private String scenario;

    @Column(name = "baseline", length = 64, nullable = false)
    private String baseline;

    @Column(name = "dataset_reference", length = 128, nullable = false)
    private String datasetReference;

    @Column(name = "twin_version", length = 32, nullable = false)
    private String twinVersion;

    @Column(name = "random_seed", nullable = false)
    private long randomSeed;

    @Column(name = "trial_runs", nullable = false)
    private int trialRuns;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "metrics_json", columnDefinition = "TEXT", nullable = false)
    private String metricsJson;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "limitations_notes", columnDefinition = "TEXT")
    private String limitationsNotes;

    protected BenchmarkExperimentJpaEntity() {}

    public BenchmarkExperimentJpaEntity(
            String id,
            String name,
            String scenario,
            String baseline,
            String datasetReference,
            String twinVersion,
            long randomSeed,
            int trialRuns,
            Instant executedAt,
            String metricsJson,
            String status,
            String limitationsNotes
    ) {
        this.id = id;
        this.name = name;
        this.scenario = scenario;
        this.baseline = baseline;
        this.datasetReference = datasetReference;
        this.twinVersion = twinVersion;
        this.randomSeed = randomSeed;
        this.trialRuns = trialRuns;
        this.executedAt = executedAt;
        this.metricsJson = metricsJson;
        this.status = status;
        this.limitationsNotes = limitationsNotes;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getScenario() { return scenario; }
    public String getBaseline() { return baseline; }
    public String getDatasetReference() { return datasetReference; }
    public String getTwinVersion() { return twinVersion; }
    public long getRandomSeed() { return randomSeed; }
    public int getTrialRuns() { return trialRuns; }
    public Instant getExecutedAt() { return executedAt; }
    public String getMetricsJson() { return metricsJson; }
    public String getStatus() { return status; }
    public String getLimitationsNotes() { return limitationsNotes; }
}

