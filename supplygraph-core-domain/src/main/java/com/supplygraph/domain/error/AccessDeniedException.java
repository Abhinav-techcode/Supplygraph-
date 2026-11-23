package com.supplygraph.domain.error;

/** The signed-in user lacks the role, permission or workspace access for the action. */
public class AccessDeniedException extends SupplyGraphException {
    private static final long serialVersionUID = 1L;
    public AccessDeniedException(String message) { super(ErrorCode.ACCESS_DENIED, message); }
    protected AccessDeniedException(ErrorCode code, String message) { super(code, message); }
}
