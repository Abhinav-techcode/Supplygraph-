package com.supplygraph.application.connector;

import com.supplygraph.domain.error.ValidationException;
import com.supplygraph.application.outbox.InboxService;
import com.supplygraph.application.outbox.OutboxService;
import com.supplygraph.application.port.out.ConnectorExecutionRepositoryPort;
import com.supplygraph.application.port.out.ConnectorRepositoryPort;
import com.supplygraph.domain.model.connector.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Authenticates an inbound webhook by HMAC (the only credential an external caller has), rejects replays, validates the payload,
 * and durably enqueues it. Callers receive only a coarse {@link Outcome}; internal causes are logged, never returned.
 */
@Service
public class WebhookIngestionService {
    public static final String EVENT_TYPE = "connector.webhook.received";
    public enum Outcome { ACCEPTED, DUPLICATE, REJECTED }

    private static final Logger log = LoggerFactory.getLogger(WebhookIngestionService.class);
    private final ConnectorRepositoryPort connectors;
    private final ConnectorExecutionRepositoryPort executions;
    private final CredentialResolver credentials;
    private final RecordPayloadInspector inspector;
    private final InboxService inbox;
    private final OutboxService outbox;
    private final WebhookSignatureVerifier verifier = new WebhookSignatureVerifier();

    public WebhookIngestionService(ConnectorRepositoryPort connectors, ConnectorExecutionRepositoryPort executions, CredentialResolver credentials,
                                   RecordPayloadInspector inspector, InboxService inbox, OutboxService outbox) {
        this.connectors = connectors;
        this.executions = executions;
        this.credentials = credentials;
        this.inspector = inspector;
        this.inbox = inbox;
        this.outbox = outbox;
    }

    @Transactional
    public Outcome receive(String connectorId, String timestamp, String signature, String deliveryId, byte[] body) {
        Optional<Connector> found = connectorId == null ? Optional.empty() : connectors.findById(ConnectorId.of(connectorId));
        if (found.isEmpty()) return Outcome.REJECTED; // indistinguishable from bad signature: no connector enumeration
        Connector c = found.get();
        ConnectorConfiguration cfg = c.configuration();
        if (c.status() != ConnectorStatus.ACTIVE || cfg == null || cfg.credential() == null) return Outcome.REJECTED;
        if (body == null || body.length == 0 || body.length > cfg.maxResponseBytes()) return Outcome.REJECTED;

        byte[] secret = credentials.resolve(cfg.credential()).orElse(null);
        ConnectorExecution run = ConnectorExecution.start(c, ConnectorOperation.WEBHOOK_INGEST, ExecutionTrigger.WEBHOOK, "webhook", 1, null, null);
        if (secret == null || !verifier.verify(secret, timestamp, signature, body, Instant.now())) {
            log.warn("Webhook rejected for connector {}: signature/timestamp invalid correlation={}", c.id(), run.correlationId());
            executions.save(run.fail(ErrorCategory.SIGNATURE_INVALID, "Signature or timestamp invalid", false));
            return Outcome.REJECTED;
        }
        RecordPayloadInspector.Inspection inspection;
        try {
            inspection = inspector.inspect(body);
        } catch (ValidationException e) {
            executions.save(run.fail(ErrorCategory.INVALID_RESPONSE, "Payload malformed", false));
            return Outcome.REJECTED;
        }
        String messageId = deliveryId != null && !deliveryId.isBlank() ? deliveryId.trim() : signature.trim().toLowerCase();
        AtomicBoolean first = new AtomicBoolean();
        first.set(inbox.processOnce(c.workspaceId(), "webhook:" + c.id().value(), messageId, () ->
                outbox.enqueue(c.workspaceId(), EVENT_TYPE, c.id().value(), new String(body, java.nio.charset.StandardCharsets.UTF_8),
                        "webhook:" + c.id().value() + ":" + messageId, run.correlationId())));
        if (!first.get()) return Outcome.DUPLICATE;
        executions.save(run.succeed(inspection.received(), inspection.valid(), inspection.rejected()));
        return Outcome.ACCEPTED;
    }
}
