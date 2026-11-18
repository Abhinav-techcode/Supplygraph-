package com.supplygraph.application.execution;

import com.supplygraph.application.port.out.ExecutionAttemptRepositoryPort;
import com.supplygraph.application.port.out.ExecutionEvidenceRepositoryPort;
import com.supplygraph.domain.model.audit.AuditAction;
import com.supplygraph.domain.model.execution.*;
import com.supplygraph.domain.model.security.Permission;
import org.springframework.stereotype.Service;

import java.util.*;

/** Resolves ambiguous outcomes with human confirmation or observed state. The twin changes only from non-simulated validated evidence. */
@Service
public class ExecutionReconciliationService {
    public enum Resolution { CONFIRMED_COMPLETED, CONFIRMED_NOT_EXECUTED, CONFIRMED_FAILED }

    private final ExecutionSupport support;
    private final ExecutionOrchestrationService orchestration;
    private final ExecutionEvidenceRepositoryPort evidence;
    private final ExecutionAttemptRepositoryPort attempts;
    private final TwinFeedback feedback;

    public ExecutionReconciliationService(ExecutionSupport support, ExecutionOrchestrationService orchestration, ExecutionEvidenceRepositoryPort evidence,
                                          ExecutionAttemptRepositoryPort attempts, TwinFeedback feedback) {
        this.support = support; this.orchestration = orchestration; this.evidence = evidence; this.attempts = attempts; this.feedback = feedback;
    }

    public ExecutionWorkflow reconcile(String taskId, Resolution resolution, String rationale, Map<String, String> observed) {
        String workflowId = ExecutionSupport.workflowIdOf(taskId);
        support.load(workflowId, Permission.EXECUTION_RECONCILE);
        if (rationale == null || rationale.isBlank()) throw new IllegalArgumentException("A reconciliation rationale is required");
        return support.withLock(workflowId, () -> {
            ExecutionWorkflow w = support.reload(workflowId);
            ExecutionTask t = w.task(taskId).orElseThrow(() -> new NoSuchElementException("Task not found: " + taskId));
            if (t.status() != TaskStatus.RECONCILIATION_REQUIRED && t.status() != TaskStatus.AWAITING_ACK)
                throw new IllegalStateException("Task " + taskId + " is " + t.status() + "; only RECONCILIATION_REQUIRED or AWAITING_ACK tasks can be reconciled");
            boolean simulated = w.simulated() || attempts.attemptsByTask(taskId).stream().anyMatch(ExecutionAttempt::simulated);
            ExecutionEvidence.Kind kind = resolution == Resolution.CONFIRMED_NOT_EXECUTED ? ExecutionEvidence.Kind.MANUAL_NOT_EXECUTED : ExecutionEvidence.Kind.MANUAL_CONFIRMATION;
            ExecutionEvidence ev = evidence.save(ExecutionEvidence.of(workflowId, taskId, kind, simulated, "manual:" + support.actor(), resolution.name(),
                    t.expectedOutcome(), observed == null ? Map.of() : observed, rationale, support.actor()));
            ExecutionTask next = switch (resolution) {
                case CONFIRMED_COMPLETED -> t.status() == TaskStatus.AWAITING_ACK || t.status() == TaskStatus.RECONCILIATION_REQUIRED ? t.complete(t.externalReference(), simulated) : t;
                case CONFIRMED_NOT_EXECUTED -> {
                    if (t.status() != TaskStatus.RECONCILIATION_REQUIRED) throw new IllegalStateException("Only unknown outcomes can be reopened");
                    yield t.reopenAfterReconciliation();
                }
                case CONFIRMED_FAILED -> t.failPermanently("Confirmed failed: " + rationale);
            };
            ExecutionWorkflow saved = support.save(w.withTasks(w.tasks().stream().map(x -> x.id().equals(taskId) ? next : x).toList()));
            support.timeline(saved, taskId, "TASK_RECONCILED", resolution + (simulated ? " [SIMULATED]" : "") + ": " + rationale);
            support.audit(saved, AuditAction.TASK_RECONCILED, taskId, t.status().name(), next.status().name(), resolution + ": " + rationale);
            if (resolution == Resolution.CONFIRMED_COMPLETED && feedback.recordVerifiedOutcome(saved, next, ev)) support.timeline(saved, taskId, "TWIN_UPDATED", "Confirmed outcome forwarded to the monitoring pipeline");
            return orchestration.advance(workflowId);
        });
    }

    /** Records an independent observation of the real world. Matching the expected outcome completes the task; a mismatch demands reconciliation. */
    public ExecutionWorkflow recordObservation(String taskId, Map<String, String> observed, String source) {
        String workflowId = ExecutionSupport.workflowIdOf(taskId);
        support.load(workflowId, Permission.EXECUTION_RECONCILE);
        if (source == null || source.isBlank()) throw new IllegalArgumentException("An observation source is required");
        return support.withLock(workflowId, () -> {
            ExecutionWorkflow w = support.reload(workflowId);
            ExecutionTask t = w.task(taskId).orElseThrow(() -> new NoSuchElementException("Task not found: " + taskId));
            if (t.status() != TaskStatus.AWAITING_ACK && t.status() != TaskStatus.RECONCILIATION_REQUIRED)
                throw new IllegalStateException("Task " + taskId + " is " + t.status() + "; observations apply to AWAITING_ACK or RECONCILIATION_REQUIRED tasks");
            boolean simulated = w.simulated();
            List<String> mismatches = mismatches(t.expectedOutcome(), observed);
            ExecutionEvidence ev = evidence.save(ExecutionEvidence.of(workflowId, taskId, ExecutionEvidence.Kind.OBSERVED_STATE, simulated, source, null,
                    t.expectedOutcome(), observed, mismatches.isEmpty() ? "Observation matches expected outcome" : "Mismatch: " + String.join("; ", mismatches), support.actor()));
            ExecutionTask next = mismatches.isEmpty() ? t.complete(t.externalReference(), simulated)
                    : (t.status() == TaskStatus.AWAITING_ACK ? t.needsReconciliation("Observed state differs from expected: " + String.join("; ", mismatches)) : t);
            ExecutionWorkflow saved = support.save(w.withTasks(w.tasks().stream().map(x -> x.id().equals(taskId) ? next : x).toList()));
            support.timeline(saved, taskId, mismatches.isEmpty() ? "TASK_COMPLETED" : "OBSERVATION_MISMATCH", ev.note());
            if (mismatches.isEmpty() && feedback.recordVerifiedOutcome(saved, next, ev)) support.timeline(saved, taskId, "TWIN_UPDATED", "Observed outcome forwarded to the monitoring pipeline");
            return orchestration.advance(workflowId);
        });
    }

    static List<String> mismatches(Map<String, String> expected, Map<String, String> observed) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, String> e : expected.entrySet()) {
            String actual = observed.get(e.getKey());
            if (actual == null) { out.add(e.getKey() + " not observed"); continue; }
            if (!equalValues(e.getValue(), actual)) out.add(e.getKey() + " expected " + e.getValue() + " but observed " + actual);
        }
        return out;
    }

    private static boolean equalValues(String a, String b) {
        try { return new java.math.BigDecimal(a.trim()).compareTo(new java.math.BigDecimal(b.trim())) == 0; }
        catch (NumberFormatException e) { return a.trim().equalsIgnoreCase(b.trim()); }
    }
}
