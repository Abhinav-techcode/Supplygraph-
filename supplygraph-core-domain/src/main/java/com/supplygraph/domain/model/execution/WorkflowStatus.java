package com.supplygraph.domain.model.execution;

public enum WorkflowStatus {
    DRAFT, VALIDATED, PENDING_APPROVAL, APPROVED, READY, EXECUTING, PARTIALLY_COMPLETED, COMPLETED,
    REJECTED, BLOCKED, FAILED, CANCELLED, COMPENSATION_REQUIRED, RECONCILIATION_REQUIRED;

    public boolean canTransitionTo(WorkflowStatus n) {
        return switch (this) {
            case DRAFT -> n == VALIDATED || n == BLOCKED || n == CANCELLED;
            case VALIDATED -> n == PENDING_APPROVAL || n == BLOCKED || n == CANCELLED;
            case PENDING_APPROVAL -> n == APPROVED || n == REJECTED || n == BLOCKED || n == CANCELLED;
            case APPROVED -> n == READY || n == BLOCKED || n == CANCELLED;
            case READY -> n == EXECUTING || n == BLOCKED || n == CANCELLED;
            case EXECUTING -> n == COMPLETED || n == PARTIALLY_COMPLETED || n == FAILED || n == RECONCILIATION_REQUIRED
                    || n == COMPENSATION_REQUIRED || n == BLOCKED || n == CANCELLED;
            case RECONCILIATION_REQUIRED -> n == EXECUTING || n == COMPLETED || n == PARTIALLY_COMPLETED || n == FAILED || n == COMPENSATION_REQUIRED || n == CANCELLED;
            case COMPENSATION_REQUIRED -> n == PARTIALLY_COMPLETED || n == CANCELLED || n == FAILED || n == EXECUTING;
            case PARTIALLY_COMPLETED -> n == EXECUTING || n == COMPENSATION_REQUIRED || n == COMPLETED || n == CANCELLED;
            case BLOCKED -> n == VALIDATED || n == CANCELLED;
            case COMPLETED, REJECTED, FAILED, CANCELLED -> false;
        };
    }

    public boolean isTerminal() { return this == COMPLETED || this == REJECTED || this == FAILED || this == CANCELLED; }
}
