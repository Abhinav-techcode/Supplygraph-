package com.supplygraph.domain.model;

import com.supplygraph.domain.model.security.*;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityAndRBACTest {

    @Test
    void systemAdmin_hasAllPermissionsAndGlobalWorkspaceAccess() {
        UserProfile admin = UserProfile.create(
                "USR-ADMIN-1",
                "admin",
                "admin@supplygraph.io",
                "System Administrator",
                UserRole.SYSTEM_ADMIN,
                Set.of(WorkspaceId.of("WS-1"))
        );

        for (Permission permission : Permission.values()) {
            assertThat(admin.hasPermission(permission))
                    .as("Admin should hold permission: %s", permission)
                    .isTrue();
        }

        // Global workspace access regardless of assigned set
        assertThat(admin.hasAccessToWorkspace(WorkspaceId.of("WS-UNASSIGNED"))).isTrue();
    }

    @Test
    void procurementManager_hasScopedPermissionsAndStrictWorkspaceEnforcement() {
        WorkspaceId ws = WorkspaceId.of("WS-EV-01");
        UserProfile pm = UserProfile.create(
                "USR-PM-1",
                "buyer",
                "buyer@supplygraph.io",
                "Senior Buyer",
                UserRole.PROCUREMENT_MANAGER,
                Set.of(ws)
        );

        assertThat(pm.hasPermission(Permission.DISRUPTION_CREATE)).isTrue();
        assertThat(pm.hasPermission(Permission.AGENT_INVESTIGATE)).isTrue();
        assertThat(pm.hasPermission(Permission.PLAN_REVIEW)).isTrue();

        // Cannot approve recovery plans or administer workspaces
        assertThat(pm.hasPermission(Permission.PLAN_APPROVE)).isFalse();
        assertThat(pm.hasPermission(Permission.WORKSPACE_ADMIN)).isFalse();

        // Strict workspace isolation
        assertThat(pm.hasAccessToWorkspace(ws)).isTrue();
        assertThat(pm.hasAccessToWorkspace(WorkspaceId.of("WS-OTHER"))).isFalse();
    }

    @Test
    void viewer_hasZeroMutatingPermissions() {
        UserProfile viewer = UserProfile.create(
                "USR-VIEW-1",
                "viewer",
                "viewer@supplygraph.io",
                "Executive Viewer",
                UserRole.VIEWER,
                Set.of(WorkspaceId.of("WS-1"))
        );

        for (Permission permission : Permission.values()) {
            assertThat(viewer.hasPermission(permission))
                    .as("Viewer must have no mutating permissions: %s", permission)
                    .isFalse();
        }
    }

    @Test
    void deactivatedUser_losesAllPermissions() {
        UserProfile active = UserProfile.create(
                "USR-SCM-1",
                "scm",
                "scm@supplygraph.io",
                "SC Manager",
                UserRole.SUPPLY_CHAIN_MANAGER,
                Set.of(WorkspaceId.of("WS-1"))
        );
        assertThat(active.hasPermission(Permission.PLAN_APPROVE)).isTrue();

        UserProfile deactivated = active.withStatus(false);
        assertThat(deactivated.hasPermission(Permission.PLAN_APPROVE)).isFalse();
        assertThat(deactivated.hasAccessToWorkspace(WorkspaceId.of("WS-1"))).isFalse();
    }

    @Test
    void exceptions_carryActionableMetadata() {
        UnauthorizedOperationException authEx = new UnauthorizedOperationException(
                UserId.of("USR-1"),
                Permission.PLAN_APPROVE
        );
        assertThat(authEx.getUserId()).isEqualTo(UserId.of("USR-1"));
        assertThat(authEx.getRequiredPermission()).isEqualTo(Permission.PLAN_APPROVE);
        assertThat(authEx.getMessage()).contains("PLAN_APPROVE");

        WorkspaceAccessDeniedException idorEx = new WorkspaceAccessDeniedException(
                UserId.of("USR-1"),
                WorkspaceId.of("WS-RESTRICTED")
        );
        assertThat(idorEx.getUserId()).isEqualTo(UserId.of("USR-1"));
        assertThat(idorEx.getTargetWorkspaceId()).isEqualTo(WorkspaceId.of("WS-RESTRICTED"));
        assertThat(idorEx.getMessage()).contains("WS-RESTRICTED");
    }
}

