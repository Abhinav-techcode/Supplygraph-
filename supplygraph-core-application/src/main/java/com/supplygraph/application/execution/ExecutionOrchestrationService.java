package com.supplygraph.application.execution;

import com.supplygraph.application.execution.ExecutionAdapter.ExecutionResponse;
import com.supplygraph.application.port.out.DigitalTwinRepositoryPort;
import com.supplygraph.application.port.out.ExecutionAttemptRepositoryPort;
import com.supplygraph.application.port.out.ExecutionEvidenceRepositoryPort;
import com.supplygraph.domain.model.audit.AuditAction;
import com.supplygraph.domain.model.execution.*;
import com.supplygraph.domain.model.security.Permission;
import com.supplygraph.domain.model.twin.DigitalTwinState;
import org.springframework.beans.factory.DisposableBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

/**
 * Dependency-aware, bounded execution engine. Order of operations for each task: persist IN_PROGRESS + STARTED attempt, THEN call the adapter,
 * THEN persist the outcome. A crash in between therefore leaves an auditable unknown outcome (reconciled, never blindly retried).
 */
@Service
public class ExecutionOrchestrationService implements DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(ExecutionOrchestrationService.class);
    private static final int MAX_PASSES = 50;

    private final ExecutionSupport support;
    private final AdapterSelector adapters;
    private final ExecutionAttemptRepositoryPort attempts;
    private final ExecutionEvidenceRepositoryPort evidence;
    private final DigitalTwinRepositoryPort twins;
    private final TwinFeedback feedback;
    private final ExecutorService pool = new ThreadPoolExecutor(2, 4, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(16), r -> {
        Thread t = new Thread(r, "execution-adapter-call"); t.setDaemon(true); return t; }, new ThreadPoolExecutor.AbortPolicy());

    public ExecutionOrchestrationService(ExecutionSupport support, AdapterSelector adapters, ExecutionAttemptRepositoryPort attempts,
                                         ExecutionEvidenceRepositoryPort evidence, DigitalTwinRepositoryPort twins, TwinFeedback feedback) {
        this.support = support; this.adapters = adapters; this.attempts = attempts; this.evidence = evidence; this.twins = twins; this.feedback = feedback;
    }

    @Override public void destroy() { pool.shutdown(); }

    /** Starts a READY workflow. Requires EXECUTION_RUN; the executor must differ from the approvers when the workspace policy says so. */
    public ExecutionWorkflow start(String workflowId) {
        support.load(workflowId, Permission.EXECUTION_RUN);
        return support.withLock(workflowId, () -> {
            ExecutionWorkflow w = support.reload(workflowId);
            if (w.status() != WorkflowStatus.READY) throw new IllegalStateException("Workflow is " + w.status() + "; only READY workflows can be started");
            String user = support.user().id().value();
            ExecutionPolicy policy = support.policy(w.workspaceId());
            if (policy.executorMustDifferFromApprover() && w.approvedBy(user))
                throw new SeparationOfDutiesException("Separation of duties: an approver of this workflow cannot execute it");
            String stale = staleReason(w);
            if (stale != null) {
                w = support.save(w.block(stale));
                support.timeline(w, null, "WORKFLOW_BLOCKED", stale);
                support.audit(w, AuditAction.WORKFLOW_BLOCKED, w.id(), WorkflowStatus.READY.name(), WorkflowStatus.BLOCKED.name(), stale);
                return w;
            }
            w = support.save(w.markExecuting());
            support.timeline(w, null, "WORKFLOW_STARTED", "Execution started by " + user + " in " + (w.simulated() ? "SIMULATED" : "LIVE") + " mode");
            support.audit(w, AuditAction.WORKFLOW_STARTED, w.id(), WorkflowStatus.READY.name(), WorkflowStatus.EXECUTING.name(), "Execution started");
            return drive(w, policy);
        });
    }

    /** Explicit, authorized retry of a RETRY_PENDING task. Unknown outcomes (RECONCILIATION_REQUIRED) are rejected here by the state machine. */
    public ExecutionWorkflow retryTask(String workflowId, String taskId) {
        support.load(workflowId, Permission.EXECUTION_RUN);
        return support.withLock(workflowId, () -> {
            ExecutionWorkflow w = support.reload(workflowId);
            ExecutionTask t = w.task(taskId).orElseThrow(() -> new NoSuchElementException("Task not found: " + taskId));
            if (t.status() != TaskStatus.RETRY_PENDING) throw new IllegalStateException("Task " + taskId + " is " + t.status() + "; only RETRY_PENDING tasks can be retried. Outcomes of unknown state must be reconciled first.");
            if (w.status() != WorkflowStatus.EXECUTING) throw new IllegalStateException("Workflow is " + w.status() + "; cannot retry");
            support.audit(w, AuditAction.TASK_RETRIED, taskId, TaskStatus.RETRY_PENDING.name(), "RETRYING", "Retry attempt " + (t.attempts() + 1) + " of " + t.maxAttempts());
            w = executeOne(w, taskId, support.policy(w.workspaceId()));
            return drive(w, support.policy(w.workspaceId()));
        });
    }

    public ExecutionWorkflow cancel(String workflowId, String reason) {
        support.load(workflowId, Permission.EXECUTION_RUN);
        return support.withLock(workflowId, () -> {
            ExecutionWorkflow w = support.reload(workflowId);
            if (reason == null || reason.isBlank()) throw new IllegalArgumentException("A cancellation reason is required");
            ExecutionWorkflow c = support.save(w.cancel(reason));
            support.timeline(c, null, "WORKFLOW_CANCELLED", reason);
            support.audit(c, AuditAction.WORKFLOW_CANCELLED, c.id(), w.status().name(), WorkflowStatus.CANCELLED.name(), reason);
            return c;
        });
    }

    /** Re-evaluates the graph after an out-of-band change (acknowledgement, reconciliation). Caller holds the lock and has authorized. */
    ExecutionWorkflow advance(String workflowId) {
        ExecutionWorkflow w = support.reload(workflowId);
        if (w.status() != WorkflowStatus.EXECUTING && w.status() != WorkflowStatus.RECONCILIATION_REQUIRED && w.status() != WorkflowStatus.PARTIALLY_COMPLETED
                && w.status() != WorkflowStatus.COMPENSATION_REQUIRED) return w;
        if (w.status() != WorkflowStatus.EXECUTING && w.tasks().stream().noneMatch(t -> t.status() == TaskStatus.PENDING || t.status() == TaskStatus.BLOCKED)) return support.save(w.settle());
        if (w.status() == WorkflowStatus.RECONCILIATION_REQUIRED || w.status() == WorkflowStatus.PARTIALLY_COMPLETED || w.status() == WorkflowStatus.COMPENSATION_REQUIRED) {
            if (w.tasks().stream().anyMatch(t -> t.status() == TaskStatus.RECONCILIATION_REQUIRED)) return support.save(w.settle());
            w = support.save(w.markExecuting());
        }
        return drive(w, support.policy(w.workspaceId()));
    }

    private ExecutionWorkflow drive(ExecutionWorkflow start, ExecutionPolicy policy) {
        ExecutionWorkflow w = start;
        for (int pass = 0; pass < MAX_PASSES; pass++) {
            TaskGraph g = w.graph();
            Map<String, ExecutionTask> byId = new LinkedHashMap<>();
            w.tasks().forEach(t -> byId.put(t.id(), t));
            boolean changed = false;
            for (ExecutionTask t : g.skippable()) { byId.put(t.id(), t.markSkipped("Condition not met")); changed = true; }
            for (ExecutionTask t : g.toBlock()) { byId.put(t.id(), t.markBlocked("Upstream task failed or was blocked")); changed = true; }
            if (changed) {
                w = support.save(w.withTasks(new ArrayList<>(byId.values())));
                for (ExecutionTask t : w.tasks()) if (t.status() == TaskStatus.BLOCKED || t.status() == TaskStatus.SKIPPED) support.timeline(w, t.id(), "TASK_" + t.status(), t.lastError());
                continue;
            }
            List<ExecutionTask> runnable = new TaskGraph(w.tasks()).runnable();
            if (runnable.isEmpty()) break;
            for (ExecutionTask t : runnable) w = executeOne(w, t.id(), policy);   // independent tasks of one pass; executed one after another, deterministically
        }
        ExecutionWorkflow before = w;
        w = support.save(w.settle());
        if (w.status() != before.status() || w == before) support.timeline(w, null, "WORKFLOW_STATUS", "Status " + w.status());
        return w;
    }

    private ExecutionWorkflow executeOne(ExecutionWorkflow w, String taskId, ExecutionPolicy policy) {
        ExecutionTask task = w.task(taskId).orElseThrow();
        if (task.status() == TaskStatus.PENDING || task.status() == TaskStatus.BLOCKED) task = task.markReady();
        if (task.status() != TaskStatus.READY && task.status() != TaskStatus.RETRY_PENDING) return w;
        Optional<ExecutionAdapter> found = adapters.select(task.type(), policy);
        if (found.isEmpty()) return replace(w, task.startAttempt().failPermanently("No execution adapter supports " + task.type()), "TASK_FAILED", "No adapter");
        ExecutionAdapter adapter = found.get();
        ExecutionAdapter.Request req = new ExecutionAdapter.Request(w.workspaceId(), w.id(), task.id(), task.type(), task.parameters(), "task:" + task.id(), w.correlationId(), support.actor());
        if (task.status() == TaskStatus.READY || task.status() == TaskStatus.RETRY_PENDING) {
            List<String> violations = adapter.checkPreconditions(req);
            if (!violations.isEmpty()) {
                ExecutionTask failed = task.startAttempt().failPermanently("Preconditions not met: " + String.join("; ", violations));
                return replace(w, failed, "TASK_FAILED", failed.lastError());
            }
        }
        ExecutionTask running = task.startAttempt();
        w = support.save(w.withTasks(swap(w.tasks(), running)));
        ExecutionAttempt attempt = attempts.save(ExecutionAttempt.start(w.id(), running.id(), running.attempts(), adapter.adapterId(), adapter.simulated(), req.idempotencyKey(), support.actor()));
        support.timeline(w, running.id(), "ATTEMPT_STARTED", "Attempt " + running.attempts() + " via " + adapter.adapterId() + (adapter.simulated() ? " [SIMULATED]" : ""));

        ExecutionResponse response = callAdapter(adapter, req);
        ExecutionTask next;
        AttemptOutcome outcome;
        ExecutionEvidence ev = null;
        switch (response.outcome()) {
            case ACKNOWLEDGED -> { next = running.awaitAck(response.externalReference()); outcome = AttemptOutcome.ACKNOWLEDGED;
                ev = ExecutionEvidence.of(w.id(), running.id(), ExecutionEvidence.Kind.ADAPTER_ACKNOWLEDGEMENT, adapter.simulated(), adapter.adapterId(), response.externalReference(), running.expectedOutcome(), Map.of(), response.message(), support.actor()); }
            case COMPLETED_CONFIRMED -> { next = running.complete(response.externalReference(), adapter.simulated()); outcome = AttemptOutcome.CONFIRMED;
                ev = ExecutionEvidence.of(w.id(), running.id(), ExecutionEvidence.Kind.ADAPTER_CONFIRMATION, adapter.simulated(), adapter.adapterId(), response.externalReference(), running.expectedOutcome(), Map.of(), response.message(), support.actor()); }
            case RETRYABLE_ERROR -> { next = running.failRetryable(response.message()); outcome = AttemptOutcome.RETRYABLE_FAILURE; }
            case REJECTED, PERMANENT_ERROR, AUTHENTICATION_FAILURE -> { next = running.failPermanently(response.outcome() + ": " + response.message()); outcome = response.outcome() == ExecutionAdapter.Outcome.REJECTED ? AttemptOutcome.REJECTED : AttemptOutcome.PERMANENT_FAILURE; }
            default -> { next = running.needsReconciliation("Outcome unknown: " + response.message()); outcome = AttemptOutcome.UNKNOWN_OUTCOME; }
        }
        attempts.save(attempt.finish(outcome, response.externalReference(), response.message()));
        if (ev != null) evidence.save(ev);
        w = replace(w, next, "TASK_" + next.status(), response.message());
        if (ev != null && next.status() == TaskStatus.COMPLETED && feedback.recordVerifiedOutcome(w, next, ev)) support.timeline(w, next.id(), "TWIN_UPDATED", "Verified outcome forwarded to the monitoring pipeline");
        return w;
    }

    private ExecutionResponse callAdapter(ExecutionAdapter adapter, ExecutionAdapter.Request req) {
        Future<ExecutionResponse> f;
        try {
            f = pool.submit(() -> adapter.execute(req));
        } catch (RejectedExecutionException e) {
            return ExecutionResponse.of(ExecutionAdapter.Outcome.RETRYABLE_ERROR, null, "Execution pool saturated; request was not sent");
        }
        try {
            ExecutionResponse r = f.get(adapter.timeout().toMillis() + 1000, TimeUnit.MILLISECONDS);
            return r != null ? r : ExecutionResponse.of(ExecutionAdapter.Outcome.TIMEOUT_UNKNOWN, null, "Adapter returned no response");
        } catch (TimeoutException e) {
            f.cancel(true);
            return ExecutionResponse.of(ExecutionAdapter.Outcome.TIMEOUT_UNKNOWN, null, "Adapter timed out after " + adapter.timeout().toMillis() + " ms; outcome unknown");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ExecutionResponse.of(ExecutionAdapter.Outcome.TIMEOUT_UNKNOWN, null, "Interrupted; outcome unknown");
        } catch (ExecutionException e) {
            log.error("Execution adapter {} failed unexpectedly", adapter.adapterId(), e.getCause());
            return ExecutionResponse.of(ExecutionAdapter.Outcome.TIMEOUT_UNKNOWN, null, "Adapter error (" + e.getCause().getClass().getSimpleName() + "); outcome unknown");
        }
    }

    private ExecutionWorkflow replace(ExecutionWorkflow w, ExecutionTask t, String event, String detail) {
        ExecutionWorkflow saved = support.save(w.withTasks(swap(w.tasks(), t)));
        support.timeline(saved, t.id(), event, detail);
        return saved;
    }

    private static List<ExecutionTask> swap(List<ExecutionTask> tasks, ExecutionTask updated) {
        return tasks.stream().map(t -> t.id().equals(updated.id()) ? updated : t).toList();
    }

    private String staleReason(ExecutionWorkflow w) {
        Optional<DigitalTwinState> twin = twins.findByWorkspaceId(w.workspaceId());
        if (twin.isEmpty()) return "Digital twin not found for workspace";
        String current = twin.get().getCurrentVersion().toString();
        return current.equals(w.sourceTwinVersion()) ? null : "Stale plan: digital twin advanced from " + w.sourceTwinVersion() + " to " + current + " since approval";
    }

    /** Startup recovery: tasks left IN_PROGRESS by a crash have an unknown external outcome and become RECONCILIATION_REQUIRED. No retry. */
    public int recoverInterrupted() {
        int recovered = 0;
        for (ExecutionWorkflow wf : support.workflowsWithStuckTasks(TaskStatus.IN_PROGRESS)) {
            recovered += support.withLock(wf.id(), () -> {
                ExecutionWorkflow w = support.reload(wf.id());
                List<ExecutionTask> updated = new ArrayList<>();
                int n = 0;
                for (ExecutionTask t : w.tasks()) {
                    if (t.status() == TaskStatus.IN_PROGRESS) { updated.add(t.needsReconciliation("Interrupted by application restart; external outcome unknown")); n++; } else updated.add(t);
                }
                if (n > 0) {
                    ExecutionWorkflow saved = support.save(w.withTasks(updated).settle());
                    support.timeline(saved, null, "RECOVERED_AFTER_RESTART", n + " in-progress task(s) marked RECONCILIATION_REQUIRED");
                }
                return n;
            });
        }
        return recovered;
    }
}
