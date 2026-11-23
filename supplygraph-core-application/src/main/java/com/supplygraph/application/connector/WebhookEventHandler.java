package com.supplygraph.application.connector;

import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.application.outbox.DeliveryException;
import com.supplygraph.application.outbox.InboxService;
import com.supplygraph.application.outbox.OutboxHandler;
import com.supplygraph.application.port.out.ConnectorRepositoryPort;
import com.supplygraph.domain.model.connector.Connector;
import com.supplygraph.domain.model.connector.ConnectorId;
import com.supplygraph.domain.model.connector.ConnectorStatus;
import com.supplygraph.domain.model.outbox.OutboxEvent;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Delivers accepted webhook payloads into reconciliation. Idempotent: a redelivered outbox event has no second effect. */
@Component
public class WebhookEventHandler implements OutboxHandler {
    private final ConnectorRepositoryPort connectors;
    private final RecordPayloadInspector inspector;
    private final SyncReconciliationService sync;
    private final InboxService inbox;

    public WebhookEventHandler(ConnectorRepositoryPort connectors, RecordPayloadInspector inspector, SyncReconciliationService sync, InboxService inbox) {
        this.connectors = connectors;
        this.inspector = inspector;
        this.sync = sync;
        this.inbox = inbox;
    }

    @Override public String eventType() { return WebhookIngestionService.EVENT_TYPE; }

    @Override
    public void handle(OutboxEvent event) {
        Connector c = connectors.findById(ConnectorId.of(event.aggregateId()))
                .orElseThrow(() -> new DeliveryException("Connector no longer exists", false));
        if (c.status() != ConnectorStatus.ACTIVE) throw new DeliveryException("Connector is " + c.status() + "; webhook payload not applied", false);
        List<ExternalRecord> records;
        try {
            records = inspector.parse(event.payload().getBytes(StandardCharsets.UTF_8));
        } catch (ValidationException e) {
            throw new DeliveryException("Payload no longer parses: " + e.getMessage(), false);
        }
        inbox.processOnce(event.workspaceId(), "webhook-handler", event.idempotencyKey(), () -> sync.apply(c, records, event.correlationId()));
    }
}
