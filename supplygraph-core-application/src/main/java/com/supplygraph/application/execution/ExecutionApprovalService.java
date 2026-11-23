package com.supplygraph.application.execution;

import com.supplygraph.domain.error.BusinessRuleException;
import com.supplygraph.domain.error.InvalidStateException;
import com.supplygraph.domain.model.audit.AuditAction;
import com.supplygraph.domain.model.execution.*;
import com.supplygraph.domain.model.security.Permission;
import com.supplygraph.domain.model.security.UserProfile;
import org.springframework.stereotype.Service;

import java.time.Instant;

/** Human approval of execution workflows with configurable separation of duties and a two-person rule for sensitive tasks. */
@Service
public class ExecutionApprovalService {
    private final ExecutionSupport support;
    private final AdapterSelector adapters;

    public ExecutionApprovalService(ExecutionSupport support, AdapterSelector adapters) { this.support = support; this.adapters = adapters; }

    public ExecutionWorkflow approve(String workflowId, WorkflowApproval.Scope scope, String rationale) {
        Permission needed = scope == WorkflowApproval.Scope.SENSITIVE_TASKS ? Permission.EXECUTION_APPROVE_SENSITIVE : Permission.EXECUTION_APPROVE;
        support.load(workflowId, needed);            // workspace + permission, server side
        return support.withLock(workflowId, () -> {
            ExecutionWorkflow w = support.reload(workflowId);
            if (w.status() != WorkflowStatus.PENDING_APPROVAL) throw new InvalidStateException("Workflow is " + w.status() + "; only PENDING_APPROVAL workflows can be approved");
            if (scope == WorkflowApproval.Scope.SENSITIVE_TASKS && !w.hasSensitiveTasks()) throw new BusinessRuleException("Workflow has no sensitive tasks");
            UserProfile user = support.user();
            ExecutionPolicy policy = support.policy(w.workspaceId());
            if (policy.approverMustDifferFromCreator() && user.id().value().equals(w.createdBy()))
                throw new SeparationOfDutiesException("Separation of duties: the creator of a workflow cannot approve it");
            if (scope == WorkflowApproval.Scope.SENSITIVE_TASKS && w.approvedBy(user.id().value()))
                throw new SeparationOfDutiesException("Two-person rule: the sensitive-task approver must differ from the workflow approver");
            if (scope == WorkflowApproval.Scope.WORKFLOW && w.approvals().stream().anyMatch(a -> a.approved() && a.scope() == WorkflowApproval.Scope.SENSITIVE_TASKS && a.userId().equals(user.id().value())))
                throw new SeparationOfDutiesException("Two-person rule: the workflow approver must differ from the sensitive-task approver");
            if (w.hasApproval(scope)) throw new InvalidStateException("Approval for scope " + scope + " already recorded");

            w = w.addApproval(new WorkflowApproval(w.id(), user.id().value(), user.role().name(), scope, true, rationale, Instant.now()));
            support.timeline(w, null, "APPROVAL_RECORDED", scope + " approved by " + user.id().value() + ": " + rationale);
            support.audit(w, AuditAction.WORKFLOW_APPROVED, w.id(), WorkflowStatus.PENDING_APPROVAL.name(), "approval:" + scope, rationale);
            boolean complete = w.hasApproval(WorkflowApproval.Scope.WORKFLOW)
                    && (!w.hasSensitiveTasks() || !policy.sensitiveTasksNeedSecondApproval() || w.hasApproval(WorkflowApproval.Scope.SENSITIVE_TASKS));
            if (complete) {
                w = w.markApproved(policy.sensitiveTasksNeedSecondApproval()).markReady(adapters.wouldBeSimulated(w.tasks(), policy));
                support.timeline(w, null, "WORKFLOW_READY", "All required approvals present; execution mode " + (w.simulated() ? "SIMULATED" : "LIVE"));
            }
            return support.save(w);
        });
    }

    public ExecutionWorkflow reject(String workflowId, String rationale) {
        support.load(workflowId, Permission.EXECUTION_APPROVE);
        return support.withLock(workflowId, () -> {
            ExecutionWorkflow w = support.reload(workflowId);
            if (w.status() != WorkflowStatus.PENDING_APPROVAL) throw new InvalidStateException("Workflow is " + w.status() + "; only PENDING_APPROVAL workflows can be rejected");
            UserProfile user = support.user();
            w = w.addApproval(new WorkflowApproval(w.id(), user.id().value(), user.role().name(), WorkflowApproval.Scope.WORKFLOW, false, rationale, Instant.now()));
            support.timeline(w, null, "WORKFLOW_REJECTED", rationale);
            support.audit(w, AuditAction.WORKFLOW_REJECTED, w.id(), WorkflowStatus.PENDING_APPROVAL.name(), WorkflowStatus.REJECTED.name(), rationale);
            return support.save(w);
        });
    }
}
