package com.supplygraph.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "replanning_runs")
public class ReplanningRunJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "workspace_id", length = 64, nullable = false)
    private String workspaceId;

    @Column(name = "trigger_alert_id", length = 64)
    private String triggerAlertId;

    @Column(name = "superseded_plan_id", length = 64)
    private String supersededPlanId;

    @Column(name = "revised_plan_id", length = 64)
    private String revisedPlanId;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "affected_agent_ids", columnDefinition = "TEXT", nullable = false)
    private String affectedAgentIds;

    @Column(name = "preserved_action_ids", columnDefinition = "TEXT", nullable = false)
    private String preservedActionIds;

    @Column(name = "reconsidered_action_ids", columnDefinition = "TEXT", nullable = false)
    private String reconsideredActionIds;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries;

    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(name = "audit_trail_json", columnDefinition = "TEXT", nullable = false)
    private String auditTrailJson;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ReplanningRunJpaEntity() {}

    public ReplanningRunJpaEntity(
            String id, String workspaceId, String triggerAlertId,
            String supersededPlanId, String revisedPlanId, String status,
            String affectedAgentIds, String preservedActionIds, String reconsideredActionIds,
            int retryCount, int maxRetries, String reason, String auditTrailJson,
            Instant startedAt, Instant completedAt
    ) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.triggerAlertId = triggerAlertId;
        this.supersededPlanId = supersededPlanId;
        this.revisedPlanId = revisedPlanId;
        this.status = status;
        this.affectedAgentIds = affectedAgentIds;
        this.preservedActionIds = preservedActionIds;
        this.reconsideredActionIds = reconsideredActionIds;
        this.retryCount = retryCount;
        this.maxRetries = maxRetries;
        this.reason = reason;
        this.auditTrailJson = auditTrailJson;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public String getId() { return id; }
    public String getWorkspaceId() { return workspaceId; }
    public String getTriggerAlertId() { return triggerAlertId; }
    public String getSupersededPlanId() { return supersededPlanId; }
    public String getRevisedPlanId() { return revisedPlanId; }
    public String getStatus() { return status; }
    public String getAffectedAgentIds() { return affectedAgentIds; }
    public String getPreservedActionIds() { return preservedActionIds; }
    public String getReconsideredActionIds() { return reconsideredActionIds; }
    public int getRetryCount() { return retryCount; }
    public int getMaxRetries() { return maxRetries; }
    public String getReason() { return reason; }
    public String getAuditTrailJson() { return auditTrailJson; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
}

