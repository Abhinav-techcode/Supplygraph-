package com.supplygraph.domain.model.event;

/**
 * Audit reason code when an operational event fails ingestion or validation.
 */
public enum EventRejectionReason {
    NONE,
    MALFORMED_PAYLOAD,
    MISSING_REQUIRED_FIELDS,
    UNKNOWN_ENTITY,
    DUPLICATE_IDEMPOTENCY_KEY,
    UNAUTHORIZED_SOURCE,
    STALE_EVENT_TIMESTAMP,
    WORKSPACE_MISMATCH
}

