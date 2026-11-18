package com.supplygraph.domain.model.connector;

import com.supplygraph.domain.model.WorkspaceId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable record of one connector run. {@code simulated} is true for sandbox connectors: such records are never real-system evidence. */
public record ConnectorExecution(
        String id,
        ConnectorId connectorId,
        WorkspaceId workspaceId,
        ConnectorOperation operation,
        ExecutionTrigger trigger,
        ExecutionStatus status,
        Instant startedAt,
        Instant finishedAt,
        int recordsReceived,
        int recordsProcessed,
        int recordsRejected,
        ErrorCategory errorCategory,
        String errorMessage,
        int attempt,
        String retryOf,
        String correlationId,
        boolean simulated,
        String requestedBy
) {
    public ConnectorExecution {
        Objects.requireNonNull(id); Objects.requireNonNull(connectorId); Objects.requireNonNull(workspaceId);
        Objects.requireNonNull(operation); Objects.requireNonNull(trigger); Objects.requireNonNull(status); Objects.requireNonNull(startedAt);
        if (attempt < 1) throw new IllegalArgumentException("attempt must be >= 1");
        if (recordsReceived < 0 || recordsProcessed < 0 || recordsRejected < 0) throw new IllegalArgumentException("record counts must be non-negative");
    }

    public static ConnectorExecution start(Connector c, ConnectorOperation op, ExecutionTrigger trigger, String requestedBy,
                                           int attempt, String retryOf, String correlationId) {
        return new ConnectorExecution(UUID.randomUUID().toString(), c.id(), c.workspaceId(), op, trigger, ExecutionStatus.RUNNING,
                Instant.now(), null, 0, 0, 0, null, null, attempt, retryOf,
                correlationId != null ? correlationId : UUID.randomUUID().toString(), c.isSimulated(), requestedBy);
    }

    public ConnectorExecution succeed(int received, int processed, int rejected) {
        requireRunning();
        return new ConnectorExecution(id, connectorId, workspaceId, operation, trigger, ExecutionStatus.SUCCEEDED, startedAt, Instant.now(),
                received, processed, rejected, null, null, attempt, retryOf, correlationId, simulated, requestedBy);
    }

    public ConnectorExecution fail(ErrorCategory category, String message, boolean retryable) {
        requireRunning();
        return new ConnectorExecution(id, connectorId, workspaceId, operation, trigger,
                retryable ? ExecutionStatus.FAILED_RETRYABLE : ExecutionStatus.FAILED_PERMANENT, startedAt, Instant.now(),
                recordsReceived, recordsProcessed, recordsRejected, category, message, attempt, retryOf, correlationId, simulated, requestedBy);
    }

    /** A retry is allowed only for retryable failures within the connector's retry budget (attempt 1 is the original run). */
    public boolean canRetry(int maxRetries) { return status == ExecutionStatus.FAILED_RETRYABLE && attempt <= maxRetries; }

    private void requireRunning() {
        if (status != ExecutionStatus.RUNNING) throw new IllegalStateException("Execution " + id + " already finished: " + status);
    }
}
