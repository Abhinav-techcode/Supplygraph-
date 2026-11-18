package com.supplygraph.application.execution;

import com.supplygraph.application.port.out.*;
import com.supplygraph.application.service.RecoveryPlanningApplicationService;
import com.supplygraph.domain.model.audit.AuditAction;
import com.supplygraph.domain.model.execution.*;
import com.supplygraph.domain.model.plan.RecoveryPlan;
import com.supplygraph.domain.model.plan.RecoveryPlanId;
import com.supplygraph.domain.model.plan.RecoveryPlanStatus;
import com.supplygraph.domain.model.security.Permission;
import com.supplygraph.domain.model.security.UserId;
import com.supplygraph.domain.model.security.UserProfile;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Deterministic approved-plan → workflow pipeline. A plan that fails any check yields a BLOCKED workflow (auditable, never executable)
 * and a governed reassessment proposal; nothing is ever silently executed from an obsolete plan.
 */
@Service
public class RecoveryPlanConversionService {
    static final Duration MAX_APPROVAL_AGE = Duration.ofDays(7);
    private static final Logger log = LoggerFactory.getLogger(RecoveryPlanConversionService.class);

    private final RecoveryPlanRepositoryPort plans;
    private final DigitalTwinRepositoryPort twins;
    private final UserProfileRepositoryPort users;
    private final RecoveryPlanningApplicationService planning;
    private final ExecutionWorkflowRepositoryPort workflows;
    private final OutcomeRepositoryPort outcomes;
    private final ExecutionSupport support;

    public RecoveryPlanConversionService(RecoveryPlanRepositoryPort plans, DigitalTwinRepositoryPort twins, UserProfileRepositoryPort users,
                                         RecoveryPlanningApplicationService planning, ExecutionWorkflowRepositoryPort workflows,
                                         OutcomeRepositoryPort outcomes, ExecutionSupport support) {
        this.plans = plans; this.twins = twins; this.users = users; this.planning = planning; this.workflows = workflows;
        this.outcomes = outcomes; this.support = support;
    }

    public ExecutionWorkflow convert(String planId) {
        RecoveryPlan plan = plans.findById(RecoveryPlanId.of(planId)).orElseThrow(() -> new NoSuchElementException("Recovery plan not found: " + planId));
        support.guard().enforceWorkspaceAndPermission(plan.workspaceId(), Permission.EXECUTION_CREATE);
        UserProfile creator = support.user();

        // 1-2. approved + approval record
        if (plan.status() != RecoveryPlanStatus.APPROVED || plan.approval() == null || !"APPROVED".equals(plan.approval().decision())) {
            throw new IllegalStateException("Only an APPROVED recovery plan can be converted (status: " + plan.status() + ")");
        }
        for (ExecutionWorkflow existing : workflows.findByPlan(plan.workspaceId(), planId)) {
            if (!existing.status().isTerminal() && existing.status() != WorkflowStatus.BLOCKED) {
                throw new IllegalStateException("An active execution workflow already exists for plan " + planId + ": " + existing.id());
            }
        }

        String workflowId = "WF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ExecutionPolicy policy = support.policy(plan.workspaceId());
        List<ExecutionTask> tasks = WorkflowPlanner.plan(workflowId, plan.actions(), policy.maxTaskAttempts());
        String twinVersion = plan.sourceTwinVersion().toString();
        ExecutionWorkflow draft = ExecutionWorkflow.draft(plan.workspaceId(), planId, creator.id().value(), plan.approval().approvedBy(), twinVersion, tasks, null, 0);

        // 3. approval identity & authorization
        String blockReason = approvalProblem(plan);
        // 4. twin snapshot validity
        if (blockReason == null) blockReason = twins.findByWorkspaceId(plan.workspaceId())
                .map(DigitalTwinState::getCurrentVersion)
                .filter(v -> !v.equals(plan.sourceTwinVersion()))
                .map(v -> "Stale plan: digital twin advanced from " + plan.sourceTwinVersion() + " to " + v + " since the plan was created").orElse(null);
        if (blockReason == null && twins.findByWorkspaceId(plan.workspaceId()).isEmpty()) blockReason = "Digital twin not found for workspace";
        // 7. stale assumptions
        if (blockReason == null && Duration.between(plan.approval().approvalTimestamp(), Instant.now()).compareTo(MAX_APPROVAL_AGE) > 0)
            blockReason = "Plan approval is older than " + MAX_APPROVAL_AGE.toDays() + " days; assumptions are considered stale";
        // 5-6. constraints + ConcurGuard against the current state
        if (blockReason == null) {
            List<String> conflicts = planning.revalidateConflicts(plan.id());
            if (!conflicts.isEmpty()) blockReason = "ConcurGuard re-validation found " + conflicts.size() + " conflict(s): " + conflicts.get(0);
        }
        // 10. dependencies: validate() fails on cycles
        ExecutionWorkflow w;
        if (blockReason == null) {
            try {
                w = draft.validate().submitForApproval();
            } catch (TaskGraph.CycleException e) {
                w = draft.block(e.getMessage());
                blockReason = e.getMessage();
            }
        } else {
            w = draft.block(blockReason);
        }
        w = workflows.save(w);
        support.timeline(w, null, w.status() == WorkflowStatus.BLOCKED ? "WORKFLOW_BLOCKED" : "WORKFLOW_CREATED",
                w.status() == WorkflowStatus.BLOCKED ? blockReason : "Workflow created from approved plan " + planId + " with " + w.tasks().size() + " task(s)");
        support.audit(w, w.status() == WorkflowStatus.BLOCKED ? AuditAction.WORKFLOW_BLOCKED : AuditAction.WORKFLOW_CREATED, w.id(), "NONE", w.status().name(),
                blockReason != null ? blockReason : "Converted approved plan " + planId);
        if (blockReason != null) {
            log.warn("Plan {} blocked from execution: {}", planId, blockReason);
            proposeReassessmentForBlocked(w, blockReason);
        }
        return w;
    }

    private String approvalProblem(RecoveryPlan plan) {
        String approver = plan.approval().approvedBy();
        Optional<UserProfile> profile = users.findById(UserId.of(approver));
        if (profile.isEmpty()) profile = users.findByUsername(approver);
        if (profile.isEmpty()) return "Approval is not traceable to a known user: '" + approver + "'";
        UserProfile p = profile.get();
        if (!p.active()) return "Approver " + approver + " is not active";
        if (!p.hasPermission(Permission.PLAN_APPROVE)) return "Approver " + approver + " does not hold PLAN_APPROVE";
        if (!p.hasAccessToWorkspace(plan.workspaceId())) return "Approver " + approver + " has no access to the plan's workspace";
        return null;
    }

    /** A blocked plan triggers a governed reassessment proposal (bounded, deduplicated, requires human decision). */
    private void proposeReassessmentForBlocked(ExecutionWorkflow blocked, String reason) {
        boolean open = workflows.findByPlan(blocked.workspaceId(), blocked.planId()).stream()
                .flatMap(x -> outcomes.findReassessmentsByWorkflow(x.id()).stream()).anyMatch(r -> r.status() == ReassessmentRequest.Status.PROPOSED);
        if (open) return;
        ReassessmentRequest r = outcomes.saveReassessment(ReassessmentRequest.propose(blocked, 0, "Plan blocked from execution: " + reason, "SYSTEM"));
        support.timeline(blocked, null, "REASSESSMENT_PROPOSED", r.reason());
        support.audit(blocked, AuditAction.REASSESSMENT_PROPOSED, r.id(), "NONE", "PROPOSED", r.reason());
    }
}
