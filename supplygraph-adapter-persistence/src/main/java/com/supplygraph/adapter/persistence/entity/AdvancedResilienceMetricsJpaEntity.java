package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "advanced_resilience_metrics")
public class AdvancedResilienceMetricsJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "time_to_survive_days", nullable = false)
    private double timeToSurviveDays;

    @Column(name = "time_to_recover_days", nullable = false)
    private double timeToRecoverDays;

    @Column(name = "supplier_exposure_idx", nullable = false)
    private double supplierExposureIdx;

    @Column(name = "recovery_flexibility_idx", nullable = false)
    private double recoveryFlexibilityIdx;

    @Column(name = "bottleneck_node_count", nullable = false)
    private int bottleneckNodeCount;

    @Column(name = "composite_resilience_score", nullable = false)
    private double compositeResilienceScore;

    @Column(name = "breakdown_json", columnDefinition = "TEXT")
    private String breakdownJson;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    protected AdvancedResilienceMetricsJpaEntity() {}

    public AdvancedResilienceMetricsJpaEntity(
            String id, String workspaceId, double timeToSurviveDays, double timeToRecoverDays,
            double supplierExposureIdx, double recoveryFlexibilityIdx, int bottleneckNodeCount,
            double compositeResilienceScore, String breakdownJson, Instant calculatedAt
    ) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.timeToSurviveDays = timeToSurviveDays;
        this.timeToRecoverDays = timeToRecoverDays;
        this.supplierExposureIdx = supplierExposureIdx;
        this.recoveryFlexibilityIdx = recoveryFlexibilityIdx;
        this.bottleneckNodeCount = bottleneckNodeCount;
        this.compositeResilienceScore = compositeResilienceScore;
        this.breakdownJson = breakdownJson;
        this.calculatedAt = calculatedAt;
    }

    public String getId() { return id; }
    public String getWorkspaceId() { return workspaceId; }
    public double getTimeToSurviveDays() { return timeToSurviveDays; }
    public double getTimeToRecoverDays() { return timeToRecoverDays; }
    public double getSupplierExposureIdx() { return supplierExposureIdx; }
    public double getRecoveryFlexibilityIdx() { return recoveryFlexibilityIdx; }
    public int getBottleneckNodeCount() { return bottleneckNodeCount; }
    public double getCompositeResilienceScore() { return compositeResilienceScore; }
    public String getBreakdownJson() { return breakdownJson; }
    public Instant getCalculatedAt() { return calculatedAt; }
}
