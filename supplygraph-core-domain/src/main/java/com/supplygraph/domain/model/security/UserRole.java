package com.supplygraph.domain.model.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Enterprise Role-Based Access Control (RBAC) roles with mapped permissions.
 */
public enum UserRole {
    SYSTEM_ADMIN(
            "System Administrator",
            EnumSet.allOf(Permission.class)
    ),
    WORKSPACE_ADMIN(
            "Workspace Administrator",
            EnumSet.allOf(Permission.class)
    ),
    SUPPLY_CHAIN_MANAGER(
            "Supply Chain Manager",
            EnumSet.of(
                    Permission.TWIN_MANAGE,
                    Permission.DISRUPTION_CREATE,
                    Permission.IMPACT_ASSESS,
                    Permission.AGENT_INVESTIGATE,
                    Permission.OPTIMIZATION_EXECUTE,
                    Permission.PLAN_REVIEW,
                    Permission.PLAN_APPROVE,
                    Permission.CONFIG_ADMIN,
                    Permission.AUDIT_VIEW,
                    Permission.CONNECTOR_MANAGE,
                    Permission.CONNECTOR_EXECUTE,
                    Permission.EVENT_REPLAY,
                    Permission.DATA_RECONCILE,
                    Permission.EXECUTION_CREATE,
                    Permission.EXECUTION_APPROVE,
                    Permission.EXECUTION_RUN,
                    Permission.EXECUTION_RECONCILE
            )
    ),
    PROCUREMENT_MANAGER(
            "Procurement Manager",
            EnumSet.of(
                    Permission.DISRUPTION_CREATE,
                    Permission.IMPACT_ASSESS,
                    Permission.AGENT_INVESTIGATE,
                    Permission.PLAN_REVIEW,
                    Permission.AUDIT_VIEW,
                    Permission.EXECUTION_RUN
            )
    ),
    PRODUCTION_MANAGER(
            "Production Manager",
            EnumSet.of(
                    Permission.DISRUPTION_CREATE,
                    Permission.IMPACT_ASSESS,
                    Permission.AGENT_INVESTIGATE,
                    Permission.PLAN_REVIEW,
                    Permission.AUDIT_VIEW,
                    Permission.EXECUTION_RUN
            )
    ),
    LOGISTICS_MANAGER(
            "Logistics Manager",
            EnumSet.of(
                    Permission.DISRUPTION_CREATE,
                    Permission.IMPACT_ASSESS,
                    Permission.AGENT_INVESTIGATE,
                    Permission.PLAN_REVIEW,
                    Permission.AUDIT_VIEW,
                    Permission.EXECUTION_RUN
            )
    ),
    QUALITY_MANAGER(
            "Quality Assurance Manager",
            EnumSet.of(
                    Permission.DISRUPTION_CREATE,
                    Permission.IMPACT_ASSESS,
                    Permission.PLAN_REVIEW,
                    Permission.AUDIT_VIEW
            )
    ),
    ANALYST(
            "Supply Chain Analyst",
            EnumSet.of(
                    Permission.IMPACT_ASSESS,
                    Permission.AGENT_INVESTIGATE,
                    Permission.OPTIMIZATION_EXECUTE,
                    Permission.PLAN_REVIEW,
                    Permission.AUDIT_VIEW
            )
    ),
    VIEWER(
            "Read-Only Viewer",
            EnumSet.noneOf(Permission.class)
    );

    private final String displayName;
    private final Set<Permission> permissions;

    UserRole(String displayName, Set<Permission> permissions) {
        this.displayName = displayName;
        this.permissions = Collections.unmodifiableSet(permissions);
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }
}

