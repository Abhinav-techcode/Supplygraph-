package com.supplygraph.domain.model.execution;

import java.time.Instant;
import java.util.*;

/** One actionable step. Immutable: transitions return new instances and are validated against {@link TaskStatus}. */
public record ExecutionTask(
        String id, String workflowId, int sequence, TaskType type, String description, Map<String, String> parameters,
        TaskStatus status, List<String> dependsOn, String conditionTaskId, TaskStatus conditionOutcome,
        int attempts, int maxAttempts, Map<String, String> expectedOutcome, String lastError, String externalReference,
        boolean completedWithSimulatedEvidence, Instant updatedAt
) {
    public ExecutionTask {
        Objects.requireNonNull(id); Objects.requireNonNull(workflowId); Objects.requireNonNull(type); Objects.requireNonNull(status);
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        dependsOn = dependsOn == null ? List.of() : List.copyOf(dependsOn);
        expectedOutcome = expectedOutcome == null ? Map.of() : Map.copyOf(expectedOutcome);
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be >= 1");
        if (conditionTaskId != null && conditionOutcome != TaskStatus.COMPLETED && conditionOutcome != TaskStatus.FAILED)
            throw new IllegalArgumentException("condition outcome must be COMPLETED or FAILED");
        if (updatedAt == null) updatedAt = Instant.now();
    }

    public static ExecutionTask create(String workflowId, int sequence, TaskType type, String description, Map<String, String> params,
                                       List<String> dependsOn, Map<String, String> expected, int maxAttempts) {
        return new ExecutionTask(workflowId + "-T" + sequence, workflowId, sequence, type, description, params, TaskStatus.PENDING, dependsOn,
                null, null, 0, maxAttempts, expected, null, null, false, Instant.now());
    }

    public ExecutionTask withCondition(String taskId, TaskStatus outcome) {
        return new ExecutionTask(id, workflowId, sequence, type, description, parameters, status, dependsOn, taskId, outcome, attempts, maxAttempts,
                expectedOutcome, lastError, externalReference, completedWithSimulatedEvidence, updatedAt);
    }

    public boolean sensitive() { return type.sensitive(); }

    public ExecutionTask markReady() { return to(TaskStatus.READY, attempts, lastError, externalReference, completedWithSimulatedEvidence); }
    public ExecutionTask markBlocked(String why) { return to(TaskStatus.BLOCKED, attempts, why, externalReference, false); }
    public ExecutionTask markSkipped(String why) { return to(TaskStatus.SKIPPED, attempts, why, externalReference, false); }
    public ExecutionTask markCancelled() { return to(TaskStatus.CANCELLED, attempts, lastError, externalReference, false); }
    public ExecutionTask startAttempt() { return to(TaskStatus.IN_PROGRESS, attempts + 1, lastError, externalReference, false); }
    public ExecutionTask awaitAck(String reference) { return to(TaskStatus.AWAITING_ACK, attempts, null, reference, false); }
    public ExecutionTask complete(String reference, boolean simulatedEvidence) { return to(TaskStatus.COMPLETED, attempts, null, reference != null ? reference : externalReference, simulatedEvidence); }
    public ExecutionTask needsReconciliation(String why) { return to(TaskStatus.RECONCILIATION_REQUIRED, attempts, why, externalReference, false); }
    public ExecutionTask compensated() { return to(TaskStatus.COMPENSATED, attempts, lastError, externalReference, completedWithSimulatedEvidence); }

    /** Retryable failure: schedules a retry while attempts remain, otherwise fails permanently. */
    public ExecutionTask failRetryable(String error) {
        return attempts < maxAttempts ? to(TaskStatus.RETRY_PENDING, attempts, error, externalReference, false) : to(TaskStatus.FAILED, attempts, error, externalReference, false);
    }
    public ExecutionTask failPermanently(String error) { return to(TaskStatus.FAILED, attempts, error, externalReference, false); }

    /** After a human confirms the external action did NOT happen, the task may be attempted again (does not consume extra budget beyond maxAttempts). */
    public ExecutionTask reopenAfterReconciliation() {
        if (attempts >= maxAttempts) throw new IllegalStateException("Attempt budget exhausted for task " + id);
        return to(TaskStatus.READY, attempts, null, externalReference, false);
    }

    private ExecutionTask to(TaskStatus next, int newAttempts, String err, String ref, boolean sim) {
        if (!status.canTransitionTo(next)) throw new IllegalStateException("Invalid task transition " + status + " -> " + next + " for " + id);
        return new ExecutionTask(id, workflowId, sequence, type, description, parameters, next, dependsOn, conditionTaskId, conditionOutcome,
                newAttempts, maxAttempts, expectedOutcome, err, ref, sim, Instant.now());
    }
}
