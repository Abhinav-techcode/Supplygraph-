package com.supplygraph.domain.error;

/** A connector, an AI provider or another outside system failed or answered badly. */
public class ExternalServiceException extends SupplyGraphException {
    private static final long serialVersionUID = 1L;
    private final String system;
    private final boolean retryable;
    public ExternalServiceException(String system, String message, boolean retryable, Throwable cause) {
        super(ErrorCode.EXTERNAL_SERVICE_FAILED, message, cause);
        this.system = system;
        this.retryable = retryable;
        with("system", system);
    }
    public ExternalServiceException(String system, String message, Throwable cause) { this(system, message, true, cause); }
    public String system() { return system; }
    public boolean retryable() { return retryable; }
}
