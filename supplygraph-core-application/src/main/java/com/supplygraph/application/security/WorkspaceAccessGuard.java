package com.supplygraph.application.security;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.security.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Server-side security guard enforcing granular permissions and workspace isolation (IDOR prevention).
 */
@Service
public class WorkspaceAccessGuard {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceAccessGuard.class);

    /**
     * Enforces that the current authenticated user possesses the required permission.
     * Throws UnauthorizedOperationException if permission is lacking or no user is logged in.
     */
    public void enforcePermission(Permission requiredPermission) {
        UserProfile user = SecurityContext.getCurrentUser();
        if (user == null) {
            log.warn("Security check failed: No authenticated user present in SecurityContext for permission [{}]", requiredPermission);
            throw new UnauthorizedOperationException("Authentication required: No active user session");
        }
        if (!user.hasPermission(requiredPermission)) {
            log.warn("Access denied: User [{}] with role [{}] lacks required permission [{}]",
                    user.id(), user.role(), requiredPermission);
            throw new UnauthorizedOperationException(user.id(), requiredPermission);
        }
    }

    /**
     * Enforces that the current user has authorization to access or mutate the given workspace.
     * Prevents cross-workspace data access and IDOR exploits.
     */
    public void enforceWorkspaceAccess(WorkspaceId workspaceId) {
        if (workspaceId == null) {
            throw new IllegalArgumentException("Target workspace ID cannot be null");
        }
        UserProfile user = SecurityContext.getCurrentUser();
        if (user == null) {
            log.warn("Workspace isolation check failed: No authenticated user in SecurityContext for workspace [{}]", workspaceId);
            throw new WorkspaceAccessDeniedException(null, workspaceId);
        }
        if (!user.hasAccessToWorkspace(workspaceId)) {
            log.warn("IDOR violation prevented: User [{}] attempted unauthorized access to workspace [{}]",
                    user.id(), workspaceId);
            throw new WorkspaceAccessDeniedException(user.id(), workspaceId);
        }
    }

    /**
     * Combined verification: verifies both workspace ownership and specific operation permission.
     */
    public void enforceWorkspaceAndPermission(WorkspaceId workspaceId, Permission requiredPermission) {
        enforceWorkspaceAccess(workspaceId);
        enforcePermission(requiredPermission);
    }
}

