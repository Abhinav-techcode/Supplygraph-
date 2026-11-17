package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "recovery_plans")
public class RecoveryPlanJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "disruption_id", length = 64, nullable = false)
    private String disruptionId;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "source_twin_version", nullable = false)
    private Long sourceTwinVersion;

    @Column(name = "impact_assessment_id", length = 64)
    private String impactAssessmentId;

    @Column(name = "simulation_run_id", length = 64)
    private String simulationRunId;

    @Column(name = "monte_carlo_run_id", length = 64)
    private String monteCarloRunId;

    @Column(name = "optimization_run_id", length = 64)
    private String optimizationRunId;

    @Column(name = "plan_name", length = 255, nullable = false)
    private String planName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "actions_json", columnDefinition = "TEXT")
    private String actionsJson;

    @Column(name = "metrics_json", columnDefinition = "TEXT")
    private String metricsJson;

    @Column(name = "concurguard_validated", nullable = false)
    private boolean concurGuardValidated;

    @Column(name = "concurguard_conflicts_json", columnDefinition = "TEXT")
    private String concurGuardConflictsJson;

    @Column(name = "approval_json", columnDefinition = "TEXT")
    private String approvalJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "version", nullable = false)
    private Long version;

    protected RecoveryPlanJpaEntity() {
    }

    public RecoveryPlanJpaEntity(
            String id, String disruptionId, String workspaceId, Long sourceTwinVersion,
            String impactAssessmentId, String simulationRunId, String monteCarloRunId,
            String optimizationRunId, String planName, String description, String status,
            String actionsJson, String metricsJson, boolean concurGuardValidated,
            String concurGuardConflictsJson, String approvalJson, Instant createdAt,
            Instant updatedAt, Long version
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.disruptionId = Objects.requireNonNull(disruptionId, "disruptionId must not be null");
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        this.sourceTwinVersion = Objects.requireNonNull(sourceTwinVersion, "sourceTwinVersion must not be null");
        this.impactAssessmentId = impactAssessmentId;
        this.simulationRunId = simulationRunId;
        this.monteCarloRunId = monteCarloRunId;
        this.optimizationRunId = optimizationRunId;
        this.planName = Objects.requireNonNull(planName, "planName must not be null");
        this.description = description;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.actionsJson = actionsJson;
        this.metricsJson = metricsJson;
        this.concurGuardValidated = concurGuardValidated;
        this.concurGuardConflictsJson = concurGuardConflictsJson;
        this.approvalJson = approvalJson;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.version = version == null ? 0L : version;
    }

    public String getId() { return id; }
    public String getDisruptionId() { return disruptionId; }
    public String getWorkspaceId() { return workspaceId; }
    public Long getSourceTwinVersion() { return sourceTwinVersion; }
    public String getImpactAssessmentId() { return impactAssessmentId; }
    public String getSimulationRunId() { return simulationRunId; }
    public String getMonteCarloRunId() { return monteCarloRunId; }
    public String getOptimizationRunId() { return optimizationRunId; }
    public String getPlanName() { return planName; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getActionsJson() { return actionsJson; }
    public String getMetricsJson() { return metricsJson; }
    public boolean isConcurGuardValidated() { return concurGuardValidated; }
    public String getConcurGuardConflictsJson() { return concurGuardConflictsJson; }
    public String getApprovalJson() { return approvalJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public void setStatus(String status) { this.status = status; }
    public void setActionsJson(String actionsJson) { this.actionsJson = actionsJson; }
    public void setMetricsJson(String metricsJson) { this.metricsJson = metricsJson; }
    public void setConcurGuardValidated(boolean concurGuardValidated) { this.concurGuardValidated = concurGuardValidated; }
    public void setConcurGuardConflictsJson(String concurGuardConflictsJson) { this.concurGuardConflictsJson = concurGuardConflictsJson; }
    public void setApprovalJson(String approvalJson) { this.approvalJson = approvalJson; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setVersion(Long version) { this.version = version; }
}
