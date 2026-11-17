package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "what_if_experiments")
public class WhatIfExperimentJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "scenario_id", length = 64, nullable = false)
    private String scenarioId;

    @Column(name = "scenario_name", length = 255, nullable = false)
    private String scenarioName;

    @Column(name = "baseline_cost_usd", precision = 19, scale = 4, nullable = false)
    private BigDecimal baselineCostUsd;

    @Column(name = "scenario_cost_usd", precision = 19, scale = 4, nullable = false)
    private BigDecimal scenarioCostUsd;

    @Column(name = "baseline_ttr_days", nullable = false)
    private double baselineTtrDays;

    @Column(name = "scenario_ttr_days", nullable = false)
    private double scenarioTtrDays;

    @Column(name = "baseline_service_pct", nullable = false)
    private double baselineServicePct;

    @Column(name = "scenario_service_pct", nullable = false)
    private double scenarioServicePct;

    @Column(name = "baseline_resilience_idx", nullable = false)
    private double baselineResilienceIdx;

    @Column(name = "scenario_resilience_idx", nullable = false)
    private double scenarioResilienceIdx;

    @Column(name = "cost_variance_pct", nullable = false)
    private double costVariancePct;

    @Column(name = "recovery_time_delta_days", nullable = false)
    private double recoveryTimeDeltaDays;

    @Column(name = "variance_summary", columnDefinition = "TEXT", nullable = false)
    private String varianceSummary;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    protected WhatIfExperimentJpaEntity() {}

    public WhatIfExperimentJpaEntity(
            String id, String scenarioId, String scenarioName, BigDecimal baselineCostUsd,
            BigDecimal scenarioCostUsd, double baselineTtrDays, double scenarioTtrDays,
            double baselineServicePct, double scenarioServicePct, double baselineResilienceIdx,
            double scenarioResilienceIdx, double costVariancePct, double recoveryTimeDeltaDays,
            String varianceSummary, Instant evaluatedAt
    ) {
        this.id = id;
        this.scenarioId = scenarioId;
        this.scenarioName = scenarioName;
        this.baselineCostUsd = baselineCostUsd;
        this.scenarioCostUsd = scenarioCostUsd;
        this.baselineTtrDays = baselineTtrDays;
        this.scenarioTtrDays = scenarioTtrDays;
        this.baselineServicePct = baselineServicePct;
        this.scenarioServicePct = scenarioServicePct;
        this.baselineResilienceIdx = baselineResilienceIdx;
        this.scenarioResilienceIdx = scenarioResilienceIdx;
        this.costVariancePct = costVariancePct;
        this.recoveryTimeDeltaDays = recoveryTimeDeltaDays;
        this.varianceSummary = varianceSummary;
        this.evaluatedAt = evaluatedAt;
    }

    public String getId() { return id; }
    public String getScenarioId() { return scenarioId; }
    public String getScenarioName() { return scenarioName; }
    public BigDecimal getBaselineCostUsd() { return baselineCostUsd; }
    public BigDecimal getScenarioCostUsd() { return scenarioCostUsd; }
    public double getBaselineTtrDays() { return baselineTtrDays; }
    public double getScenarioTtrDays() { return scenarioTtrDays; }
    public double getBaselineServicePct() { return baselineServicePct; }
    public double getScenarioServicePct() { return scenarioServicePct; }
    public double getBaselineResilienceIdx() { return baselineResilienceIdx; }
    public double getScenarioResilienceIdx() { return scenarioResilienceIdx; }
    public double getCostVariancePct() { return costVariancePct; }
    public double getRecoveryTimeDeltaDays() { return recoveryTimeDeltaDays; }
    public String getVarianceSummary() { return varianceSummary; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
}
