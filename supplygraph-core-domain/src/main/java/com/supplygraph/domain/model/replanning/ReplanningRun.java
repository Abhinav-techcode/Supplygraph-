package com.supplygraph.domain.model.replanning;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.monitoring.RiskAlertId;
import com.supplygraph.domain.model.plan.RecoveryPlanId;

import java.time.Instant;
import java.util.*;

/**
 * Domain aggregate capturing an adaptive selective replanning execution run.
 * Manages lifecycle transitions, preserves valid actions, and enforces maximum retry limits.
 */
public class ReplanningRun {

    private final ReplanningRunId id;
    private final WorkspaceId workspaceId;
    private final RiskAlertId triggerAlertId;
    private final RecoveryPlanId supersededPlanId;
    private RecoveryPlanId revisedPlanId;
    private ReplanningStatus status;
    private final Set<String> affectedAgentIds;
    private final Set<String> preservedActionIds;
    private final Set<String> reconsideredActionIds;
    private int retryCount;
    private final int maxRetries;
    private final String reason;
    private final List<String> auditTrail;
    private final Instant startedAt;
    private Instant completedAt;

    public ReplanningRun(
            ReplanningRunId id,
            WorkspaceId workspaceId,
            RiskAlertId triggerAlertId,
            RecoveryPlanId supersededPlanId,
            RecoveryPlanId revisedPlanId,
            ReplanningStatus status,
            Set<String> affectedAgentIds,
            Set<String> preservedActionIds,
            Set<String> reconsideredActionIds,
            int retryCount,
            int maxRetries,
            String reason,
            List<String> auditTrail,
            Instant startedAt,
            Instant completedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        this.triggerAlertId = triggerAlertId;
        this.supersededPlanId = supersededPlanId;
        this.revisedPlanId = revisedPlanId;
        this.status = status != null ? status : ReplanningStatus.DETECTED;
        this.affectedAgentIds = affectedAgentIds != null ? new LinkedHashSet<>(affectedAgentIds) : new LinkedHashSet<>();
        this.preservedActionIds = preservedActionIds != null ? new LinkedHashSet<>(preservedActionIds) : new LinkedHashSet<>();
        this.reconsideredActionIds = reconsideredActionIds != null ? new LinkedHashSet<>(reconsideredActionIds) : new LinkedHashSet<>();
        this.retryCount = Math.max(0, retryCount);
        this.maxRetries = maxRetries <= 0 ? 3 : maxRetries;
        this.reason = reason != null ? reason : "Operational change triggered adaptive replanning";
        this.auditTrail = auditTrail != null ? new ArrayList<>(auditTrail) : new ArrayList<>();
        this.startedAt = startedAt != null ? startedAt : Instant.now();
        this.completedAt = completedAt;
    }

    public static ReplanningRun start(
            WorkspaceId workspaceId,
            RiskAlertId triggerAlertId,
            RecoveryPlanId supersededPlanId,
            String reason
    ) {
        ReplanningRun run = new ReplanningRun(
                ReplanningRunId.generate(),
                workspaceId,
                triggerAlertId,
                supersededPlanId,
                null,
                ReplanningStatus.DETECTED,
                Set.of(),
                Set.of(),
                Set.of(),
                0,
                3,
                reason,
                new ArrayList<>(),
                Instant.now(),
                null
        );
        run.recordAudit("Replanning initiated for workspace " + workspaceId.value() + ": " + reason);
        return run;
    }

    public void advanceStatus(ReplanningStatus nextStatus, String note) {
        Objects.requireNonNull(nextStatus, "nextStatus must not be null");
        this.status = nextStatus;
        recordAudit("Status changed to " + nextStatus + ": " + note);
        if (nextStatus == ReplanningStatus.READY_FOR_REVIEW
                || nextStatus == ReplanningStatus.NO_ACTION_REQUIRED
                || nextStatus == ReplanningStatus.FAILED
                || nextStatus == ReplanningStatus.CANCELLED
                || nextStatus == ReplanningStatus.SUPERSEDED) {
            this.completedAt = Instant.now();
        }
    }

    public void fail(String failureReason) {
        this.status = ReplanningStatus.FAILED;
        this.completedAt = Instant.now();
        recordAudit("Replanning failed: " + failureReason);
    }

    public void incrementRetry() {
        this.retryCount++;
        if (this.retryCount > this.maxRetries) {
            fail("Exceeded maximum replanning retry limit (" + maxRetries + ")");
        } else {
            recordAudit("Retry iteration " + retryCount + " of " + maxRetries);
        }
    }

    public void recordAudit(String entry) {
        this.auditTrail.add("[" + Instant.now() + "] " + entry);
    }

    public void setRevisedPlanId(RecoveryPlanId planId) {
        this.revisedPlanId = planId;
    }

    public void addAffectedAgents(Collection<String> agents) {
        if (agents != null) {
            this.affectedAgentIds.addAll(agents);
        }
    }

    public void addPreservedActions(Collection<String> actions) {
        if (actions != null) {
            this.preservedActionIds.addAll(actions);
        }
    }

    public void addReconsideredActions(Collection<String> actions) {
        if (actions != null) {
            this.reconsideredActionIds.addAll(actions);
        }
    }

    public ReplanningRunId getId() {
        return id;
    }

    public WorkspaceId getWorkspaceId() {
        return workspaceId;
    }

    public RiskAlertId getTriggerAlertId() {
        return triggerAlertId;
    }

    public RecoveryPlanId getSupersededPlanId() {
        return supersededPlanId;
    }

    public RecoveryPlanId getRevisedPlanId() {
        return revisedPlanId;
    }

    public ReplanningStatus getStatus() {
        return status;
    }

    public Set<String> getAffectedAgentIds() {
        return Collections.unmodifiableSet(affectedAgentIds);
    }

    public Set<String> getPreservedActionIds() {
        return Collections.unmodifiableSet(preservedActionIds);
    }

    public Set<String> getReconsideredActionIds() {
        return Collections.unmodifiableSet(reconsideredActionIds);
    }

    public int getRetryCount() {
        return retryCount;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public String getReason() {
        return reason;
    }

    public List<String> getAuditTrail() {
        return Collections.unmodifiableList(auditTrail);
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}

