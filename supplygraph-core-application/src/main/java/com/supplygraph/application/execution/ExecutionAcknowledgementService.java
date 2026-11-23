package com.supplygraph.application.execution;

import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.domain.error.ResourceNotFoundException;
import com.supplygraph.application.port.out.ExecutionAttemptRepositoryPort;
import com.supplygraph.application.port.out.ExecutionEvidenceRepositoryPort;
import com.supplygraph.domain.model.execution.*;
import com.supplygraph.domain.model.security.Permission;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/** Handles asynchronous acknowledgements: idempotent (duplicates ignored), tolerant of delay (late acks recorded without illegal transitions). */
@Service
public class ExecutionAcknowledgementService {
    public enum Result { APPLIED, DUPLICATE, RECORDED_NO_TRANSITION }

    private final ExecutionSupport support;
    private final ExecutionOrchestrationService orchestration;
    private final ExecutionEvidenceRepositoryPort evidence;
    private final ExecutionAttemptRepositoryPort attempts;
    private final TwinFeedback feedback;

    public ExecutionAcknowledgementService(ExecutionSupport support, ExecutionOrchestrationService orchestration, ExecutionEvidenceRepositoryPort evidence,
                                           ExecutionAttemptRepositoryPort attempts, TwinFeedback feedback) {
        this.support = support; this.orchestration = orchestration; this.evidence = evidence; this.attempts = attempts; this.feedback = feedback;
    }

    public Result record(String taskId, String reference, boolean success, String source, String message) {
        String workflowId = ExecutionSupport.workflowIdOf(taskId);
        support.load(workflowId, Permission.EXECUTION_RECONCILE);
        if (reference == null || reference.isBlank()) throw new ValidationException("An acknowledgement reference is required");
        return support.withLock(workflowId, () -> {
            ExecutionWorkflow w = support.reload(workflowId);
            ExecutionTask t = w.task(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
            String key = reference + ":" + (success ? "OK" : "FAIL");
            if (evidence.exists(taskId, ExecutionEvidence.Kind.EXTERNAL_ACKNOWLEDGEMENT, key)) return Result.DUPLICATE;
            List<ExecutionAttempt> history = attempts.attemptsByTask(taskId);
            boolean simulated = history.isEmpty() ? w.simulated() : history.get(history.size() - 1).simulated();
            ExecutionEvidence ev = evidence.save(ExecutionEvidence.of(workflowId, taskId, ExecutionEvidence.Kind.EXTERNAL_ACKNOWLEDGEMENT, simulated, source, key,
                    t.expectedOutcome(), Map.of(), message, support.actor()));
            if (t.status() != TaskStatus.AWAITING_ACK && t.status() != TaskStatus.RECONCILIATION_REQUIRED) {
                support.timeline(w, taskId, "LATE_ACKNOWLEDGEMENT", "Acknowledgement received while task is " + t.status() + "; recorded without state change");
                return Result.RECORDED_NO_TRANSITION;
            }
            ExecutionTask next = success ? t.complete(reference, simulated) : t.failPermanently("External system reported failure: " + message);
            ExecutionWorkflow saved = support.save(w.withTasks(w.tasks().stream().map(x -> x.id().equals(taskId) ? next : x).toList()));
            support.timeline(saved, taskId, success ? "TASK_COMPLETED" : "TASK_FAILED",
                    (simulated ? "[SIMULATED] " : "") + "Acknowledgement from " + source + " ref " + reference + (message != null ? ": " + message : ""));
            if (success && feedback.recordVerifiedOutcome(saved, next, ev)) support.timeline(saved, taskId, "TWIN_UPDATED", "Verified outcome forwarded to the monitoring pipeline");
            orchestration.advance(workflowId);
            return Result.APPLIED;
        });
    }
}
