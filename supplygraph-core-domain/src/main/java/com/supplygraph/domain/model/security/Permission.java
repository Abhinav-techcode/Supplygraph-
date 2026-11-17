package com.supplygraph.domain.model.security;

/**
 * Granular permissions governing operations across SupplyGraph.
 */
public enum Permission {
    WORKSPACE_ADMIN("Administer workspace settings, users, and industry configurations"),
    TWIN_MANAGE("Create, update, and advance digital twin entities and graphs"),
    DISRUPTION_CREATE("Register, simulate, and manage supply chain disruptions"),
    IMPACT_ASSESS("Initiate and evaluate cascading multi-tier impact assessments"),
    AGENT_INVESTIGATE("Trigger bounded AI agent investigations and proposals"),
    OPTIMIZATION_EXECUTE("Run discrete-event simulations and OR-Tools optimization solvers"),
    PLAN_REVIEW("Examine candidate recovery alternatives and ConcurGuard arbitration results"),
    PLAN_APPROVE("Perform binding human sign-off on recovery plans (Requires human authorization)"),
    CONFIG_ADMIN("Modify operational monitoring rules, thresholds, and industry policy limits"),
    AUDIT_VIEW("Inspect immutable audit logs, tamper-evident signatures, and provenance traces");

    private final String description;

    Permission(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

