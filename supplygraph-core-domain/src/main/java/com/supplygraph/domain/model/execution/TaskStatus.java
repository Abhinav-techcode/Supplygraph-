package com.supplygraph.domain.model.execution;

public enum TaskStatus {
    PENDING, READY, IN_PROGRESS,
    /** Request accepted by the target but completion NOT yet evidenced. */
    AWAITING_ACK,
    RETRY_PENDING,
    /** Outcome unknown (timeout/crash): never retried automatically. */
    RECONCILIATION_REQUIRED,
    COMPLETED, FAILED, BLOCKED, SKIPPED, CANCELLED, COMPENSATED;

    public boolean canTransitionTo(TaskStatus n) {
        return switch (this) {
            case PENDING -> n == READY || n == BLOCKED || n == SKIPPED || n == CANCELLED;
            case READY -> n == IN_PROGRESS || n == BLOCKED || n == CANCELLED;
            case IN_PROGRESS -> n == AWAITING_ACK || n == COMPLETED || n == RETRY_PENDING || n == FAILED || n == RECONCILIATION_REQUIRED;
            case AWAITING_ACK -> n == COMPLETED || n == FAILED || n == RECONCILIATION_REQUIRED;
            case RETRY_PENDING -> n == IN_PROGRESS || n == FAILED || n == CANCELLED;
            case RECONCILIATION_REQUIRED -> n == COMPLETED || n == READY || n == FAILED;
            case BLOCKED -> n == READY || n == CANCELLED;
            case COMPLETED -> n == COMPENSATED;
            case FAILED -> n == CANCELLED;
            case SKIPPED, CANCELLED, COMPENSATED -> false;
        };
    }

    public boolean isTerminal() { return this == COMPLETED || this == FAILED || this == SKIPPED || this == CANCELLED || this == COMPENSATED; }
    /** Counts as a satisfied predecessor. */
    public boolean isSuccess() { return this == COMPLETED; }
    /** Downstream tasks can never run. */
    public boolean isDeadEnd() { return this == FAILED || this == BLOCKED || this == CANCELLED || this == SKIPPED; }
}
