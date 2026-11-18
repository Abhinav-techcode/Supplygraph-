package com.supplygraph.application.outbox;

/** Thrown by an {@link OutboxHandler}; the retryable flag drives backoff versus immediate dead-lettering. */
public class DeliveryException extends RuntimeException {
    private final boolean retryable;
    public DeliveryException(String message, boolean retryable, Throwable cause) { super(message, cause); this.retryable = retryable; }
    public DeliveryException(String message, boolean retryable) { this(message, retryable, null); }
    public boolean retryable() { return retryable; }
}
