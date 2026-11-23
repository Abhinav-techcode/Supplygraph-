package com.supplygraph.domain.error;

/** A client exceeded its allowed request rate. */
public class RateLimitExceededException extends SupplyGraphException {
    private static final long serialVersionUID = 1L;
    private final String clientKey;
    private final String operationType;
    public RateLimitExceededException(String clientKey, String operationType) {
        super(ErrorCode.RATE_LIMITED, String.format("Rate limit exceeded for client [%s] on operation [%s]. Please throttle requests.", clientKey, operationType));
        this.clientKey = clientKey;
        this.operationType = operationType;
    }
    public String getClientKey() { return clientKey; }
    public String getOperationType() { return operationType; }
}
