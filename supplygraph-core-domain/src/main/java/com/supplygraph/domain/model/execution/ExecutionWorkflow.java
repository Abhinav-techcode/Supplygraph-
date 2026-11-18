package com.supplygraph.domain.model.execution;

import com.supplygraph.domain.model.WorkspaceId;

import java.time.Instant;
import java.util.*;

/** Workflow aggregate: tasks, approvals and a validated state machine. Immutable; transitions return new instances. */
public record ExecutionWorkflow(
        String id, WorkspaceId workspaceId, String planId, String correlationId, WorkflowStatus status,
        String createdBy, String planApprovedBy, String sourceTwinVersion, boolean simulated, int replanDepth,
        String statusReason, List<ExecutionTask> tasks, List<WorkflowApproval> approvals, Instant createdAt, Instant updatedAt, long version
) {
    public ExecutionWorkflow {
        Objects.requireNonNull(id); Objects.requireNonNull(workspaceId); Objects.requireNonNull(planId); Objects.requireNonNull(status);
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
        approvals = approvals == null ? List.of() : List.copyOf(approvals);
    }

    public static ExecutionWorkflow draft(WorkspaceId ws, String planId, String createdBy, String planApprovedBy, String twinVersion,
                                          List<ExecutionTask> tasks, String correlationId, int replanDepth) {
        Instant now = Instant.now();
        return new ExecutionWorkflow(tasks.isEmpty() ? UUID.randomUUID().toString() : tasks.get(0).workflowId(), ws, planId,
                correlationId != null ? correlationId : UUID.randomUUID().toString(), WorkflowStatus.DRAFT, createdBy, planApprovedBy, twinVersion,
                true, replanDepth, null, tasks, List.of(), now, now, 0);
    }

    public TaskGraph graph() { return new TaskGraph(tasks); }
    public Optional<ExecutionTask> task(String taskId) { return tasks.stream().filter(t -> t.id().equals(taskId)).findFirst(); }
    public boolean hasSensitiveTasks() { return tasks.stream().anyMatch(ExecutionTask::sensitive); }

    /** DRAFT -> VALIDATED; fails with {@link TaskGraph.CycleException} for cyclic graphs. */
    public ExecutionWorkflow validate() {
        if (tasks.isEmpty()) throw new IllegalStateException("A workflow needs at least one task");
        graph().topologicalOrder();
        return to(WorkflowStatus.VALIDATED, null);
    }
    public ExecutionWorkflow submitForApproval() { return to(WorkflowStatus.PENDING_APPROVAL, null); }
    public ExecutionWorkflow block(String reason) { return to(WorkflowStatus.BLOCKED, reason); }
    public ExecutionWorkflow cancel(String reason) {
        List<ExecutionTask> cancelled = tasks.stream().map(t -> (!t.status().isTerminal() && t.status() != TaskStatus.AWAITING_ACK && t.status() != TaskStatus.IN_PROGRESS
                && t.status() != TaskStatus.RECONCILIATION_REQUIRED) ? t.markCancelled() : t).toList();
        return new ExecutionWorkflow(id, workspaceId, planId, correlationId, checked(WorkflowStatus.CANCELLED), createdBy, planApprovedBy, sourceTwinVersion, simulated,
                replanDepth, reason, cancelled, approvals, createdAt, Instant.now(), version + 1);
    }

    public ExecutionWorkflow addApproval(WorkflowApproval a) {
        List<WorkflowApproval> list = new ArrayList<>(approvals);
        list.add(a);
        ExecutionWorkflow w = copy(status, statusReason, tasks, list);
        if (!a.approved()) return w.to(WorkflowStatus.REJECTED, a.rationale());
        return w;
    }

    public boolean hasApproval(WorkflowApproval.Scope scope) { return approvals.stream().anyMatch(a -> a.scope() == scope && a.approved()); }
    public boolean approvedBy(String userId) { return approvals.stream().anyMatch(a -> a.approved() && a.userId().equals(userId)); }

    /** PENDING_APPROVAL -> APPROVED once the required approvals are present. */
    public ExecutionWorkflow markApproved(boolean sensitiveSecondApprovalRequired) {
        if (!hasApproval(WorkflowApproval.Scope.WORKFLOW)) throw new IllegalStateException("Workflow approval missing");
        if (hasSensitiveTasks() && sensitiveSecondApprovalRequired && !hasApproval(WorkflowApproval.Scope.SENSITIVE_TASKS))
            throw new IllegalStateException("Sensitive tasks require an additional approval");
        return to(WorkflowStatus.APPROVED, null);
    }
    public ExecutionWorkflow markReady(boolean simulatedMode) {
        ExecutionWorkflow w = to(WorkflowStatus.READY, null);
        return new ExecutionWorkflow(w.id, workspaceId, planId, correlationId, w.status, createdBy, planApprovedBy, sourceTwinVersion, simulatedMode, replanDepth,
                null, tasks, approvals, createdAt, w.updatedAt, w.version);
    }
    public ExecutionWorkflow markExecuting() { return status == WorkflowStatus.EXECUTING ? this : to(WorkflowStatus.EXECUTING, null); }

    public ExecutionWorkflow withTasks(List<ExecutionTask> updated) { return copy(status, statusReason, updated, approvals); }

    /** Derives the workflow status from task states after a scheduling pass. Only valid transitions are applied; otherwise status is unchanged. */
    public ExecutionWorkflow settle() {
        if (status.isTerminal() || (status != WorkflowStatus.EXECUTING && status != WorkflowStatus.RECONCILIATION_REQUIRED
                && status != WorkflowStatus.PARTIALLY_COMPLETED && status != WorkflowStatus.COMPENSATION_REQUIRED)) return this;
        boolean anyRecon = tasks.stream().anyMatch(t -> t.status() == TaskStatus.RECONCILIATION_REQUIRED);
        boolean inFlight = tasks.stream().anyMatch(t -> switch (t.status()) {
            case PENDING, READY, IN_PROGRESS, AWAITING_ACK, RETRY_PENDING -> true;
            default -> false;
        });
        boolean anyCompleted = tasks.stream().anyMatch(t -> t.status() == TaskStatus.COMPLETED);
        boolean anyDead = tasks.stream().anyMatch(t -> t.status() == TaskStatus.FAILED || t.status() == TaskStatus.BLOCKED);
        boolean compensableDone = tasks.stream().anyMatch(t -> t.status() == TaskStatus.COMPLETED && t.type().compensable());
        WorkflowStatus target;
        if (anyRecon) target = WorkflowStatus.RECONCILIATION_REQUIRED;
        else if (inFlight) target = WorkflowStatus.EXECUTING;
        else if (!anyDead) target = WorkflowStatus.COMPLETED;
        else if (!anyCompleted) target = WorkflowStatus.FAILED;
        else target = compensableDone ? WorkflowStatus.COMPENSATION_REQUIRED : WorkflowStatus.PARTIALLY_COMPLETED;
        if (target == status) return this;
        return status.canTransitionTo(target) ? to(target, null) : this;
    }

    /** Operator accepts the partial result without compensating. */
    public ExecutionWorkflow acceptPartial() { return to(WorkflowStatus.PARTIALLY_COMPLETED, "Partial result accepted"); }

    private ExecutionWorkflow to(WorkflowStatus next, String reason) {
        return copy(checked(next), reason, tasks, approvals);
    }
    private WorkflowStatus checked(WorkflowStatus next) {
        if (!status.canTransitionTo(next)) throw new IllegalStateException("Invalid workflow transition " + status + " -> " + next);
        return next;
    }
    private ExecutionWorkflow copy(WorkflowStatus s, String reason, List<ExecutionTask> t, List<WorkflowApproval> a) {
        return new ExecutionWorkflow(id, workspaceId, planId, correlationId, s, createdBy, planApprovedBy, sourceTwinVersion, simulated, replanDepth, reason, t, a, createdAt, Instant.now(), version + 1);
    }
}
