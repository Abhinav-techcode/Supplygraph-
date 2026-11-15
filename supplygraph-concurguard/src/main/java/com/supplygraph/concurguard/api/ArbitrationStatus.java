package com.supplygraph.concurguard.api;

/**
 * Deterministic arbitration outcomes from ConcurGuard.
 */
public enum ArbitrationStatus {
    ALLOW,
    DELAY,
    RESCHEDULE,
    MODIFY,
    REJECT,
    REQUEST_REPLANNING
}

