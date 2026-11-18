package com.supplygraph.application.execution;

import com.supplygraph.application.port.out.ExecutionEvidenceRepositoryPort;
import com.supplygraph.domain.model.audit.AuditAction;
import com.supplygraph.domain.model.execution.*;
import com.supplygraph.domain.model.security.Permission;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.NoSuchElementException;

/** Compensation exists only where a valid business compensation does; irreversible tasks are refused with an explanation, never faked. */
@Service
public class ExecutionCompensationService {
    private final ExecutionSupport support;
    private final AdapterSelector adapters;
    private final ExecutionEvidenceRepositoryPort evidence;

    public ExecutionCompensationService(ExecutionSupport support, AdapterSelector adapters, ExecutionEvidenceRepositoryPort evidence) {
        this.support = support; this.adapters = adapters; this.evidence = evidence;
    }

    public ExecutionWorkflow compensate(String taskId, String rationale) {
        String workflowId = ExecutionSupport.workflowIdOf(taskId);
        support.load(workflowId, Permission.EXECUTION_COMPENSATE);
        if (rationale == null || rationale.isBlank()) throw new IllegalArgumentException("A compensation rationale is required");
        return support.withLock(workflowId, () -> {
            ExecutionWorkflow w = support.reload(workflowId);
            ExecutionTask t = w.task(taskId).orElseThrow(() -> new NoSuchElementException("Task not found: " + taskId));
            if (t.status() != TaskStatus.COMPLETED) throw new IllegalStateException("Only COMPLETED tasks can be compensated (task is " + t.status() + ")");
            if (!t.type().compensable()) throw new IllegalStateException("No valid compensation exists for " + t.type() + ": " + t.type().compensation());
            if (w.status() == WorkflowStatus.COMPLETED) throw new IllegalStateException("Workflow completed successfully; nothing to compensate");
            ExecutionPolicy policy = support.policy(w.workspaceId());
            ExecutionAdapter adapter = adapters.select(t.type(), policy).orElseThrow(() -> new IllegalStateException("No adapter available for " + t.type()));
            ExecutionAdapter.ExecutionResponse r = adapter.compensate(new ExecutionAdapter.Request(w.workspaceId(), w.id(), t.id(), t.type(), t.parameters(),
                    "compensate:" + t.id(), w.correlationId(), support.actor()));
            if (r.outcome() != ExecutionAdapter.Outcome.ACKNOWLEDGED && r.outcome() != ExecutionAdapter.Outcome.COMPLETED_CONFIRMED)
                throw new IllegalStateException("Compensation not accepted by " + adapter.adapterId() + ": " + r.message());
            evidence.save(ExecutionEvidence.of(workflowId, taskId, ExecutionEvidence.Kind.ADAPTER_ACKNOWLEDGEMENT, adapter.simulated(), adapter.adapterId(), "compensate:" + r.externalReference(),
                    Map.of(), Map.of(), "Compensation: " + t.type().compensation(), support.actor()));
            ExecutionTask next = t.compensated();
            ExecutionWorkflow saved = support.save(w.withTasks(w.tasks().stream().map(x -> x.id().equals(taskId) ? next : x).toList()));
            support.timeline(saved, taskId, "TASK_COMPENSATED", (adapter.simulated() ? "[SIMULATED] " : "") + t.type().compensation() + " — " + rationale);
            support.audit(saved, AuditAction.TASK_COMPENSATED, taskId, TaskStatus.COMPLETED.name(), TaskStatus.COMPENSATED.name(), rationale);
            return saved;
        });
    }

    /** Operator accepts the partial result instead of compensating. */
    public ExecutionWorkflow acceptPartial(String workflowId, String rationale) {
        support.load(workflowId, Permission.EXECUTION_COMPENSATE);
        if (rationale == null || rationale.isBlank()) throw new IllegalArgumentException("A rationale is required");
        return support.withLock(workflowId, () -> {
            ExecutionWorkflow saved = support.save(support.reload(workflowId).acceptPartial());
            support.timeline(saved, null, "PARTIAL_ACCEPTED", rationale);
            return saved;
        });
    }
}
