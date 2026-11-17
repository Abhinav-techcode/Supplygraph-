package com.supplygraph.application.security;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.security.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkspaceAccessGuardTest {

    private WorkspaceAccessGuard guard;

    @BeforeEach
    void setUp() {
        guard = new WorkspaceAccessGuard();
        SecurityContext.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContext.clear();
    }

    @Test
    void enforcePermission_whenNoUserLoggedIn_throwsUnauthorizedOperationException() {
        assertThatThrownBy(() -> guard.enforcePermission(Permission.PLAN_APPROVE))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("No active user session");
    }

    @Test
    void enforcePermission_whenUserLacksPermission_throwsUnauthorizedOperationException() {
        UserProfile viewer = UserProfile.create(
                "USR-VIEW-1",
                "viewer",
                "viewer@supplygraph.io",
                "Viewer",
                UserRole.VIEWER,
                Set.of(WorkspaceId.of("WS-1"))
        );
        SecurityContext.setCurrentUser(viewer);

        assertThatThrownBy(() -> guard.enforcePermission(Permission.PLAN_APPROVE))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("PLAN_APPROVE");
    }

    @Test
    void enforcePermission_whenUserHoldsPermission_succeeds() {
        UserProfile scm = UserProfile.create(
                "USR-SCM-1",
                "manager",
                "manager@supplygraph.io",
                "SC Manager",
                UserRole.SUPPLY_CHAIN_MANAGER,
                Set.of(WorkspaceId.of("WS-1"))
        );
        SecurityContext.setCurrentUser(scm);

        assertThatCode(() -> guard.enforcePermission(Permission.PLAN_APPROVE))
                .doesNotThrowAnyException();
    }

    @Test
    void enforceWorkspaceAccess_whenUserAccessesUnassignedWorkspace_throwsWorkspaceAccessDeniedException() {
        UserProfile buyer = UserProfile.create(
                "USR-BUYER-1",
                "buyer",
                "buyer@supplygraph.io",
                "Buyer",
                UserRole.PROCUREMENT_MANAGER,
                Set.of(WorkspaceId.of("WS-EV-01"))
        );
        SecurityContext.setCurrentUser(buyer);

        // Accessing assigned workspace succeeds
        assertThatCode(() -> guard.enforceWorkspaceAccess(WorkspaceId.of("WS-EV-01")))
                .doesNotThrowAnyException();

        // IDOR attempt: accessing unassigned workspace is blocked
        assertThatThrownBy(() -> guard.enforceWorkspaceAccess(WorkspaceId.of("WS-CONFIDENTIAL-02")))
                .isInstanceOf(WorkspaceAccessDeniedException.class)
                .hasMessageContaining("WS-CONFIDENTIAL-02");
    }

    @Test
    void systemAdmin_bypassesWorkspaceBoundaries() {
        UserProfile admin = UserProfile.create(
                "USR-ADMIN-1",
                "admin",
                "admin@supplygraph.io",
                "Sys Admin",
                UserRole.SYSTEM_ADMIN,
                Set.of(WorkspaceId.of("WS-1"))
        );
        SecurityContext.setCurrentUser(admin);

        assertThatCode(() -> guard.enforceWorkspaceAccess(WorkspaceId.of("WS-ANY-OTHER-WORKSPACE")))
                .doesNotThrowAnyException();
    }
}

