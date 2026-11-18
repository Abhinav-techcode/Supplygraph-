package com.supplygraph.application.execution;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.execution.TaskType;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Execution adapter contract. Implementations declare what they support, validate preconditions, honour the idempotency key,
 * bound their own latency (see {@link #timeout()}), and MUST NOT throw for remote problems: they return an {@link ExecutionResponse}.
 * An ACKNOWLEDGED response means "request accepted", never "work done".
 */
public interface ExecutionAdapter {
    record Request(WorkspaceId workspaceId, String workflowId, String taskId, TaskType type, Map<String, String> parameters,
                   String idempotencyKey, String correlationId, String requestedBy) {}

    enum Outcome { ACKNOWLEDGED, COMPLETED_CONFIRMED, REJECTED, RETRYABLE_ERROR, PERMANENT_ERROR, AUTHENTICATION_FAILURE, TIMEOUT_UNKNOWN }

    record ExecutionResponse(Outcome outcome, String externalReference, String message) {
        public static ExecutionResponse of(Outcome o, String ref, String msg) { return new ExecutionResponse(o, ref, msg); }
    }

    String adapterId();
    /** True for sandbox adapters: every acknowledgement they produce is simulated, never real-world confirmation. */
    boolean simulated();
    Set<TaskType> supportedTypes();
    Duration timeout();
    /** @return human-readable violations; empty when the request may be sent. */
    List<String> checkPreconditions(Request request);
    ExecutionResponse execute(Request request);
    /** Compensating action; adapters without one return REJECTED with an explanation. */
    default ExecutionResponse compensate(Request request) {
        return ExecutionResponse.of(Outcome.REJECTED, null, "Adapter " + adapterId() + " has no compensation for " + request.type());
    }
}
