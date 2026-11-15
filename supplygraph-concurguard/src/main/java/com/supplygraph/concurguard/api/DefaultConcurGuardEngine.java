package com.supplygraph.concurguard.api;

import com.supplygraph.concurguard.arbitration.ConcurGuardArbitrator;
import com.supplygraph.concurguard.conflict.ConcurGuardConflictDetector;
import com.supplygraph.concurguard.model.ResourceAliasRegistry;
import com.supplygraph.concurguard.replanning.SelectiveReplanningCoordinator;

public class DefaultConcurGuardEngine implements ConcurGuardEngine {

    private final ResourceAliasRegistry aliasRegistry;
    private final ConcurGuardConflictDetector conflictDetector;
    private final ConcurGuardArbitrator arbitrator;
    private final SelectiveReplanningCoordinator replanningCoordinator;

    public DefaultConcurGuardEngine() {
        this.aliasRegistry = new ResourceAliasRegistry();
        this.conflictDetector = new ConcurGuardConflictDetector(aliasRegistry);
        this.arbitrator = new ConcurGuardArbitrator();
        this.replanningCoordinator = new SelectiveReplanningCoordinator(conflictDetector, arbitrator);
    }

    public DefaultConcurGuardEngine(ResourceAliasRegistry aliasRegistry) {
        this.aliasRegistry = aliasRegistry;
        this.conflictDetector = new ConcurGuardConflictDetector(aliasRegistry);
        this.arbitrator = new ConcurGuardArbitrator();
        this.replanningCoordinator = new SelectiveReplanningCoordinator(conflictDetector, arbitrator);
    }

    @Override
    public String getVersion() {
        return "1.0.0-CONCURGUARD";
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    @Override
    public ConcurGuardConflictDetector getConflictDetector() {
        return conflictDetector;
    }

    @Override
    public ConcurGuardArbitrator getArbitrator() {
        return arbitrator;
    }

    @Override
    public SelectiveReplanningCoordinator getReplanningCoordinator() {
        return replanningCoordinator;
    }

    @Override
    public ResourceAliasRegistry getAliasRegistry() {
        return aliasRegistry;
    }
}
