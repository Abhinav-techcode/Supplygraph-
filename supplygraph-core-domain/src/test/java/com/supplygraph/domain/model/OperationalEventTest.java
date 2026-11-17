package com.supplygraph.domain.model;

import com.supplygraph.domain.model.event.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Operational Event Domain Tests")
class OperationalEventTest {

    @Test
    @DisplayName("Should successfully create valid operational event and enforce lifecycle transitions")
    void shouldCreateAndTransitionEvent() {
        OperationalEvent event = OperationalEvent.createIngested(
                EventType.SUPPLIER_DELIVERY_DELAYED,
                EventSource.EXTERNAL_INTEGRATION,
                Instant.now().minusSeconds(120),
                "SUP-KYOTO",
                "NODE",
                WorkspaceId.of("WS-TEST-1"),
                Map.of("delayHours", "48", "carrier", "DHL_EXPRESS"),
                EventProvenance.imported("ERP_SYSTEM", "BATCH-901", "operator@example.com"),
                "IDEMP-KEY-999"
        );

        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getStatus()).isEqualTo(EventStatus.INGESTED);
        assertThat(event.getRejectionReason()).isEqualTo(EventRejectionReason.NONE);
        assertThat(event.getPayload()).containsEntry("delayHours", "48");

        event.markValidated();
        assertThat(event.getStatus()).isEqualTo(EventStatus.VALIDATED);

        event.markProcessed();
        assertThat(event.getStatus()).isEqualTo(EventStatus.PROCESSED);
    }

    @Test
    @DisplayName("Should reject transitioning previously rejected event to validated")
    void shouldPreventValidatingRejectedEvent() {
        OperationalEvent event = OperationalEvent.createIngested(
                EventType.QUALITY_HOLD_RECORDED,
                EventSource.MANUAL_OPERATOR,
                Instant.now(),
                "LOT-99",
                "MATERIAL",
                WorkspaceId.of("WS-TEST-1"),
                Map.of(),
                EventProvenance.internal("qa_lead"),
                "KEY-123"
        );

        event.markRejected(EventRejectionReason.MALFORMED_PAYLOAD);
        assertThat(event.getStatus()).isEqualTo(EventStatus.REJECTED);
        assertThat(event.getRejectionReason()).isEqualTo(EventRejectionReason.MALFORMED_PAYLOAD);

        assertThatThrownBy(event::markValidated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("previously rejected");
    }

    @Test
    @DisplayName("Should handle duplicate event marking")
    void shouldHandleDuplicateMarking() {
        OperationalEvent event = OperationalEvent.createIngested(
                EventType.INVENTORY_LEVEL_CHANGED,
                EventSource.CSV_IMPORT,
                Instant.now(),
                "HUB-FRANKFURT",
                "NODE",
                WorkspaceId.of("WS-TEST-1"),
                Map.of(),
                EventProvenance.imported("CSV", "B1", "user"),
                "DUP-KEY-1"
        );

        event.markDuplicate();
        assertThat(event.getStatus()).isEqualTo(EventStatus.DUPLICATE);
        assertThat(event.getRejectionReason()).isEqualTo(EventRejectionReason.DUPLICATE_IDEMPOTENCY_KEY);
    }
}

