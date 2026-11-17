package com.supplygraph.domain.model.event;

/**
 * Lifecycle status of an ingested operational event.
 */
public enum EventStatus {
    INGESTED,
    VALIDATED,
    PROCESSED,
    REJECTED,
    DUPLICATE
}

