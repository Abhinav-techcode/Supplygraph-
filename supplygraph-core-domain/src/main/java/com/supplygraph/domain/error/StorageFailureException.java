package com.supplygraph.domain.error;

/** Stored data could not be written or read back (for example a JSON column that no longer parses). Never replaced by an empty value. */
public class StorageFailureException extends SupplyGraphException {
    private static final long serialVersionUID = 1L;
    public StorageFailureException(String message, Throwable cause) { super(ErrorCode.STORAGE_FAILURE, message, cause); }
}
