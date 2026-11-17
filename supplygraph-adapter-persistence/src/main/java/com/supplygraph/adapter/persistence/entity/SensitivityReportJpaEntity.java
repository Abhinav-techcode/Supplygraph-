package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "sensitivity_analysis_reports")
public class SensitivityReportJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "alternative_id", length = 64, nullable = false)
    private String alternativeId;

    @Column(name = "baseline_cost_usd", precision = 19, scale = 4, nullable = false)
    private BigDecimal baselineCostUsd;

    @Column(name = "baseline_recovery_days", nullable = false)
    private double baselineRecoveryDays;

    @Column(name = "baseline_service_pct", nullable = false)
    private double baselineServicePct;

    @Column(name = "trials_json", columnDefinition = "TEXT", nullable = false)
    private String trialsJson;

    @Column(name = "overall_robustness_score", nullable = false)
    private double overallRobustnessScore;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    protected SensitivityReportJpaEntity() {}

    public SensitivityReportJpaEntity(
            String id, String workspaceId, String alternativeId, BigDecimal baselineCostUsd,
            double baselineRecoveryDays, double baselineServicePct, String trialsJson,
            double overallRobustnessScore, Instant generatedAt
    ) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.alternativeId = alternativeId;
        this.baselineCostUsd = baselineCostUsd;
        this.baselineRecoveryDays = baselineRecoveryDays;
        this.baselineServicePct = baselineServicePct;
        this.trialsJson = trialsJson;
        this.overallRobustnessScore = overallRobustnessScore;
        this.generatedAt = generatedAt;
    }

    public String getId() { return id; }
    public String getWorkspaceId() { return workspaceId; }
    public String getAlternativeId() { return alternativeId; }
    public BigDecimal getBaselineCostUsd() { return baselineCostUsd; }
    public double getBaselineRecoveryDays() { return baselineRecoveryDays; }
    public double getBaselineServicePct() { return baselineServicePct; }
    public String getTrialsJson() { return trialsJson; }
    public double getOverallRobustnessScore() { return overallRobustnessScore; }
    public Instant getGeneratedAt() { return generatedAt; }
}
