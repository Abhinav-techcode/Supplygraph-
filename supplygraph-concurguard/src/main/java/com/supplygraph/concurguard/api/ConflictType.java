package com.supplygraph.concurguard.api;

/**
 * The 7 canonical conflict classes detected by ConcurGuard.
 */
public enum ConflictType {
    QUANTITY_CONFLICT,
    CAPACITY_CONFLICT,
    TEMPORAL_OVERLAP,
    SEMANTIC_ALIAS,
    DEPENDENCY_CONFLICT,
    STALE_STATE_CONFLICT,
    INDUSTRY_POLICY_VIOLATION
}

