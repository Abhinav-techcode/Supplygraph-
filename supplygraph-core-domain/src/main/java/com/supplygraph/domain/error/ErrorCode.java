package com.supplygraph.domain.error;

/** Stable, machine-readable error codes. The HTTP status and the retry hint are properties of the code, not of each throw site. */
public enum ErrorCode {
    VALIDATION_FAILED(400, false, "The input is not valid."),
    AUTHENTICATION_FAILED(401, false, "Sign-in is required or failed."),
    ACCESS_DENIED(403, false, "You are not allowed to do that."),
    SECURITY_VIOLATION(403, false, "The request was blocked by a security rule."),
    NOT_FOUND(404, false, "The requested item does not exist."),
    INVALID_STATE(409, false, "That is not possible in the item's current state."),
    CONFLICT(409, true, "The data changed or already exists. Reload and try again."),
    BUSINESS_RULE_VIOLATION(422, false, "A business rule does not allow that."),
    RATE_LIMITED(429, true, "Too many requests. Slow down and try again shortly."),
    STORAGE_FAILURE(500, false, "Stored data could not be read or written."),
    EXTERNAL_SERVICE_FAILED(502, true, "An external system did not respond correctly."),
    INTERNAL_ERROR(500, false, "Something unexpected went wrong.");

    private final int httpStatus;
    private final boolean retryable;
    private final String defaultMessage;

    ErrorCode(int httpStatus, boolean retryable, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.retryable = retryable;
        this.defaultMessage = defaultMessage;
    }

    public int httpStatus() { return httpStatus; }
    public boolean retryable() { return retryable; }
    public String defaultMessage() { return defaultMessage; }

    /** Whether the exception's own message is safe and useful to show to an end user (storage and internal details are not). */
    public boolean messageIsUserSafe() { return this != STORAGE_FAILURE && this != INTERNAL_ERROR && this != EXTERNAL_SERVICE_FAILED; }
}
