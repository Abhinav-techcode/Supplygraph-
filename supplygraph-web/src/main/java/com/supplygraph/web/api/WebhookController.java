package com.supplygraph.web.api;

import com.supplygraph.application.connector.WebhookIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Inbound connector webhooks. Authentication is the HMAC signature (no session). Responses are deliberately coarse:
 * 202 accepted (also for idempotent replays), 401 for anything rejected, 500 with no detail for internal failures.
 */
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {
    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    private final WebhookIngestionService ingestion;

    public WebhookController(WebhookIngestionService ingestion) { this.ingestion = ingestion; }

    @PostMapping(path = "/{connectorId}", consumes = "application/json")
    public ResponseEntity<Void> receive(@PathVariable String connectorId,
                                        @RequestHeader(name = "X-SG-Timestamp", required = false) String timestamp,
                                        @RequestHeader(name = "X-SG-Signature", required = false) String signature,
                                        @RequestHeader(name = "X-SG-Delivery", required = false) String delivery,
                                        @RequestBody byte[] body) {
        try {
            return switch (ingestion.receive(connectorId, timestamp, signature, delivery, body)) {
                case ACCEPTED, DUPLICATE -> ResponseEntity.accepted().build();
                case REJECTED -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            };
        } catch (RuntimeException e) {
            log.error("Webhook processing failed for connector {}", connectorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
