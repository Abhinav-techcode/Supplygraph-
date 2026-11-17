package com.supplygraph.domain.model.security;

import com.supplygraph.domain.model.WorkspaceId;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Enterprise user profile capturing identity, assigned role, workspace memberships, and status.
 */
public record UserProfile(
        UserId id,
        String username,
        String email,
        String displayName,
        UserRole role,
        Set<WorkspaceId> assignedWorkspaces,
        boolean active,
        Instant createdAt
) implements Serializable {

    public UserProfile {
        Objects.requireNonNull(id, "User ID cannot be null");
        Objects.requireNonNull(username, "Username cannot be null");
        Objects.requireNonNull(email, "Email cannot be null");
        Objects.requireNonNull(displayName, "Display name cannot be null");
        Objects.requireNonNull(role, "User role cannot be null");
        Objects.requireNonNull(createdAt, "Creation timestamp cannot be null");
        assignedWorkspaces = assignedWorkspaces != null
                ? Collections.unmodifiableSet(new HashSet<>(assignedWorkspaces))
                : Collections.emptySet();
    }

    public static UserProfile create(
            String id,
            String username,
            String email,
            String displayName,
            UserRole role,
            Set<WorkspaceId> assignedWorkspaces
    ) {
        return new UserProfile(
                UserId.of(id),
                username,
                email,
                displayName,
                role,
                assignedWorkspaces,
                true,
                Instant.now()
        );
    }

    /**
     * Checks if the user possesses the required permission.
     */
    public boolean hasPermission(Permission permission) {
        return active && role.hasPermission(permission);
    }

    /**
     * Checks if the user is authorized to access the given workspace.
     * System admins have global workspace access.
     */
    public boolean hasAccessToWorkspace(WorkspaceId workspaceId) {
        if (!active) {
            return false;
        }
        if (role == UserRole.SYSTEM_ADMIN) {
            return true;
        }
        return assignedWorkspaces.contains(workspaceId);
    }

    public UserProfile withRole(UserRole newRole) {
        return new UserProfile(
                id,
                username,
                email,
                displayName,
                newRole,
                assignedWorkspaces,
                active,
                createdAt
        );
    }

    public UserProfile withStatus(boolean newActive) {
        return new UserProfile(
                id,
                username,
                email,
                displayName,
                role,
                assignedWorkspaces,
                newActive,
                createdAt
        );
    }
}

