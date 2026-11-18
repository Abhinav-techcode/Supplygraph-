package com.supplygraph.domain.model.connector;

/** Lifecycle: REGISTERED -> CONFIGURED -> ACTIVE <-> INACTIVE (DEACTIVATED is terminal for runtime use). */
public enum ConnectorStatus {
    REGISTERED, CONFIGURED, ACTIVE, INACTIVE;

    public boolean canTransitionTo(ConnectorStatus next) {
        return switch (this) {
            case REGISTERED -> next == CONFIGURED;
            case CONFIGURED -> next == ACTIVE || next == CONFIGURED || next == INACTIVE;
            case ACTIVE -> next == INACTIVE || next == CONFIGURED;
            case INACTIVE -> next == ACTIVE || next == CONFIGURED;
        };
    }
}
