package com.supplygraph.domain.model.security;

import com.supplygraph.domain.error.AccessDeniedException;
import com.supplygraph.domain.model.WorkspaceId;

/**
 * Thrown when an actor attempts cross-workspace access or insecure direct object reference (IDOR).
 */
public class WorkspaceAccessDeniedException extends AccessDeniedException {

    private static final long serialVersionUID = 1L;

    private final UserId userId;
    private final WorkspaceId targetWorkspaceId;

    public WorkspaceAccessDeniedException(UserId userId, WorkspaceId targetWorkspaceId) {
        super(String.format("Access denied: User [%s] is not authorized to access or mutate workspace [%s]",
                userId != null ? userId.value() : "ANONYMOUS",
                targetWorkspaceId != null ? targetWorkspaceId.value() : "UNKNOWN"));
        this.userId = userId;
        this.targetWorkspaceId = targetWorkspaceId;
    }

    public UserId getUserId() {
        return userId;
    }

    public WorkspaceId getTargetWorkspaceId() {
        return targetWorkspaceId;
    }
}
