package com.supplygraph.application.security;

/**
 * Thrown when an actor or workspace exceeds allowed operation rates for expensive compute tasks.
 */
public class RateLimitExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String clientKey;
    private final String operationType;

    public RateLimitExceededException(String clientKey, String operationType) {
        super(String.format("Rate limit exceeded for client [%s] on operation [%s]. Please throttle requests.",
                clientKey, operationType));
        this.clientKey = clientKey;
        this.operationType = operationType;
    }

    public String getClientKey() {
        return clientKey;
    }

    public String getOperationType() {
        return operationType;
    }
}

