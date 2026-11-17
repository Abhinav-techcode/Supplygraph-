package com.supplygraph.domain.model.plan;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.disruption.DisruptionId;
import com.supplygraph.domain.model.impact.ImpactAssessmentId;
import com.supplygraph.domain.model.twin.StateVersion;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable recovery plan aggregate representing a candidate recovery strategy.
 * Governed by lifecycle transitions, simulation verification, ConcurGuard validation,
 * stale snapshot version checks, and explicit audited human approval.
 */
public record RecoveryPlan(
        RecoveryPlanId id,
        DisruptionId disruptionId,
        WorkspaceId workspaceId,
        StateVersion sourceTwinVersion,
        ImpactAssessmentId impactAssessmentId,
        String simulationRunId,
        String monteCarloRunId,
        String optimizationRunId,
        String planName,
        String description,
        RecoveryPlanStatus status,
        List<PlanAction> actions,
        PlanComparisonMetrics metrics,
        boolean concurGuardValidated,
        List<String> concurGuardConflictIds,
        PlanApproval approval,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public RecoveryPlan {
        Objects.requireNonNull(id, "RecoveryPlanId must not be null");
        Objects.requireNonNull(disruptionId, "DisruptionId must not be null");
        Objects.requireNonNull(workspaceId, "WorkspaceId must not be null");
        Objects.requireNonNull(sourceTwinVersion, "SourceTwinVersion must not be null");
        Objects.requireNonNull(planName, "PlanName must not be null");
        Objects.requireNonNull(status, "Status must not be null");
        Objects.requireNonNull(createdAt, "CreatedAt must not be null");
        Objects.requireNonNull(updatedAt, "UpdatedAt must not be null");

        actions = actions == null ? List.of() : Collections.unmodifiableList(List.copyOf(actions));
        concurGuardConflictIds = concurGuardConflictIds == null ? List.of() : Collections.unmodifiableList(List.copyOf(concurGuardConflictIds));
        metrics = metrics == null ? PlanComparisonMetrics.defaultMetrics() : metrics;
        description = description == null ? "" : description;

        if (version < 0) {
            throw new IllegalArgumentException("Version must be non-negative");
        }
    }

    public static RecoveryPlan createDraft(
            DisruptionId disruptionId,
            WorkspaceId workspaceId,
            StateVersion sourceTwinVersion,
            ImpactAssessmentId impactAssessmentId,
            String planName,
            String description,
            List<PlanAction> actions,
            PlanComparisonMetrics metrics
    ) {
        Instant now = Instant.now();
        return new RecoveryPlan(
                RecoveryPlanId.generate(),
                disruptionId,
                workspaceId,
                sourceTwinVersion,
                impactAssessmentId,
                null,
                null,
                null,
                planName,
                description,
                RecoveryPlanStatus.DRAFT,
                actions,
                metrics,
                false,
                List.of(),
                null,
                now,
                now,
                0L
        );
    }

    public RecoveryPlan markSimulated(String simulationRunId, PlanComparisonMetrics updatedMetrics, Instant timestamp) {
        ensureCanTransition(RecoveryPlanStatus.SIMULATED);
        return new RecoveryPlan(
                id, disruptionId, workspaceId, sourceTwinVersion, impactAssessmentId,
                simulationRunId, monteCarloRunId, optimizationRunId, planName, description,
                RecoveryPlanStatus.SIMULATED, actions, updatedMetrics != null ? updatedMetrics : metrics,
                concurGuardValidated, concurGuardConflictIds, approval,
                createdAt, timestamp, version + 1
        );
    }

    public RecoveryPlan markValidated(boolean isValid, List<String> conflictIds, Instant timestamp) {
        ensureCanTransition(RecoveryPlanStatus.VALIDATED);
        return new RecoveryPlan(
                id, disruptionId, workspaceId, sourceTwinVersion, impactAssessmentId,
                simulationRunId, monteCarloRunId, optimizationRunId, planName, description,
                RecoveryPlanStatus.VALIDATED, actions, metrics,
                isValid, conflictIds, approval,
                createdAt, timestamp, version + 1
        );
    }

    public RecoveryPlan markPendingApproval(Instant timestamp) {
        if (!concurGuardValidated) {
            throw new IllegalStateException("Plan cannot be marked PENDING_APPROVAL without passing ConcurGuard validation");
        }
        ensureCanTransition(RecoveryPlanStatus.PENDING_APPROVAL);
        return new RecoveryPlan(
                id, disruptionId, workspaceId, sourceTwinVersion, impactAssessmentId,
                simulationRunId, monteCarloRunId, optimizationRunId, planName, description,
                RecoveryPlanStatus.PENDING_APPROVAL, actions, metrics,
                concurGuardValidated, concurGuardConflictIds, approval,
                createdAt, timestamp, version + 1
        );
    }

    public RecoveryPlan approve(String approvedBy, String rationale, StateVersion currentTwinVersion, Instant timestamp) {
        if (status != RecoveryPlanStatus.PENDING_APPROVAL && status != RecoveryPlanStatus.VALIDATED) {
            throw new IllegalStateException("Plan must be in VALIDATED or PENDING_APPROVAL state to be approved: " + status);
        }
        if (!concurGuardValidated) {
            throw new IllegalStateException("Plan cannot be approved because ConcurGuard validation failed or has conflicts");
        }
        // Stale-state check: Plan formulated on an older twin state must not be approved without revalidation
        if (!sourceTwinVersion.equals(currentTwinVersion)) {
            throw new IllegalStateException("Stale plan cannot be approved! Plan was created on " + sourceTwinVersion
                    + " but current twin state is " + currentTwinVersion + ". Revalidation required.");
        }

        PlanApproval approvalRecord = new PlanApproval(approvedBy, timestamp, "APPROVED", rationale, currentTwinVersion);
        return new RecoveryPlan(
                id, disruptionId, workspaceId, sourceTwinVersion, impactAssessmentId,
                simulationRunId, monteCarloRunId, optimizationRunId, planName, description,
                RecoveryPlanStatus.APPROVED, actions, metrics,
                concurGuardValidated, concurGuardConflictIds, approvalRecord,
                createdAt, timestamp, version + 1
        );
    }

    public RecoveryPlan reject(String rejectedBy, String rationale, StateVersion currentTwinVersion, Instant timestamp) {
        if (status.isTerminal()) {
            throw new IllegalStateException("Cannot reject plan already in terminal state: " + status);
        }
        PlanApproval rejectionRecord = new PlanApproval(rejectedBy, timestamp, "REJECTED", rationale, currentTwinVersion);
        return new RecoveryPlan(
                id, disruptionId, workspaceId, sourceTwinVersion, impactAssessmentId,
                simulationRunId, monteCarloRunId, optimizationRunId, planName, description,
                RecoveryPlanStatus.REJECTED, actions, metrics,
                concurGuardValidated, concurGuardConflictIds, rejectionRecord,
                createdAt, timestamp, version + 1
        );
    }

    private void ensureCanTransition(RecoveryPlanStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException("Invalid recovery plan transition from " + status + " to " + target);
        }
    }
}

