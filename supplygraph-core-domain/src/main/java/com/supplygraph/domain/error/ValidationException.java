package com.supplygraph.domain.error;

/** Input from a user or another system is malformed or incomplete. */
public class ValidationException extends SupplyGraphException {
    private static final long serialVersionUID = 1L;
    public ValidationException(String message) { super(ErrorCode.VALIDATION_FAILED, message); }
    public ValidationException(String field, String message) { super(ErrorCode.VALIDATION_FAILED, message); with("field", field); }
}
