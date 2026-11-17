package com.supplygraph.application.service;

import com.supplygraph.application.port.out.OperationalEventRepositoryPort;
import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.event.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Operational Event Ingestion Service Tests")
class OperationalEventIngestionServiceTest {

    private OperationalEventRepositoryPort eventRepo;
    private OperationalMonitoringService monitoringService;
    private OperationalEventIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        eventRepo = mock(OperationalEventRepositoryPort.class);
        monitoringService = mock(OperationalMonitoringService.class);
        ingestionService = new OperationalEventIngestionService(eventRepo, monitoringService);
    }

    @Test
    @DisplayName("Should successfully ingest, validate and process valid event")
    void shouldIngestValidEvent() {
        when(eventRepo.findByIdempotencyKey("KEY-123")).thenReturn(Optional.empty());

        OperationalEvent event = OperationalEvent.createIngested(
                EventType.SUPPLIER_DELIVERY_DELAYED,
                EventSource.EXTERNAL_INTEGRATION,
                Instant.now(),
                "SUP-100",
                "NODE",
                WorkspaceId.of("WS-TEST-1"),
                Map.of("delayHours", "36"),
                EventProvenance.imported("SAP_ERP", "BATCH-01", "operator"),
                "KEY-123"
        );

        OperationalEvent ingested = ingestionService.ingestEvent(event);

        assertThat(ingested.getStatus()).isEqualTo(EventStatus.PROCESSED);
        assertThat(ingested.getRejectionReason()).isEqualTo(EventRejectionReason.NONE);
        verify(eventRepo, atLeast(2)).save(event);
        verify(monitoringService).evaluateEvent(event);
    }

    @Test
    @DisplayName("Should reject and record event with duplicate idempotency key")
    void shouldRejectDuplicateEvent() {
        OperationalEvent existing = OperationalEvent.createIngested(
                EventType.INVENTORY_LEVEL_CHANGED,
                EventSource.INTERNAL_WORKFLOW,
                Instant.now(),
                "HUB-1",
                "NODE",
                WorkspaceId.of("WS-TEST-1"),
                Map.of(),
                EventProvenance.internal("system"),
                "KEY-DUP"
        );
        when(eventRepo.findByIdempotencyKey("KEY-DUP")).thenReturn(Optional.of(existing));

        OperationalEvent duplicate = OperationalEvent.createIngested(
                EventType.INVENTORY_LEVEL_CHANGED,
                EventSource.INTERNAL_WORKFLOW,
                Instant.now(),
                "HUB-1",
                "NODE",
                WorkspaceId.of("WS-TEST-1"),
                Map.of(),
                EventProvenance.internal("system"),
                "KEY-DUP"
        );

        OperationalEvent result = ingestionService.ingestEvent(duplicate);

        assertThat(result.getStatus()).isEqualTo(EventStatus.DUPLICATE);
        assertThat(result.getRejectionReason()).isEqualTo(EventRejectionReason.DUPLICATE_IDEMPOTENCY_KEY);
        verify(monitoringService, never()).evaluateEvent(any());
    }

    @Test
    @DisplayName("Should parse and ingest valid CSV payload batch")
    void shouldIngestCsvBatch() {
        String csv = """
                eventType,affectedEntityId,affectedEntityType,idempotencyKey,payload
                SUPPLIER_DELIVERY_DELAYED,SUP-KYOTO,NODE,CSV-IDEMP-1,delayHours=24;carrier=FEDEX
                INVENTORY_LEVEL_CHANGED,HUB-FRANKFURT,NODE,CSV-IDEMP-2,onHandQuantity=1200
                """;

        when(eventRepo.findByIdempotencyKey(any())).thenReturn(Optional.empty());

        List<OperationalEvent> ingested = ingestionService.ingestCsv(csv, WorkspaceId.of("WS-TEST-1"), "csv_importer");

        assertThat(ingested).hasSize(2);
        assertThat(ingested.get(0).getEventType()).isEqualTo(EventType.SUPPLIER_DELIVERY_DELAYED);
        assertThat(ingested.get(0).getPayload()).containsEntry("delayHours", "24");
        assertThat(ingested.get(1).getEventType()).isEqualTo(EventType.INVENTORY_LEVEL_CHANGED);
        assertThat(ingested.get(1).getPayload()).containsEntry("onHandQuantity", "1200");
    }
}

