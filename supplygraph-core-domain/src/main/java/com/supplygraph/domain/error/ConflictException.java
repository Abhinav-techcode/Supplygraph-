package com.supplygraph.domain.error;

/** The data changed underneath the caller or the item already exists (optimistic locking, stale plans, duplicates). Retrying after a reload may succeed. */
public class ConflictException extends SupplyGraphException {
    private static final long serialVersionUID = 1L;
    public ConflictException(String message) { super(ErrorCode.CONFLICT, message); }
    public ConflictException(String message, Throwable cause) { super(ErrorCode.CONFLICT, message, cause); }
}
