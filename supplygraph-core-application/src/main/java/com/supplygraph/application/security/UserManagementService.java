package com.supplygraph.application.security;

import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.application.port.out.UserProfileRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.security.Permission;
import com.supplygraph.domain.model.security.UserId;
import com.supplygraph.domain.model.security.UserProfile;
import com.supplygraph.domain.model.security.UserRole;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service managing user identity, role assignments, and workspace isolation boundaries.
 */
@Service
public class UserManagementService {

    private final UserProfileRepositoryPort userProfileRepository;
    private final WorkspaceAccessGuard workspaceAccessGuard;

    public UserManagementService(
            UserProfileRepositoryPort userProfileRepository,
            WorkspaceAccessGuard workspaceAccessGuard
    ) {
        this.userProfileRepository = userProfileRepository;
        this.workspaceAccessGuard = workspaceAccessGuard;
    }

    public UserProfile createUser(
            String id,
            String username,
            String email,
            String displayName,
            UserRole role,
            Set<WorkspaceId> assignedWorkspaces
    ) {
        // Enforce administrative permission to create users
        if (SecurityContext.getCurrentUser() != null) {
            workspaceAccessGuard.enforcePermission(Permission.WORKSPACE_ADMIN);
        }

        UserProfile user = UserProfile.create(id, username, email, displayName, role, assignedWorkspaces);
        return userProfileRepository.save(user);
    }

    public Optional<UserProfile> findById(UserId id) {
        return userProfileRepository.findById(id);
    }

    public Optional<UserProfile> findByUsername(String username) {
        return userProfileRepository.findByUsername(username);
    }

    public List<UserProfile> listAllUsers() {
        return userProfileRepository.findAll();
    }

    public UserProfile updateUserRole(UserId id, UserRole newRole) {
        workspaceAccessGuard.enforcePermission(Permission.WORKSPACE_ADMIN);

        UserProfile existing = userProfileRepository.findById(id)
                .orElseThrow(() -> new ValidationException("User not found: " + id));

        UserProfile updated = existing.withRole(newRole);
        return userProfileRepository.save(updated);
    }

    public UserProfile toggleUserStatus(UserId id, boolean active) {
        workspaceAccessGuard.enforcePermission(Permission.WORKSPACE_ADMIN);

        UserProfile existing = userProfileRepository.findById(id)
                .orElseThrow(() -> new ValidationException("User not found: " + id));

        UserProfile updated = existing.withStatus(active);
        return userProfileRepository.save(updated);
    }
}

