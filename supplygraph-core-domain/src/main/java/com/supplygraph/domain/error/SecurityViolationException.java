package com.supplygraph.domain.error;

/** A guard blocked a dangerous request (SSRF, cross-workspace access by an agent, prompt manipulation). Always logged. */
public class SecurityViolationException extends SupplyGraphException {
    private static final long serialVersionUID = 1L;
    public SecurityViolationException(String message) { super(ErrorCode.SECURITY_VIOLATION, message); }
}
