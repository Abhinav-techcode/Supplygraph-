package com.supplygraph.domain.model.replanning;

/**
 * Lifecycle states of an adaptive agentic replanning workflow.
 */
public enum ReplanningStatus {
    DETECTED,
    ASSESSING,
    REPLANNING,
    VALIDATING,
    SIMULATING,
    READY_FOR_REVIEW,
    NO_ACTION_REQUIRED,
    FAILED,
    CANCELLED,
    SUPERSEDED
}

