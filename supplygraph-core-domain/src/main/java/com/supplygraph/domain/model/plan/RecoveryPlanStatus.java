package com.supplygraph.domain.model.plan;

/**
 * Lifecycle states and transition rules for recovery plans.
 * Progression: DRAFT -> SIMULATED -> VALIDATED -> PENDING_APPROVAL -> APPROVED (or REJECTED / SUPERSEDED).
 */
public enum RecoveryPlanStatus {
    DRAFT,
    SIMULATED,
    VALIDATED,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    SUPERSEDED;

    public boolean canTransitionTo(RecoveryPlanStatus next) {
        if (next == null) {
            return false;
        }
        return switch (this) {
            case DRAFT -> next == SIMULATED || next == VALIDATED || next == REJECTED || next == SUPERSEDED;
            case SIMULATED -> next == VALIDATED || next == PENDING_APPROVAL || next == REJECTED || next == SUPERSEDED;
            case VALIDATED -> next == SIMULATED || next == PENDING_APPROVAL || next == REJECTED || next == SUPERSEDED;
            case PENDING_APPROVAL -> next == APPROVED || next == REJECTED || next == SUPERSEDED;
            case APPROVED, REJECTED, SUPERSEDED -> false;
        };
    }

    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED || this == SUPERSEDED;
    }
}
