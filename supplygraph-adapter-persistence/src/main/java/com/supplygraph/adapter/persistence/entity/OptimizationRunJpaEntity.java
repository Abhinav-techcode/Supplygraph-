package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "optimization_runs")
public class OptimizationRunJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "disruption_id", length = 64, nullable = false)
    private String disruptionId;

    @Column(name = "solver_status", length = 32, nullable = false)
    private String solverStatus;

    @Column(name = "objective_value", nullable = false)
    private double objectiveValue;

    @Column(name = "unmet_demand_quantity", precision = 19, scale = 4, nullable = false)
    private BigDecimal unmetDemandQuantity;

    @Column(name = "expected_delay_days", nullable = false)
    private double expectedDelayDays;

    @Column(name = "estimated_cost", precision = 19, scale = 4, nullable = false)
    private BigDecimal estimatedCost;

    @Column(name = "solver_runtime_millis", nullable = false)
    private long solverRuntimeMillis;

    @Column(name = "decisions_json", columnDefinition = "TEXT")
    private String decisionsJson;

    @Column(name = "diagnostics_json", columnDefinition = "TEXT")
    private String diagnosticsJson;

    @Column(name = "explanations_json", columnDefinition = "TEXT")
    private String explanationsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OptimizationRunJpaEntity() {
    }

    public OptimizationRunJpaEntity(
            String id, String workspaceId, String disruptionId, String solverStatus,
            double objectiveValue, BigDecimal unmetDemandQuantity, double expectedDelayDays,
            BigDecimal estimatedCost, long solverRuntimeMillis, String decisionsJson,
            String diagnosticsJson, String explanationsJson, Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        this.disruptionId = Objects.requireNonNull(disruptionId, "disruptionId must not be null");
        this.solverStatus = Objects.requireNonNull(solverStatus, "solverStatus must not be null");
        this.objectiveValue = objectiveValue;
        this.unmetDemandQuantity = unmetDemandQuantity == null ? BigDecimal.ZERO : unmetDemandQuantity;
        this.expectedDelayDays = expectedDelayDays;
        this.estimatedCost = estimatedCost == null ? BigDecimal.ZERO : estimatedCost;
        this.solverRuntimeMillis = solverRuntimeMillis;
        this.decisionsJson = decisionsJson;
        this.diagnosticsJson = diagnosticsJson;
        this.explanationsJson = explanationsJson;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public String getId() { return id; }
    public String getWorkspaceId() { return workspaceId; }
    public String getDisruptionId() { return disruptionId; }
    public String getSolverStatus() { return solverStatus; }
    public double getObjectiveValue() { return objectiveValue; }
    public BigDecimal getUnmetDemandQuantity() { return unmetDemandQuantity; }
    public double getExpectedDelayDays() { return expectedDelayDays; }
    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public long getSolverRuntimeMillis() { return solverRuntimeMillis; }
    public String getDecisionsJson() { return decisionsJson; }
    public String getDiagnosticsJson() { return diagnosticsJson; }
    public String getExplanationsJson() { return explanationsJson; }
    public Instant getCreatedAt() { return createdAt; }
}

