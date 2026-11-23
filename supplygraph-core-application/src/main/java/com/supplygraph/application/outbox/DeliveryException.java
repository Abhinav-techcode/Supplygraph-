package com.supplygraph.application.outbox;

import com.supplygraph.domain.error.ExternalServiceException;

/** A delivery attempt to a connector failed; {@link #retryable()} tells the outbox whether another attempt makes sense. */
public class DeliveryException extends ExternalServiceException {
    private static final long serialVersionUID = 1L;
    public DeliveryException(String message, boolean retryable, Throwable cause) { super("connector", message, retryable, cause); }
    public DeliveryException(String message, boolean retryable) { this(message, retryable, null); }
}
