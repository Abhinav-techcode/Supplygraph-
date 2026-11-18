package com.supplygraph.application.outbox;

import com.supplygraph.domain.model.outbox.OutboxEvent;

/** Delivers one event type. Implementations must be idempotent or use {@link InboxService#processOnce}. */
public interface OutboxHandler {
    String eventType();
    void handle(OutboxEvent event) throws DeliveryException;
}
