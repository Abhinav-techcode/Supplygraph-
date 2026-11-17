package com.supplygraph.domain.model.disruption;

import java.util.Set;

/**
 * Lifecycle states and transition validation for disruptions.
 * Valid progression: DRAFT -> VALIDATED -> ACTIVE -> RESOLVED (or CANCELLED).
 */
public enum DisruptionStatus {
    DRAFT,
    VALIDATED,
    ACTIVE,
    RESOLVED,
    CANCELLED;

    public boolean canTransitionTo(DisruptionStatus next) {
        if (next == null) {
            return false;
        }
        return switch (this) {
            case DRAFT -> next == VALIDATED || next == CANCELLED;
            case VALIDATED -> next == ACTIVE || next == CANCELLED;
            case ACTIVE -> next == RESOLVED || next == CANCELLED;
            case RESOLVED, CANCELLED -> false; // Terminal states
        };
    }

    public boolean isTerminal() {
        return this == RESOLVED || this == CANCELLED;
    }
}

