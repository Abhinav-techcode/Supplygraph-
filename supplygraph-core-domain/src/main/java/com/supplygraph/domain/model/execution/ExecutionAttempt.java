package com.supplygraph.domain.model.execution;

import java.time.Instant;
import java.util.UUID;

/** One call to an execution adapter. Persisted as STARTED before the call so a crash leaves an auditable unknown outcome. */
public record ExecutionAttempt(String id, String taskId, String workflowId, int attemptNo, String adapterId, boolean simulated, String idempotencyKey,
                               AttemptOutcome outcome, String externalReference, String message, Instant startedAt, Instant finishedAt, String requestedBy) {
    public static ExecutionAttempt start(String workflowId, String taskId, int attemptNo, String adapterId, boolean simulated, String idempotencyKey, String requestedBy) {
        return new ExecutionAttempt(UUID.randomUUID().toString(), taskId, workflowId, attemptNo, adapterId, simulated, idempotencyKey, AttemptOutcome.STARTED, null, null, Instant.now(), null, requestedBy);
    }
    public ExecutionAttempt finish(AttemptOutcome o, String reference, String message) {
        if (outcome != AttemptOutcome.STARTED) throw new IllegalStateException("Attempt already finished");
        return new ExecutionAttempt(id, taskId, workflowId, attemptNo, adapterId, simulated, idempotencyKey, o, reference, message, startedAt, Instant.now(), requestedBy);
    }
}
