package com.supplygraph.domain.model.security;

import com.supplygraph.domain.error.AccessDeniedException;
/**
 * Thrown when an actor attempts an operation without holding the requisite permission.
 */
public class UnauthorizedOperationException extends AccessDeniedException {

    private static final long serialVersionUID = 1L;

    private final UserId userId;
    private final Permission requiredPermission;

    public UnauthorizedOperationException(UserId userId, Permission requiredPermission) {
        super(String.format("User [%s] is unauthorized for operation requiring permission [%s]",
                userId != null ? userId.value() : "ANONYMOUS",
                requiredPermission));
        this.userId = userId;
        this.requiredPermission = requiredPermission;
    }

    public UnauthorizedOperationException(String message) {
        super(message);
        this.userId = null;
        this.requiredPermission = null;
    }

    public UserId getUserId() {
        return userId;
    }

    public Permission getRequiredPermission() {
        return requiredPermission;
    }
}
