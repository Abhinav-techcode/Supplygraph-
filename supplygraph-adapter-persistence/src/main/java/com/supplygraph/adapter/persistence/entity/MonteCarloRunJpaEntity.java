package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "monte_carlo_runs")
public class MonteCarloRunJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "disruption_id", length = 64, nullable = false)
    private String disruptionId;

    @Column(name = "plan_id", length = 64)
    private String planId;

    @Column(name = "seed", nullable = false)
    private Long seed;

    @Column(name = "replication_count", nullable = false)
    private int replicationCount;

    @Column(name = "mean_delay_days", nullable = false)
    private double meanDelayDays;

    @Column(name = "median_delay_days", nullable = false)
    private double medianDelayDays;

    @Column(name = "p90_delay_days", nullable = false)
    private double p90DelayDays;

    @Column(name = "p95_delay_days", nullable = false)
    private double p95DelayDays;

    @Column(name = "mean_shortage_quantity", precision = 19, scale = 4, nullable = false)
    private BigDecimal meanShortageQuantity;

    @Column(name = "max_shortage_quantity", precision = 19, scale = 4, nullable = false)
    private BigDecimal maxShortageQuantity;

    @Column(name = "sla_violation_probability", nullable = false)
    private double slaViolationProbability;

    @Column(name = "recovery_within_deadline_probability", nullable = false)
    private double recoveryWithinDeadlineProbability;

    @Column(name = "estimated_mean_cost", precision = 19, scale = 4, nullable = false)
    private BigDecimal estimatedMeanCost;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MonteCarloRunJpaEntity() {
    }

    public MonteCarloRunJpaEntity(
            String id, String workspaceId, String disruptionId, String planId,
            Long seed, int replicationCount, double meanDelayDays, double medianDelayDays,
            double p90DelayDays, double p95DelayDays, BigDecimal meanShortageQuantity,
            BigDecimal maxShortageQuantity, double slaViolationProbability,
            double recoveryWithinDeadlineProbability, BigDecimal estimatedMeanCost,
            String status, Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        this.disruptionId = Objects.requireNonNull(disruptionId, "disruptionId must not be null");
        this.planId = planId;
        this.seed = Objects.requireNonNull(seed, "seed must not be null");
        this.replicationCount = replicationCount;
        this.meanDelayDays = meanDelayDays;
        this.medianDelayDays = medianDelayDays;
        this.p90DelayDays = p90DelayDays;
        this.p95DelayDays = p95DelayDays;
        this.meanShortageQuantity = meanShortageQuantity == null ? BigDecimal.ZERO : meanShortageQuantity;
        this.maxShortageQuantity = maxShortageQuantity == null ? BigDecimal.ZERO : maxShortageQuantity;
        this.slaViolationProbability = slaViolationProbability;
        this.recoveryWithinDeadlineProbability = recoveryWithinDeadlineProbability;
        this.estimatedMeanCost = estimatedMeanCost == null ? BigDecimal.ZERO : estimatedMeanCost;
        this.status = status == null ? "COMPLETED" : status;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public String getId() { return id; }
    public String getWorkspaceId() { return workspaceId; }
    public String getDisruptionId() { return disruptionId; }
    public String getPlanId() { return planId; }
    public Long getSeed() { return seed; }
    public int getReplicationCount() { return replicationCount; }
    public double getMeanDelayDays() { return meanDelayDays; }
    public double getMedianDelayDays() { return medianDelayDays; }
    public double getP90DelayDays() { return p90DelayDays; }
    public double getP95DelayDays() { return p95DelayDays; }
    public BigDecimal getMeanShortageQuantity() { return meanShortageQuantity; }
    public BigDecimal getMaxShortageQuantity() { return maxShortageQuantity; }
    public double getSlaViolationProbability() { return slaViolationProbability; }
    public double getRecoveryWithinDeadlineProbability() { return recoveryWithinDeadlineProbability; }
    public BigDecimal getEstimatedMeanCost() { return estimatedMeanCost; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}

