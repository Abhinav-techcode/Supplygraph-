package com.supplygraph.concurguard.api;

import com.supplygraph.concurguard.arbitration.ConcurGuardArbitrator;
import com.supplygraph.concurguard.conflict.ConcurGuardConflictDetector;
import com.supplygraph.concurguard.model.ResourceAliasRegistry;
import com.supplygraph.concurguard.replanning.SelectiveReplanningCoordinator;

/**
 * Public contract for the ConcurGuard conflict coordination engine.
 */
public interface ConcurGuardEngine {
    String getVersion();
    boolean isHealthy();
    ConcurGuardConflictDetector getConflictDetector();
    ConcurGuardArbitrator getArbitrator();
    SelectiveReplanningCoordinator getReplanningCoordinator();
    ResourceAliasRegistry getAliasRegistry();
}
